package net.tearpelato.craftcorelib.platform;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.tearpelato.craftcorelib.api.config.*;
import net.tearpelato.craftcorelib.config.NeoForgeClientConfig;
import net.tearpelato.craftcorelib.platform.services.IConfigHelper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NeoForgeConfigHelper implements IConfigHelper {

    private static final Set<String> SCREEN_REGISTERED = new HashSet<>();

    @Override
    public void register(ConfigSpec spec) {
        ModContainer container = ModList.get().getModContainerById(spec.modId())
                .orElseThrow(() -> new IllegalStateException("Mod " + spec.modId() + " not found"));

        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        for (ConfigOptions op : spec.ops()) {
            switch (op) {
                case ConfigOptions.Push p -> {
                    meta(b, p.comment(), p.translation());
                    b.push(p.name());
                }
                case ConfigOptions.Pop ignored -> b.pop();
                case ConfigOptions.Entry e -> {
                    meta(b, e.entry().comment(), e.entry().translation());
                    bind(e.entry().value(), define(b, e.entry()));
                }
            }
        }

        container.registerConfig(toModConfigType(spec.type()), b.build());

        if (FMLEnvironment.dist == Dist.CLIENT && SCREEN_REGISTERED.add(spec.modId())) {
            NeoForgeClientConfig.registerScreen(container);
        }
    }

    private static void meta(ModConfigSpec.Builder b, List<String> comment, String translation) {
        if (!comment.isEmpty()) b.comment(comment.toArray(new String[0]));
        if (translation != null) b.translation(translation);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ModConfigSpec.ConfigValue<?> define(ModConfigSpec.Builder b, ConfigEntry<?> e) {
        String key = e.key();
        Object def = e.defaultValue();
        return switch (e.kind()) {
            case BOOLEAN -> b.define(key, (boolean) def);
            case INT     -> b.defineInRange(key, (int) def, (int) e.min(), (int) e.max());
            case LONG    -> b.defineInRange(key, (long) def, (long) e.min(), (long) e.max());
            case DOUBLE  -> b.defineInRange(key, (double) def, (double) e.min(), (double) e.max());
            case STRING  -> b.define(key, (String) def);
            case ENUM    -> b.defineEnum(key, (Enum) def);
            case GENERIC -> b.define(key, def, e.validator());
        };
    }

    @SuppressWarnings("unchecked")
    private static <T> void bind(ConfigValue<T> value, ModConfigSpec.ConfigValue<?> raw) {
        ModConfigSpec.ConfigValue<T> typed = (ModConfigSpec.ConfigValue<T>) raw;
        value.bind(typed::get, typed::set, typed::save);
    }

    private static ModConfig.Type toModConfigType(ConfigType type) {
        return switch (type) {
            case CLIENT -> ModConfig.Type.CLIENT;
            case SERVER -> ModConfig.Type.SERVER;
            case COMMON -> ModConfig.Type.COMMON;
        };
    }
}