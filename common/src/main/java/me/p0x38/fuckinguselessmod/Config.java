package me.p0x38.fuckinguselessmod;

import me.p0x38.fuckinguselessmod.config.ConfigOption;

import java.util.ArrayList;
import java.util.List;

public final class Config {
    private static Data data = new Data();

    public enum Mode {
        INSERT("Insert"),
        REPLACE("Replace"),
        ENCODE("Encode");

        private final String displayName;

        Mode(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public enum TextCase {
        PRESERVE("Preserve"),
        LOWERCASE("Lowercase"),
        UPPERCASE("Uppercase");

        private final String displayName;

        TextCase(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public enum BlockMode {
        DYNAMIC("Dynamic"),
        FULL_WIDTH("Full Width"),
        RANDOM("Random");

        private final String displayName;

        BlockMode(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private Config() {
    }

    public static Data get() {
        return data;
    }

    public static void set(Data newData) {
        data = newData;
    }

    public static final class Data {
        @ConfigOption(name = "Enabled", category = "General")
        public boolean enabled = true;

        @ConfigOption(name = "Mode", category = "General")
        public Mode mode = Mode.REPLACE;

        @ConfigOption(name = "Text Case", category = "Text Case")
        public TextCase textCase = TextCase.PRESERVE;

        @ConfigOption(name = "Block Mode", category = "Blocks")
        public BlockMode blockMode = BlockMode.DYNAMIC;

        @ConfigOption(
                name = "Encoding Pipeline",
                category = "Encoding"
        )
        public List<String> encodingPipeline = new ArrayList<>(List.of(
                "binary",
                "bit_rotate",
                "bit_xor",
                "bit_reversal",
                "base64",
                "alpha_rotation",
                "url_encode"
        ));

        @ConfigOption(
                name = "Bit Rotation",
                category = "Encoding",
                hasMin = true,
                min = 0,
                hasMax = true,
                max = 7
        )
        public int bitRotation = 1;

        @ConfigOption(
                name = "XOR Key",
                category = "Encoding",
                hasMin = true,
                min = 0,
                hasMax = true,
                max = 255
        )
        public int xorKey = 38;

        @ConfigOption(name = "Enable Uwuifier", category = "Effects")
        public boolean uwuifierEnabled = true;

        @ConfigOption(name = "Replace R/L with W", category = "Uwuifier")
        public boolean uwuifierReplaceRl = true;

        @ConfigOption(
                name = "Replace N + Vowel with Ny + Vowel",
                category = "Uwuifier"
        )
        public boolean uwuifierReplaceNVowel = true;

        @ConfigOption(name = "Replace Ove with Uv", category = "Uwuifier")
        public boolean uwuifierReplaceOve = true;

        @ConfigOption(name = "Replace Th with D", category = "Uwuifier")
        public boolean uwuifierReplaceTh = false;

        @ConfigOption(name = "Replace You with Yuo", category = "Uwuifier")
        public boolean uwuifierReplaceYou = false;

        @ConfigOption(name = "Enable Stutter", category = "Uwuifier")
        public boolean uwuifierStutterEnabled = false;

        @ConfigOption(
                name = "Stutter Chance",
                category = "Uwuifier",
                hasMin = true,
                min = 0.0,
                hasMax = true,
                max = 1.0
        )
        public float uwuifierStutterChance = 0.20f;

        @ConfigOption(name = "Enable Action Texts", category = "Uwuifier")
        public boolean uwuifierActionsEnabled = false;

        @ConfigOption(
                name = "Action Text Chance",
                category = "Uwuifier",
                hasMin = true,
                min = 0.0,
                hasMax = true,
                max = 1.0
        )
        public float uwuifierActionChance = 0.15f;

        @ConfigOption(name = "Action Texts", category = "Uwuifier")
        public List<String> uwuifierActionTexts = new ArrayList<>(List.of(
                "*boops*",
                "*wiggles*",
                "*happy noises*",
                "*pounces*",
                "*tail wags*"
        ));

        @ConfigOption(name = "Enable Emoticons", category = "Uwuifier")
        public boolean uwuifierEmoticonsEnabled = false;

        @ConfigOption(
                name = "Emoticon Chance",
                category = "Uwuifier",
                hasMin = true,
                min = 0.0,
                hasMax = true,
                max = 1.0
        )
        public float uwuifierEmoticonChance = 0.25f;

        @ConfigOption(name = "Emoticons", category = "Uwuifier")
        public List<String> uwuifierEmoticons = new ArrayList<>(List.of(
                ":3",
                "OwO",
                "UwU",
                "^w^",
                ">w<",
                "x3"
        ));

        @ConfigOption(name = "Enable Zalgo", category = "Zalgo")
        public boolean zalgoEnabled = true;

        @ConfigOption(
                name = "Zalgo Chance",
                category = "Zalgo",
                hasMin = true,
                min = 0.0,
                hasMax = true,
                max = 1.0
        )
        public float zalgoChance = 0.40f;

        @ConfigOption(name = "Enable Blocks", category = "Blocks")
        public boolean blocksEnabled = true;

        @ConfigOption(
                name = "Block Chance",
                category = "Blocks",
                hasMin = true,
                min = 0.0,
                hasMax = true,
                max = 1.0
        )
        public float blockChance = 0.20f;

        @ConfigOption(
                name = "Maximum Length",
                category = "Limits",
                hasMin = true,
                min = 1,
                hasMax = true,
                max = 256
        )
        public int maxLength = 256;

        @ConfigOption(name = "Presets", category = "Effects")
        public List<String> presets = new ArrayList<>();

        public void clamp() {
            zalgoChance = Math.clamp(zalgoChance, 0.0f, 1.0f);
            blockChance = Math.clamp(blockChance, 0.0f, 1.0f);
            uwuifierStutterChance =
                    Math.clamp(uwuifierStutterChance, 0.0f, 1.0f);
            uwuifierActionChance =
                    Math.clamp(uwuifierActionChance, 0.0f, 1.0f);
            uwuifierEmoticonChance =
                    Math.clamp(uwuifierEmoticonChance, 0.0f, 1.0f);
            maxLength = Math.clamp(maxLength, 1, 256);
            bitRotation = Math.clamp(bitRotation, 0, 7);
            xorKey = Math.clamp(xorKey, 0, 255);
        }
    }
}
