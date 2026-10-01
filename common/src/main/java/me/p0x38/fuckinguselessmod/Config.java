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

    public enum SentenceEndEffect {
        TILDE,
        ELLIPSIS,
        EXCLAMATION
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

        @ConfigOption(
                name = "Word Transformation Chance",
                category = "Uwuifier",
                hasMin = true,
                min = 0.0,
                hasMax = true,
                max = 1.0
        )
        public float uwuifierWordChance = 1.0f;

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

        @ConfigOption(name = "Enable Exclamations", category = "Uwuifier")
        public boolean uwuifierExclamationsEnabled = false;

        @ConfigOption(
                name = "Exclamation Chance",
                category = "Uwuifier",
                hasMin = true,
                min = 0.0,
                hasMax = true,
                max = 1.0
        )
        public float uwuifierExclamationChance = 1.0f;

        @ConfigOption(name = "Exclamations", category = "Uwuifier")
        public List<String> uwuifierExclamations = new ArrayList<>(List.of(
                "!?",
                "?!!",
                "?!?!",
                "!!11",
                "?!?"
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

        @ConfigOption(name = "Enable Leet Speak", category = "Leet Speak")
        public boolean leetSpeakEnabled = false;

        @ConfigOption(
                name = "Leet Speak Chance",
                category = "Leet Speak",
                hasMin = true,
                min = 0.0,
                hasMax = true,
                max = 1.0
        )
        public float leetSpeakChance = 1.0f;

        @ConfigOption(name = "Leet Speak Rules", category = "Leet Speak")
        public List<String> leetSpeakRules = new ArrayList<>(List.of(
                "a=>4", "e=>3", "i=>1", "o=>0", "s=>5", "t=>7"
        ));

        @ConfigOption(name = "Enable Gamer Slang", category = "Gamer Slang")
        public boolean gamerSlangEnabled = false;

        @ConfigOption(
                name = "Gamer Slang Chance",
                category = "Gamer Slang",
                hasMin = true,
                min = 0.0,
                hasMax = true,
                max = 1.0
        )
        public float gamerSlangChance = 1.0f;

        @ConfigOption(name = "Gamer Slang Rules", category = "Gamer Slang")
        public List<String> gamerSlangRules = new ArrayList<>(List.of(
                "\\byou\\b=>u", "\\byour\\b=>ur", "\\bare\\b=>r", "\\bwhy\\b=>y", "\\bpeople\\b=>ppl",
                "\\bplease\\b=>pls", "\\bthanks\\b=>thx", "\\bthank\\b=>thx", "\\bbecause\\b=>cuz",
                "\\bbefore\\b=>b4", "\\breally\\b=>rly", "\\bprobably\\b=>prolly"
        ));

        @ConfigOption(name = "Enable Text Speak", category = "Text Speak")
        public boolean textSpeakEnabled = false;

        @ConfigOption(
                name = "Text Speak Chance",
                category = "Text Speak",
                hasMin = true,
                min = 0.0,
                hasMax = true,
                max = 1.0
        )
        public float textSpeakChance = 1.0f;

        @ConfigOption(name = "Text Speak Rules", category = "Text Speak")
        public List<String> textSpeakRules = new ArrayList<>(List.of(
                "\\bsee you\\b=>c u", "\\bsee\\b=>c", "\\byou\\b=>u", "\\bare\\b=>r", "\\bwhy\\b=>y",
                "\\bbe right back\\b=>brb", "\\bas soon as possible\\b=>asap",
                "\\blaughing out loud\\b=>lol", "\\bby the way\\b=>btw",
                "\\bin my opinion\\b=>imo", "\\bfor your information\\b=>fyi",
                "\\bI don't know\\b=>idk", "\\boh my god\\b=>omg", "\\bright now\\b=>rn",
                "\\bto be honest\\b=>tbh", "\\bbecause\\b=>bc", "\\bwithout\\b=>w/o"
        ));

        @ConfigOption(name = "Enable Lolcat", category = "Lolcat")
        public boolean lolcatEnabled = false;

        @ConfigOption(
                name = "Lolcat Chance",
                category = "Lolcat",
                hasMin = true,
                min = 0.0,
                hasMax = true,
                max = 1.0
        )
        public float lolcatChance = 1.0f;

        @ConfigOption(name = "Lolcat Rules", category = "Lolcat")
        public List<String> lolcatRules = new ArrayList<>(List.of(
                "\\bthe\\b=>teh", "\\bthis\\b=>dis", "\\bthat\\b=>dat", "\\bwhat\\b=>wat",
                "\\bwith\\b=>wit", "\\bis\\b=>iz", "\\bmy\\b=>mah", "\\bI am\\b=>I can haz",
                "\\bhas\\b=>haz", "\\bhave\\b=>hav"
        ));
        @ConfigOption(name = "Presets", category = "Effects")
        public List<String> presets = new ArrayList<>();

        @ConfigOption(name = "Enable Sentence End Effects", category = "Effects")
        public boolean sentenceEndEffectsEnabled = true;

        @ConfigOption(name = "Sentence End Effects", category = "Effects")
        public List<SentenceEndEffect> sentenceEndEffects =
                new ArrayList<>(List.of(
                        SentenceEndEffect.TILDE,
                        SentenceEndEffect.ELLIPSIS,
                        SentenceEndEffect.EXCLAMATION
                ));

        public void clamp() {
            zalgoChance = Math.clamp(zalgoChance, 0.0f, 1.0f);
            blockChance = Math.clamp(blockChance, 0.0f, 1.0f);
            uwuifierWordChance =
                    Math.clamp(uwuifierWordChance, 0.0f, 1.0f);
            uwuifierStutterChance =
                    Math.clamp(uwuifierStutterChance, 0.0f, 1.0f);
            uwuifierActionChance =
                    Math.clamp(uwuifierActionChance, 0.0f, 1.0f);
            uwuifierEmoticonChance =
                    Math.clamp(uwuifierEmoticonChance, 0.0f, 1.0f);
            uwuifierExclamationChance =
                    Math.clamp(uwuifierExclamationChance, 0.0f, 1.0f);
            leetSpeakChance =
                    Math.clamp(leetSpeakChance, 0.0f, 1.0f);
            gamerSlangChance =
                    Math.clamp(gamerSlangChance, 0.0f, 1.0f);
            textSpeakChance =
                    Math.clamp(textSpeakChance, 0.0f, 1.0f);
            lolcatChance =
                    Math.clamp(lolcatChance, 0.0f, 1.0f);
            maxLength = Math.clamp(maxLength, 1, 256);
            bitRotation = Math.clamp(bitRotation, 0, 7);
            xorKey = Math.clamp(xorKey, 0, 255);
        }
    }
}
