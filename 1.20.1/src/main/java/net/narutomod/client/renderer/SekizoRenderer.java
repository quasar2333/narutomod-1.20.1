package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.LegacyLongCubeModel;
import net.narutomod.client.render.NarutoRenderTypes;
import net.narutomod.entity.SekizoEntity;

public final class SekizoRenderer extends EntityRenderer<SekizoEntity> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/longcube_white.png");

    private final LegacyLongCubeModel model = new LegacyLongCubeModel();

    public SekizoRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.1F;
    }

    @Override
    public void render(SekizoEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        float length = getRenderLength(entity, partialTick);
        if (length <= 0.0F) {
            return;
        }
        float modelScale = 1.0F + length * 0.15F;
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot())));
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F - Mth.rotLerp(partialTick, entity.xRotO, entity.getXRot())));

        VertexConsumer consumer = bufferSource.getBuffer(NarutoRenderTypes.scrollingTranslucent(TEXTURE, -0.01F, -0.02F));
        this.model.renderSekizo(poseStack, consumer, length, modelScale);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, LightTexture.FULL_BRIGHT);
    }

    @Override
    public ResourceLocation getTextureLocation(SekizoEntity entity) {
        return TEXTURE;
    }

    private static float getRenderLength(SekizoEntity entity, float partialTick) {
        float age = entity.tickCount + partialTick;
        return Mth.clamp(entity.getBeamLength() * age / 30.0F, 1.0F, entity.getBeamLength());
    }
}
