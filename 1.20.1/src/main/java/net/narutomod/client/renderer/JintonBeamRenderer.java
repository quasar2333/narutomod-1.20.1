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
import net.narutomod.entity.JintonBeamEntity;

public final class JintonBeamRenderer extends EntityRenderer<JintonBeamEntity> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/longcube_white.png");

    private final LegacyLongCubeModel model = new LegacyLongCubeModel();

    public JintonBeamRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.1F;
    }

    @Override
    public void render(JintonBeamEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        float renderLength = getRenderLength(entity);
        if (renderLength <= 0.0F) {
            return;
        }
        float modelScale = getModelScale(entity, renderLength);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot())));
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F - Mth.rotLerp(partialTick, entity.xRotO, entity.getXRot())));

        VertexConsumer consumer = bufferSource.getBuffer(NarutoRenderTypes.scrollingTranslucent(TEXTURE, -0.01F, -0.02F));
        this.model.renderJintonBeam(poseStack, consumer, renderLength / modelScale, modelScale);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, LightTexture.FULL_BRIGHT);
    }

    @Override
    public ResourceLocation getTextureLocation(JintonBeamEntity entity) {
        return TEXTURE;
    }

    private static float getRenderLength(JintonBeamEntity entity) {
        int activeTicks = entity.tickCount - entity.getWaitTicks();
        if (activeTicks <= 0) {
            return 1.0F;
        }
        return Mth.clamp(entity.getBeamLength() * activeTicks / 10.0F, 1.0F, entity.getBeamLength());
    }

    private static float getModelScale(JintonBeamEntity entity, float renderLength) {
        if (entity.tickCount <= entity.getWaitTicks()) {
            return 1.0F;
        }
        return Math.max(entity.getBeamScale() * 2.0F * renderLength / entity.getBeamLength(), 0.001F);
    }
}
