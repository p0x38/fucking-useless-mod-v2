package me.p0x38.fabric.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.p0x38.fuckinguselessmod.util.DebugLogger;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class CensorBoxRenderer {
    public static final RenderStateDataKey<UUID> ENTITY_UUID =
            RenderStateDataKey.create(
                    () -> "fuckinguselessmod:censor_entity_uuid"
            );

    private static final Set<UUID> CENSORED_ENTITIES =
            new HashSet<>();

    private CensorBoxRenderer() {
    }

    public static void initialize() {
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register(
                (entityType, entityRenderer, registrationHelper, context) -> {
                    registerLayer(
                            entityRenderer,
                            registrationHelper
                    );

                    DebugLogger.debug(
                            "[CensorBox] registered for entity type={}",
                            entityType
                    );
                }
        );
    }

    @SuppressWarnings({
            "rawtypes",
            "unchecked"
    })
    private static void registerLayer(
            LivingEntityRenderer<?, ?, ?> entityRenderer,
            LivingEntityFeatureRendererRegistrationCallback.RegistrationHelper
                    registrationHelper
    ) {
        RenderLayerParent parent =
                (RenderLayerParent) entityRenderer;

        registrationHelper.register(
                new CensorBoxLayer(parent)
        );
    }

    public static void toggle(UUID uuid) {
        if (!CENSORED_ENTITIES.add(uuid)) {
            CENSORED_ENTITIES.remove(uuid);
        }

        DebugLogger.debug(
                "[CensorBox] uuid={} enabled={}",
                uuid,
                CENSORED_ENTITIES.contains(uuid)
        );
    }

    public static void add(UUID uuid) {
        CENSORED_ENTITIES.add(uuid);

        DebugLogger.debug(
                "[CensorBox] added uuid={}",
                uuid
        );
    }

    public static boolean remove(UUID uuid) {
        boolean removed = CENSORED_ENTITIES.remove(uuid);

        if (removed) {
            DebugLogger.debug(
                    "[CensorBox] removed uuid={}",
                    uuid
            );
        }

        return removed;
    }

    public static Set<UUID> getCensoredEntities() {
        return Set.copyOf(CENSORED_ENTITIES);
    }

    public static void clear() {
        CENSORED_ENTITIES.clear();

        DebugLogger.debug(
                "[CensorBox] cleared"
        );
    }

    public static boolean isCensored(UUID uuid) {
        return CENSORED_ENTITIES.contains(uuid);
    }

    @SuppressWarnings({
            "rawtypes",
            "unchecked"
    })
    private static final class CensorBoxLayer
            extends RenderLayer<
                    EntityRenderState,
                    EntityModel<EntityRenderState>
                    > {

        private CensorBoxLayer(
                RenderLayerParent parent
        ) {
            super(parent);
        }

        @Override
        public void submit(
                PoseStack poseStack,
                SubmitNodeCollector collector,
                int packedLight,
                EntityRenderState state,
                float limbAngle,
                float limbDistance
        ) {
            UUID uuid =
                    ((FabricRenderState) state).getData(
                            ENTITY_UUID
                    );

            if (uuid == null) {
                return;
            }

            if (!CENSORED_ENTITIES.contains(uuid)) {
                return;
            }

            DebugLogger.debug(
                    "[CensorBox] rendering uuid={} type={}",
                    uuid,
                    state.entityType
            );

            collector
                    .order(1)
                    .submitCustomGeometry(
                            poseStack,
                            RenderTypes.debugQuads(),
                            CensorBoxLayer::renderPlane
                    );
        }

        private static void renderPlane(
                PoseStack.Pose pose,
                VertexConsumer consumer
        ) {
            float halfWidth = 0.75F;
            float minY = 0.0F;
            float maxY = 2.5F;
            float z = -0.45F;

            consumer.addVertex(
                    pose,
                    -halfWidth,
                    minY,
                    z
            ).setColor(
                    0,
                    0,
                    0,
                    255
            );

            consumer.addVertex(
                    pose,
                    -halfWidth,
                    maxY,
                    z
            ).setColor(
                    0,
                    0,
                    0,
                    255
            );

            consumer.addVertex(
                    pose,
                    halfWidth,
                    maxY,
                    z
            ).setColor(
                    0,
                    0,
                    0,
                    255
            );

            consumer.addVertex(
                    pose,
                    halfWidth,
                    minY,
                    z
            ).setColor(
                    0,
                    0,
                    0,
                    255
            );
        }
    }
}
