package me.p0x38.fabric.client.renderers.noise;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.util.Random;

public final class DynamicNoiseTexture implements AutoCloseable {
    private static final int WIDTH = 64;
    private static final int HEIGHT = 64;
    private static final Identifier TEXTURE_LOCATION =
            Identifier.fromNamespaceAndPath(
                    "fuckinguselessmod",
                    "dnoise"
            );

    private final NativeImage image;
    private final DynamicTexture texture;

    private final Random random = new Random();

    public DynamicNoiseTexture() {

        this.image = new NativeImage(
                NativeImage.Format.RGBA,
                WIDTH,
                HEIGHT,
                false
        );

        this.texture = new DynamicTexture(
                () -> "fuckinguselessmod_dnoise",
                image
        );

        Minecraft.getInstance()
                .getTextureManager()
                .register(TEXTURE_LOCATION, texture);

        generateNoise();
        texture.upload();
    }

    public Identifier getTextureLocation() {
        return TEXTURE_LOCATION;
    }

    public void generateNoise() {
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                int value = random.nextInt(256);

                int argb =
                        0xFF000000
                        | (value << 16)
                        | (value << 8)
                        | value;

                image.setPixelABGR(x, y, argb);
            }
        }
    }

    public void update() {
        generateNoise();
        texture.upload();
    }

    @Override
    public void close() {
        texture.close();
        image.close();
    }
}
