package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.RasenganModel;
import net.narutomod.client.render.NarutoRenderTypes;
import net.narutomod.entity.RasenganEntity;
import net.narutomod.network.NetworkHandler;
import net.narutomod.network.RasenganHandPositionMessage;
import org.joml.Quaternionf;

public final class RasenganDebugRenderer extends EntityRenderer<RasenganEntity> {
    private static final float INV_SQRT_2 = 0.70710677F;
    private static final float BASE_SIZE = 0.35F;
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/longcube_white.png");

    private final Random random = new Random();
    private final Map<Integer, Integer> handSyncTicks = new HashMap<>();
    private final RasenganModel model;

    public RasenganDebugRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.1F;
        this.model = new RasenganModel(context.bakeLayer(RasenganModel.LAYER_LOCATION));
    }

    @Override
    public void render(RasenganEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        float age = entity.tickCount + partialTick;
        float scale = entity.getRasenganScale();
        syncClientHandPosition(entity, partialTick);

        poseStack.pushPose();
        applyFirstPersonOwnerTransform(entity, partialTick, poseStack);
        poseStack.translate(0.0F, 0.5F - 0.175F * scale, 0.0F);
        poseStack.scale(scale, scale, scale);
        poseStack.mulPose(axisRotation(age * 30.0F));

        VertexConsumer consumer = bufferSource.getBuffer(NarutoRenderTypes.translucentEmissiveNoCull(TEXTURE));
        this.model.setupAnim(entity, 0.0F, 0.0F, age, 0.0F, 0.0F);
        for (int i = 0; i < 10; i++) {
            poseStack.mulPose(Axis.YP.rotationDegrees(this.random.nextFloat() * 30.0F));
            poseStack.mulPose(axisRotation(this.random.nextFloat() * 30.0F));
            this.model.renderCore(
                poseStack,
                consumer,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                1.0F,
                1.0F,
                1.0F,
                0.3F
            );
            this.model.renderShell(
                poseStack,
                consumer,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                0.66F,
                0.87F,
                1.0F,
                0.3F
            );
        }

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(RasenganEntity entity) {
        return TEXTURE;
    }

    private static Quaternionf axisRotation(float degrees) {
        return new Quaternionf().rotationAxis(degrees * Mth.DEG_TO_RAD, INV_SQRT_2, INV_SQRT_2, 0.0F);
    }

    private void syncClientHandPosition(RasenganEntity entity, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || entity.getOwner() != player) {
            return;
        }
        Integer syncedTick = this.handSyncTicks.get(entity.getId());
        if (syncedTick != null && syncedTick == entity.tickCount) {
            return;
        }

        Vec3 handPosition = computeThirdPersonHandPosition(player, entity, partialTick);
        if (handPosition != null) {
            this.handSyncTicks.put(entity.getId(), entity.tickCount);
            NetworkHandler.sendToServer(new RasenganHandPositionMessage(entity.getId(), handPosition.x(), handPosition.y(), handPosition.z()));
        }
    }

    private void applyFirstPersonOwnerTransform(RasenganEntity entity, float partialTick, PoseStack poseStack) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null
                || !minecraft.options.getCameraType().equals(CameraType.FIRST_PERSON)
                || entity.getOwner() != player) {
            return;
        }

        double entityX = Mth.lerp(partialTick, entity.xOld, entity.getX()) - this.entityRenderDispatcher.camera.getPosition().x();
        double entityY = Mth.lerp(partialTick, entity.yOld, entity.getY()) - this.entityRenderDispatcher.camera.getPosition().y();
        double entityZ = Mth.lerp(partialTick, entity.zOld, entity.getZ()) - this.entityRenderDispatcher.camera.getPosition().z();
        poseStack.translate(-entityX, -entityY, -entityZ);
        poseStack.translate(0.0F, 1.925F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partialTick, player.yRotO, player.getYRot())));
        poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partialTick, player.xRotO, player.getXRot())));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        applyLegacyFirstPersonArmTransform(player, partialTick, poseStack);
        translateToRightHand(player, partialTick, poseStack);
        poseStack.translate(-0.125F, BASE_SIZE * entity.getRasenganScale() - 0.025F, 0.0F);
    }

    private static void applyLegacyFirstPersonArmTransform(LocalPlayer player, float partialTick, PoseStack poseStack) {
        float swingProgress = player.getAttackAnim(partialTick);
        float armPitch = Mth.lerp(partialTick, player.xBobO, player.xBob);
        float armYaw = Mth.lerp(partialTick, player.yBobO, player.yBob);
        poseStack.mulPose(Axis.XP.rotationDegrees((player.getXRot() - armPitch) * 0.1F));
        poseStack.mulPose(Axis.YP.rotationDegrees((player.getYRot() - armYaw) * 0.1F));

        float f = 1.0F;
        float rootSwing = Mth.sqrt(swingProgress);
        float x = -0.3F * Mth.sin(rootSwing * Mth.PI);
        float y = 0.4F * Mth.sin(rootSwing * (Mth.PI * 2.0F));
        float z = -0.4F * Mth.sin(swingProgress * Mth.PI);
        poseStack.translate(f * (x + 0.64000005F), y - 0.6F, z - 0.71999997F);
        poseStack.mulPose(Axis.YP.rotationDegrees(f * 45.0F));

        float rollSwing = Mth.sin(swingProgress * swingProgress * Mth.PI);
        float yawSwing = Mth.sin(rootSwing * Mth.PI);
        poseStack.mulPose(Axis.YP.rotationDegrees(f * yawSwing * 70.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(f * rollSwing * -20.0F));
        poseStack.translate(f * -1.0F, 3.6F, 3.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(f * 120.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(200.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(f * -135.0F));
        poseStack.translate(f * 5.6F, 0.0F, 0.0F);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void translateToRightHand(LocalPlayer player, float partialTick, PoseStack poseStack) {
        HumanoidModel model = getPlayerHumanoidModel(player);
        if (model != null) {
            model.rightArmPose = HumanoidModel.ArmPose.BOW_AND_ARROW;
            model.setupAnim((LivingEntity)player, 0.0F, 0.0F, player.tickCount + partialTick, 0.0F, player.getXRot());
            model.translateToHand(HumanoidArm.RIGHT, poseStack);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Vec3 computeThirdPersonHandPosition(LocalPlayer player, RasenganEntity entity, float partialTick) {
        HumanoidModel model = getPlayerHumanoidModel(player);
        if (model == null) {
            return null;
        }

        model.rightArmPose = HumanoidModel.ArmPose.BOW_AND_ARROW;
        model.setupAnim((LivingEntity)player, 0.0F, 0.0F, player.tickCount + partialTick, 0.0F, player.getXRot());
        ModelPart rightArm = model.rightArm;
        double ballHeight = BASE_SIZE * entity.getRasenganScale();
        float bodyYaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot) * Mth.DEG_TO_RAD;
        Vec3 relativeHand = new Vec3(0.0D, -0.5825D - ballHeight * 0.5D, 0.0D)
                .zRot(rightArm.zRot)
                .xRot(-rightArm.xRot)
                .yRot(-rightArm.yRot)
                .add(0.0586D * -6.0D, 1.02D - (player.isCrouching() ? 0.3D : 0.0D), 0.0D)
                .yRot(-bodyYaw)
                .add(0.0D, 0.275D - ballHeight * 0.5D, 0.0D);
        return relativeHand.add(
                Mth.lerp(partialTick, player.xo, player.getX()),
                Mth.lerp(partialTick, player.yo, player.getY()),
                Mth.lerp(partialTick, player.zo, player.getZ()));
    }

    @SuppressWarnings("rawtypes")
    private HumanoidModel getPlayerHumanoidModel(LocalPlayer player) {
        if (this.entityRenderDispatcher.getRenderer(player) instanceof LivingEntityRenderer renderer
                && renderer.getModel() instanceof HumanoidModel model) {
            return model;
        }
        return null;
    }
}
