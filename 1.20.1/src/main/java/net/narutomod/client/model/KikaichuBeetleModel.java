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
import net.narutomod.entity.BugSwarmEntity;

public final class KikaichuBeetleModel extends EntityModel<BugSwarmEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        NarutomodMod.location("kikaichu_beetle"),
        "main"
    );

    private final ModelPart root;
    private final ModelPart wingLeft;
    private final ModelPart wingRight;
    private final ModelPart leg1;
    private final ModelPart leg2;
    private final ModelPart leg3;
    private final ModelPart leg4;
    private final ModelPart leg5;
    private final ModelPart leg6;

    public KikaichuBeetleModel(ModelPart root) {
        this.root = root;
        ModelPart body = root.getChild("body");
        this.wingLeft = body.getChild("wing_left");
        this.wingRight = body.getChild("wing_right");
        this.leg1 = root.getChild("leg1");
        this.leg2 = root.getChild("leg2");
        this.leg3 = root.getChild("leg3");
        this.leg4 = root.getChild("leg4");
        this.leg5 = root.getChild("leg5");
        this.leg6 = root.getChild("leg6");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild(
            "head",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-1.5F, 0.5F, -7.0F, 3.0F, 2.0F, 1.0F, CubeDeformation.NONE)
                .texOffs(15, 24)
                .addBox(-1.5F, 0.0F, -6.0F, 3.0F, 3.0F, 3.0F, CubeDeformation.NONE)
                .texOffs(16, 17)
                .addBox(-2.5F, 0.0F, -4.0F, 5.0F, 3.0F, 4.0F, new CubeDeformation(0.1F)),
            PartPose.offsetAndRotation(0.0F, -1.5F, -5.0F, 0.3491F, 0.0F, 0.0F)
        );
        head.addOrReplaceChild(
            "mouth_left",
            CubeListBuilder.create()
                .texOffs(4, 6)
                .addBox(-0.5F, -0.4852F, -1.9881F, 1.0F, 1.0F, 2.0F, new CubeDeformation(-0.1F)),
            PartPose.offsetAndRotation(-1.0F, 1.7352F, -6.5119F, 0.3491F, -0.2618F, 0.0F)
        );
        head.addOrReplaceChild(
            "mouth_right",
            mirroredBox(4, 6, -0.5F, -0.4852F, -1.9881F, 1.0F, 1.0F, 2.0F, -0.1F),
            PartPose.offsetAndRotation(1.0F, 1.7352F, -6.5119F, 0.3491F, 0.2618F, 0.0F)
        );
        head.addOrReplaceChild(
            "eye_left",
            CubeListBuilder.create()
                .texOffs(4, 3)
                .addBox(-0.5F, -0.7352F, -0.4881F, 1.0F, 1.0F, 2.0F, CubeDeformation.NONE),
            PartPose.offset(-1.25F, 0.4852F, -6.0119F)
        );
        head.addOrReplaceChild(
            "eye_right",
            mirroredBox(4, 3, -0.5F, -0.7352F, -0.4881F, 1.0F, 1.0F, 2.0F, 0.0F),
            PartPose.offset(1.25F, 0.4852F, -6.0119F)
        );

        PartDefinition body = root.addOrReplaceChild(
            "body",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-4.0F, -1.5F, -5.0F, 8.0F, 4.0F, 10.0F, CubeDeformation.NONE)
                .texOffs(0, 14)
                .addBox(-3.5F, -1.0F, 5.0F, 7.0F, 4.0F, 3.0F, CubeDeformation.NONE)
                .texOffs(0, 21)
                .addBox(-3.0F, 0.0F, 7.0F, 6.0F, 3.0F, 3.0F, CubeDeformation.NONE)
                .texOffs(26, 0)
                .addBox(-2.5F, 1.0F, 9.0F, 5.0F, 2.0F, 2.0F, CubeDeformation.NONE),
            PartPose.ZERO
        );
        body.addOrReplaceChild(
            "wing_left",
            CubeListBuilder.create()
                .texOffs(28, 0)
                .addBox(-3.0F, 0.0F, 0.0F, 6.0F, 0.0F, 12.0F, CubeDeformation.NONE),
            PartPose.offsetAndRotation(1.0F, -1.75F, -4.0F, 0.0873F, 0.2618F, 0.0F)
        );
        body.addOrReplaceChild(
            "wing_right",
            mirroredBox(28, 0, -3.0F, 0.0F, 0.0F, 6.0F, 0.0F, 12.0F, 0.0F),
            PartPose.offsetAndRotation(-1.0F, -1.75F, -4.0F, 0.0873F, -0.2618F, 0.0F)
        );

        addLeg(root, "leg1", -3.0F, -4.0F, -0.5236F, 0.5236F, false);
        addLeg(root, "leg2", 3.0F, -4.0F, 0.5236F, -0.5236F, true);
        addLeg(root, "leg3", -3.0F, 0.0F, 0.0F, 0.5236F, false);
        addLeg(root, "leg4", 3.0F, 0.0F, 0.0F, -0.5236F, true);
        addLeg(root, "leg5", -3.0F, 4.0F, 0.5236F, 0.5236F, false);
        addLeg(root, "leg6", 3.0F, 4.0F, -0.5236F, -0.5236F, true);

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(BugSwarmEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
        float wing = Mth.sin(ageInTicks);
        this.wingLeft.xRot = 0.1964F + wing * 0.0655F;
        this.wingLeft.yRot = 0.7854F + wing * 0.2618F;
        this.wingRight.xRot = 0.1964F + wing * 0.0655F;
        this.wingRight.yRot = -0.7854F - wing * 0.2618F;

        this.leg1.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        this.leg2.xRot = Mth.cos(limbSwing * 0.6662F + Mth.PI) * 1.4F * limbSwingAmount;
        this.leg3.xRot = Mth.cos(limbSwing * 0.6662F + Mth.PI) * 1.4F * limbSwingAmount;
        this.leg4.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        this.leg5.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        this.leg6.xRot = Mth.cos(limbSwing * 0.6662F + Mth.PI) * 1.4F * limbSwingAmount;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay,
            float red, float green, float blue, float alpha) {
        this.root.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    private static void addLeg(PartDefinition root, String name, float x, float z, float yRot, float zRot, boolean mirrored) {
        CubeListBuilder upper = mirrored
            ? mirroredBox(17, 14, 0.0F, -0.5F, -0.5F, 5.0F, 1.0F, 1.0F, 0.0F)
            : CubeListBuilder.create()
                .texOffs(17, 14)
                .addBox(-5.0F, -0.5F, -0.5F, 5.0F, 1.0F, 1.0F, CubeDeformation.NONE);
        PartDefinition leg = root.addOrReplaceChild(
            name,
            upper,
            PartPose.offsetAndRotation(x, 2.0F, z, 0.0F, yRot, zRot)
        );
        CubeListBuilder lower = mirrored
            ? mirroredBox(0, 3, -0.5F, -0.25F, -0.5F, 1.0F, 5.0F, 1.0F, 0.0F)
            : CubeListBuilder.create()
                .texOffs(0, 3)
                .addBox(-0.5F, -0.25F, -0.5F, 1.0F, 5.0F, 1.0F, CubeDeformation.NONE);
        leg.addOrReplaceChild(
            name + "_foreleg",
            lower,
            PartPose.offset(mirrored ? 4.5F : -4.5F, 0.25F, 0.0F)
        );
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
