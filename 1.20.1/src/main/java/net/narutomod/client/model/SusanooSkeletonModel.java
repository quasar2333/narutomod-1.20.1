package net.narutomod.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.generated.LegacyModelLayerDefinitions;
import net.narutomod.entity.SusanooSkeletonEntity;

public final class SusanooSkeletonModel extends HumanoidModel<SusanooSkeletonEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            NarutomodMod.location("susanooskeleton"),
            "main"
    );

    private static final float OLD_MODEL_HEIGHT = SusanooSkeletonEntity.HEIGHT;
    private final ModelPart hornStyle1;

    public SusanooSkeletonModel(ModelPart root) {
        super(root);
        this.hornStyle1 = this.head.getChild("HornStyle1");
    }

    public static LayerDefinition createBodyLayer() {
        return LegacyModelLayerDefinitions.EntitySusanooSkeleton_ModelSusanooSkeleton_135();
    }

    @Override
    public void setupAnim(SusanooSkeletonEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing * 2.0F / OLD_MODEL_HEIGHT, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        this.head.y += -8.0F;
        this.hat.copyFrom(this.head);
        this.body.y = -8.0F;
        this.rightArm.y = -7.0F;
        this.leftArm.y = -7.0F;
        this.rightArm.z += -1.0F;
        this.rightArm.x += -12.0F;
        this.leftArm.z += -1.0F;
        this.leftArm.x += 12.0F;
        this.rightLeg.visible = false;
        this.leftLeg.visible = false;
        this.hornStyle1.visible = false;
    }

    public void renderFlameToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        boolean bodyVisible = this.body.visible;
        boolean hatVisible = this.hat.visible;
        boolean rightLegVisible = this.rightLeg.visible;
        boolean leftLegVisible = this.leftLeg.visible;
        boolean hornStyle1Visible = this.hornStyle1.visible;
        this.body.visible = false;
        this.hat.visible = false;
        this.rightLeg.visible = false;
        this.leftLeg.visible = false;
        this.hornStyle1.visible = false;
        super.renderToBuffer(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.body.visible = bodyVisible;
        this.hat.visible = hatVisible;
        this.rightLeg.visible = rightLegVisible;
        this.leftLeg.visible = leftLegVisible;
        this.hornStyle1.visible = hornStyle1Visible;
    }
}
