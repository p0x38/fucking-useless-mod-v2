package me.p0x38.fabric.client.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import me.p0x38.fabric.client.renderers.CensorBoxRenderer;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {
    @Inject(
            method = "renderHand",
            at = @At("TAIL")
    )
    private void fuckingUselessMod$captureHand(
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int lightCoords,
            Identifier skinTexture,
            ModelPart arm,
            boolean hasSleeve,
            CallbackInfo callbackInfo
    ) {
        Minecraft client = Minecraft.getInstance();

        if (!client.options.getCameraType().isFirstPerson()) {
            return;
        }

        if (client.player == null) {
            return;
        }

        /*
         * PlayerModel's left arm has a positive X origin and
         * the right arm has a negative X origin.
         *
         * renderHand() calls resetPose() before reaching here,
         * so this reliably identifies the actual arm being drawn.
         */
        HumanoidArm humanoidArm =
                arm.x > 0.0f
                ? HumanoidArm.LEFT
                        : HumanoidArm.RIGHT;

        /*
         * At this point poseStack already contains:
         *
         *   camera inverse
         *   view/hurt bobbing
         *   hand/item transform
         *   vanilla attack/swing transform
         *
         * getExtentsForGui() then adds the actual ModelPart
         * transform and its real cube vertices.
         */
        float fov =
                ((GameRendererMixin) client.gameRenderer)
                        .fuckingUselessMod$getFov(
                                client.gameRenderer.getMainCamera(),
                                CensorBoxRenderer.getFirstPersonPartialTick(),
                                false
                        );

        Matrix4f projectionMatrix =
                client.gameRenderer.getProjectionMatrix(fov);

        /*
         * Vanilla's hand pass renders with:
         *
         *   projection * modelView * poseStack * vertex
         *
         * and poseStack starts with modelView^-1. Reuse the live
         * model-view matrix from the hand pass so the captured
         * bounds exactly follow the same transform as the arm.
         */
        Matrix4f modelViewMatrix =
                new Matrix4f(RenderSystem.getModelViewMatrix());

        Matrix4f handMvp =
                new Matrix4f(projectionMatrix)
                        .mul(modelViewMatrix);

        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;

        final float[] bounds = {
                minX,
                minY,
                maxX,
                maxY
        };

        arm.getExtentsForGui(
                poseStack,
                position -> {
                    Vector3f projected =
                            new Matrix4f(handMvp)
                                    .transformProject(
                                            new Vector3f(position)
                                    );

                    bounds[0] =
                            Math.min(
                                    bounds[0],
                                    projected.x()
                            );

                    bounds[1] =
                            Math.min(
                                    bounds[1],
                                    projected.y()
                            );

                    bounds[2] =
                            Math.max(
                                    bounds[2],
                                    projected.x()
                            );

                    bounds[3] =
                            Math.max(
                                    bounds[3],
                                    projected.y()
                            );
                }
        );

        if (!Float.isFinite(bounds[0])
                || !Float.isFinite(bounds[1])
                || !Float.isFinite(bounds[2])
                || !Float.isFinite(bounds[3])) {
            return;
        }

        CensorBoxRenderer.markFirstPersonHand(
                humanoidArm,
                bounds[0],
                bounds[1],
                bounds[2],
                bounds[3]
        );
    }
}
