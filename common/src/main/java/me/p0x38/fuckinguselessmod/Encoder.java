package me.p0x38.fuckinguselessmod;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class Encoder {
    private Encoder() {
    }

    public static String encode(
            String input,
            Config.Data config
    ) {
        String value = input;

        for (String rawStep : config.encodingPipeline) {
            if (rawStep == null || rawStep.isBlank()) {
                continue;
            }

            EncodingStep step = EncodingStep.parse(rawStep);

            value = switch (step) {
                case BINARY -> binary(value);
                case BIT_ROTATE -> bitRotate(value, config.bitRotation);
                case BIT_XOR -> bitXor(value, config.xorKey);
                case BIT_REVERSAL -> bitReversal(value);
                case BASE64 -> base64(value);
                case ALPHA_ROTATION -> alphaRotation(value);
                case URL_ENCODE -> urlEncode(value);
            };
        }

        return value;
    }

    private static String binary(String input) {
        byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
        StringBuilder result = new StringBuilder(bytes.length * 8);

        for (byte value : bytes) {
            result.append(
                    String.format(
                            "%8s",
                            Integer.toBinaryString(value & 0xff)
                    ).replace(' ', '0')
            );
        }

        return result.toString();
    }

    private static String bitRotate(String input, int amount) {
        if (input.length() % 8 != 0) {
            throw new IllegalArgumentException(
                    "bit_rotate requires input length divisible by 8"
            );
        }

        amount = Math.floorMod(amount, 8);

        StringBuilder result = new StringBuilder(input.length());

        for (int i = 0; i < input.length(); i += 8) {
            int value = Integer.parseInt(
                    input.substring(i, i + 8),
                    2
            );

            int rotated = (
                    (value << amount)
                            | (value >>> (8 - amount))
            ) & 0xff;

            result.append(
                    String.format(
                            "%8s",
                            Integer.toBinaryString(rotated)
                    ).replace(' ', '0')
            );
        }

        return result.toString();
    }

    private static String bitXor(String input, int key) {
        if (input.length() % 8 != 0) {
            throw new IllegalArgumentException(
                    "bit_xor requires input length divisible by 8"
            );
        }

        key &= 0xff;
        StringBuilder result = new StringBuilder(input.length());

        for (int i = 0; i < input.length(); i += 8) {
            int value = Integer.parseInt(
                    input.substring(i, i + 8),
                    2
            );

            int transformed = value ^ key;

            result.append(
                    String.format(
                            "%8s",
                            Integer.toBinaryString(transformed)
                    ).replace(' ', '0')
            );
        }

        return result.toString();
    }

    private static String bitReversal(String input) {
        if (input.length() % 8 != 0) {
            throw new IllegalArgumentException(
                    "bit_reversal requires input length divisible by 8"
            );
        }

        StringBuilder result = new StringBuilder(input.length());

        for (int i = 0; i < input.length(); i += 8) {
            result.append(
                    new StringBuilder(
                            input.substring(i, i + 8)
                    ).reverse()
            );
        }

        return result.toString();
    }

    private static String base64(String input) {
        return Base64.getEncoder().encodeToString(
                input.getBytes(StandardCharsets.UTF_8)
        );
    }

    private static String alphaRotation(String input) {
        StringBuilder result = new StringBuilder(input.length());

        for (char character : input.toCharArray()) {
            if (character >= 'A' && character <= 'Z') {
                result.append(
                        (char) ('A'
                                + (character - 'A' + 13) % 26)
                );
            } else if (character >= 'a' && character <= 'z') {
                result.append(
                        (char) ('a'
                                + (character - 'a' + 13) % 26)
                );
            } else {
                result.append(character);
            }
        }

        return result.toString();
    }

    private static String urlEncode(String input) {
        return URLEncoder.encode(
                input,
                StandardCharsets.UTF_8
        );
    }
}
