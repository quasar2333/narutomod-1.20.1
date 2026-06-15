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
import net.minecraft.world.phys.Vec3;
import net.narutomod.NarutomodMod;
import net.narutomod.entity.PuppetKarasuEntity;

public final class PuppetKarasuModel extends HumanoidModel<PuppetKarasuEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            NarutomodMod.location("puppet_karasu_legacy"),
            "main"
    );

    private static final float DEG_45 = 45.0F * ((float) Math.PI / 180.0F);

    private final ModelPart root;
    private final ModelPart rightArm2;
    private final ModelPart leftArm2;

    public PuppetKarasuModel(ModelPart root) {
        super(root);
        this.root = root;
        this.rightArm2 = root.getChild("RightArm2");
        this.leftArm2 = root.getChild("LeftArm2");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild(
                "head",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );
        root.addOrReplaceChild(
                "hat",
                CubeListBuilder.create()
                        .texOffs(32, 0)
                        .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.25F)),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );
        root.addOrReplaceChild(
                "body",
                CubeListBuilder.create()
                        .texOffs(16, 16)
                        .addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(16, 32)
                        .addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.25F)),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );
        root.addOrReplaceChild(
                "right_arm",
                CubeListBuilder.create()
                        .texOffs(40, 16)
                        .addBox(-2.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(40, 32)
                        .addBox(-2.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, new CubeDeformation(0.25F)),
                PartPose.offsetAndRotation(-5.0F, 2.5F, 0.0F, 0.0F, 0.0F, 0.3491F)
        );
        root.addOrReplaceChild(
                "left_arm",
                CubeListBuilder.create()
                        .texOffs(32, 48)
                        .addBox(-1.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(48, 48)
                        .addBox(-1.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, new CubeDeformation(0.25F)),
                PartPose.offsetAndRotation(5.0F, 2.5F, 0.0F, 0.0F, 0.0F, -0.3491F)
        );
        root.addOrReplaceChild(
                "right_leg",
                CubeListBuilder.create()
                        .texOffs(0, 16)
                        .addBox(-1.5F, 0.0F, -2.0F, 3.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 32)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.25F)),
                PartPose.offsetAndRotation(-1.9F, 12.0F, 0.0F, 0.0F, 0.0F, 0.0873F)
        );
        root.addOrReplaceChild(
                "left_leg",
                CubeListBuilder.create()
                        .texOffs(16, 48)
                        .addBox(-1.5F, 0.0F, -2.0F, 3.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 48)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.25F)),
                PartPose.offsetAndRotation(1.9F, 12.0F, 0.0F, 0.0F, 0.0F, -0.0873F)
        );
        root.addOrReplaceChild(
                "RightArm2",
                CubeListBuilder.create()
                        .texOffs(40, 16)
                        .addBox(-2.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(40, 32)
                        .addBox(-2.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, new CubeDeformation(0.25F)),
                PartPose.offsetAndRotation(-5.0F, 7.5F, 0.0F, 0.0F, 0.0F, 0.2182F)
        );
        root.addOrReplaceChild(
                "LeftArm2",
                CubeListBuilder.create()
                        .texOffs(32, 48)
                        .addBox(-1.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(48, 48)
                        .addBox(-1.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, new CubeDeformation(0.25F)),
                PartPose.offsetAndRotation(5.0F, 7.5F, 0.0F, 0.0F, 0.0F, -0.2618F)
        );

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(PuppetKarasuEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
        this.root.getAllParts().forEach(ModelPart::resetPose);
        this.rightArm.zRot = 0.0F;
        this.leftArm.zRot = 0.0F;
        super.setupAnim(entity, 0.0F, 0.0F, ageInTicks, netHeadYaw, headPitch);
        this.rightArm.zRot += 0.3491F;
        this.leftArm.zRot += -0.3491F;

        Vec3 motion = entity.getDeltaMovement();
        float velocity = Mth.sqrt((float) (motion.x * motion.x + motion.z * motion.z));
        if (velocity > 0.001F) {
            float lift = Mth.clamp(velocity, 0.0F, 1.0F) * DEG_45;
            this.rightArm.xRot += lift;
            this.leftArm.xRot += lift;
            this.rightArm2.xRot = this.rightArm.xRot;
            this.rightArm2.yRot = this.rightArm.yRot;
            this.rightArm2.zRot = this.rightArm.zRot - 0.1309F;
            this.leftArm2.xRot = this.leftArm.xRot;
            this.leftArm2.yRot = this.leftArm.yRot;
            this.leftArm2.zRot = this.leftArm.zRot + 0.1309F;
            this.rightLeg.xRot += lift;
            this.leftLeg.xRot += lift;
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay,
            float red, float green, float blue, float alpha) {
        super.renderToBuffer(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.rightArm2.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.leftArm2.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
