package me.p0x38.fuckinguselessmod.presets;

import java.util.List;

public final class RegexPreset implements TextPreset {
    private final String id;
    private final String displayName;
    private final List<PresetRule> rules;

    public RegexPreset(
            String id,
            String displayName,
            List<PresetRule> rules
    ) {
        this.id = id;
        this.displayName = displayName;
        this.rules = List.copyOf(rules);
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String displayName() {
        return displayName;
    }

    @Override
    public String apply(String input) {
        String result = input;

        for (PresetRule rule : rules) {
            result = rule.pattern()
                    .matcher(result)
                    .replaceAll(rule.replacement());
        }

        return result;
    }
}
