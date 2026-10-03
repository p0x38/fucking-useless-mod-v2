package me.p0x38.fabric.client;

import me.p0x38.fuckinguselessmod.FuckingUselessMod;
import me.p0x38.fuckinguselessmod.entity.UselessEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public class UselessEntityRenderer extends HumanoidMobRenderer<
        UselessEntity,
        HumanoidRenderState,
        HumanoidModel<HumanoidRenderState>
        > {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(
                    FuckingUselessMod.MOD_ID,
                    "textures/entity/useless.png"
            );

    public UselessEntityRenderer(EntityRendererProvider.Context context) {
        super(
                context,
                new HumanoidModel<>(
                        context.bakeLayer(ModelLayers.PLAYER)
                ),
                0.5f
        );
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
