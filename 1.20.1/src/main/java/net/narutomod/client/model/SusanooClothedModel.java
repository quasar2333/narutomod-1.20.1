package net.narutomod.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.util.Mth;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.generated.LegacyModelLayerDefinitions;
import net.narutomod.entity.SusanooClothedEntity;

public final class SusanooClothedModel extends HumanoidModel<SusanooClothedEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            NarutomodMod.location("susanooclothed"),
            "main"
    );

    private final ModelPart sword;

    public SusanooClothedModel(ModelPart root) {
        super(root);
        this.sword = this.rightArm.getChild("bone3").getChild("bone").getChild("sword");
    }

    public static LayerDefinition createBodyLayer() {
        return LegacyModelLayerDefinitions.EntitySusanooClothed_ModelSusanooClothed_537();
    }

    @Override
    public void setupAnim(SusanooClothedEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.leftArmPose = ArmPose.EMPTY;
        this.rightArmPose = ArmPose.EMPTY;
        float scaledSwing = limbSwing * 2.0F / Math.max(entity.getBbHeight(), 0.001F);
        super.setupAnim(entity, scaledSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        if (entity.isSwingingArms()) {
            this.leftArm.yRot = 0.1F + this.head.yRot;
            this.leftArm.xRot = -Mth.HALF_PI + this.head.xRot;
        }
        this.rightLeg.visible = entity.hasLegs();
        this.leftLeg.visible = entity.hasLegs();
        this.sword.visible = entity.shouldShowSword();
    }

    public void renderFlameToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        boolean hatVisible = this.hat.visible;
        this.hat.visible = false;
        super.renderToBuffer(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.hat.visible = hatVisible;
    }
}
