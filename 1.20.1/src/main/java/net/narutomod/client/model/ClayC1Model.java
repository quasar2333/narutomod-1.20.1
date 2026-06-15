package net.narutomod.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
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

public final class ClayC1Model extends HumanoidModel<ExplosiveClayEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        NarutomodMod.location("clay_c1_legacy"),
        "main"
    );

    private final ModelPart rightWing;
    private final ModelPart leftWing;

    public ClayC1Model(ModelPart root) {
        super(root);
        this.leftLeg.visible = false;
        this.hat.visible = false;
        this.rightWing = root.getChild("right_wing");
        this.leftWing = root.getChild("left_wing");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild(
            "right_leg",
            CubeListBuilder.create()
                .texOffs(32, 0)
                .addBox(-1.0F, -1.0F, -2.0F, 6.0F, 10.0F, 4.0F, CubeDeformation.NONE),
            PartPose.offset(-1.9F, 12.0F, 0.0F)
        );
        root.addOrReplaceChild(
            "right_wing",
            CubeListBuilder.create()
                .texOffs(0, 32)
                .addBox(-20.0F, 0.0F, 0.0F, 20.0F, 12.0F, 1.0F, CubeDeformation.NONE),
            PartPose.ZERO
        );
        root.addOrReplaceChild(
            "left_wing",
            CubeListBuilder.create()
                .texOffs(0, 32)
                .mirror()
                .addBox(0.0F, 0.0F, 0.0F, 20.0F, 12.0F, 1.0F, CubeDeformation.NONE)
                .mirror(false),
            PartPose.ZERO
        );
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(ExplosiveClayEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        this.rightLeg.xRot += Mth.PI / 5.0F;

        this.rightWing.z = 2.0F;
        this.leftWing.z = 2.0F;
        this.rightWing.y = 1.0F;
        this.leftWing.y = 1.0F;
        this.rightWing.yRot = 0.47123894F + Mth.cos(ageInTicks * 0.8F) * Mth.PI * 0.05F;
        this.leftWing.yRot = -this.rightWing.yRot;
        this.leftWing.zRot = -0.47123894F;
        this.leftWing.xRot = 0.47123894F;
        this.rightWing.xRot = 0.47123894F;
        this.rightWing.zRot = 0.47123894F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay,
            float red, float green, float blue, float alpha) {
        super.renderToBuffer(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.rightWing.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.leftWing.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
