package me.p0x38.fabric.client.renderers;

import me.p0x38.fuckinguselessmod.FuckingUselessMod;
import me.p0x38.fuckinguselessmod.entity.ChatEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

/** Client renderer for the Unknown Entity using a humanoid model and armor layer. */
public class UnknownEntityRenderer extends HumanoidMobRenderer<
        ChatEntity,
        HumanoidRenderState,
        HumanoidModel<HumanoidRenderState>
        > {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(
                    FuckingUselessMod.MOD_ID,
                    "textures/entity/blackpoint.png"
            );

    /** Creates the renderer and attaches the humanoid armor layer. */
    public UnknownEntityRenderer(EntityRendererProvider.Context context) {
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
    }

    /** @return a new humanoid render state. */
    @Override
    public @NonNull HumanoidRenderState createRenderState() {
        return new HumanoidRenderState();
    }

    /** @return the texture used by the entity's base model. */
    @Override
    public @NonNull Identifier getTextureLocation(@NonNull HumanoidRenderState state) {
        return TEXTURE;
    }
}
