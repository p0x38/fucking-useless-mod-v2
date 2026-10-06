package me.p0x38.fabric.client.renderers.noise;

import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/** Render state for a Noise Entity. */
public final class NoiseEntityRenderState extends HumanoidRenderState {
    public Identifier noiseTextureLocation;
    public boolean alwaysUpdate;
}
