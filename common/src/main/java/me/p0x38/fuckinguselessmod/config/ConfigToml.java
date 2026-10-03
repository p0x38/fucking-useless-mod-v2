package me.p0x38.fuckinguselessmod.config;

import me.p0x38.fuckinguselessmod.Config;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ConfigToml {
    private ConfigToml() {
    }

    public static String serialize(Config.Data data) {
        StringBuilder output = new StringBuilder();
        output.append("# Fucking Useless Mod configuration\n");
        output.append("# Values are grouped by the same categories shown in the config screen.\n");
        output.append("# Lines beginning with # are comments and can be freely edited or removed.\n\n");

        String currentCategory = null;

        for (Field field : fields()) {
            ConfigOption option = field.getAnnotation(ConfigOption.class);
            if (option == null || Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            String category = option.category();
            if (!category.equals(currentCategory)) {
                currentCategory = category;
                output.append('[')
                        .append(toKey(category))
                        .append("]\n\n");
            }

            appendComments(output, field.getName());

            try {
                output.append(field.getName())
                        .append(" = ")
                        .append(formatValue(field.get(data)))
                        .append("\n\n");
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException(
                        "Failed to serialize config field: " + field.getName(),
                        exception
                );
            }
        }

        return output.toString();
    }

    public static void deserializeInto(
            String content,
            Config.Data data
    ) {
        Map<String, String> values = parse(content);

        for (Field field : fields()) {
            ConfigOption option = field.getAnnotation(ConfigOption.class);
            if (option == null || Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            String key = field.getName();
            String sectionKey = toKey(option.category()) + "." + key;
            String raw = values.get(sectionKey);

            if (raw == null) {
                raw = values.get(key);
            }

            if (raw == null) {
                continue;
            }

            try {
                Object parsed = parseValue(raw, field.getType(), field.getGenericType());
                if (parsed != null) {
                    field.set(data, parsed);
                }
            } catch (RuntimeException | IllegalAccessException exception) {
                System.err.println(
                        "[Fucking Useless Mod] Invalid TOML value for "
                                + sectionKey + ": " + raw
                );
            }
        }
    }

    private static List<Field> fields() {
        List<Field> fields = new ArrayList<>();

        for (Field field : Config.Data.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())
                    && field.getAnnotation(ConfigOption.class) != null) {
                field.setAccessible(true);
                fields.add(field);
            }
        }

        return fields;
    }

    private static void appendComments(
            StringBuilder output,
            String fieldName
    ) {
        switch (fieldName) {
            case "encodingPipeline" -> {
                output.append("# Encoding steps are executed from left to right.\n");
                output.append("# Example: binary, base64, url_encode\n");
            }
            case "uwuifierActionTexts" -> {
                output.append("# Action texts are inserted at word boundaries.\n");
                output.append("# Examples: *boops*, *wiggles*, *happy noises*\n");
            }
            case "uwuifierEmoticons" -> {
                output.append("# Emoticons are inserted at word boundaries.\n");
                output.append("# Examples: :3, OwO, UwU, ^w^, >w<, x3\n");
            }
            case "uwuifierExclamations" -> {
                output.append("# Replacements for trailing ! and ? characters.\n");
                output.append("# Examples: !?, ?!!, ?!?!\n");
            }
            case "leetSpeakRules", "gamerSlangRules", "textSpeakRules", "lolcatRules" -> {
                output.append("# Rule syntax: [condition::]regex=>replacement\n");
                output.append("# No condition means the rule always applies.\n");
                output.append("# Regex conditions use find() unless *_matches is used.\n");
                output.append("# Examples:\n");
                output.append("#   regex:\\bhello\\b=>hewwo\n");
                output.append("#   random:0.25::\\bthe\\b=>teh\n");
                output.append("#   length:>20::\\bvery\\b=>rly\n");
                output.append("#   is_question::\\byou\\b=>u\n");
            }
            case "presets" -> output.append(
                    "# Preset IDs are applied in list order.\n"
            );
            case "sentenceEndEffects" -> {
                output.append("# Sentence-end effects are applied from left to right.\n");
                output.append("# Valid values: TILDE, ELLIPSIS, EXCLAMATION\n");
            }
            case "dialogueSoundPool" -> {
                output.append("# Entries can be legacy numeric indices (0-68) or SoundEvent IDs.\n");
                output.append("# Examples: 0, dialogtxt/t_69, minecraft:block.note_block.hat\n");
                output.append("# Duplicate entries increase that sound's selection weight.\n");
                output.append("# SoundEvent IDs must have a corresponding sound definition in a resource pack/mod.\n");
            }
            case "censorBoxDefaultSelectors" -> {
                output.append("# Entity selectors applied automatically once when entering a world.\n");
                output.append("# Examples: @e[type=minecraft:zombie], @e[type=!minecraft:player,distance=..16]\n");
                output.append("# Leave empty to disable automatic censoring.\n");
            }
            case "censorBoxColor" -> output.append("# ARGB color in #AARRGGBB form. #RRGGBB is also accepted with full opacity.\n");
            case "censorBoxEffects" -> {
                output.append("# Effects: STEPPY, JITTER, PULSE, RAINBOW, FLASH, DOUBLE.\n");
                output.append("# STEPPY and JITTER reproduce the default broken tracking behavior.\n");
            }
            default -> {
            }
        }
    }

    private static String formatValue(Object value) {
        if (value instanceof String string) {
            return quote(string);
        }

        if (value instanceof Enum<?> enumValue) {
            return quote(enumValue.name());
        }

        if (value instanceof List<?> list) {
            return list.stream()
                    .map(item -> quote(
                            item instanceof Enum<?> enumValue
                                    ? enumValue.name()
                                    : String.valueOf(item)
                    ))
                    .collect(java.util.stream.Collectors.joining(
                            ", ",
                            "[",
                            "]"
                    ));
        }

        return String.valueOf(value);
    }

    private static String quote(String value) {
        StringBuilder result = new StringBuilder(value.length() + 2);
        result.append('"');

        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            switch (character) {
                case '\\' -> result.append("\\\\");
                case '"' -> result.append("\\\"");
                case '\n' -> result.append("\\n");
                case '\r' -> result.append("\\r");
                case '\t' -> result.append("\\t");
                case '\b' -> result.append("\\b");
                case '\f' -> result.append("\\f");
                default -> result.append(character);
            }
        }

        return result.append('"').toString();
    }

    private static Object parseValue(
            String raw,
            Class<?> type,
            Type genericType
    ) {
        String value = raw.trim();

        if (type == boolean.class || type == Boolean.class) {
            return Boolean.parseBoolean(value);
        }

        if (type == int.class || type == Integer.class) {
            return Integer.parseInt(value);
        }

        if (type == float.class || type == Float.class) {
            return Float.parseFloat(value);
        }

        if (type == double.class || type == Double.class) {
            return Double.parseDouble(value);
        }

        if (type == long.class || type == Long.class) {
            return Long.parseLong(value);
        }

        if (type == String.class) {
            return parseString(value);
        }

        if (type.isEnum()) {
            String name = parseString(value);
            for (Object constant : type.getEnumConstants()) {
                if (((Enum<?>) constant).name().equalsIgnoreCase(name)) {
                    return constant;
                }
            }
            throw new IllegalArgumentException("Unknown enum value: " + name);
        }

        if (List.class.isAssignableFrom(type)
                && genericType instanceof ParameterizedType parameterizedType) {
            Type elementType = parameterizedType.getActualTypeArguments()[0];

            if (elementType == String.class) {
                return parseStringArray(value);
            }

            if (elementType instanceof Class<?> elementClass
                    && elementClass.isEnum()) {
                return parseEnumArray(value, elementClass);
            }
        }

        return null;
    }

    private static String parseString(String value) {
        if (value.length() >= 2
                && value.startsWith("\"")
                && value.endsWith("\"")) {
            String body = value.substring(1, value.length() - 1);
            StringBuilder result = new StringBuilder(body.length());

            for (int i = 0; i < body.length(); i++) {
                char character = body.charAt(i);
                if (character != '\\' || i + 1 >= body.length()) {
                    result.append(character);
                    continue;
                }

                char escaped = body.charAt(++i);
                result.append(switch (escaped) {
                    case '\\' -> '\\';
                    case '"' -> '"';
                    case 'n' -> '\n';
                    case 'r' -> '\r';
                    case 't' -> '\t';
                    case 'b' -> '\b';
                    case 'f' -> '\f';
                    default -> escaped;
                });
            }

            return result.toString();
        }

        if (value.length() >= 2
                && value.startsWith("'")
                && value.endsWith("'")) {
            return value.substring(1, value.length() - 1);
        }

        return value;
    }

    private static List<?> parseEnumArray(
            String value,
            Class<?> enumType
    ) {
        List<String> names = parseStringArray(value);
        List<Object> result = new ArrayList<>();

        for (String name : names) {
            Object matched = null;

            for (Object constant : enumType.getEnumConstants()) {
                if (((Enum<?>) constant).name().equalsIgnoreCase(name)) {
                    matched = constant;
                    break;
                }
            }

            if (matched == null) {
                throw new IllegalArgumentException(
                        "Unknown enum value: " + name
                );
            }

            result.add(matched);
        }

        return result;
    }

    private static List<String> parseStringArray(String value) {
        if (!value.startsWith("[") || !value.endsWith("]")) {
            throw new IllegalArgumentException("Expected TOML array");
        }

        String body = value.substring(1, value.length() - 1).trim();
        List<String> result = new ArrayList<>();

        if (body.isEmpty()) {
            return result;
        }

        for (String item : splitArray(body)) {
            result.add(parseString(item.trim()));
        }

        return result;
    }

    private static List<String> splitArray(String value) {
        List<String> items = new ArrayList<>();
        boolean quoted = false;
        char quote = 0;
        int start = 0;

        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);

            if (quoted) {
                if (character == quote && (quote == '\'' || value.charAt(i - 1) != '\\')) {
                    quoted = false;
                }
                continue;
            }

            if (character == '"' || character == '\'') {
                quoted = true;
                quote = character;
            } else if (character == ',') {
                items.add(value.substring(start, i));
                start = i + 1;
            }
        }

        items.add(value.substring(start));
        return items;
    }

    private static Map<String, String> parse(String content) {
        Map<String, String> values = new LinkedHashMap<>();
        String section = "";
        StringBuilder pending = new StringBuilder();

        for (String line : content.split("\\R")) {
            String stripped = stripComment(line).trim();
            if (stripped.isEmpty()) {
                continue;
            }

            if (!pending.isEmpty()) {
                pending.append(' ').append(stripped);
            } else {
                pending.append(stripped);
            }

            if (!isCompleteValue(pending.toString())) {
                continue;
            }

            String statement = pending.toString();
            pending.setLength(0);

            if (statement.startsWith("[") && statement.endsWith("]")) {
                section = statement.substring(
                        1,
                        statement.length() - 1
                ).trim();
                continue;
            }

            int separator = findAssignment(statement);
            if (separator <= 0) {
                continue;
            }

            String key = statement.substring(0, separator).trim();
            String value = statement.substring(separator + 1).trim();

            values.put(
                    section.isEmpty() ? key : section + "." + key,
                    value
            );
        }

        return values;
    }

    private static String stripComment(String line) {
        boolean quoted = false;
        char quote = 0;

        for (int i = 0; i < line.length(); i++) {
            char character = line.charAt(i);

            if (quoted) {
                if (character == quote && (quote == '\'' || line.charAt(i - 1) != '\\')) {
                    quoted = false;
                }
                continue;
            }

            if (character == '"' || character == '\'') {
                quoted = true;
                quote = character;
            } else if (character == '#') {
                return line.substring(0, i);
            }
        }

        return line;
    }

    private static int findAssignment(String line) {
        boolean quoted = false;
        char quote = 0;

        for (int i = 0; i < line.length(); i++) {
            char character = line.charAt(i);

            if (quoted) {
                if (character == quote && (quote == '\'' || line.charAt(i - 1) != '\\')) {
                    quoted = false;
                }
            } else if (character == '"' || character == '\'') {
                quoted = true;
                quote = character;
            } else if (character == '=') {
                return i;
            }
        }

        return -1;
    }

    private static boolean isCompleteValue(String statement) {
        int squareBrackets = 0;
        boolean quoted = false;
        char quote = 0;

        for (int i = 0; i < statement.length(); i++) {
            char character = statement.charAt(i);

            if (quoted) {
                if (character == quote && (quote == '\'' || statement.charAt(i - 1) != '\\')) {
                    quoted = false;
                }
                continue;
            }

            if (character == '"' || character == '\'') {
                quoted = true;
                quote = character;
            } else if (character == '[') {
                squareBrackets++;
            } else if (character == ']') {
                squareBrackets--;
            }
        }

        return !quoted && squareBrackets <= 0;
    }

    private static String toKey(String category) {
        return category
                .replace('&', '_')
                .replaceAll("[^A-Za-z0-9]+", "_")
                .replaceAll("^_+|_+$", "")
                .toLowerCase(Locale.ROOT);
    }
}
