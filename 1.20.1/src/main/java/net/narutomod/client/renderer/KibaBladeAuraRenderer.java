package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.Random;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.narutomod.NarutomodMod;
import net.narutomod.client.render.NarutoRenderTypes;
import net.narutomod.entity.KibaBladeAuraEntity;
import org.joml.Matrix4f;

public final class KibaBladeAuraRenderer extends EntityRenderer<KibaBladeAuraEntity> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/white_square.png");
    private static final Vec3 LEGACY_BLADE_START = new Vec3(0.0D, -0.725D, 0.2D);
    private static final Vec3 LEGACY_BLADE_END = new Vec3(0.0D, -0.725D, 1.6D);
    private static final int LEGACY_ARC_COLOR = 0xC00000FF;
    private static final double LEGACY_SPARK_LENGTH = 0.01D;
    private static final double LEGACY_INACCURACY = 0.1D;
    private static final double SEGMENT_OFFSET = 0.1D;
    private static final float SPARK_CHANCE_PER_RENDER = 0.01F;

    private final Random random = new Random();

    public KibaBladeAuraRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void render(KibaBladeAuraEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        LivingEntity owner = entity.getOwnerForRender();
        if (owner != null) {
            HumanoidModel model = getHumanoidModel(owner);
            if (model != null) {
                model.setupAnim(owner, 0.0F, 0.0F, owner.tickCount + partialTick, 0.0F, owner.getXRot());
                Vec3 entityPosition = interpolatedPosition(entity, partialTick);
                HumanoidArm mainArm = owner.getMainArm();
                if (entity.isOwnerHoldingBladeForRender(InteractionHand.MAIN_HAND)) {
                    maybeRenderBladeSpark(model, mainArm, owner, entityPosition, partialTick, poseStack, bufferSource);
                }
                if (entity.isOwnerHoldingBladeForRender(InteractionHand.OFF_HAND)) {
                    maybeRenderBladeSpark(model, mainArm.getOpposite(), owner, entityPosition, partialTick, poseStack, bufferSource);
                }
            }
        }
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, LightTexture.FULL_BRIGHT);
    }

    @Override
    public boolean shouldRender(KibaBladeAuraEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(KibaBladeAuraEntity entity) {
        return TEXTURE;
    }

    @SuppressWarnings("rawtypes")
    private void maybeRenderBladeSpark(HumanoidModel model, HumanoidArm armSide, LivingEntity owner, Vec3 entityPosition,
            float partialTick, PoseStack poseStack, MultiBufferSource bufferSource) {
        if (this.random.nextFloat() >= SPARK_CHANCE_PER_RENDER) {
            return;
        }
        ModelPart arm = armSide == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
        Vec3 start = transformThirdPerson(LEGACY_BLADE_START, arm, armSide, owner, partialTick);
        Vec3 bladeStep = transformThirdPerson(LEGACY_BLADE_END, arm, armSide, owner, partialTick).subtract(start).scale(0.2D);
        Vec3 sparkStart = start.add(bladeStep);
        Vec3 sparkEnd = sparkStart.add(randomTinyArcOffset());
        renderArc(poseStack, bufferSource, sparkStart.subtract(entityPosition), sparkEnd.subtract(entityPosition));
    }

    private Vec3 randomTinyArcOffset() {
        return new Vec3(
                (this.random.nextDouble() - 0.5D) * LEGACY_SPARK_LENGTH * 2.0D,
                (this.random.nextDouble() - 0.5D) * LEGACY_SPARK_LENGTH * 2.0D,
                (this.random.nextDouble() - 0.5D) * LEGACY_SPARK_LENGTH * 2.0D
        ).add(
                (this.random.nextFloat() - 0.5D) * LEGACY_INACCURACY * 2.0D,
                this.random.nextFloat() * LEGACY_INACCURACY * 2.0D,
                this.random.nextFloat() * LEGACY_INACCURACY * 2.0D
        );
    }

    private void renderArc(PoseStack poseStack, MultiBufferSource bufferSource, Vec3 from, Vec3 to) {
        Vec3 line = to.subtract(from);
        if (line.lengthSqr() <= 1.0E-8D) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(from.x(), from.y(), from.z());
        float yaw = (float)(Mth.atan2(line.x(), line.z()) * Mth.RAD_TO_DEG);
        float pitch = (float)(-Mth.atan2(line.y(), Math.sqrt(line.x() * line.x() + line.z() * line.z())) * Mth.RAD_TO_DEG);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        double thickness = Math.max(line.length() * 0.004D, 0.0005D);
        VertexConsumer consumer = bufferSource.getBuffer(NarutoRenderTypes.colorAdditiveTriangleStrip());
        renderSection(
                poseStack.last().pose(),
                consumer,
                new Vec3(0.0D, 0.0D, 0.0D),
                new Vec3(0.0D, 0.0D, line.length()),
                thickness,
                0,
                4,
                false);
        poseStack.popPose();
    }

    private void renderSection(
            Matrix4f matrix,
            VertexConsumer consumer,
            Vec3 from,
            Vec3 to,
            double thickness,
            int recursiveDepth,
            int maxRecursiveDepth,
            boolean branch) {
        if (recursiveDepth >= maxRecursiveDepth) {
            emitBolt(matrix, consumer, from, to, thickness, branch);
            return;
        }
        Vec3 mid = to.subtract(from).scale(0.5D);
        double offset = mid.length() * SEGMENT_OFFSET;
        mid = mid.add(this.random.nextGaussian() * offset, this.random.nextGaussian() * offset, this.random.nextGaussian() * offset);
        Vec3 middle = from.add(mid);
        renderSection(matrix, consumer, from, middle, thickness, recursiveDepth + 1, maxRecursiveDepth, branch);
        renderSection(matrix, consumer, middle, to, thickness, recursiveDepth + 1, maxRecursiveDepth, branch);
        if (this.random.nextInt(5) == 0) {
            renderSection(matrix, consumer, middle, middle.add(mid.scale(1.8D)), thickness * 0.6D, recursiveDepth + 1, maxRecursiveDepth, true);
        }
    }

    private static void emitBolt(Matrix4f matrix, VertexConsumer consumer, Vec3 from, Vec3 to, double thickness, boolean branch) {
        int red = LEGACY_ARC_COLOR >> 16 & 0xFF;
        int green = LEGACY_ARC_COLOR >> 8 & 0xFF;
        int blue = LEGACY_ARC_COLOR & 0xFF;
        for (int i = 1; i <= 3; i++) {
            if (!branch || i >= 2) {
                double width = thickness * i;
                int alpha = i == 3 ? 0x20 : i == 2 ? 0x80 : 0xF0;
                int r = i == 1 ? 255 : red;
                int g = i == 1 ? 255 : green;
                int b = i == 1 ? 255 : blue;
                vertex(matrix, consumer, from.x() - width, from.y() - width, from.z(), r, g, b, alpha);
                vertex(matrix, consumer, to.x() - width, to.y() - width, to.z(), r, g, b, alpha);
                vertex(matrix, consumer, from.x() - width, from.y() + width, from.z(), r, g, b, alpha);
                vertex(matrix, consumer, to.x() - width, to.y() + width, to.z(), r, g, b, alpha);
                vertex(matrix, consumer, from.x() + width, from.y() + width, from.z(), r, g, b, alpha);
                vertex(matrix, consumer, to.x() + width, to.y() + width, to.z(), r, g, b, alpha);
                vertex(matrix, consumer, from.x() + width, from.y() - width, from.z(), r, g, b, alpha);
                vertex(matrix, consumer, to.x() + width, to.y() - width, to.z(), r, g, b, alpha);
                vertex(matrix, consumer, from.x() - width, from.y() - width, from.z(), r, g, b, alpha);
                vertex(matrix, consumer, to.x() - width, to.y() - width, to.z(), r, g, b, alpha);
            }
        }
    }

    private static void vertex(Matrix4f matrix, VertexConsumer consumer, double x, double y, double z, int red, int green, int blue, int alpha) {
        consumer.vertex(matrix, (float) x, (float) y, (float) z).color(red, green, blue, alpha).endVertex();
    }

    private static Vec3 transformThirdPerson(Vec3 local, ModelPart arm, HumanoidArm armSide, LivingEntity owner, float partialTick) {
        float bodyYaw = Mth.rotLerp(partialTick, owner.yBodyRotO, owner.yBodyRot) * Mth.DEG_TO_RAD;
        double armOffset = armSide == HumanoidArm.RIGHT ? -0.3125D : 0.3125D;
        Vec3 relative = local
                .zRot(-arm.zRot)
                .yRot(-arm.yRot)
                .xRot(-arm.xRot)
                .add(armOffset, 1.5D - (owner.isCrouching() ? 0.3D : 0.0D), -0.05D)
                .yRot(-bodyYaw);
        return relative.add(
                Mth.lerp(partialTick, owner.xOld, owner.getX()),
                Mth.lerp(partialTick, owner.yOld, owner.getY()),
                Mth.lerp(partialTick, owner.zOld, owner.getZ()));
    }

    private static Vec3 interpolatedPosition(KibaBladeAuraEntity entity, float partialTick) {
        return new Vec3(
                Mth.lerp(partialTick, entity.xOld, entity.getX()),
                Mth.lerp(partialTick, entity.yOld, entity.getY()),
                Mth.lerp(partialTick, entity.zOld, entity.getZ()));
    }

    @SuppressWarnings("rawtypes")
    private HumanoidModel getHumanoidModel(LivingEntity owner) {
        if (this.entityRenderDispatcher.getRenderer(owner) instanceof LivingEntityRenderer renderer
                && renderer.getModel() instanceof HumanoidModel model) {
            return model;
        }
        return null;
    }
}
