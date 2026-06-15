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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.SealingModel;
import net.narutomod.client.render.NarutoRenderTypes;
import net.narutomod.entity.SealingEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class SealingRenderer extends EntityRenderer<SealingEntity> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/sealing_circle.png");
    private static final ResourceLocation FUUIN_BEAM_TEXTURE = NarutomodMod.location("textures/fuuin_beam.png");
    private static final float LEGACY_MODEL_SCALE = 0.85F / 0.0625F;

    private final SealingModel model;

    public SealingRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
        this.model = new SealingModel(context.bakeLayer(SealingModel.LAYER_LOCATION));
    }

    @Override
    public void render(SealingEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        float age = entity.tickCount + partialTick;
        float alpha = Mth.clamp(age / 60.0F, 0.0F, 1.0F);
        VertexConsumer consumer = bufferSource.getBuffer(NarutoRenderTypes.translucentEmissiveNoCull(TEXTURE));
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.01D, 0.0D);
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        poseStack.scale(LEGACY_MODEL_SCALE, LEGACY_MODEL_SCALE, LEGACY_MODEL_SCALE);
        this.model.setupAnim(entity, 0.0F, 0.0F, age, 0.0F, 0.0F);
        this.model.renderToBuffer(
                poseStack,
                consumer,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                1.0F,
                1.0F,
                1.0F,
                alpha);
        poseStack.popPose();
        renderFuuinBeam(entity, partialTick, poseStack, bufferSource);
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, LightTexture.FULL_BRIGHT);
    }

    @Override
    public ResourceLocation getTextureLocation(SealingEntity entity) {
        return TEXTURE;
    }

    private void renderFuuinBeam(SealingEntity seal, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource) {
        if (seal.getFuuinProgress() <= 0 || seal.getTargetEntityId() < 0) {
            return;
        }
        Entity target = seal.level().getEntity(seal.getTargetEntityId());
        if (target == null) {
            return;
        }
        Entity owner = seal.getOwnerEntityId() >= 0 ? seal.level().getEntity(seal.getOwnerEntityId()) : null;
        Vec3 sealPos = interpolatedPosition(seal, partialTick);
        Vec3 from = interpolatedPosition(target, partialTick)
                .add(0.0D, target.getBbHeight() * 0.6D, 0.0D)
                .subtract(sealPos);
        Vec3 to = owner == null
                ? new Vec3(0.0D, 0.8D, 0.0D)
                : interpolatedPosition(owner, partialTick)
                        .add(0.0D, owner.getBbHeight() * 0.5D, 0.0D)
                        .subtract(sealPos);
        Vec3 line = to.subtract(from);
        if (line.lengthSqr() <= 1.0E-6D) {
            return;
        }

        VertexConsumer consumer = bufferSource.getBuffer(NarutoRenderTypes.translucentEmissiveNoCull(FUUIN_BEAM_TEXTURE));
        float age = seal.tickCount + partialTick;
        float progressAlpha = Mth.clamp(seal.getFuuinProgress() / 400.0F, 0.25F, 1.0F);
        renderBeamStrip(poseStack.last().pose(), poseStack.last().normal(), consumer, from, to, age, progressAlpha);
    }

    private static Vec3 interpolatedPosition(Entity entity, float partialTick) {
        return new Vec3(
                Mth.lerp(partialTick, entity.xOld, entity.getX()),
                Mth.lerp(partialTick, entity.yOld, entity.getY()),
                Mth.lerp(partialTick, entity.zOld, entity.getZ()));
    }

    private static void renderBeamStrip(Matrix4f matrix, Matrix3f normal, VertexConsumer consumer, Vec3 from, Vec3 to, float age, float alphaScale) {
        Vec3 axis = to.subtract(from);
        double length = axis.length();
        if (length <= 1.0E-6D) {
            return;
        }
        axis = axis.scale(1.0D / length);
        Vec3 reference = Math.abs(axis.y()) > 0.9D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 right = axis.cross(reference).normalize();
        Vec3 up = right.cross(axis).normalize();
        float v0 = -age * 0.01F;
        float v1 = (float) length / 32.0F - age * 0.01F;
        for (int j = 0; j < 8; j++) {
            double angle0 = j * Math.PI * 2.0D / 8.0D;
            double angle1 = (j + 1) * Math.PI * 2.0D / 8.0D;
            float u0 = j / 8.0F;
            float u1 = (j + 1) / 8.0F;
            Vec3 start0 = ringPoint(from, right, up, angle0, 0.75D);
            Vec3 start1 = ringPoint(from, right, up, angle1, 0.75D);
            Vec3 end1 = ringPoint(to, right, up, angle1, 0.375D);
            Vec3 end0 = ringPoint(to, right, up, angle0, 0.375D);
            beamVertex(matrix, normal, consumer, start0, u0, v0, 0, 0, 0, (int)(128 * alphaScale));
            beamVertex(matrix, normal, consumer, start1, u1, v0, 0, 0, 0, (int)(128 * alphaScale));
            beamVertex(matrix, normal, consumer, end1, u1, v1, 255, 255, 255, (int)(192 * alphaScale));
            beamVertex(matrix, normal, consumer, end0, u0, v1, 255, 255, 255, (int)(192 * alphaScale));
        }
    }

    private static Vec3 ringPoint(Vec3 center, Vec3 right, Vec3 up, double angle, double radius) {
        return center.add(right.scale(Math.sin(angle) * radius)).add(up.scale(Math.cos(angle) * radius));
    }

    private static void beamVertex(Matrix4f matrix, Matrix3f normal, VertexConsumer consumer, Vec3 point, float u, float v,
            int red, int green, int blue, int alpha) {
        consumer.vertex(matrix, (float)point.x(), (float)point.y(), (float)point.z())
                .color(red, green, blue, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, 0.0F, 1.0F, 0.0F)
                .endVertex();
    }
}
