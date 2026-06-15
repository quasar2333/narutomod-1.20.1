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
import net.narutomod.entity.WoodBurialEntity;

public final class WoodBurialRenderer extends EntityRenderer<WoodBurialEntity> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/woodblock.png");
    private static final int GROW_TICKS = 50;
    private static final double SEGMENT_SPACING = 0.42D;
    private static final int MAX_SEGMENTS_PER_BRANCH = 48;

    private final WoodSegmentModel<WoodBurialEntity> model;

    public WoodBurialRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.4F;
        this.model = new WoodSegmentModel<>(context.bakeLayer(WoodSegmentModel.LAYER_LOCATION));
    }

    @Override
    public void render(WoodBurialEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        Vec3 entityPos = interpolatedPosition(entity, partialTick);
        LivingEntity target = entity.getTargetForRender();
        Vec3 targetCenter = target != null ? interpolatedTargetCenter(target, partialTick) : entityPos.add(0.0D, 1.0D, 0.0D);
        float targetWidth = target != null ? target.getBbWidth() : 0.6F;
        float targetHeight = target != null ? target.getBbHeight() : 1.8F;
        int branches = Mth.clamp((int)Math.max(targetWidth * 10.0F, 6.0F), 6, 12);
        float growth = Mth.clamp((entity.tickCount + partialTick) / GROW_TICKS, 0.05F, 1.0F);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));

        for (int branch = 0; branch < branches; branch++) {
            renderBranch(entity, branch, branches, growth, targetWidth, targetHeight, entityPos,
                targetCenter, poseStack, consumer, packedLight, partialTick);
        }

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, LightTexture.FULL_BRIGHT);
    }

    @Override
    public boolean shouldRender(WoodBurialEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(WoodBurialEntity entity) {
        return TEXTURE;
    }

    private void renderBranch(WoodBurialEntity entity, int branch, int branchCount, float growth, float targetWidth,
            float targetHeight, Vec3 entityPos, Vec3 targetCenter, PoseStack poseStack, VertexConsumer consumer,
            int packedLight, float partialTick) {
        double baseAngle = branch * Math.PI * 2.0D / branchCount + randomUnit(entity, branch, 0) * 0.6D;
        double radius = Math.max(targetWidth * 0.95D, 0.55D) + randomUnit(entity, branch, 1) * 0.25D;
        Vec3 start = entityPos.add(Math.cos(baseAngle) * radius, 0.0D, Math.sin(baseAngle) * radius);
        Vec3 end = targetCenter.add(
            Math.cos(baseAngle + Math.PI) * radius * 0.18D,
            targetHeight * 0.55D + 0.35D,
            Math.sin(baseAngle + Math.PI) * radius * 0.18D
        );
        double length = start.distanceTo(end) + targetHeight * 0.5D;
        int totalSegments = Mth.clamp((int)Math.ceil(length / SEGMENT_SPACING), 4, MAX_SEGMENTS_PER_BRANCH);
        int visibleSegments = Mth.clamp((int)Math.ceil(totalSegments * growth), 1, totalSegments);
        for (int segment = 0; segment <= visibleSegments; segment++) {
            float t = segment / (float)totalSegments;
            Vec3 point = curvePoint(start, end, targetCenter, baseAngle, radius, t);
            float t2 = Math.min((segment + 1) / (float)totalSegments, 1.0F);
            Vec3 next = curvePoint(start, end, targetCenter, baseAngle, radius, t2);
            renderSegment(entity, segment, point.subtract(entityPos), next.subtract(point),
                poseStack, consumer, packedLight, partialTick);
        }
    }

    private void renderSegment(WoodBurialEntity entity, int segment, Vec3 point, Vec3 tangent, PoseStack poseStack,
            VertexConsumer consumer, int packedLight, float partialTick) {
        if (tangent.lengthSqr() <= 1.0E-6D) {
            tangent = new Vec3(0.0D, 1.0D, 0.0D);
        }
        float yaw = yawFrom(tangent);
        float pitch = pitchFrom(tangent);
        poseStack.pushPose();
        poseStack.translate(point.x(), point.y(), point.z());
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch - 180.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(5.0F * segment));
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

    private static Vec3 curvePoint(Vec3 start, Vec3 end, Vec3 targetCenter, double baseAngle, double radius, float t) {
        float eased = 1.0F - (1.0F - t) * (1.0F - t);
        Vec3 linear = start.lerp(end, eased);
        double wrapAngle = baseAngle + t * Math.PI * 1.75D;
        double wrapRadius = radius * (1.0D - t * 0.55D);
        Vec3 wrapCenter = new Vec3(targetCenter.x(), linear.y(), targetCenter.z());
        Vec3 wrapped = wrapCenter.add(Math.cos(wrapAngle) * wrapRadius, 0.0D, Math.sin(wrapAngle) * wrapRadius);
        return linear.lerp(wrapped, 0.45D * (1.0D - t * 0.35D));
    }

    private static Vec3 interpolatedPosition(WoodBurialEntity entity, float partialTick) {
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

    private static double randomUnit(WoodBurialEntity entity, int branch, int salt) {
        int value = entity.getId() * 73428767 + branch * 912271 + salt * 42349;
        value ^= value >>> 16;
        value *= 0x7feb352d;
        value ^= value >>> 15;
        value *= 0x846ca68b;
        value ^= value >>> 16;
        return ((value & 0xFFFF) / 32767.5D) - 1.0D;
    }
}
