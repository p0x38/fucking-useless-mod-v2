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
        public List<String> presets = new ArrayList<>(List.of(
                "uwuify"
        ));

        public void clamp() {
            zalgoChance = Math.clamp(zalgoChance, 0.0f, 1.0f);
            blockChance = Math.clamp(blockChance, 0.0f, 1.0f);
            maxLength = Math.clamp(maxLength, 1, 256);
            bitRotation = Math.clamp(bitRotation, 0, 7);
            xorKey = Math.clamp(xorKey, 0, 255);
        }
    }
}
