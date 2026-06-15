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
import net.narutomod.entity.ExplosiveClayEntity;

public final class ClayC3Model extends EntityModel<ExplosiveClayEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        NarutomodMod.location("clay_c3_legacy"),
        "main"
    );

    private static final float GROW_TIME = 30.0F;
    private static final float FUSE_TIME = 100.0F;

    private final ModelPart body;
    private final ModelPart leftWing;
    private final ModelPart leftWingTip;
    private final ModelPart rightWing;
    private final ModelPart rightWingTip;

    public ClayC3Model(ModelPart root) {
        this.body = root.getChild("body");
        this.leftWing = this.body.getChild("left_wing");
        this.leftWingTip = this.leftWing.getChild("left_wing_tip");
        this.rightWing = this.body.getChild("right_wing");
        this.rightWingTip = this.rightWing.getChild("right_wing_tip");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild(
            "body",
            CubeListBuilder.create()
                .texOffs(0, 22)
                .addBox(-5.0F, 4.0F, -5.0F, 10.0F, 10.0F, 10.0F, CubeDeformation.NONE)
                .texOffs(0, 0)
                .addBox(-6.0F, 14.0F, -6.0F, 12.0F, 10.0F, 12.0F, CubeDeformation.NONE),
            PartPose.ZERO
        );
        PartDefinition leftWing = body.addOrReplaceChild(
            "left_wing",
            CubeListBuilder.create()
                .texOffs(0, 42)
                .addBox(0.0F, -2.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.4F)),
            PartPose.offsetAndRotation(5.0F, 8.0F, 0.0F, -0.6981F, 0.0F, 0.0F)
        );
        PartDefinition leftWingTip = leftWing.addOrReplaceChild(
            "left_wing_tip",
            CubeListBuilder.create()
                .texOffs(48, 12)
                .addBox(-2.5F, -2.0F, -1.5F, 5.0F, 6.0F, 3.0F, new CubeDeformation(0.4F)),
            PartPose.offsetAndRotation(2.0F, 4.0F, 0.0F, 0.0F, 0.0F, 0.7854F)
        );
        leftWingTip.addOrReplaceChild(
            "left_wing_tip_1",
            CubeListBuilder.create()
                .texOffs(0, 54)
                .addBox(-1.0F, -4.0F, -1.0F, 2.0F, 8.0F, 2.0F, new CubeDeformation(0.4F)),
            PartPose.offsetAndRotation(2.5F, 7.0F, 0.0F, 0.0F, 0.0F, -0.2618F)
        );
        leftWingTip.addOrReplaceChild(
            "left_wing_tip_2",
            CubeListBuilder.create()
                .texOffs(0, 54)
                .addBox(-1.0F, -4.0F, -1.0F, 2.0F, 8.0F, 2.0F, new CubeDeformation(0.4F)),
            PartPose.offset(0.0F, 7.0F, 0.0F)
        );
        leftWingTip.addOrReplaceChild(
            "left_wing_tip_3",
            CubeListBuilder.create()
                .texOffs(0, 54)
                .addBox(-1.0F, -4.0F, -1.0F, 2.0F, 8.0F, 2.0F, new CubeDeformation(0.4F)),
            PartPose.offsetAndRotation(-2.5F, 7.0F, 0.0F, 0.0F, 0.0F, 0.2618F)
        );
        PartDefinition rightWing = body.addOrReplaceChild(
            "right_wing",
            mirroredBox(0, 42, -4.0F, -2.0F, -2.0F, 4.0F, 6.0F, 4.0F, 0.4F),
            PartPose.offsetAndRotation(-5.0F, 8.0F, 0.0F, -0.8727F, 0.0F, 0.0F)
        );
        PartDefinition rightWingTip = rightWing.addOrReplaceChild(
            "right_wing_tip",
            mirroredBox(48, 12, -2.5F, -2.0F, -1.5F, 5.0F, 6.0F, 3.0F, 0.4F),
            PartPose.offsetAndRotation(-2.0F, 4.0F, 0.0F, 0.0F, 0.0F, -0.7854F)
        );
        rightWingTip.addOrReplaceChild(
            "right_wing_tip_1",
            mirroredBox(0, 54, -1.0F, -4.0F, -1.0F, 2.0F, 8.0F, 2.0F, 0.4F),
            PartPose.offsetAndRotation(-2.5F, 7.0F, 0.0F, 0.0F, 0.0F, 0.2618F)
        );
        rightWingTip.addOrReplaceChild(
            "right_wing_tip_2",
            mirroredBox(0, 54, -1.0F, -4.0F, -1.0F, 2.0F, 8.0F, 2.0F, 0.4F),
            PartPose.offset(0.0F, 7.0F, 0.0F)
        );
        rightWingTip.addOrReplaceChild(
            "right_wing_tip_3",
            mirroredBox(0, 54, -1.0F, -4.0F, -1.0F, 2.0F, 8.0F, 2.0F, 0.4F),
            PartPose.offsetAndRotation(2.5F, 7.0F, 0.0F, 0.0F, 0.0F, -0.2618F)
        );
        body.addOrReplaceChild(
            "head",
            CubeListBuilder.create()
                .texOffs(44, 31)
                .addBox(-2.5F, -3.5F, -2.5F, 5.0F, 6.0F, 5.0F, CubeDeformation.NONE),
            PartPose.offset(0.0F, 4.5F, -4.5F)
        );
        PartDefinition hump = body.addOrReplaceChild(
            "hump",
            CubeListBuilder.create()
                .texOffs(30, 22)
                .addBox(-4.5F, -2.0F, -3.0F, 9.0F, 2.0F, 7.0F, CubeDeformation.NONE),
            PartPose.offset(0.0F, 4.0F, -0.5F)
        );
        hump.addOrReplaceChild(
            "hump_box",
            CubeListBuilder.create()
                .texOffs(36, 0)
                .addBox(-3.0F, -3.0F, -2.05F, 6.0F, 6.0F, 6.0F, CubeDeformation.NONE),
            PartPose.offsetAndRotation(0.0F, -2.0F, -0.45F, 0.0F, 0.0F, 0.7854F)
        );
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(ExplosiveClayEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
        if (ageInTicks >= GROW_TIME) {
            float swing = Mth.clamp((ageInTicks - GROW_TIME) / (FUSE_TIME - GROW_TIME), 0.0F, 1.0F);
            this.leftWing.xRot = (swing - 1.0F) * 0.6981F;
            this.leftWing.zRot = swing * -1.7453F;
            this.leftWingTip.zRot = (1.0F - swing) * 0.7854F;
            this.rightWing.xRot = (swing - 1.0F) * 0.8727F;
            this.rightWing.zRot = swing * 1.7453F;
            this.rightWingTip.zRot = (swing - 1.0F) * 0.7854F;
        } else {
            this.leftWing.xRot = -0.6981F;
            this.leftWing.zRot = 0.0F;
            this.leftWingTip.zRot = 0.7854F;
            this.rightWing.xRot = -0.8727F;
            this.rightWing.zRot = 0.0F;
            this.rightWingTip.zRot = -0.7854F;
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay,
            float red, float green, float blue, float alpha) {
        this.body.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    private static CubeListBuilder mirroredBox(int u, int v, float x, float y, float z, float width, float height,
            float depth, float deformation) {
        return CubeListBuilder.create()
            .texOffs(u, v)
            .mirror()
            .addBox(x, y, z, width, height, depth, new CubeDeformation(deformation))
            .mirror(false);
    }
}
