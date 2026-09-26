package net.tearpelato.craftcorelib.platform;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.tearpelato.craftcorelib.api.config.*;
import net.tearpelato.craftcorelib.platform.services.IConfigHelper;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NeoForgeConfigHelper implements IConfigHelper {

    private static final Map<String, Map<ConfigType, ModConfigSpec>> SPECS = new HashMap<>();

    @Override
    public void register(String modId, List<ConfigCategory> categories) {
        Map<ConfigType, List<ConfigCategory>> byType = ConfigBinder.groupByType(categories);

        ModContainer container = ModList.get().getModContainerById(modId)
                .orElseThrow(() -> new IllegalStateException("Mod " + modId + " not found"));

        for (Map.Entry<ConfigType, List<ConfigCategory>> entry : byType.entrySet()) {
            registerType(container, modId, entry.getKey(), entry.getValue());
        }

        if (!ModList.get().isLoaded("configured")) {
            container.registerExtensionPoint(IConfigScreenFactory.class,
                    (mc, parent) -> new ConfigurationScreen(container, parent));
        }
    }

    private void registerType(ModContainer container, String modId, ConfigType type, List<ConfigCategory> categories) {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        Map<String, ModConfigSpec.ConfigValue<?>> specValues = new HashMap<>();

        for (ConfigCategory category : categories) {
            builder.push(category.getName());

            if (category.getTitleKey() != null) {
                builder.translation(category.getTitleKey());
            }
            if (category.getCommentKey() != null) {
                builder.comment(category.getCommentKey());
            }

            for (ConfigValue<?> value : category.getValues()) {
                specValues.put(ConfigBinder.fullKey(category, value), registerSpecValue(builder, value));
            }

            builder.pop();
        }

        ModConfigSpec spec = builder.build();
        SPECS.computeIfAbsent(modId, key -> new EnumMap<>(ConfigType.class)).put(type, spec);

        container.registerConfig(toModConfigType(type), spec);

        ConfigBinder.bindAll(categories, new ConfigBackend() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> T get(ConfigCategory category, ConfigValue<T> value) {
                ModConfigSpec.ConfigValue<?> specValue = specValues.get(ConfigBinder.fullKey(category, value));
                return specValue != null ? (T) specValue.get() : value.getDefault();
            }

            @Override
            @SuppressWarnings("unchecked")
            public <T> void set(ConfigCategory category, ConfigValue<T> value, T newValue) {
                ModConfigSpec.ConfigValue<Object> specValue =
                        (ModConfigSpec.ConfigValue<Object>) specValues.get(ConfigBinder.fullKey(category, value));
                if (specValue != null) {
                    specValue.set(newValue);
                }
            }
        });
    }

    private static ModConfig.Type toModConfigType(ConfigType type) {
        return switch (type) {
            case CLIENT -> ModConfig.Type.CLIENT;
            case SERVER -> ModConfig.Type.SERVER;
            case COMMON -> ModConfig.Type.COMMON;
        };
    }

    private static <T> ModConfigSpec.ConfigValue<?> registerSpecValue(ModConfigSpec.Builder builder, ConfigValue<T> value) {
        if (value.getCommentKey() != null) {
            builder.comment(value.getCommentKey());
        }

        T def = value.getDefault();

        if (def instanceof Boolean bool) {
            return builder.define(value.getKey(), bool.booleanValue());
        }
        if (def instanceof Integer intVal) {
            int min = value.getMin() != null ? (Integer) value.getMin() : Integer.MIN_VALUE;
            int max = value.getMax() != null ? (Integer) value.getMax() : Integer.MAX_VALUE;
            return builder.defineInRange(value.getKey(), intVal.intValue(), min, max);
        }
        if (def instanceof Double doubleVal) {
            double min = value.getMin() != null ? (Double) value.getMin() : -Double.MAX_VALUE;
            double max = value.getMax() != null ? (Double) value.getMax() : Double.MAX_VALUE;
            return builder.defineInRange(value.getKey(), doubleVal.doubleValue(), min, max);
        }
        if (def instanceof Long longVal) {
            long min = value.getMin() != null ? (Long) value.getMin() : Long.MIN_VALUE;
            long max = value.getMax() != null ? (Long) value.getMax() : Long.MAX_VALUE;
            return builder.defineInRange(value.getKey(), longVal.longValue(), min, max);
        }
        if (def instanceof String stringVal) {
            return builder.define(value.getKey(), stringVal);
        }
        if (def instanceof Enum<?>) {
            return defineEnumValue(builder, value.getKey(), (Enum<?>) def);
        }

        return builder.define(value.getKey(), def);
    }

    private static <V extends Enum<V>> ModConfigSpec.ConfigValue<V> defineEnumValue(
            ModConfigSpec.Builder builder, String key, Enum<?> rawDefault) {
        V typedDefault = (V) rawDefault;
        return builder.defineEnum(key, typedDefault);
    }
}
