package me.p0x38.fuckinguselessmod.config;

import me.p0x38.fuckinguselessmod.Config;
import me.p0x38.fuckinguselessmod.ConfigManager;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ConfigScreen {
    private ConfigScreen() {
    }

    public static Screen create(Screen parent) {
        Config.Data config = Config.get();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(
                        Component.translatable(
                                "text.fuckinguselessmod.config.title"
                        )
                );

        ConfigEntryBuilder entries = builder.entryBuilder();
        Map<String, ConfigCategory> categories = new LinkedHashMap<>();

        for (Field field : Config.Data.class.getDeclaredFields()) {
            ConfigOption option = field.getAnnotation(ConfigOption.class);

            if (option == null) {
                continue;
            }

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

    private static void addField(
            ConfigCategory category,
            ConfigEntryBuilder entries,
            Config.Data config,
            Field field,
            ConfigOption option
    ) {
        Component label = Component.translatable(
                "text.fuckinguselessmod.config.option." + field.getName()
        );
        Component tooltip = Component.translatable(
                "text.fuckinguselessmod.config.description." + field.getName()
        );
        Class<?> type = field.getType();

        if (type == boolean.class) {
            addBoolean(category, entries, config, field, label, tooltip);
            return;
        }

        if (type == int.class) {
            addInt(category, entries, config, field, label, tooltip, option);
            return;
        }

        if (type == float.class) {
            addFloat(category, entries, config, field, label, tooltip, option);
            return;
        }

        if (type == String.class) {
            addString(category, entries, config, field, label, tooltip);
            return;
        }

        if (type.isEnum()) {
            addEnum(category, entries, config, field, label, tooltip);
            return;
        }

        if (List.class.isAssignableFrom(type)) {
            if (getEnumListType(field) != null) {
                addEnumList(category, entries, config, field, label, tooltip);
            } else {
                addStringList(category, entries, config, field, label, tooltip);
            }
            return;
        }

        throw new IllegalArgumentException(
                "Unsupported config field type: "
                        + type.getName()
                        + " for "
                        + field.getName()
        );
    }

    private static void addBoolean(
            ConfigCategory category,
            ConfigEntryBuilder entries,
            Config.Data config,
            Field field,
            Component label,
            Component tooltip
    ) {
        try {
            boolean value = field.getBoolean(config);

            category.addEntry(
                    entries.startBooleanToggle(label, value)
                            .setTooltip(tooltip)
                            .setDefaultValue(value)
                            .setSaveConsumer(newValue -> {
                                try {
                                    field.setBoolean(config, newValue);
                                } catch (IllegalAccessException exception) {
                                    throw new RuntimeException(exception);
                                }
                            })
                            .build()
            );
        } catch (IllegalAccessException exception) {
            throw new RuntimeException(exception);
        }
    }

    private static void addInt(
            ConfigCategory category,
            ConfigEntryBuilder entries,
            Config.Data config,
            Field field,
            Component label,
            Component tooltip,
            ConfigOption option
    ) {
        try {
            int value = field.getInt(config);

            var builder = entries.startIntField(label, value)
                    .setTooltip(tooltip)
                    .setDefaultValue(value);

            if (option.hasMin()) {
                builder.setMin((int) option.min());
            }

            if (option.hasMax()) {
                builder.setMax((int) option.max());
            }

            category.addEntry(
                    builder
                            .setSaveConsumer(newValue -> {
                                try {
                                    field.setInt(config, newValue);
                                } catch (IllegalAccessException exception) {
                                    throw new RuntimeException(exception);
                                }
                            })
                            .build()
            );
        } catch (IllegalAccessException exception) {
            throw new RuntimeException(exception);
        }
    }

    private static void addFloat(
            ConfigCategory category,
            ConfigEntryBuilder entries,
            Config.Data config,
            Field field,
            Component label,
            Component tooltip,
            ConfigOption option
    ) {
        try {
            float value = field.getFloat(config);

            var builder = entries.startFloatField(label, value)
                    .setTooltip(tooltip)
                    .setDefaultValue(value);

            if (option.hasMin()) {
                builder.setMin((float) option.min());
            }

            if (option.hasMax()) {
                builder.setMax((float) option.max());
            }

            category.addEntry(
                    builder
                            .setSaveConsumer(newValue -> {
                                try {
                                    field.setFloat(config, newValue);
                                } catch (IllegalAccessException exception) {
                                    throw new RuntimeException(exception);
                                }
                            })
                            .build()
            );
        } catch (IllegalAccessException exception) {
            throw new RuntimeException(exception);
        }
    }

    private static void addString(
            ConfigCategory category,
            ConfigEntryBuilder entries,
            Config.Data config,
            Field field,
            Component label,
            Component tooltip
    ) {
        try {
            String value = (String) field.get(config);

            category.addEntry(
                    entries.startStrField(label, value)
                            .setTooltip(tooltip)
                            .setDefaultValue(value)
                            .setSaveConsumer(newValue -> {
                                try {
                                    field.set(config, newValue);
                                } catch (IllegalAccessException exception) {
                                    throw new RuntimeException(exception);
                                }
                            })
                            .build()
            );
        } catch (IllegalAccessException exception) {
            throw new RuntimeException(exception);
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Enum<E>> void addEnum(
            ConfigCategory category,
            ConfigEntryBuilder entries,
            Config.Data config,
            Field field,
            Component label,
            Component tooltip
    ) {
        try {
            E value = (E) field.get(config);
            Class<E> enumClass = (Class<E>) field.getType();

            category.addEntry(
                    entries.startEnumSelector(
                                    label,
                                    enumClass,
                                    value
                            )
                            .setTooltip(tooltip)
                            .setDefaultValue(value)
                            .setEnumNameProvider(enumValue ->
                                    Component.translatable(
                                            enumTranslationKey(
                                                    field.getName(),
                                                    enumValue
                                            )
                                    )
                            )
                            .setSaveConsumer(newValue -> {
                                try {
                                    field.set(config, newValue);
                                } catch (IllegalAccessException exception) {
                                    throw new RuntimeException(exception);
                                }
                            })
                            .build()
            );
        } catch (IllegalAccessException exception) {
            throw new RuntimeException(exception);
        }
    }

    @SuppressWarnings({"rawtypes"})
    private static void addEnumList(
            ConfigCategory category,
            ConfigEntryBuilder entries,
            Config.Data config,
            Field field,
            Component label,
            Component tooltip
    ) {
        try {
            Class<? extends Enum> enumClass = getEnumListType(field);
            List<?> value = (List<?>) field.get(config);
            List<String> editableValue = new ArrayList<>();

            if (value != null) {
                for (Object item : value) {
                    if (item instanceof Enum<?> enumValue) {
                        editableValue.add(enumValue.name());
                    }
                }
            }

            category.addEntry(
                    entries.startStrList(label, editableValue)
                            .setTooltip(tooltip)
                            .setDefaultValue(new ArrayList<>(editableValue))
                            .setSaveConsumer(newValue -> {
                                try {
                                    List<Enum> parsed = new ArrayList<>();
                                    for (String name : newValue) {
                                        for (Enum enumValue : enumClass.getEnumConstants()) {
                                            if (enumValue.name().equalsIgnoreCase(name)) {
                                                parsed.add(enumValue);
                                                break;
                                            }
                                        }
                                    }
                                    field.set(config, parsed);
                                } catch (IllegalAccessException exception) {
                                    throw new RuntimeException(exception);
                                }
                            })
                            .build()
            );
        } catch (IllegalAccessException exception) {
            throw new RuntimeException(exception);
        }
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Enum> getEnumListType(Field field) {
        Type genericType = field.getGenericType();
        if (!(genericType instanceof ParameterizedType parameterizedType)) {
            return null;
        }
        Type elementType = parameterizedType.getActualTypeArguments()[0];
        if (!(elementType instanceof Class<?> elementClass) || !elementClass.isEnum()) {
            return null;
        }
        return (Class<? extends Enum>) elementClass;
    }

    private static void addStringList(
            ConfigCategory category,
            ConfigEntryBuilder entries,
            Config.Data config,
            Field field,
            Component label,
            Component tooltip
    ) {
        try {
            @SuppressWarnings("unchecked")
            List<String> value = (List<String>) field.get(config);

            List<String> editableValue = new ArrayList<>(
                    value == null ? List.of() : value
            );

            category.addEntry(
                    entries.startStrList(label, editableValue)
                            .setTooltip(tooltip)
                            .setDefaultValue(
                                    new ArrayList<>(editableValue)
                            )
                            .setSaveConsumer(newValue -> {
                                try {
                                    field.set(
                                            config,
                                            new ArrayList<>(newValue)
                                    );
                                } catch (IllegalAccessException exception) {
                                    throw new RuntimeException(exception);
                                }
                            })
                            .build()
            );
        } catch (IllegalAccessException exception) {
            throw new RuntimeException(exception);
        }
    }

    private static String translationKeyPart(String value) {
        return value.toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    private static String enumTranslationKey(
            String fieldName,
            Enum<?> value
    ) {
        return "text.fuckinguselessmod.config.value."
                + toSnakeCase(fieldName)
                + "."
                + toSnakeCase(value.name());
    }

    private static String toSnakeCase(String value) {
        return value
                .replaceAll("([a-z])([A-Z])", "$1_$2")
                .replace('-', '_')
                .toLowerCase(Locale.ROOT);
    }
}
