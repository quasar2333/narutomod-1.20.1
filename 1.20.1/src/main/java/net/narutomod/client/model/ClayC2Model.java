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

public final class ClayC2Model extends EntityModel<ExplosiveClayEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        NarutomodMod.location("clay_c2_legacy"),
        "main"
    );

    private final ModelPart body;
    private final ModelPart leftWingBody;
    private final ModelPart leftWing;
    private final ModelPart rightWingBody;
    private final ModelPart rightWing;
    private final ModelPart tail;
    private final ModelPart tailTip;

    public ClayC2Model(ModelPart root) {
        this.body = root.getChild("body");
        this.leftWingBody = this.body.getChild("left_wing_body");
        this.leftWing = this.leftWingBody.getChild("left_wing");
        this.rightWingBody = this.body.getChild("right_wing_body");
        this.rightWing = this.rightWingBody.getChild("right_wing");
        this.tail = this.body.getChild("tail");
        this.tailTip = this.tail.getChild("tail_tip");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild(
            "body",
            CubeListBuilder.create()
                .texOffs(0, 8)
                .addBox(-2.5F, -2.0F, -8.0F, 5.0F, 3.0F, 9.0F, CubeDeformation.NONE),
            PartPose.ZERO
        );
        PartDefinition leftWingBody = body.addOrReplaceChild(
            "left_wing_body",
            CubeListBuilder.create()
                .texOffs(23, 12)
                .addBox(0.0F, 0.0F, 0.0F, 6.0F, 2.0F, 9.0F, CubeDeformation.NONE),
            PartPose.offsetAndRotation(2.5F, -2.0F, -8.0F, 0.0F, 0.0F, 0.0873F)
        );
        leftWingBody.addOrReplaceChild(
            "left_wing",
            CubeListBuilder.create()
                .texOffs(16, 24)
                .addBox(0.0F, 0.0F, 0.0F, 13.0F, 1.0F, 9.0F, CubeDeformation.NONE),
            PartPose.offsetAndRotation(6.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.1745F)
        );
        PartDefinition rightWingBody = body.addOrReplaceChild(
            "right_wing_body",
            mirroredBox(23, 12, -6.0F, 0.0F, 0.0F, 6.0F, 2.0F, 9.0F, 0.0F),
            PartPose.offsetAndRotation(-2.5F, -2.0F, -8.0F, 0.0F, 0.0F, -0.0873F)
        );
        rightWingBody.addOrReplaceChild(
            "right_wing",
            mirroredBox(16, 24, -13.0F, 0.0F, 0.0F, 13.0F, 1.0F, 9.0F, 0.0F),
            PartPose.offsetAndRotation(-6.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.1745F)
        );
        body.addOrReplaceChild(
            "head",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-4.0F, -2.0F, -5.0F, 7.0F, 3.0F, 5.0F, CubeDeformation.NONE),
            PartPose.offset(0.5F, 1.0F, -7.0F)
        );
        PartDefinition tail = body.addOrReplaceChild(
            "tail",
            CubeListBuilder.create()
                .texOffs(3, 20)
                .addBox(-2.0F, 0.0F, 0.0F, 3.0F, 2.0F, 6.0F, CubeDeformation.NONE),
            PartPose.offsetAndRotation(0.5F, -2.0F, 1.0F, -0.0873F, 0.0F, 0.0F)
        );
        tail.addOrReplaceChild(
            "tail_tip",
            CubeListBuilder.create()
                .texOffs(4, 29)
                .addBox(-1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 6.0F, CubeDeformation.NONE),
            PartPose.offsetAndRotation(0.0F, 0.5F, 6.0F, -0.0873F, 0.0F, 0.0F)
        );
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(ExplosiveClayEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
        float phase = ((float)(entity.getId() * 3) + ageInTicks) * 0.13F;
        if (entity.getDeltaMovement().lengthSqr() > 0.01D) {
            this.leftWingBody.zRot = Mth.cos(phase) * 16.0F * Mth.DEG_TO_RAD;
            this.leftWing.zRot = this.leftWingBody.zRot;
            this.rightWingBody.zRot = -this.leftWingBody.zRot;
            this.rightWing.zRot = -this.leftWing.zRot;
        } else {
            this.leftWingBody.zRot = -0.5236F;
            this.leftWing.zRot = 1.0472F;
            this.rightWingBody.zRot = 0.5236F;
            this.rightWing.zRot = -1.0472F;
        }
        float tailRot = -(5.0F + Mth.cos(phase * 2.0F) * 5.0F) * Mth.DEG_TO_RAD;
        this.tail.xRot = tailRot;
        this.tailTip.xRot = tailRot;
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
