package net.tearpelato.craftcorelib.api.config;

import net.tearpelato.craftcorelib.platform.Services;

import java.util.*;
import java.util.function.Predicate;

public final class ConfigBuilder {

    private final String modId;
    private final ConfigType type;
    private final Deque<String> path = new ArrayDeque<>();
    private final List<ConfigOptions> ops = new ArrayList<>();
    private final Set<String> usedPaths = new HashSet<>();

    private String[] pendingComment;
    private String pendingTranslation;
    private boolean built;

    public ConfigBuilder(String modId, ConfigType type) {
        this.modId = Objects.requireNonNull(modId);
        this.type = Objects.requireNonNull(type);
    }

    public ConfigBuilder comment(String... lines) {
        this.pendingComment = lines;
        return this;
    }

    public ConfigBuilder translation(String translationKey) {
        this.pendingTranslation = translationKey;
        return this;
    }

    public ConfigBuilder start(String category) {
        checkOpen();
        ops.add(new ConfigOptions.Push(category, takeComment(), takeTranslation()));
        path.addLast(category);
        return this;
    }

    public ConfigBuilder end() {
        checkOpen();
        if (path.isEmpty()) throw new IllegalStateException("end() without start()");
        path.removeLast();
        ops.add(new ConfigOptions.Pop());
        return this;
    }

    public ConfigValue<Boolean> defineBoolean(String key, boolean def) {
        return add(key, ConfigEntry.Kind.BOOLEAN, def, null, null, v -> v instanceof Boolean);
    }

    public ConfigValue<Integer> defineInt(String key, int def, int min, int max) {
        checkRange(key, def, min, max);
        return add(key, ConfigEntry.Kind.INT, def, min, max, v -> v instanceof Integer i && i >= min && i <= max);
    }

    public ConfigValue<Long> defineLong(String key, long def, long min, long max) {
        checkRange(key, def, min, max);
        return add(key, ConfigEntry.Kind.LONG, def, min, max, v -> v instanceof Long l && l >= min && l <= max);
    }

    public ConfigValue<Double> defineDouble(String key, double def, double min, double max) {
        checkRange(key, def, min, max);
        return add(key, ConfigEntry.Kind.DOUBLE, def, min, max, v -> v instanceof Double d && d >= min && d <= max);
    }

    public ConfigValue<String> defineString(String key, String def) {
        return add(key, ConfigEntry.Kind.STRING, def, null, null, v -> v instanceof String);
    }

    public <E extends Enum<E>> ConfigValue<E> defineEnum(String key, E def) {
        return add(key, ConfigEntry.Kind.ENUM, def, null, null, v -> v instanceof Enum<?>);
    }

    public <T> ConfigValue<T> define(String key, T def, Predicate<Object> validator) {
        return add(key, ConfigEntry.Kind.GENERIC, def, null, null, validator);
    }

    public ConfigSpec build() {
        checkOpen();
        if (!path.isEmpty()) {
            throw new IllegalStateException("Unclosed category '" + path.peekLast() + "': missing end()");
        }
        built = true;
        ConfigSpec spec = new ConfigSpec(modId, type, List.copyOf(ops));
        Services.CONFIG.register(spec);
        return spec;
    }

    private <T> ConfigValue<T> add(String key, ConfigEntry.Kind kind, T def,
                                   Object min, Object max, Predicate<Object> validator) {
        checkOpen();
        String full = path.isEmpty() ? key : String.join(".", path) + "." + key;
        if (!usedPaths.add(full)) throw new IllegalStateException("Duplicate config key: " + full);

        ConfigValue<T> value = new ConfigValue<>(full, def);
        ops.add(new ConfigOptions.Entry(new ConfigEntry<>(
                key, full, kind, def, min, max, validator, takeComment(), takeTranslation(), value)));
        return value;
    }

    private List<String> takeComment() {
        List<String> c = pendingComment == null ? List.of() : List.of(pendingComment);
        pendingComment = null;
        return c;
    }

    private String takeTranslation() {
        String t = pendingTranslation;
        pendingTranslation = null;
        return t;
    }

    private void checkOpen() {
        if (built) throw new IllegalStateException("ConfigBuilder already built");
    }

    private static <N extends Comparable<N>> void checkRange(String key, N def, N min, N max) {
        if (min.compareTo(max) > 0 || def.compareTo(min) < 0 || def.compareTo(max) > 0) {
            throw new IllegalArgumentException("Invalid range/default for '" + key + "'");
        }
    }
}