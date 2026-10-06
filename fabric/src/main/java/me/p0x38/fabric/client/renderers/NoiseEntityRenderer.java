package me.p0x38.fabric.client.renderers;

import me.p0x38.fabric.client.renderers.noise.DynamicNoiseTexture;
import me.p0x38.fabric.client.renderers.noise.NoiseRenderMask;
import me.p0x38.fuckinguselessmod.entity.NoiseEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

/** Client renderer for the Noise Entity using a humanoid model and armor layer. */
public class NoiseEntityRenderer extends HumanoidMobRenderer<
        NoiseEntity,
        HumanoidRenderState,
        HumanoidModel<HumanoidRenderState>
        > {
    private final DynamicNoiseTexture noiseTexture;
    private final NoiseRenderMask noiseMask;

    /** Creates the renderer and attaches the humanoid armor layer. */
    public NoiseEntityRenderer(EntityRendererProvider.Context context) {
        super(
                context,
                new HumanoidModel<>(
                        context.bakeLayer(ModelLayers.PLAYER)
                ),
                0.5f
        );

        this.addLayer(new HumanoidArmorLayer<>(
                this,
                ArmorModelSet.bake(
                        ModelLayers.PLAYER_ARMOR,
                        context.getModelSet(),
                        HumanoidModel::new
                ),
                context.getEquipmentRenderer()
        ));

        this.noiseTexture = new DynamicNoiseTexture();

        this.noiseMask = new NoiseRenderMask();
        noiseMask.enable(NoiseRenderMask.Layer.BODY);
    }

    /** @return a new humanoid render state. */
    @Override
    public @NonNull HumanoidRenderState createRenderState() {
        return new HumanoidRenderState();
    }

    /** @return the texture used by the entity's base model. */
    @Override
    public @NonNull Identifier getTextureLocation(@NonNull HumanoidRenderState state) {
        return noiseTexture.getTextureLocation();
    }

    public NoiseRenderMask getNoiseMask() {
        return noiseMask;
    }

    public void updateNoiseTexture() {
        noiseTexture.update();
    }

    public void close() {
        noiseTexture.close();
    }
}
