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
import net.narutomod.entity.WaterSharkEntity;

public final class WaterSharkModel extends EntityModel<WaterSharkEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        NarutomodMod.location("water_shark_legacy"),
        "main"
    );

    private final ModelPart body;
    private final ModelPart foreHead;
    private final ModelPart jaw;
    private final ModelPart tail;
    private final ModelPart tailFin;

    public WaterSharkModel(ModelPart root) {
        this.body = root.getChild("body");
        ModelPart head = this.body.getChild("head");
        this.foreHead = head.getChild("foreHead");
        this.jaw = head.getChild("jaw");
        this.tail = this.body.getChild("tail");
        this.tailFin = this.tail.getChild("tailFin");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild(
            "body",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-4.0F, -7.0F, 0.0F, 8.0F, 7.0F, 13.0F, CubeDeformation.NONE),
            PartPose.offset(0.0F, 0.0F, -5.0F)
        );

        PartDefinition head = body.addOrReplaceChild(
            "head",
            CubeListBuilder.create(),
            PartPose.offset(0.0F, -3.0F, 0.0F)
        );

        head.addOrReplaceChild(
            "foreHead",
            CubeListBuilder.create()
                .texOffs(19, 20)
                .addBox(-4.0F, 0.0F, -6.0F, 8.0F, 4.0F, 6.0F, CubeDeformation.NONE),
            PartPose.offsetAndRotation(0.0F, -3.5F, 0.0F, 0.1745F, 0.0F, 0.0F)
        );

        head.addOrReplaceChild(
            "jaw",
            CubeListBuilder.create()
                .texOffs(29, 0)
                .addBox(-3.5F, -1.5F, -4.75F, 7.0F, 2.0F, 5.0F, CubeDeformation.NONE),
            PartPose.offset(0.0F, 1.5F, 0.25F)
        );

        PartDefinition tail = body.addOrReplaceChild(
            "tail",
            CubeListBuilder.create()
                .texOffs(0, 20)
                .addBox(-2.0F, -2.5F, -1.0F, 4.0F, 5.0F, 11.0F, CubeDeformation.NONE),
            PartPose.offset(0.0F, -3.5F, 13.0F)
        );

        PartDefinition tailFin = tail.addOrReplaceChild(
            "tailFin",
            CubeListBuilder.create(),
            PartPose.offset(0.0F, -0.5F, 8.0F)
        );

        tailFin.addOrReplaceChild(
            "tailFinUpper",
            CubeListBuilder.create()
                .texOffs(0, 20)
                .addBox(-0.5F, -6.9924F, -1.1743F, 1.0F, 8.0F, 3.0F, CubeDeformation.NONE),
            PartPose.offsetAndRotation(0.0F, -1.0F, 1.0F, -0.6109F, 0.0F, 0.0F)
        );

        tailFin.addOrReplaceChild(
            "tailFinLower",
            CubeListBuilder.create()
                .texOffs(0, 36)
                .addBox(-0.5F, -1.4924F, -1.0403F, 1.0F, 6.0F, 3.0F, CubeDeformation.NONE),
            PartPose.offsetAndRotation(0.0F, 1.0F, 1.0F, 0.5236F, 0.0F, 0.0F)
        );

        body.addOrReplaceChild(
            "backFin",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-0.5F, -7.75F, -1.5F, 1.0F, 8.0F, 4.0F, CubeDeformation.NONE),
            PartPose.offsetAndRotation(0.0F, -6.0F, 6.0F, -0.5236F, 0.0F, 0.0F)
        );

        body.addOrReplaceChild(
            "leftFin",
            CubeListBuilder.create()
                .texOffs(32, 34)
                .addBox(0.0F, -4.0F, -1.5F, 1.0F, 4.0F, 7.0F, CubeDeformation.NONE),
            PartPose.offsetAndRotation(3.0F, -3.0F, 8.0F, 0.9599F, 0.0F, 1.8675F)
        );

        body.addOrReplaceChild(
            "rightFin",
            CubeListBuilder.create()
                .texOffs(32, 34)
                .addBox(-1.0F, -4.0F, -1.5F, 1.0F, 4.0F, 7.0F, CubeDeformation.NONE),
            PartPose.offsetAndRotation(-3.0F, -3.0F, 8.0F, 0.9599F, 0.0F, -1.8675F)
        );

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(WaterSharkEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
        float tailSwing = Mth.cos(limbSwing * 0.6662F) * 0.4F * limbSwingAmount;
        this.tail.yRot = tailSwing;
        this.tailFin.yRot = tailSwing;

        float mouthOpen = Mth.clamp(headPitch, 0.0F, 1.0F);
        this.foreHead.xRot = 0.1745F - mouthOpen * 0.4363F;
        this.jaw.xRot = mouthOpen * 0.5236F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay,
            float red, float green, float blue, float alpha) {
        this.body.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
