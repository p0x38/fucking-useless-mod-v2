package me.p0x38.fuckinguselessmod.effects;

public enum TextEffect {
    UWUIFY("Uwuify"),
    ZALGO("Zalgo"),
    RANDOM_CASE("Random Case"),
    STUTTER("Stutter"),
    EMOTICONS("Emoticons");

    private final String displayName;

    TextEffect(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
