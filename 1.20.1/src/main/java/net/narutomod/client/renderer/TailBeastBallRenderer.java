package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.TailBeastBallModel;
import net.narutomod.client.render.NarutoRenderTypes;
import net.narutomod.entity.TailBeastBallEntity;
import org.joml.Quaternionf;

public final class TailBeastBallRenderer extends EntityRenderer<TailBeastBallEntity> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/longcube_white.png");
    private static final float AXIS = 0.70710677F;

    private final TailBeastBallModel model;

    public TailBeastBallRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
        this.model = new TailBeastBallModel(context.bakeLayer(TailBeastBallModel.LAYER_LOCATION));
    }

    @Override
    public void render(TailBeastBallEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        float scale = Math.max(entity.getCurrentScale(), 0.01F);
        VertexConsumer consumer = bufferSource.getBuffer(NarutoRenderTypes.translucentEmissiveNoCull(TEXTURE));
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.125D * scale, 0.0D);
        poseStack.scale(scale, scale, scale);
        poseStack.mulPose(Axis.XP.rotationDegrees((entity.tickCount + partialTick) * 30.0F));

        RandomSource random = RandomSource.create(Mth.getSeed(entity.blockPosition()) ^ entity.getId() ^ entity.tickCount);
        for (int i = 0; i < 6; i++) {
            poseStack.mulPose(Axis.YP.rotationDegrees(random.nextFloat() * 30.0F));
            poseStack.mulPose(new Quaternionf().rotationAxis(random.nextFloat() * 30.0F * Mth.DEG_TO_RAD, AXIS, AXIS, 0.0F));
            this.model.renderCore(poseStack, consumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                    0.0F, 0.0F, 0.0F, 1.0F);
            this.model.renderShell(poseStack, consumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                    1.0F, 1.0F, 1.0F, 0.15F);
        }

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, LightTexture.FULL_BRIGHT);
    }

    @Override
    public ResourceLocation getTextureLocation(TailBeastBallEntity entity) {
        return TEXTURE;
    }
}
