package net.narutomod.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.util.Mth;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.generated.LegacyModelLayerDefinitions;
import net.narutomod.entity.Buddha1000Entity;

public final class Buddha1000Model extends EntityModel<Buddha1000Entity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        NarutomodMod.location("buddha_1000_legacy"),
        "main"
    );

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart hat;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart rightForeArm;
    private final ModelPart leftArm;
    private final ModelPart leftForeArm;
    private final ModelPart rightLeg;
    private final ModelPart rightCalf;
    private final ModelPart leftLeg;
    private final ModelPart leftCalf;
    private final ModelPart armStand;
    private boolean firstPersonRiderView;

    public Buddha1000Model(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.hat = root.getChild("hat");
        this.body = root.getChild("body");
        this.rightArm = root.getChild("right_arm");
        this.rightForeArm = this.rightArm.getChild("rightForeArm");
        this.leftArm = root.getChild("left_arm");
        this.leftForeArm = this.leftArm.getChild("leftForeArm");
        this.rightLeg = root.getChild("right_leg");
        this.rightCalf = this.rightLeg.getChild("rightCalf");
        this.leftLeg = root.getChild("left_leg");
        this.leftCalf = this.leftLeg.getChild("leftCalf");
        this.armStand = root.getChild("armStand");
    }

    public static LayerDefinition createBodyLayer() {
        return LegacyModelLayerDefinitions.EntityBuddha1000_ModelBudha1000_616();
    }

    public void setFirstPersonRiderView(boolean firstPersonRiderView) {
        this.firstPersonRiderView = firstPersonRiderView;
    }

    @Override
    public void setupAnim(Buddha1000Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root.getAllParts().forEach(part -> {
            part.resetPose();
            part.visible = true;
        });
        this.armStand.visible = entity.isSitting();
        applyLegacyBipedAnimation(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        this.head.visible = !this.firstPersonRiderView;
        this.hat.visible = !this.firstPersonRiderView;
        if (entity.isSitting()) {
            poseSitting();
        }
        this.hat.copyFrom(this.head);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        this.root.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    private void poseSitting() {
        this.rightArm.xRot = -1.0472F;
        this.rightArm.yRot = -0.4363F;
        this.rightArm.zRot = 0.0F;
        this.rightForeArm.xRot = 0.0F;
        this.rightForeArm.yRot = 0.0F;
        this.rightForeArm.zRot = -1.0472F;
        this.leftArm.xRot = -1.0472F;
        this.leftArm.yRot = 0.4363F;
        this.leftArm.zRot = 0.0F;
        this.leftForeArm.xRot = 0.0F;
        this.leftForeArm.yRot = 0.0F;
        this.leftForeArm.zRot = 1.0472F;
        this.rightLeg.xRot = -2.0071F;
        this.rightLeg.yRot = 0.1745F;
        this.rightLeg.zRot = -1.309F;
        this.rightCalf.xRot = 1.5272F;
        this.rightCalf.yRot = 0.0F;
        this.rightCalf.zRot = 0.0F;
        this.leftLeg.xRot = -2.0071F;
        this.leftLeg.yRot = -0.1745F;
        this.leftLeg.zRot = 1.309F;
        this.leftCalf.xRot = 1.5272F;
        this.leftCalf.yRot = 0.0F;
        this.leftCalf.zRot = 0.0F;
    }

    private void applyLegacyBipedAnimation(Buddha1000Entity entity, float limbSwing, float limbSwingAmount,
            float ageInTicks, float netHeadYaw, float headPitch) {
        float scaledSwing = limbSwing * 2.0F / Math.max(entity.getBbHeight(), 0.001F);
        this.head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        this.head.xRot = headPitch * Mth.DEG_TO_RAD;
        this.body.yRot = 0.0F;

        this.rightArm.x = -5.0F;
        this.rightArm.z = 0.0F;
        this.leftArm.x = 5.0F;
        this.leftArm.z = 0.0F;
        this.rightArm.yRot = 0.0F;
        this.leftArm.yRot = 0.0F;
        this.rightArm.zRot = 0.0F;
        this.leftArm.zRot = 0.0F;
        this.rightForeArm.xRot = 0.0F;
        this.rightForeArm.yRot = 0.0F;
        this.rightForeArm.zRot = 0.0F;
        this.leftForeArm.xRot = 0.0F;
        this.leftForeArm.yRot = 0.0F;
        this.leftForeArm.zRot = 0.0F;
        this.rightLeg.yRot = 0.0F;
        this.leftLeg.yRot = 0.0F;
        this.rightLeg.zRot = 0.0F;
        this.leftLeg.zRot = 0.0F;
        this.rightCalf.xRot = 0.0F;
        this.rightCalf.yRot = 0.0F;
        this.rightCalf.zRot = 0.0F;
        this.leftCalf.xRot = 0.0F;
        this.leftCalf.yRot = 0.0F;
        this.leftCalf.zRot = 0.0F;

        this.rightArm.xRot = Mth.cos(scaledSwing * 0.6662F + Mth.PI) * 2.0F * limbSwingAmount * 0.5F;
        this.leftArm.xRot = Mth.cos(scaledSwing * 0.6662F) * 2.0F * limbSwingAmount * 0.5F;
        this.rightLeg.xRot = Mth.cos(scaledSwing * 0.6662F) * 1.4F * limbSwingAmount;
        this.leftLeg.xRot = Mth.cos(scaledSwing * 0.6662F + Mth.PI) * 1.4F * limbSwingAmount;

        applyLegacySwingAnimation();

        this.rightArm.zRot += Mth.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
        this.leftArm.zRot -= Mth.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
        this.rightArm.xRot += Mth.sin(ageInTicks * 0.067F) * 0.05F;
        this.leftArm.xRot -= Mth.sin(ageInTicks * 0.067F) * 0.05F;
    }

    private void applyLegacySwingAnimation() {
        float swing = this.attackTime;
        if (swing <= 0.0F) {
            return;
        }
        this.body.yRot = Mth.sin(Mth.sqrt(swing) * Mth.TWO_PI) * 0.2F;
        this.rightArm.z = Mth.sin(this.body.yRot) * 5.0F;
        this.rightArm.x = -Mth.cos(this.body.yRot) * 5.0F;
        this.leftArm.z = -Mth.sin(this.body.yRot) * 5.0F;
        this.leftArm.x = Mth.cos(this.body.yRot) * 5.0F;
        this.rightArm.yRot += this.body.yRot;
        this.leftArm.yRot += this.body.yRot;
        this.leftArm.xRot += this.body.yRot;

        float eased = 1.0F - swing;
        eased *= eased;
        eased *= eased;
        eased = 1.0F - eased;
        float swingSin = Mth.sin(eased * Mth.PI);
        float headAdjustedSin = Mth.sin(swing * Mth.PI) * -(this.head.xRot - 0.7F) * 0.75F;
        this.rightArm.xRot -= swingSin * 1.2F + headAdjustedSin;
        this.rightArm.yRot += this.body.yRot * 2.0F;
        this.rightArm.zRot += Mth.sin(swing * Mth.PI) * -0.4F;
    }
}
