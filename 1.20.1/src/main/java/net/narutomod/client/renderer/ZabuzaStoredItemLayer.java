package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.item.ItemStack;
import net.narutomod.client.model.ZabuzaMomochiModel;
import net.narutomod.entity.NinjaMobEntity;

public final class ZabuzaStoredItemLayer extends RenderLayer<NinjaMobEntity, ZabuzaMomochiModel> {
    public ZabuzaStoredItemLayer(RenderLayerParent<NinjaMobEntity, ZabuzaMomochiModel> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, NinjaMobEntity entity,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity.isInvisible()) {
            return;
        }
        ItemStack stack = entity.getLegacyZabuzaStoredMainHandForRender();
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
