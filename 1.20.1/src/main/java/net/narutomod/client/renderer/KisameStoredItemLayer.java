package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.item.ItemStack;
import net.narutomod.client.model.KisameHoshigakiModel;
import net.narutomod.entity.NinjaMobEntity;

public final class KisameStoredItemLayer extends RenderLayer<NinjaMobEntity, KisameHoshigakiModel> {
    public KisameStoredItemLayer(RenderLayerParent<NinjaMobEntity, KisameHoshigakiModel> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, NinjaMobEntity entity,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity.isInvisible() || entity.isLegacyKisameFusedForRender()) {
            return;
        }
        ItemStack stack = entity.getLegacyKisameStoredMainHandForRender();
        if (!stack.isEmpty()) {
            BodyMountedItemRenderer.renderStack(
                    poseStack,
                    bufferSource,
                    packedLight,
                    entity,
                    stack,
                    getParentModel()::translateToBodyPart);
        }
    }
}
