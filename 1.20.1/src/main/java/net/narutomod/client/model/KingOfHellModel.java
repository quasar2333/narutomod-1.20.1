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
import net.narutomod.entity.KingOfHellEntity;

public final class KingOfHellModel extends EntityModel<KingOfHellEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            NarutomodMod.location("kingofhellentity"),
            "main"
    );

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart maskRight;
    private final ModelPart maskLeft;
    private final ModelPart crown;
    private final ModelPart collarOuter;
    private final ModelPart collarInner;

    public KingOfHellModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.maskRight = this.head.getChild("bone3").getChild("mask_right");
        this.maskLeft = this.head.getChild("bone4").getChild("mask_left");
        this.crown = root.getChild("crown");
        this.collarOuter = root.getChild("collar_outer");
        this.collarInner = root.getChild("collar_inner");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild(
                "head",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-8.0F, -24.0F, -8.0F, 16.0F, 24.0F, 16.0F, CubeDeformation.NONE)
                        .texOffs(39, 117)
                        .addBox(-2.5F, -28.5F, -2.5F, 5.0F, 5.0F, 5.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, 24.0F, 0.0F)
        );

        PartDefinition bone3 = head.addOrReplaceChild(
                "bone3",
                CubeListBuilder.create()
                        .texOffs(81, 101)
                        .addBox(-0.1F, -15.0F, -0.1F, 0.0F, 19.0F, 16.0F, CubeDeformation.NONE),
                PartPose.offset(-8.0F, 4.0F, -8.0F)
        );
        bone3.addOrReplaceChild(
                "mask_right",
                CubeListBuilder.create()
                        .texOffs(64, 16)
                        .addBox(0.0F, -15.0F, 0.0F, 8.0F, 19.0F, 0.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-0.1F, 0.0F, 0.0F, 0.0F, 0.0873F, 0.0F)
        );

        PartDefinition bone4 = head.addOrReplaceChild(
                "bone4",
                CubeListBuilder.create().mirror()
                        .texOffs(64, 0)
                        .addBox(0.1F, -15.0F, -0.1F, 0.0F, 19.0F, 16.0F, CubeDeformation.NONE),
                PartPose.offset(8.0F, 4.0F, -8.0F)
        );
        bone4.addOrReplaceChild(
                "mask_left",
                CubeListBuilder.create().mirror()
                        .texOffs(64, 16)
                        .addBox(-8.0F, -15.0F, 0.0F, 8.0F, 19.0F, 0.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.1F, 0.0F, 0.0F, 0.0F, -0.0873F, 0.0F)
        );

        PartDefinition crown = root.addOrReplaceChild(
                "crown",
                CubeListBuilder.create()
                        .texOffs(72, 38)
                        .addBox(-9.0F, -9.0F, -9.0F, 18.0F, 12.0F, 18.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, 5.0F, 0.0F)
        );
        addCrownHorns(crown);

        PartDefinition collarOuter = root.addOrReplaceChild(
                "collar_outer",
                CubeListBuilder.create()
                        .texOffs(0, 40)
                        .addBox(-4.0F, -21.0F, 0.0F, 8.0F, 28.0F, 0.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.0F, 25.0F, 11.1F, -0.4363F, 0.0F, 0.0F)
        );
        addCollarPanels(collarOuter, 40);

        PartDefinition collarInner = root.addOrReplaceChild(
                "collar_inner",
                CubeListBuilder.create()
                        .texOffs(0, 72)
                        .addBox(-4.0F, -21.0F, 0.0F, 8.0F, 28.0F, 0.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.0F, 25.0F, 11.0F, -0.4363F, 0.0F, 0.0F)
        );
        addCollarPanels(collarInner, 72);

        return LayerDefinition.create(mesh, 144, 144);
    }

    @Override
    public void setupAnim(KingOfHellEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float yaw = netHeadYaw * Mth.DEG_TO_RAD;
        this.head.yRot = yaw;
        this.crown.yRot = yaw;
        this.collarOuter.yRot = yaw;
        this.collarInner.yRot = yaw;
        float mask = Mth.sin(entity.getMaskOpenAmount(0.0F) * Mth.HALF_PI);
        this.maskRight.yRot = 0.0873F + mask * 2.0F;
        this.maskLeft.yRot = -0.0873F - mask * 2.0F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        this.root.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    private static void addCrownHorns(PartDefinition crown) {
        PartDefinition bone5 = crown.addOrReplaceChild(
                "bone5",
                CubeListBuilder.create().texOffs(127, 28).addBox(-0.5F, -8.0F, -0.5F, 1.0F, 8.0F, 0.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-5.0F, 2.0F, -8.5F, 0.0F, -0.4363F, 0.0F)
        );
        PartDefinition bone6 = bone5.addOrReplaceChild(
                "bone6",
                CubeListBuilder.create().texOffs(127, 25).addBox(-0.5845F, -8.2961F, -1.0524F, 1.0F, 8.0F, 0.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.0F, -7.0F, 0.0F, -0.7854F, 0.0F, 0.0F)
        );
        PartDefinition bone7 = bone6.addOrReplaceChild(
                "bone7",
                CubeListBuilder.create().texOffs(127, 25).addBox(-0.5F, -8.0F, -1.6F, 1.0F, 8.0F, 0.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.0F, -7.0F, 0.0F, -0.7854F, 0.0F, 0.0F)
        );
        bone7.addOrReplaceChild(
                "bone8",
                CubeListBuilder.create()
                        .texOffs(127, 25)
                        .addBox(-0.5F, -7.2929F, -1.2071F, 1.0F, 8.0F, 0.0F, CubeDeformation.NONE)
                        .texOffs(127, 25)
                        .addBox(-0.5F, -14.364F, -1.2071F, 1.0F, 8.0F, 0.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.0F, -8.0F, -0.2F, -0.5236F, 0.0F, 0.0F)
        );

        PartDefinition bone10 = crown.addOrReplaceChild(
                "bone10",
                CubeListBuilder.create().mirror().texOffs(127, 28).addBox(-0.5F, -8.0F, -0.5F, 1.0F, 8.0F, 0.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(4.75F, 2.0F, -8.5F, 0.0F, 0.4363F, 0.0F)
        );
        PartDefinition bone11 = bone10.addOrReplaceChild(
                "bone11",
                CubeListBuilder.create().mirror().texOffs(127, 25).addBox(-0.4155F, -8.2961F, -1.0524F, 1.0F, 8.0F, 0.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.0F, -7.0F, 0.0F, -0.7854F, 0.0F, 0.0F)
        );
        PartDefinition bone12 = bone11.addOrReplaceChild(
                "bone12",
                CubeListBuilder.create().mirror().texOffs(127, 25).addBox(-0.5F, -8.0F, -1.6F, 1.0F, 8.0F, 0.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.0F, -7.0F, 0.0F, -0.7854F, 0.0F, 0.0F)
        );
        bone12.addOrReplaceChild(
                "bone13",
                CubeListBuilder.create().mirror()
                        .texOffs(127, 25)
                        .addBox(-0.5F, -7.2929F, -1.2071F, 1.0F, 8.0F, 0.0F, CubeDeformation.NONE)
                        .texOffs(127, 25)
                        .addBox(-0.5F, -14.364F, -1.2071F, 1.0F, 8.0F, 0.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.0F, -8.0F, -0.2F, -0.5236F, 0.0F, 0.0F)
        );
    }

    private static void addCollarPanels(PartDefinition collar, int texY) {
        PartDefinition leftA = collar.addOrReplaceChild(
                "left_a_" + texY,
                CubeListBuilder.create().texOffs(16, texY).addBox(-8.0F, -21.0F, 0.0F, 8.0F, 30.0F, 0.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-4.0F, 0.0F, 0.0F, 0.0F, -0.6981F, 0.0F)
        );
        leftA.addOrReplaceChild(
                "left_b_" + texY,
                CubeListBuilder.create().texOffs(32, texY).addBox(-12.0F, -21.0F, 0.0F, 12.0F, 32.0F, 0.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-8.0F, 0.0F, 0.0F, 0.0F, -0.5236F, 0.0F)
        );
        PartDefinition rightA = collar.addOrReplaceChild(
                "right_a_" + texY,
                CubeListBuilder.create().mirror().texOffs(16, texY).addBox(0.0F, -21.0F, 0.0F, 8.0F, 30.0F, 0.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(4.0F, 0.0F, 0.0F, 0.0F, 0.6981F, 0.0F)
        );
        rightA.addOrReplaceChild(
                "right_b_" + texY,
                CubeListBuilder.create().mirror().texOffs(32, texY).addBox(0.0F, -21.0F, 0.0F, 12.0F, 32.0F, 0.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(8.0F, 0.0F, 0.0F, 0.0F, 0.5236F, 0.0F)
        );
    }
}
