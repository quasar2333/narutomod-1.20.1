package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.narutomod.entity.ChibakuSatelliteEntity;

public final class ChibakuSatelliteRenderer extends EntityRenderer<ChibakuSatelliteEntity> {
    private final BlockRenderDispatcher blockRenderer;

    public ChibakuSatelliteRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockRenderer = context.getBlockRenderDispatcher();
        this.shadowRadius = 2.0F;
    }

    @Override
    public void render(ChibakuSatelliteEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        for (ChibakuSatelliteEntity.BlockEntry entry : entity.getBlocks()) {
            poseStack.pushPose();
            poseStack.translate(entry.offset().getX(), entry.offset().getY(), entry.offset().getZ());
            this.blockRenderer.renderSingleBlock(entry.state(), poseStack, bufferSource, packedLight, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(ChibakuSatelliteEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
