package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.narutomod.NarutomodMod;
import net.narutomod.client.render.NarutoRenderTypes;
import net.narutomod.entity.ShadowImitationEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class ShadowImitationRenderer extends EntityRenderer<ShadowImitationEntity> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/black.png");
    private static final int ALPHA = 128;
    private static final double SURFACE_OFFSET = 0.01D;

    public ShadowImitationRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(ShadowImitationEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        LivingEntity owner = entity.getOwnerForRender();
        LivingEntity target = entity.getTargetForRender();
        if (owner == null || target == null) {
            return;
        }

        Vec3 ownerPos = interpolatedFoot(owner, partialTick);
        Vec3 targetPos = interpolatedFoot(target, partialTick);
        Vec3 entityPos = interpolatedEntity(entity, partialTick);
        int minX = Mth.floor(Math.min(ownerPos.x(), targetPos.x()));
        int minY = Mth.floor(Math.min(ownerPos.y(), targetPos.y())) - 10;
        int minZ = Mth.floor(Math.min(ownerPos.z(), targetPos.z()));
        int maxX = Mth.floor(Math.max(ownerPos.x(), targetPos.x()));
        int maxY = Mth.floor(Math.max(ownerPos.y(), targetPos.y())) + 1;
        int maxZ = Mth.floor(Math.max(ownerPos.z(), targetPos.z()));
        double growthRadiusSqr = 0.25D * (entity.tickCount + partialTick) * (entity.tickCount + partialTick);

        VertexConsumer consumer = bufferSource.getBuffer(NarutoRenderTypes.translucentEmissiveNoCull(TEXTURE));
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();
        for (BlockPos pos : BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ)) {
            if (!isLegacyShadowSurface(entity, pos, ownerPos, targetPos, maxY, growthRadiusSqr)) {
                continue;
            }
            AABB block = new AABB(pos).move(-entityPos.x(), -entityPos.y(), -entityPos.z());
            renderBlockShadow(matrix, normal, consumer, block);
        }

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, LightTexture.FULL_BRIGHT);
    }

    @Override
    public boolean shouldRender(ShadowImitationEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(ShadowImitationEntity entity) {
        return TEXTURE;
    }

    private static boolean isLegacyShadowSurface(ShadowImitationEntity entity, BlockPos pos, Vec3 ownerPos,
            Vec3 targetPos, int maxY, double growthRadiusSqr) {
        BlockState state = entity.level().getBlockState(pos);
        if (state.getRenderShape() == RenderShape.INVISIBLE || !state.isCollisionShapeFullBlock(entity.level(), pos)) {
            return false;
        }
        if (pos.distToCenterSqr(ownerPos.x(), ownerPos.y(), ownerPos.z()) >= growthRadiusSqr) {
            return false;
        }
        AABB column = new AABB(
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                pos.getX() + 1.0D,
                Math.max(maxY, pos.getY() + 1.0D),
                pos.getZ() + 1.0D);
        return column.clip(ownerPos, targetPos).isPresent();
    }

    private static Vec3 interpolatedFoot(LivingEntity entity, float partialTick) {
        return new Vec3(
                Mth.lerp(partialTick, entity.xOld, entity.getX()),
                Mth.lerp(partialTick, entity.yOld, entity.getY()),
                Mth.lerp(partialTick, entity.zOld, entity.getZ()));
    }

    private static Vec3 interpolatedEntity(ShadowImitationEntity entity, float partialTick) {
        return new Vec3(
                Mth.lerp(partialTick, entity.xOld, entity.getX()),
                Mth.lerp(partialTick, entity.yOld, entity.getY()),
                Mth.lerp(partialTick, entity.zOld, entity.getZ()));
    }

    private static void renderBlockShadow(Matrix4f matrix, Matrix3f normal, VertexConsumer consumer, AABB block) {
        double minX = block.minX;
        double minY = block.minY;
        double minZ = block.minZ;
        double maxX = block.maxX;
        double maxY = block.maxY;
        double maxZ = block.maxZ;
        face(matrix, normal, consumer,
                minX, maxY + SURFACE_OFFSET, minZ,
                minX, maxY + SURFACE_OFFSET, maxZ,
                maxX, maxY + SURFACE_OFFSET, maxZ,
                maxX, maxY + SURFACE_OFFSET, minZ);
        face(matrix, normal, consumer,
                maxX + SURFACE_OFFSET, maxY, minZ,
                maxX + SURFACE_OFFSET, maxY, maxZ,
                maxX + SURFACE_OFFSET, minY, maxZ,
                maxX + SURFACE_OFFSET, minY, minZ);
        face(matrix, normal, consumer,
                minX - SURFACE_OFFSET, minY, minZ,
                minX - SURFACE_OFFSET, minY, maxZ,
                minX - SURFACE_OFFSET, maxY, maxZ,
                minX - SURFACE_OFFSET, maxY, minZ);
        face(matrix, normal, consumer,
                maxX, minY, maxZ + SURFACE_OFFSET,
                maxX, maxY, maxZ + SURFACE_OFFSET,
                minX, maxY, maxZ + SURFACE_OFFSET,
                minX, minY, maxZ + SURFACE_OFFSET);
        face(matrix, normal, consumer,
                minX, minY, minZ - SURFACE_OFFSET,
                minX, maxY, minZ - SURFACE_OFFSET,
                maxX, maxY, minZ - SURFACE_OFFSET,
                maxX, minY, minZ - SURFACE_OFFSET);
    }

    private static void face(Matrix4f matrix, Matrix3f normal, VertexConsumer consumer,
            double x0, double y0, double z0,
            double x1, double y1, double z1,
            double x2, double y2, double z2,
            double x3, double y3, double z3) {
        vertex(matrix, normal, consumer, x0, y0, z0, 0.0F, 1.0F);
        vertex(matrix, normal, consumer, x1, y1, z1, 0.0F, 0.0F);
        vertex(matrix, normal, consumer, x2, y2, z2, 1.0F, 0.0F);
        vertex(matrix, normal, consumer, x3, y3, z3, 1.0F, 1.0F);
    }

    private static void vertex(Matrix4f matrix, Matrix3f normal, VertexConsumer consumer,
            double x, double y, double z, float u, float v) {
        consumer.vertex(matrix, (float)x, (float)y, (float)z)
                .color(255, 255, 255, ALPHA)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, 0.0F, 1.0F, 0.0F)
                .endVertex();
    }
}
