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

public class UnknownEntityRenderer extends HumanoidMobRenderer<
        ChatEntity,
        HumanoidRenderState,
        HumanoidModel<HumanoidRenderState>
        > {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(
                    FuckingUselessMod.MOD_ID,
                    "textures/entity/WW91IG1pZ2h0.png"
            );

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

    @Override
    public @NonNull HumanoidRenderState createRenderState() {
        return new HumanoidRenderState();
    }

    @Override
    public @NonNull Identifier getTextureLocation(@NonNull HumanoidRenderState state) {
        return TEXTURE;
    }
}
