package me.p0x38.fabric.client.renderers.noise;

import com.mojang.blaze3d.vertex.PoseStack;
import me.p0x38.fabric.client.renderers.NoiseEntityRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import org.jspecify.annotations.NonNull;

public final class NoiseArmorLayer extends RenderLayer<
        NoiseEntityRenderState,
        HumanoidModel<NoiseEntityRenderState>
        > {
    private final NoiseEntityRenderer renderer;
    private final ArmorModelSet<HumanoidModel<NoiseEntityRenderState>> modelSet;
    private final net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer equipmentRenderer;

    public NoiseArmorLayer(
            NoiseEntityRenderer renderer,
            ArmorModelSet<ModelLayerLocationHolder> unused
    ) {
        super(renderer);
        this.renderer = renderer;
        this.modelSet = null;
        this.equipmentRenderer = null;
    }

    public NoiseArmorLayer(
            NoiseEntityRenderer renderer,
            ArmorModelSet<net.minecraft.client.model.geom.ModelLayerLocation> modelSet,
            net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer equipmentRenderer
    ) {
        super(renderer);
        this.renderer = renderer;
        this.modelSet = net.minecraft.client.renderer.entity.ArmorModelSet.bake(
                modelSet,
                renderer.getContextModelSet(),
                HumanoidModel::new
        );
        this.equipmentRenderer = equipmentRenderer;
    }

    @Override
    public void submit(
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int lightCoords,
            NoiseEntityRenderState state,
            float yRot,
            float xRot
    ) {
        renderArmorPiece(
                poseStack,
                submitNodeCollector,
                state.chestEquipment,
                EquipmentSlot.CHEST,
                lightCoords,
                state
        );
        renderArmorPiece(
                poseStack,
                submitNodeCollector,
                state.legsEquipment,
                EquipmentSlot.LEGS,
                lightCoords,
                state
        );
        renderArmorPiece(
                poseStack,
                submitNodeCollector,
                state.feetEquipment,
                EquipmentSlot.FEET,
                lightCoords,
                state
        );
        renderArmorPiece(
                poseStack,
                submitNodeCollector,
                state.headEquipment,
                EquipmentSlot.HEAD,
                lightCoords,
                state
        );
    }

    private void renderArmorPiece(
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            ItemStack itemStack,
            EquipmentSlot slot,
            int lightCoords,
            NoiseEntityRenderState state
    ) {
        if (itemStack.isEmpty()) {
            return;
        }

        Equippable equippable = itemStack.get(DataComponents.EQUIPPABLE);

        if (equippable == null
                || !HumanoidArmorLayer.shouldRender(itemStack, slot)) {
            return;
        }

        HumanoidModel<NoiseEntityRenderState> model = this.modelSet.get(slot);

        if (this.renderer.getNoiseMask().isEnabled(NoiseRenderMask.Layer.ARMOR)) {
            submitNodeCollector.submitModel(
                    model,
                    state,
                    poseStack,
                    RenderTypes.armorCutoutNoCull(
                            state.noiseTextureLocation
                    ),
                    lightCoords,
                    OverlayTexture.NO_OVERLAY,
                    -1,
                    null,
                    state.outlineColor,
                    null
            );
            return;
        }

        EquipmentClientInfo.LayerType layerType =
                slot == EquipmentSlot.LEGS
                        ? EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS
                        : EquipmentClientInfo.LayerType.HUMANOID;

        this.equipmentRenderer.renderLayers(
                layerType,
                equippable.assetId().orElseThrow(),
                model,
                state,
                itemStack,
                poseStack,
                submitNodeCollector,
                lightCoords,
                state.outlineColor
        );
    }

    private record ModelLayerLocationHolder(
            net.minecraft.client.model.geom.ModelLayerLocation value
    ) {
    }
}
