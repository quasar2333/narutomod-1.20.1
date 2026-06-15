package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.WoodSegmentModel;
import net.narutomod.entity.WoodArmEntity;

public final class WoodArmRenderer extends EntityRenderer<WoodArmEntity> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/woodblock.png");
    private static final double SEGMENT_SPACING = 0.42D;
    private static final int MAX_SEGMENTS = 80;

    private final WoodSegmentModel<WoodArmEntity> model;

    public WoodArmRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.4F;
        this.model = new WoodSegmentModel<>(context.bakeLayer(WoodSegmentModel.LAYER_LOCATION));
    }

    @Override
    public void render(WoodArmEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        Vec3 entityPos = interpolatedPosition(entity, partialTick);
        Vec3 base = renderBase(entity, partialTick, entityPos);
        Vec3 end = renderEnd(entity, partialTick, entityPos);
        Vec3 delta = end.subtract(base);
        double length = delta.length();
        if (length <= 1.0E-5D) {
            return;
        }

        int segments = Mth.clamp((int)Math.ceil(length / SEGMENT_SPACING), 1, MAX_SEGMENTS);
        Vec3 step = delta.scale(1.0D / segments);
        float yaw = yawFrom(delta);
        float pitch = pitchFrom(delta);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        for (int i = 0; i <= segments; i++) {
            Vec3 point = base.add(step.scale(i)).subtract(entityPos);
            poseStack.pushPose();
            poseStack.translate(point.x(), point.y(), point.z());
            poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
            poseStack.mulPose(Axis.XP.rotationDegrees(pitch - 180.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(5.0F * i));
            poseStack.scale(2.0F, 2.0F, 2.0F);
            this.model.setupAnim(entity, 0.0F, 0.0F, entity.tickCount + partialTick, 0.0F, 0.0F);
            this.model.renderToBuffer(
                poseStack,
                consumer,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                1.0F,
                1.0F,
                1.0F,
                1.0F
            );
            poseStack.popPose();
        }

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, LightTexture.FULL_BRIGHT);
    }

    @Override
    public boolean shouldRender(WoodArmEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(WoodArmEntity entity) {
        return TEXTURE;
    }

    private static Vec3 renderBase(WoodArmEntity entity, float partialTick, Vec3 fallback) {
        LivingEntity owner = entity.getOwnerForRender();
        if (owner == null) {
            return fallback;
        }
        return WoodArmEntity.armBaseForRender(owner, partialTick);
    }

    private static Vec3 renderEnd(WoodArmEntity entity, float partialTick, Vec3 fallback) {
        if (entity.hasReachedTargetForRender()) {
            LivingEntity target = entity.getTargetForRender();
            if (target != null) {
                return interpolatedTargetCenter(target, partialTick);
            }
        }
        return fallback;
    }

    private static Vec3 interpolatedPosition(WoodArmEntity entity, float partialTick) {
        return new Vec3(
            Mth.lerp(partialTick, entity.xOld, entity.getX()),
            Mth.lerp(partialTick, entity.yOld, entity.getY()),
            Mth.lerp(partialTick, entity.zOld, entity.getZ())
        );
    }

    private static Vec3 interpolatedTargetCenter(LivingEntity target, float partialTick) {
        return new Vec3(
            Mth.lerp(partialTick, target.xOld, target.getX()),
            Mth.lerp(partialTick, target.yOld, target.getY()) + target.getBbHeight() * 0.5D,
            Mth.lerp(partialTick, target.zOld, target.getZ())
        );
    }

    private static float yawFrom(Vec3 delta) {
        return (float)(Mth.atan2(delta.x(), delta.z()) * Mth.RAD_TO_DEG);
    }

    private static float pitchFrom(Vec3 delta) {
        double horizontal = Math.sqrt(delta.x() * delta.x() + delta.z() * delta.z());
        return (float)(-Mth.atan2(delta.y(), horizontal) * Mth.RAD_TO_DEG);
    }
}
