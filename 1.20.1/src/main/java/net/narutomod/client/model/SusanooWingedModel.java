package net.narutomod.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.generated.LegacyModelLayerDefinitions;
import net.narutomod.entity.SusanooWingedEntity;
import net.narutomod.procedure.ProcedureUtils;

public final class SusanooWingedModel extends HumanoidModel<SusanooWingedEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            NarutomodMod.location("susanoowinged"),
            "main"
    );
    private static final float[] RIGHT_FLAP_DEGREES = {140.0F, 135.0F, 130.0F, 125.0F, 120.0F, 115.0F, 110.0F};
    private static final float[] LEFT_FLAP_DEGREES = {-140.0F, -135.0F, -130.0F, -125.0F, -120.0F, -115.0F, -110.0F};

    private final ModelPart sword;
    private final ModelPart rightWing;
    private final ModelPart leftWing;
    private final ModelPart[] rightFlaps;
    private final ModelPart[] leftFlaps;

    public SusanooWingedModel(ModelPart root) {
        super(root);
        this.sword = this.rightArm.getChild("sword");
        this.rightWing = root.getChild("rightWing");
        this.leftWing = root.getChild("leftWing");
        ModelPart rightWingBone = this.rightWing.getChild("bone3");
        ModelPart leftWingBone = this.leftWing.getChild("bone2");
        this.rightFlaps = new ModelPart[] {
                rightWingBone.getChild("flap1"),
                rightWingBone.getChild("flap2"),
                rightWingBone.getChild("flap3"),
                rightWingBone.getChild("flap4"),
                rightWingBone.getChild("flap5"),
                rightWingBone.getChild("flap6"),
                rightWingBone.getChild("flap7")
        };
        this.leftFlaps = new ModelPart[] {
                leftWingBone.getChild("flap8"),
                leftWingBone.getChild("flap9"),
                leftWingBone.getChild("flap10"),
                leftWingBone.getChild("flap11"),
                leftWingBone.getChild("flap12"),
                leftWingBone.getChild("flap13"),
                leftWingBone.getChild("flap14")
        };
    }

    public static LayerDefinition createBodyLayer() {
        return LegacyModelLayerDefinitions.EntitySusanooWinged_ModelSusanooWinged_435();
    }

    @Override
    public void setupAnim(SusanooWingedEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.leftArmPose = ArmPose.EMPTY;
        this.rightArmPose = entity.getMainHandItem().isEmpty() ? ArmPose.EMPTY : ArmPose.ITEM;
        float adjustedHeadPitch = headPitch;
        float adjustedLimbSwingAmount = limbSwingAmount;
        if (!entity.onGround()) {
            adjustedLimbSwingAmount = 0.0F;
            if (isMovingTowardsLookDirection(entity)) {
                adjustedHeadPitch += flyingBodyRotationAmount(entity) * -90.0F;
            }
        }
        float scaledSwing = limbSwing * 2.0F / Math.max(entity.getBbHeight(), 0.001F);
        super.setupAnim(entity, scaledSwing, adjustedLimbSwingAmount, ageInTicks, netHeadYaw, adjustedHeadPitch);
        if (entity.isSwingingArms()) {
            this.leftArm.yRot = 0.1F + this.head.yRot;
            this.leftArm.xRot = -Mth.HALF_PI + this.head.xRot;
        }
        this.sword.visible = entity.shouldShowSword();
        applyWingSwing(entity.getWingSwingProgress());
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay,
            float red, float green, float blue, float alpha) {
        super.renderToBuffer(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.rightWing.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.leftWing.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    public void renderFlameToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay,
            float red, float green, float blue, float alpha) {
        boolean hatVisible = this.hat.visible;
        this.hat.visible = false;
        renderToBuffer(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.hat.visible = hatVisible;
    }

    private void applyWingSwing(float wingSwingProgress) {
        this.rightWing.zRot = -0.4363F + wingSwingProgress * -30.0F * Mth.DEG_TO_RAD;
        this.leftWing.zRot = 0.4363F + wingSwingProgress * 30.0F * Mth.DEG_TO_RAD;
        for (int i = 0; i < this.rightFlaps.length; i++) {
            this.rightFlaps[i].zRot = wingSwingProgress * RIGHT_FLAP_DEGREES[i] * Mth.DEG_TO_RAD;
            this.leftFlaps[i].zRot = wingSwingProgress * LEFT_FLAP_DEGREES[i] * Mth.DEG_TO_RAD;
        }
    }

    private static float flyingBodyRotationAmount(SusanooWingedEntity entity) {
        return Mth.clamp((float)(entity.getSyncedMotionXZ().length() * 1.2D), 0.0F, 1.0F);
    }

    private static boolean isMovingTowardsLookDirection(SusanooWingedEntity entity) {
        Vec3 motion = entity.getSyncedMotionXZ();
        if (motion.lengthSqr() <= 1.0E-12D) {
            return false;
        }
        float motionYaw = ProcedureUtils.getYawFromVec(motion);
        float yawDelta = Math.abs(Mth.wrapDegrees(motionYaw - entity.getSyncedHeadYaw()));
        return yawDelta < 90.0F;
    }
}
