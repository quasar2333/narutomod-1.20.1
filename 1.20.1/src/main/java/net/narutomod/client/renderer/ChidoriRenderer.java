package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.narutomod.NarutomodMod;
import net.narutomod.client.render.NarutoRenderTypes;
import net.narutomod.entity.ChidoriEntity;
import net.narutomod.entity.ChidoriSpearEntity;
import net.narutomod.network.ChidoriHandPositionMessage;
import net.narutomod.network.NetworkHandler;
import net.narutomod.procedure.ProcedureUtils;
import org.joml.Matrix4f;

public final class ChidoriRenderer<T extends Entity> extends EntityRenderer<T> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/white_square.png");
    private static final Vec3 LEGACY_EMPTY_HAND_POINT = new Vec3(0.0D, -0.7D, 0.0D);
    private static final Vec3 LEGACY_WEAPON_START = new Vec3(0.0D, -0.6875D, 0.2D);
    private static final Vec3 LEGACY_WEAPON_END = new Vec3(0.0D, -0.6875D, 1.6D);
    private static final int LEGACY_ARC_COLOR = 0xC00000FF;
    private static final double LEGACY_SPARK_LENGTH = 0.01D;
    private static final double LEGACY_INACCURACY = 0.1D;
    private static final double SEGMENT_OFFSET = 0.1D;
    private static final float SPARK_CHANCE_PER_RENDER = 0.01F;

    private final Map<Integer, Integer> handSyncTicks = new HashMap<>();
    private final Random random = new Random();

    public ChidoriRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        syncClientHandPosition(entity, partialTick);
        renderWeaponSparks(entity, partialTick, poseStack, bufferSource);
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return TEXTURE;
    }

    private void syncClientHandPosition(T entity, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || getChidoriOwner(entity) != player) {
            return;
        }
        Integer syncedTick = this.handSyncTicks.get(entity.getId());
        if (syncedTick != null && syncedTick == entity.tickCount) {
            return;
        }
        Vec3 handPosition = computeThirdPersonHandPosition(player, entity, partialTick);
        if (handPosition != null) {
            this.handSyncTicks.put(entity.getId(), entity.tickCount);
            NetworkHandler.sendToServer(new ChidoriHandPositionMessage(entity.getId(), handPosition.x(), handPosition.y(), handPosition.z()));
        }
    }

    private static LivingEntity getChidoriOwner(Entity entity) {
        if (entity instanceof ChidoriEntity chidori) {
            return chidori.getOwner();
        }
        if (entity instanceof ChidoriSpearEntity spear) {
            return spear.getOwner();
        }
        return null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void renderWeaponSparks(T entity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource) {
        LivingEntity owner = getChidoriOwner(entity);
        if (owner == null || !ProcedureUtils.isWeapon(owner.getMainHandItem())) {
            return;
        }
        HumanoidModel model = getHumanoidModel(owner);
        if (model == null) {
            return;
        }
        model.setupAnim(owner, 0.0F, 0.0F, owner.tickCount + partialTick, 0.0F, owner.getXRot());
        Vec3 entityPosition = interpolatedPosition(entity, partialTick);
        HumanoidArm mainArm = owner.getMainArm();
        maybeRenderWeaponSpark(model, mainArm, owner, entityPosition, partialTick, poseStack, bufferSource);
        if (ProcedureUtils.isWeapon(owner.getOffhandItem())) {
            maybeRenderWeaponSpark(model, mainArm.getOpposite(), owner, entityPosition, partialTick, poseStack, bufferSource);
        }
    }

    @SuppressWarnings("rawtypes")
    private void maybeRenderWeaponSpark(HumanoidModel model, HumanoidArm armSide, LivingEntity owner, Vec3 entityPosition,
            float partialTick, PoseStack poseStack, MultiBufferSource bufferSource) {
        if (this.random.nextFloat() >= SPARK_CHANCE_PER_RENDER) {
            return;
        }
        ModelPart arm = armSide == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
        Vec3 start = transformThirdPerson(LEGACY_WEAPON_START, arm, armSide, owner, partialTick);
        Vec3 step = transformThirdPerson(LEGACY_WEAPON_END, arm, armSide, owner, partialTick).subtract(start).scale(0.2D);
        Vec3 sparkStart = start.add(step);
        Vec3 sparkEnd = sparkStart.add(randomTinyArcOffset(step));
        renderArc(poseStack, bufferSource, sparkStart.subtract(entityPosition), sparkEnd.subtract(entityPosition));
    }

    private Vec3 randomTinyArcOffset(Vec3 motion) {
        return motion.add(
                (this.random.nextDouble() - 0.5D) * LEGACY_SPARK_LENGTH * 2.0D,
                (this.random.nextDouble() - 0.5D) * LEGACY_SPARK_LENGTH * 2.0D,
                (this.random.nextDouble() - 0.5D) * LEGACY_SPARK_LENGTH * 2.0D
        ).add(
                (this.random.nextFloat() - 0.5D) * LEGACY_INACCURACY * 2.0D,
                this.random.nextFloat() * LEGACY_INACCURACY * 2.0D,
                this.random.nextFloat() * LEGACY_INACCURACY * 2.0D
        );
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Vec3 computeThirdPersonHandPosition(LocalPlayer player, T entity, float partialTick) {
        HumanoidModel model = getPlayerHumanoidModel(player);
        if (model == null) {
            return null;
        }
        HumanoidArm mainArm = player.getMainArm();
        boolean weaponPoint = ProcedureUtils.isWeapon(player.getMainHandItem());
        if (!weaponPoint || entity instanceof ChidoriSpearEntity) {
            if (mainArm == HumanoidArm.RIGHT) {
                model.rightArmPose = HumanoidModel.ArmPose.BOW_AND_ARROW;
            } else {
                model.leftArmPose = HumanoidModel.ArmPose.BOW_AND_ARROW;
            }
        }
        model.setupAnim((LivingEntity)player, 0.0F, 0.0F, player.tickCount + partialTick, 0.0F, player.getXRot());
        ModelPart arm = mainArm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
        if (!weaponPoint) {
            return transformThirdPerson(LEGACY_EMPTY_HAND_POINT, arm, mainArm, player, partialTick);
        }
        Vec3 start = transformThirdPerson(LEGACY_WEAPON_START, arm, mainArm, player, partialTick);
        Vec3 step = transformThirdPerson(LEGACY_WEAPON_END, arm, mainArm, player, partialTick).subtract(start).scale(0.2D);
        return start.add(step);
    }

    private static Vec3 transformThirdPerson(Vec3 local, ModelPart arm, HumanoidArm armSide, LivingEntity owner, float partialTick) {
        float bodyYaw = Mth.rotLerp(partialTick, owner.yBodyRotO, owner.yBodyRot) * Mth.DEG_TO_RAD;
        double armOffset = 0.0586D * (armSide == HumanoidArm.RIGHT ? -6.0D : 6.0D);
        Vec3 relative = local
                .zRot(-arm.zRot)
                .yRot(-arm.yRot)
                .xRot(-arm.xRot)
                .add(armOffset, 1.3D - (owner.isCrouching() ? 0.3D : 0.0D), -0.05D)
                .yRot(-bodyYaw);
        return relative.add(
                Mth.lerp(partialTick, owner.xOld, owner.getX()),
                Mth.lerp(partialTick, owner.yOld, owner.getY()),
                Mth.lerp(partialTick, owner.zOld, owner.getZ()));
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

    private static Vec3 interpolatedPosition(Entity entity, float partialTick) {
        return new Vec3(
                Mth.lerp(partialTick, entity.xOld, entity.getX()),
                Mth.lerp(partialTick, entity.yOld, entity.getY()),
                Mth.lerp(partialTick, entity.zOld, entity.getZ()));
    }

    @SuppressWarnings("rawtypes")
    private HumanoidModel getPlayerHumanoidModel(LocalPlayer player) {
        if (this.entityRenderDispatcher.getRenderer(player) instanceof LivingEntityRenderer renderer
                && renderer.getModel() instanceof HumanoidModel model) {
            return model;
        }
        return null;
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
