package net.narutomod.client.model;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.narutomod.NarutomodMod;
import net.narutomod.entity.ExplosiveClayEntity;

public final class ClayC1Model extends HierarchicalModel<ExplosiveClayEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        NarutomodMod.location("clay_c1_legacy"),
        "main"
    );

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart torso;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart body;
    private final ModelPart rightWing;
    private final ModelPart leftWing;

    public ClayC1Model(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.torso = root.getChild("torso");
        this.rightArm = root.getChild("rightArm");
        this.leftArm = root.getChild("leftArm");
        this.body = root.getChild("body");
        this.rightWing = root.getChild("rightWing");
        this.leftWing = root.getChild("leftWing");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild(
            "head",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F),
            PartPose.offset(0.0F, 0.0F, 0.0F)
        );
        root.addOrReplaceChild(
            "torso",
            CubeListBuilder.create()
                .texOffs(16, 16)
                .addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F),
            PartPose.offset(0.0F, 0.0F, 0.0F)
        );
        root.addOrReplaceChild(
            "rightArm",
            CubeListBuilder.create()
                .texOffs(40, 16)
                .addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F),
            PartPose.offset(-5.0F, 2.0F, 0.0F)
        );
        root.addOrReplaceChild(
            "leftArm",
            CubeListBuilder.create()
                .mirror()
                .texOffs(40, 16)
                .addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F),
            PartPose.offset(5.0F, 2.0F, 0.0F)
        );
        root.addOrReplaceChild(
            "body",
            CubeListBuilder.create()
                .texOffs(32, 0)
                .addBox(-1.0F, -1.0F, -2.0F, 6.0F, 10.0F, 4.0F),
            PartPose.offset(-1.9F, 12.0F, 0.0F)
        );
        root.addOrReplaceChild(
            "rightWing",
            CubeListBuilder.create()
                .texOffs(0, 32)
                .addBox(-20.0F, 0.0F, 0.0F, 20.0F, 12.0F, 1.0F),
            PartPose.offsetAndRotation(0.0F, 1.0F, 2.0F, 0.47123894F, 0.0F, 0.47123894F)
        );
        root.addOrReplaceChild(
            "leftWing",
            CubeListBuilder.create()
                .texOffs(0, 32)
                .mirror()
                .addBox(0.0F, 0.0F, 0.0F, 20.0F, 12.0F, 1.0F),
            PartPose.offsetAndRotation(0.0F, 1.0F, 2.0F, 0.47123894F, 0.0F, -0.47123894F)
        );
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(ExplosiveClayEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
        this.head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        this.head.xRot = headPitch * Mth.DEG_TO_RAD;
        this.torso.xRot = 0.0F;
        this.rightArm.xRot = 0.0F;
        this.leftArm.xRot = 0.0F;
        this.body.xRot = Mth.PI / 5.0F;
        this.rightWing.zRot = 0.47123894F + Mth.cos(ageInTicks * 0.8F) * Mth.PI * 0.05F;
        this.leftWing.zRot = -this.rightWing.zRot;
        this.leftWing.yRot = -0.47123894F;
        this.leftWing.xRot = 0.47123894F;
        this.rightWing.xRot = 0.47123894F;
        this.rightWing.yRot = 0.47123894F;
    }
}
