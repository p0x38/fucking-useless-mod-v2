package me.p0x38.fuckinguselessmod.effects;

public final class Uwuifier {
    private Uwuifier() {
    }

    public static String apply(String input) {
        return input
                .replaceAll("[rl]", "w")
                .replaceAll("[RL]", "W")
                .replaceAll("n([aeiou])", "ny$1")
                .replaceAll("N([aeiou])", "Ny$1")
                .replaceAll("ove", "uv")
                .replaceAll("Ove", "Uv")
                .replaceAll("OVE", "UV");
    }
}
