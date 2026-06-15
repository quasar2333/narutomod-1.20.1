package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.narutomod.client.model.SuitonStreamModel;
import net.narutomod.client.render.NarutoRenderTypes;
import net.narutomod.entity.SuitonStreamEntity;

public final class SuitonStreamRenderer extends EntityRenderer<SuitonStreamEntity> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/water_flow.png");
    private static final float LEGACY_LENGTH_PAD = 0.1F;

    private final SuitonStreamModel model = new SuitonStreamModel();

    public SuitonStreamRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.1F;
    }

    @Override
    public void render(SuitonStreamEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        float lifeScale = getLifeScale(entity, partialTick);
        float length = (entity.getPowerForRender() + LEGACY_LENGTH_PAD) * lifeScale;
        if (length <= 0.0F) {
            return;
        }

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot())));
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F - Mth.rotLerp(partialTick, entity.xRotO, entity.getXRot())));

        VertexConsumer consumer = bufferSource.getBuffer(NarutoRenderTypes.scrollingTranslucent(TEXTURE, -0.01F, -0.02F));
        this.model.render(poseStack, consumer, length);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, LightTexture.FULL_BRIGHT);
    }

    @Override
    public boolean shouldRender(SuitonStreamEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(SuitonStreamEntity entity) {
        return TEXTURE;
    }

    private static float getLifeScale(SuitonStreamEntity entity, float partialTick) {
        float age = entity.tickCount + partialTick;
        int maxLife = entity.getMaxLifeForRender();
        if (age >= maxLife - 10.0F) {
            return Mth.clamp((maxLife - age) / 10.0F, 0.0F, 1.0F);
        }
        return Mth.clamp(age / 10.0F, 0.0F, 1.0F);
    }
}
