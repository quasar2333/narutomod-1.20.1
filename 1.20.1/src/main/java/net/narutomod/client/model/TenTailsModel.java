package net.narutomod.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Random;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.generated.LegacyModelLayerDefinitions;
import net.narutomod.entity.TenTailsEntity;

public final class TenTailsModel extends EntityModel<TenTailsEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            NarutomodMod.location("ten_tails"),
            "main"
    );

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart hat;
    private final ModelPart jaw;
    private final ModelPart body;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;
    private final ModelPart[][] tails = new ModelPart[10][10];
    private final float[][] tailSwayX = new float[10][10];
    private final float[][] tailSwayZ = new float[10][10];

    public TenTailsModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.hat = root.getChild("hat");
        this.jaw = this.head.getChild("jaw");
        this.body = root.getChild("body");
        this.leftArm = root.getChild("left_arm");
        this.rightArm = root.getChild("right_arm");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg.visible = false;
        this.rightLeg.visible = false;

        Random random = new Random();
        for (int i = 0; i < this.tails.length; i++) {
            ModelPart segment = root.getChild("Tail_" + i + "_0");
            this.tails[i][0] = segment;
            for (int j = 1; j < this.tails[i].length; j++) {
                segment = segment.getChild("Tail_" + i + "_" + j);
                this.tails[i][j] = segment;
                this.tailSwayX[i][j] = (random.nextFloat() * 0.1309F + 0.1309F) * (random.nextBoolean() ? -1.0F : 1.0F);
                this.tailSwayZ[i][j] = (random.nextFloat() * 0.1309F + 0.1309F) * (random.nextBoolean() ? -1.0F : 1.0F);
            }
        }
    }

    public static LayerDefinition createBodyLayer() {
        return LegacyModelLayerDefinitions.EntityTenTails_ModelTenTailsV1_1003();
    }

    @Override
    public void setupAnim(TenTailsEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
        this.root.getAllParts().forEach(ModelPart::resetPose);
        this.leftLeg.visible = false;
        this.rightLeg.visible = false;

        applyLegacyBipedAnimation(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        if (entity.isShooting()) {
            this.head.xRot -= 0.3491F;
            this.hat.xRot -= 0.3491F;
            this.jaw.xRot = 0.5236F;
        } else {
            this.jaw.xRot = -0.3491F;
        }

        for (int i = 0; i < this.tails.length; i++) {
            for (int j = 1; j < this.tails[i].length; j++) {
                this.tails[i][j].xRot = (j < 8 ? 0.2618F : -0.1745F)
                        + Mth.sin((ageInTicks - j) * 0.1F) * this.tailSwayX[i][j];
                this.tails[i][j].zRot = Mth.cos((ageInTicks - j) * 0.1F) * this.tailSwayZ[i][j];
            }
        }
    }

    private void applyLegacyBipedAnimation(float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
        float scaledSwing = limbSwing * 2.0F / TenTailsEntity.HEIGHT;
        this.head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        this.head.xRot = headPitch * Mth.DEG_TO_RAD;
        this.head.y = 16.0F;
        this.hat.yRot = this.head.yRot;
        this.hat.xRot = this.head.xRot;
        this.hat.y = 16.0F;

        this.body.yRot = 0.0F;
        this.rightArm.yRot = 0.0F;
        this.leftArm.yRot = 0.0F;
        this.rightArm.zRot = 0.0F;
        this.leftArm.zRot = 0.0F;
        this.rightLeg.yRot = 0.0F;
        this.leftLeg.yRot = 0.0F;

        this.rightArm.xRot = Mth.cos(scaledSwing * 0.6662F + Mth.PI) * 2.0F * limbSwingAmount * 0.5F;
        this.leftArm.xRot = Mth.cos(scaledSwing * 0.6662F) * 2.0F * limbSwingAmount * 0.5F;
        this.rightLeg.xRot = Mth.cos(scaledSwing * 0.6662F) * 1.4F * limbSwingAmount;
        this.leftLeg.xRot = Mth.cos(scaledSwing * 0.6662F + Mth.PI) * 1.4F * limbSwingAmount;
        this.rightLeg.zRot = 0.0F;
        this.leftLeg.zRot = 0.0F;

        this.rightArm.zRot += Mth.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
        this.leftArm.zRot -= Mth.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
        this.rightArm.xRot += Mth.sin(ageInTicks * 0.067F) * 0.05F;
        this.leftArm.xRot -= Mth.sin(ageInTicks * 0.067F) * 0.05F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay,
            float red, float green, float blue, float alpha) {
        boolean hatVisible = this.hat.visible;
        this.hat.visible = false;
        this.root.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.hat.visible = hatVisible;
        if (hatVisible) {
            this.hat.render(poseStack, consumer, LightTexture.FULL_BRIGHT, packedOverlay, red, green, blue, alpha);
        }
    }
}
