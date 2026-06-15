package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.BuddhaArmModel;
import net.narutomod.client.render.NarutoRenderTypes;
import net.narutomod.entity.BuddhaArmEntity;

public final class BuddhaArmRenderer extends EntityRenderer<BuddhaArmEntity> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/woodfist.png");
    private static final float WIDTH = 5.0F;
    private static final float WORLD_UNITS_PER_LEGACY_UNIT = WIDTH / 4.0F;
    private final BuddhaArmModel model = new BuddhaArmModel();

    public BuddhaArmRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.1F;
    }

    @Override
    public void render(BuddhaArmEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        float length = entity.getRenderLength();

        poseStack.pushPose();
        poseStack.translate(0.0D, WIDTH * 0.5D, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch - 180.0F));
        poseStack.translate(0.0D, 0.0D, -length * 0.5D);

        VertexConsumer consumer = bufferSource.getBuffer(entity.shouldGrow()
                ? RenderType.entityCutoutNoCull(TEXTURE)
                : NarutoRenderTypes.energyAdditive(TEXTURE));
        this.model.renderToBuffer(poseStack, consumer, WIDTH, WIDTH, length,
                legacyModelLength(length), entity.shouldGrow() ? 255 : 128);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, LightTexture.FULL_BRIGHT);
    }

    @Override
    public ResourceLocation getTextureLocation(BuddhaArmEntity entity) {
        return TEXTURE;
    }

    private static float legacyModelLength(float renderLength) {
        return Math.max(renderLength / WORLD_UNITS_PER_LEGACY_UNIT, 1.0F);
    }
}
