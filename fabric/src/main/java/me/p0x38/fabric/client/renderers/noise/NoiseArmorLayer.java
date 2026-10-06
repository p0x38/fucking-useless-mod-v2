package me.p0x38.fabric.client.renderers.noise;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;

public final class NoiseArmorLayer extends RenderLayer {
    private final ArmorModelSet modelSet;
    private final EquipmentLayerRenderer equipmentRenderer;

    public NoiseArmorLayer(
            net.minecraft.client.renderer.entity.RenderLayerParent renderer,
            ArmorModelSet modelSet,
            EquipmentLayerRenderer equipmentRenderer
    ) {
        super(renderer);
        this.modelSet = modelSet;
        this.equipmentRenderer = equipmentRenderer;
    }

    @Override
    public void submit(
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int lightCoords,
            net.minecraft.client.renderer.entity.state.EntityRenderState renderState,
            float yRot,
            float xRot
    ) {
        if (!(renderState instanceof NoiseEntityRenderState state)) {
            return;
        }

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
        Equippable equippable = itemStack.get(DataComponents.EQUIPPABLE);

        if (equippable == null || !HumanoidArmorLayer.shouldRender(itemStack, slot)) {
            return;
        }

        HumanoidModel model =
                (HumanoidModel) this.modelSet.get(slot);

        if (state.noiseArmor) {
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

        equipmentRenderer.renderLayers(
                layerType,
                (ResourceKey) equippable.assetId().orElseThrow(),
                model,
                state,
                itemStack,
                poseStack,
                submitNodeCollector,
                lightCoords,
                state.outlineColor
        );
    }
}
