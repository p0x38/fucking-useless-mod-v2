package me.p0x38.fabric.client.renderers;

import me.p0x38.fabric.client.renderers.noise.DynamicNoiseTexture;
import me.p0x38.fabric.client.renderers.noise.NoiseArmorLayer;
import me.p0x38.fabric.client.renderers.noise.NoiseEntityRenderState;
import me.p0x38.fabric.client.renderers.noise.NoiseRenderMask;
import me.p0x38.fuckinguselessmod.entity.NoiseEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Client renderer for the Noise Entity using a humanoid model and armor layer. */
public class NoiseEntityRenderer extends HumanoidMobRenderer<
        NoiseEntity,
        NoiseEntityRenderState,
        HumanoidModel<NoiseEntityRenderState>
        > {
    private final Map<UUID, DynamicNoiseTexture> noiseTextures = new HashMap<>();
    private final NoiseRenderMask noiseMask;

    public NoiseEntityRenderer(EntityRendererProvider.Context context) {
        super(
                context,
                new HumanoidModel<>(
                        context.bakeLayer(ModelLayers.PLAYER)
                ),
                0.5F
        );

        ArmorModelSet armorModels = ArmorModelSet.bake(
                ModelLayers.PLAYER_ARMOR,
                context.getModelSet(),
                HumanoidModel::new
        );

        this.addLayer(new NoiseArmorLayer(
                this,
                armorModels,
                context.getEquipmentRenderer()
        ));

        this.noiseMask = new NoiseRenderMask();
        this.noiseMask.enable(NoiseRenderMask.Layer.BODY);
    }

    @Override
    public @NonNull NoiseEntityRenderState createRenderState() {
        return new NoiseEntityRenderState();
    }

    @Override
    public void extractRenderState(
            @NonNull NoiseEntity entity,
            @NonNull NoiseEntityRenderState state,
            float partialTicks
    ) {
        super.extractRenderState(entity, state, partialTicks);

        DynamicNoiseTexture texture =
                this.noiseTextures.computeIfAbsent(
                        entity.getUUID(),
                        ignored -> new DynamicNoiseTexture(entity.getUUID())
                );

        state.noiseTextureLocation = texture.getTextureLocation();
        state.alwaysUpdate = entity.isAlwaysUpdate();
        state.noiseArmor =
                this.noiseMask.isEnabled(NoiseRenderMask.Layer.ARMOR);

        if (state.alwaysUpdate) {
            texture.update();
        }
    }

    @Override
    public @NonNull Identifier getTextureLocation(
            @NonNull NoiseEntityRenderState state
    ) {
        if (this.noiseMask.isEnabled(NoiseRenderMask.Layer.BODY)) {
            return state.noiseTextureLocation;
        }

        return DynamicNoiseTexture.getFallbackTextureLocation();
    }

    public NoiseRenderMask getNoiseMask() {
        return this.noiseMask;
    }

    public void updateNoiseTexture(NoiseEntity entity) {
        if (entity.isAlwaysUpdate()) {
            this.noiseTextures
                    .computeIfAbsent(
                            entity.getUUID(),
                            ignored -> new DynamicNoiseTexture(entity.getUUID())
                    )
                    .update();
        }
    }

    public void close() {
        this.noiseTextures.values().forEach(DynamicNoiseTexture::close);
        this.noiseTextures.clear();
    }
}
