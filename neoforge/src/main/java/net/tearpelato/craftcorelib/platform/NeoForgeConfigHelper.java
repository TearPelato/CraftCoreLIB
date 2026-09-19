package net.tearpelato.craftcorelib.platform;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.tearpelato.craftcorelib.api.config.ConfigCategory;
import net.tearpelato.craftcorelib.api.config.ConfigValue;
import net.tearpelato.craftcorelib.platform.services.IConfigHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NeoForgeConfigHelper implements IConfigHelper {

    private static final Map<String, ModConfigSpec> SPECS = new HashMap<>();

    @Override
    public void register(String modId, List<ConfigCategory> categories) {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        for (ConfigCategory category : categories) {
            String section = category.getName();
            builder.push(section);

            if (category.getTitleKey() != null) {
                builder.translation(category.getTitleKey());
            }
            if (category.getCommentKey() != null) {
                builder.comment(category.getCommentKey());
            }

            for (ConfigValue<?> value : category.getValues()) {
                defineValue(builder, value);
            }

            builder.pop();
        }

        ModConfigSpec spec = builder.build();
        SPECS.put(modId, spec);

        ModContainer container = ModList.get().getModContainerById(modId)
                .orElseThrow(() -> new IllegalStateException("Mod " + modId + " not found"));

        container.registerConfig(ModConfig.Type.COMMON, spec);
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (mc, parent) -> new ConfigurationScreen(container, parent));
    }

    @SuppressWarnings("unchecked")
    private static <T> void defineValue(ModConfigSpec.Builder builder, ConfigValue<T> value) {
        if (value.getCommentKey() != null) {
            builder.comment(value.getCommentKey());
        }

        T def = value.getDefault();
        ModConfigSpec.ConfigValue<T> neoValue;

        if (def instanceof Boolean) {
            neoValue = (ModConfigSpec.ConfigValue<T>) builder.define(value.getKey(), (Boolean) def);
        } else if (def instanceof Integer) {
            if (value.getMin() != null && value.getMax() != null) {
                neoValue = (ModConfigSpec.ConfigValue<T>) builder.defineInRange(
                        value.getKey(),
                        (Integer) def,
                        (Integer) value.getMin(),
                        (Integer) value.getMax()
                );
            } else {
                neoValue = (ModConfigSpec.ConfigValue<T>) builder.define(value.getKey(), (Integer) def);
            }
        } else if (def instanceof Double) {
            if (value.getMin() != null && value.getMax() != null) {
                neoValue = (ModConfigSpec.ConfigValue<T>) builder.defineInRange(
                        value.getKey(),
                        (Double) def,
                        (Double) value.getMin(),
                        (Double) value.getMax()
                );
            } else {
                neoValue = (ModConfigSpec.ConfigValue<T>) builder.define(value.getKey(), (Double) def);
            }
        } else if (def instanceof Long) {
            if (value.getMin() != null && value.getMax() != null) {
                neoValue = (ModConfigSpec.ConfigValue<T>) builder.defineInRange(
                        value.getKey(),
                        (Long) def,
                        (Long) value.getMin(),
                        (Long) value.getMax()
                );
            } else {
                neoValue = (ModConfigSpec.ConfigValue<T>) builder.define(value.getKey(), (Long) def);
            }
        } else if (def instanceof String) {
            neoValue = (ModConfigSpec.ConfigValue<T>) builder.define(value.getKey(), (String) def);
        } else if (def instanceof Enum) {
            neoValue = (ModConfigSpec.ConfigValue<T>) builder.defineEnum(value.getKey(), (Enum) def);
        } else {
            neoValue = builder.define(value.getKey(), def);
        }

        value.bind(neoValue::get, neoValue::set);
    }
}