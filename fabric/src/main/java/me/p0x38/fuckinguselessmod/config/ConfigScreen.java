package me.p0x38.fuckinguselessmod.config;

import me.p0x38.fuckinguselessmod.Config;
import me.p0x38.fuckinguselessmod.ConfigManager;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ConfigScreen {
    private ConfigScreen() {}

    public static Screen create(Screen parent) {
        Config.Data config = Config.get();
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("text.fuckinguselessmod.config.title"));
        ConfigEntryBuilder entries = builder.entryBuilder();
        Map<String, ConfigCategory> categories = new LinkedHashMap<>();

        for (Field field : Config.Data.class.getDeclaredFields()) {
            ConfigOption option = field.getAnnotation(ConfigOption.class);
            if (option == null) continue;
            field.setAccessible(true);
            ConfigCategory category = categories.computeIfAbsent(
                    option.category(),
                    name -> builder.getOrCreateCategory(
                            Component.translatable(
                                    "text.fuckinguselessmod.config.category."
                                            + translationKeyPart(name)
                            )
                    )
            );
            addField(category, entries, config, field, option);
        }

        builder.setSavingRunnable(() -> {
            config.clamp();
            ConfigManager.save();
        });
        return builder.build();
    }

    private static void addField(ConfigCategory category, ConfigEntryBuilder entries,
                                 Config.Data config, Field field, ConfigOption option) {
        Component label = Component.translatable(
                "text.fuckinguselessmod.config.option." + field.getName()
        );
        Class<?> type = field.getType();

        if (type == boolean.class) {
            try {
                boolean value = field.getBoolean(config);
                category.addEntry(entries.startBooleanToggle(label, value)
                        .setDefaultValue(value)
                        .setSaveConsumer(v -> setBoolean(field, config, v))
                        .build());
            } catch (IllegalAccessException e) { throw new RuntimeException(e); }
            return;
        }

        if (type == int.class) {
            try {
                int value = field.getInt(config);
                var b = entries.startIntField(label, value).setDefaultValue(value);
                if (option.hasMin()) b.setMin((int) option.min());
                if (option.hasMax()) b.setMax((int) option.max());
                category.addEntry(b.setSaveConsumer(v -> setInt(field, config, v)).build());
            } catch (IllegalAccessException e) { throw new RuntimeException(e); }
            return;
        }

        if (type == float.class) {
            try {
                float value = field.getFloat(config);
                var b = entries.startFloatField(label, value).setDefaultValue(value);
                if (option.hasMin()) b.setMin((float) option.min());
                if (option.hasMax()) b.setMax((float) option.max());
                category.addEntry(b.setSaveConsumer(v -> setFloat(field, config, v)).build());
            } catch (IllegalAccessException e) { throw new RuntimeException(e); }
            return;
        }

        if (type.isEnum()) {
            addEnum(category, entries, config, field, label);
            return;
        }

        if (List.class.isAssignableFrom(type)) {
            try {
                @SuppressWarnings("unchecked")
                List<String> value = (List<String>) field.get(config);
                List<String> editable = new ArrayList<>(value == null ? List.of() : value);
                category.addEntry(entries.startStrList(label, editable)
                        .setDefaultValue(new ArrayList<>(editable))
                        .setSaveConsumer(v -> {
                            try { field.set(config, new ArrayList<>(v)); }
                            catch (IllegalAccessException e) { throw new RuntimeException(e); }
                        }).build());
            } catch (IllegalAccessException e) { throw new RuntimeException(e); }
            return;
        }

        throw new IllegalArgumentException("Unsupported config field type: " + type.getName());
    }

    private static void setBoolean(Field f, Config.Data c, boolean v) {
        try { f.setBoolean(c, v); } catch (IllegalAccessException e) { throw new RuntimeException(e); }
    }

    private static void setInt(Field f, Config.Data c, int v) {
        try { f.setInt(c, v); } catch (IllegalAccessException e) { throw new RuntimeException(e); }
    }

    private static void setFloat(Field f, Config.Data c, float v) {
        try { f.setFloat(c, v); } catch (IllegalAccessException e) { throw new RuntimeException(e); }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Enum<E>> void addEnum(ConfigCategory category, ConfigEntryBuilder entries,
                                                     Config.Data config, Field field, Component label) {
        try {
            E value = (E) field.get(config);
            Class<E> enumClass = (Class<E>) field.getType();
            category.addEntry(entries.startEnumSelector(label, enumClass, value)
                    .setDefaultValue(value)
                    .setSaveConsumer(v -> {
                        try { field.set(config, v); }
                        catch (IllegalAccessException e) { throw new RuntimeException(e); }
                    }).build());
        } catch (IllegalAccessException e) { throw new RuntimeException(e); }
    }

    private static String translationKeyPart(String value) {
        return value.toLowerCase(Locale.ROOT).replace(' ', '_');
    }
}
