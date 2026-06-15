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
import net.narutomod.NarutomodMod;
import net.narutomod.entity.HirudoraEntity;

public final class HirudoraTigerModel extends EntityModel<HirudoraEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            NarutomodMod.location("hirudora_tiger"),
            "main");

    private final ModelPart head;
    private final ModelPart eyes;

    public HirudoraTigerModel(ModelPart root) {
        this.head = root.getChild("head");
        this.eyes = root.getChild("eyes");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild(
                "head",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-4.0F, 8.0F, -8.0F, 8.0F, 8.0F, 16.0F, CubeDeformation.NONE)
                        .texOffs(0, 24)
                        .addBox(-3.0F, 11.0F, -12.0F, 6.0F, 3.0F, 4.0F, CubeDeformation.NONE)
                        .texOffs(0, 0)
                        .addBox(-1.0F, 10.75F, -12.25F, 2.0F, 1.0F, 1.0F, CubeDeformation.NONE),
                PartPose.ZERO);
        head.addOrReplaceChild(
                "jaw",
                CubeListBuilder.create()
                        .texOffs(20, 24)
                        .addBox(-3.0F, 0.0F, -4.0F, 6.0F, 2.0F, 4.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.0F, 14.0F, -8.0F, 0.6109F, 0.0F, 0.0F));
        head.addOrReplaceChild(
                "leftear",
                CubeListBuilder.create()
                        .texOffs(38, 0)
                        .addBox(-1.0F, -2.0F, 0.0F, 1.0F, 4.0F, 4.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(4.0F, 9.0F, -6.0F, 0.0F, 0.5236F, 0.0F));
        head.addOrReplaceChild(
                "rightear",
                CubeListBuilder.create()
                        .mirror()
                        .texOffs(38, 0)
                        .addBox(0.0F, -1.0F, 0.0F, 1.0F, 4.0F, 4.0F, CubeDeformation.NONE)
                        .mirror(false),
                PartPose.offsetAndRotation(-4.0F, 8.0F, -6.0F, 0.0F, -0.5236F, 0.0F));
        root.addOrReplaceChild(
                "eyes",
                CubeListBuilder.create()
                        .texOffs(0, 33)
                        .addBox(-3.0F, 9.0F, -8.1F, 6.0F, 2.0F, 0.0F, CubeDeformation.NONE),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(HirudoraEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay,
            float red, float green, float blue, float alpha) {
        renderHead(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        renderEyes(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    public void renderHead(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay,
            float red, float green, float blue, float alpha) {
        this.head.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    public void renderEyes(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay,
            float red, float green, float blue, float alpha) {
        this.eyes.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
