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
import net.narutomod.entity.GiantDog2hEntity;

public final class GiantDog2hModel extends EntityModel<GiantDog2hEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            NarutomodMod.location("giant_dog_2h"),
            "main"
    );

    private final ModelPart root;
    private final ModelPart headRight;
    private final ModelPart headLeft;
    private final ModelPart leg0;
    private final ModelPart leg1;
    private final ModelPart leg2;
    private final ModelPart leg3;
    private final ModelPart tail;

    public GiantDog2hModel(ModelPart root) {
        this.root = root;
        this.headRight = root.getChild("head_right");
        this.headLeft = root.getChild("head_left");
        this.leg0 = root.getChild("leg0");
        this.leg1 = root.getChild("leg1");
        this.leg2 = root.getChild("leg2");
        this.leg3 = root.getChild("leg3");
        this.tail = root.getChild("tail");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild(
                "head_right",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-2.0F, -3.0F, -2.0F, 5.0F, 6.0F, 4.0F, CubeDeformation.NONE)
                        .texOffs(0, 10)
                        .addBox(-1.0F, -0.0156F, -5.0F, 3.0F, 3.0F, 4.0F, CubeDeformation.NONE)
                        .texOffs(16, 11)
                        .addBox(0.0539F, -2.0F, -4.0F, 1.0F, 2.0F, 1.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-4.0F, 13.5F, -7.0F, 0.0F, 0.5236F, 0.0F)
        );
        root.addOrReplaceChild(
                "head_left",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-3.0F, -3.0F, -2.0F, 5.0F, 6.0F, 4.0F, CubeDeformation.NONE)
                        .texOffs(0, 10)
                        .addBox(-2.0F, -0.0156F, -5.0F, 3.0F, 3.0F, 4.0F, CubeDeformation.NONE)
                        .texOffs(16, 11)
                        .addBox(-1.0F, -2.0F, -4.0F, 1.0F, 2.0F, 1.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(2.0F, 13.5F, -7.0F, 0.0F, -0.5236F, 0.0F)
        );
        root.addOrReplaceChild(
                "body",
                CubeListBuilder.create()
                        .texOffs(18, 14)
                        .addBox(-4.0F, -2.0F, -3.0F, 6.0F, 9.0F, 6.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.0F, 14.0F, 2.0F, 1.5708F, 0.0F, 0.0F)
        );
        root.addOrReplaceChild(
                "upper_body",
                CubeListBuilder.create()
                        .texOffs(21, 0)
                        .addBox(-4.0F, 2.0F, -4.0F, 8.0F, 6.0F, 7.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-1.0F, 14.0F, 2.0F, -1.5708F, 0.0F, 0.0F)
        );
        root.addOrReplaceChild(
                "leg0",
                CubeListBuilder.create()
                        .texOffs(0, 18)
                        .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F, CubeDeformation.NONE),
                PartPose.offset(-2.5F, 16.0F, 7.0F)
        );
        root.addOrReplaceChild(
                "leg1",
                CubeListBuilder.create()
                        .texOffs(0, 18)
                        .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F, CubeDeformation.NONE),
                PartPose.offset(0.5F, 16.0F, 7.0F)
        );
        root.addOrReplaceChild(
                "leg2",
                CubeListBuilder.create()
                        .texOffs(0, 18)
                        .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F, CubeDeformation.NONE),
                PartPose.offset(-2.5F, 16.0F, -4.0F)
        );
        root.addOrReplaceChild(
                "leg3",
                CubeListBuilder.create()
                        .texOffs(0, 18)
                        .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F, CubeDeformation.NONE),
                PartPose.offset(0.5F, 16.0F, -4.0F)
        );
        root.addOrReplaceChild(
                "tail",
                CubeListBuilder.create()
                        .texOffs(9, 18)
                        .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-1.0F, 12.0F, 8.0F, 0.9599F, 0.0F, 0.0F)
        );

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(GiantDog2hEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float scaledSwing = limbSwing * 2.0F / Math.max(entity.getBbHeight(), 0.1F);
        this.headRight.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        this.headRight.xRot = headPitch * Mth.DEG_TO_RAD;
        this.headLeft.yRot = this.headRight.yRot;
        this.headLeft.xRot = this.headRight.xRot;
        this.leg0.xRot = Mth.cos(scaledSwing) * -limbSwingAmount;
        this.leg1.xRot = Mth.cos(scaledSwing) * limbSwingAmount;
        this.leg2.xRot = Mth.cos(scaledSwing) * limbSwingAmount;
        this.leg3.xRot = Mth.cos(scaledSwing) * -limbSwingAmount;
        this.tail.xRot = 0.9599F;
        this.tail.zRot = ageInTicks * 0.2F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        this.root.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
