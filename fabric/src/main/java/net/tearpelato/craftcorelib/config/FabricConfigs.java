package net.tearpelato.craftcorelib.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.file.FileNotFoundAction;
import com.electronwill.nightconfig.core.io.WritingMode;
import net.fabricmc.loader.api.FabricLoader;
import net.tearpelato.craftcorelib.api.config.ConfigEntry;
import net.tearpelato.craftcorelib.api.config.ConfigOptions;
import net.tearpelato.craftcorelib.api.config.ConfigSpec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class FabricConfigs {

    private FabricConfigs() {}

    public static void register(ConfigSpec spec) {
        Path file = FabricLoader.getInstance().getConfigDir()
                .resolve(spec.modId() + "-" + spec.type().name().toLowerCase(Locale.ROOT) + ".toml");

        List<ConfigEntry<?>> entries = new ArrayList<>();
        Map<String, List<String>> categoryComments = new LinkedHashMap<>();
        Deque<String> stack = new ArrayDeque<>();

        for (ConfigOptions op : spec.ops()) {
            switch (op) {
                case ConfigOptions.Push p -> {
                    stack.addLast(p.name());
                    if (!p.comment().isEmpty()) categoryComments.put(String.join(".", stack), p.comment());
                }
                case ConfigOptions.Pop ignored -> stack.removeLast();
                case ConfigOptions.Entry e -> entries.add(e.entry());
            }
        }

        Map<String, Object> cache = new ConcurrentHashMap<>();
        Object lock = new Object();
        Runnable saver = () -> {
            synchronized (lock) { flush(file, entries, categoryComments, cache); }
        };

        try (CommentedFileConfig cfg = open(file)) {
            cfg.load();
            for (ConfigEntry<?> e : entries) {
                cache.put(e.path(), sanitize(e, cfg.contains(e.path()) ? cfg.get(e.path()) : null));
            }
        }
        saver.run();

        for (ConfigEntry<?> e : entries) bind(e, cache, saver);
    }

    @SuppressWarnings("unchecked")
    private static <T> void bind(ConfigEntry<T> e, Map<String, Object> cache, Runnable saver) {
        e.value().bind(
                () -> (T) cache.get(e.path()),
                v -> {
                    if (v == null || !e.validator().test(v)) {
                        throw new IllegalArgumentException("Invalid value for " + e.path() + ": " + v);
                    }
                    cache.put(e.path(), v);
                },
                saver);
    }

    private static Object sanitize(ConfigEntry<?> e, Object raw) {
        if (raw == null) return e.defaultValue();
        Object v = switch (e.kind()) {
            case INT    -> raw instanceof Number n ? (Object) n.intValue() : raw;
            case LONG   -> raw instanceof Number n ? (Object) n.longValue() : raw;
            case DOUBLE -> raw instanceof Number n ? (Object) n.doubleValue() : raw;
            case ENUM   -> parseEnum((Enum<?>) e.defaultValue(), raw);
            default     -> raw;
        };
        return (v != null && e.validator().test(v)) ? v : e.defaultValue();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object parseEnum(Enum<?> def, Object raw) {
        if (!(raw instanceof String s)) return null;
        try { return Enum.valueOf((Class) def.getDeclaringClass(), s); }
        catch (IllegalArgumentException ex) { return null; }
    }

    private static void flush(Path file, List<ConfigEntry<?>> entries,
                              Map<String, List<String>> categoryComments, Map<String, Object> cache) {
        try (CommentedFileConfig cfg = open(file)) {
            cfg.load();
            for (ConfigEntry<?> e : entries) {
                Object v = cache.get(e.path());
                cfg.set(e.path(), v instanceof Enum<?> en ? en.name() : v);
                cfg.setComment(e.path(), describe(e));
            }
            categoryComments.forEach((p, lines) -> cfg.setComment(p, joinLines(lines)));
            cfg.save();
        }
    }

    private static String describe(ConfigEntry<?> e) {
        List<String> lines = new ArrayList<>(e.comment());
        if (e.min() != null) lines.add("Range: " + e.min() + " ~ " + e.max());
        lines.add("Default: " + (e.defaultValue() instanceof Enum<?> en ? en.name() : e.defaultValue()));
        if (e.kind() == ConfigEntry.Kind.ENUM) {
            lines.add("Allowed: " + Arrays.toString(((Enum<?>) e.defaultValue()).getDeclaringClass().getEnumConstants()));
        }
        return joinLines(lines);
    }

    private static String joinLines(List<String> lines) {
        return " " + String.join("\n ", lines);
    }

    private static CommentedFileConfig open(Path file) {
        try { Files.createDirectories(file.getParent()); } catch (IOException ignored) {}
        return CommentedFileConfig.builder(file)
                .sync()
                .onFileNotFound(FileNotFoundAction.CREATE_EMPTY)
                .writingMode(WritingMode.REPLACE)
                .build();
    }
}