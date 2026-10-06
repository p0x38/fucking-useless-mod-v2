package me.p0x38.fabric.client.renderers.noise;

import com.mojang.blaze3d.platform.NativeImage;
import me.p0x38.fuckinguselessmod.FuckingUselessMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.util.Random;
import java.util.UUID;

public final class DynamicNoiseTexture implements AutoCloseable {
    private static final int WIDTH = 64;
    private static final int HEIGHT = 64;

    private static final Identifier FALLBACK_TEXTURE_LOCATION =
            Identifier.fromNamespaceAndPath(
                    FuckingUselessMod.MOD_ID,
                    "textures/entity/white.png"
            );

    private final Identifier textureLocation;
    private final NativeImage image;
    private final DynamicTexture texture;
    private final Random random;

    public DynamicNoiseTexture(UUID entityId) {
        this.textureLocation =
                Identifier.fromNamespaceAndPath(
                        FuckingUselessMod.MOD_ID,
                        "dnoise/" + entityId
                );

        this.image = new NativeImage(
                NativeImage.Format.RGBA,
                WIDTH,
                HEIGHT,
                false
        );

        this.texture = new DynamicTexture(
                () -> "fuckinguselessmod_dnoise_" + entityId,
                image
        );

        this.random = new Random(entityId.getMostSignificantBits()
                ^ entityId.getLeastSignificantBits());

        Minecraft.getInstance()
                .getTextureManager()
                .register(this.textureLocation, this.texture);

        this.generateNoise();
        this.texture.upload();
    }

    public Identifier getTextureLocation() {
        return this.textureLocation;
    }

    public static Identifier getFallbackTextureLocation() {
        return FALLBACK_TEXTURE_LOCATION;
    }

    private void generateNoise() {
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                int value = this.random.nextInt(256);

                int argb =
                        0xFF000000
                                | (value << 16)
                                | (value << 8)
                                | value;

                this.image.setPixelABGR(x, y, argb);
            }
        }
    }

    public void update() {
        this.generateNoise();
        this.texture.upload();
    }

    @Override
    public void close() {
        this.texture.close();
        this.image.close();
    }
}
