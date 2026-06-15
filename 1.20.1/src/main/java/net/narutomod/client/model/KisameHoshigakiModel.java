package net.narutomod.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.narutomod.NarutomodMod;
import net.narutomod.entity.NinjaMobEntity;
import net.narutomod.item.ItemOnBody;

public final class KisameHoshigakiModel extends EntityModel<NinjaMobEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            NarutomodMod.location("kisame_hoshigaki_legacy"),
            "main"
    );

    private final ModelPart head;
    private final ModelPart hat;
    private final ModelPart hatDetails;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart leftArmNormal;
    private final ModelPart leftArmFused;
    private final ModelPart rightFin;
    private final ModelPart leftFin;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public KisameHoshigakiModel(ModelPart root) {
        this.head = root.getChild("head");
        this.hat = root.getChild("hat");
        this.hatDetails = this.hat.getChild("hat_details");
        this.body = root.getChild("body");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.leftArmNormal = this.leftArm.getChild("left_arm_normal");
        this.leftArmFused = this.leftArm.getChild("left_arm_fused");
        this.rightFin = this.rightArm.getChild("right_fin");
        this.leftFin = this.leftArm.getChild("left_fin");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild(
                "head",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.ZERO);
        PartDefinition hat = root.addOrReplaceChild(
                "hat",
                CubeListBuilder.create()
                        .texOffs(32, 0)
                        .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.5F)),
                PartPose.ZERO);
        hat.addOrReplaceChild(
                "hat_details",
                CubeListBuilder.create()
                        .texOffs(40, 8)
                        .addBox(-3.0F, -9.25F, -4.75F, 6.0F, 1.0F, 1.0F, new CubeDeformation(-0.2F))
                        .texOffs(40, 8)
                        .addBox(-3.0F, -10.0F, -5.0F, 6.0F, 2.0F, 1.0F, new CubeDeformation(-0.4F))
                        .texOffs(40, 8)
                        .addBox(-3.0F, -10.5F, -5.25F, 6.0F, 2.0F, 1.0F, new CubeDeformation(-0.6F)),
                PartPose.ZERO);
        root.addOrReplaceChild(
                "body",
                CubeListBuilder.create()
                        .texOffs(16, 16)
                        .addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(16, 32)
                        .addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.25F)),
                PartPose.ZERO);
        PartDefinition rightArm = root.addOrReplaceChild(
                "right_arm",
                CubeListBuilder.create()
                        .texOffs(40, 16)
                        .addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(40, 32)
                        .addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.25F)),
                PartPose.offset(-5.0F, 2.0F, 0.0F));
        rightArm.addOrReplaceChild(
                "right_fin",
                CubeListBuilder.create()
                        .texOffs(32, 52)
                        .addBox(0.0F, -3.0F, 0.0F, 0.0F, 6.0F, 6.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-3.0F, 5.0F, 2.0F, 0.0F, -0.7854F, 0.0F));
        PartDefinition leftArm = root.addOrReplaceChild(
                "left_arm",
                CubeListBuilder.create(),
                PartPose.offset(5.0F, 2.0F, 0.0F));
        leftArm.addOrReplaceChild(
                "left_arm_normal",
                CubeListBuilder.create()
                        .texOffs(32, 48)
                        .addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(48, 48)
                        .addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.25F)),
                PartPose.ZERO);
        leftArm.addOrReplaceChild(
                "left_arm_fused",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(40, 16)
                        .addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(40, 32)
                        .addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.25F))
                        .mirror(false),
                PartPose.ZERO);
        leftArm.addOrReplaceChild(
                "left_fin",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(32, 52)
                        .addBox(0.0F, -3.0F, 0.0F, 0.0F, 6.0F, 6.0F, new CubeDeformation(0.0F))
                        .mirror(false),
                PartPose.offsetAndRotation(3.0F, 5.0F, 2.0F, 0.0F, 0.7854F, 0.0F));
        root.addOrReplaceChild(
                "right_leg",
                CubeListBuilder.create()
                        .texOffs(0, 16)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 32)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.25F)),
                PartPose.offset(-1.9F, 12.0F, 0.0F));
        root.addOrReplaceChild(
                "left_leg",
                CubeListBuilder.create()
                        .texOffs(16, 48)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 48)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.25F)),
                PartPose.offset(1.9F, 12.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(NinjaMobEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.rightArm.xRot = Mth.cos(limbSwing * 0.6662F + Mth.PI) * 2.0F * limbSwingAmount * 0.5F;
        this.leftArm.xRot = Mth.cos(limbSwing * 0.6662F) * 2.0F * limbSwingAmount * 0.5F;
        this.rightLeg.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        this.leftLeg.xRot = Mth.cos(limbSwing * 0.6662F + Mth.PI) * 1.4F * limbSwingAmount;
        this.head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        this.head.xRot = headPitch * Mth.DEG_TO_RAD;
        this.hat.copyFrom(this.head);
        boolean fused = entity.isLegacyKisameFusedForRender();
        this.hat.visible = true;
        this.hatDetails.visible = !fused;
        this.leftArmNormal.visible = !fused;
        this.leftArmFused.visible = fused;
        this.rightFin.visible = fused;
        this.leftFin.visible = fused;
    }

    public void translateToBodyPart(PoseStack poseStack, ItemOnBody.BodyPart bodyPart) {
        ModelPart part = switch (bodyPart) {
            case HEAD -> this.head;
            case TORSO -> this.body;
            case RIGHT_ARM -> this.rightArm;
            case LEFT_ARM -> this.leftArm;
            case RIGHT_LEG -> this.rightLeg;
            case LEFT_LEG -> this.leftLeg;
            case NONE -> this.body;
        };
        part.translateAndRotate(poseStack);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        this.head.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.hat.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.body.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.rightArm.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.leftArm.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.rightLeg.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.leftLeg.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
