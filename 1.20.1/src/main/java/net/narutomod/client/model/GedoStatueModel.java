package net.narutomod.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.util.Mth;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.generated.LegacyModelLayerDefinitions;
import net.narutomod.entity.GedoStatueEntity;

public final class GedoStatueModel extends EntityModel<GedoStatueEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            NarutomodMod.location("gedo_statue"),
            "main"
    );

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart hat;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public GedoStatueModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.hat = root.getChild("hat");
        this.body = root.getChild("body");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        return LegacyModelLayerDefinitions.EntityGedoStatue_ModelGedoMazo_696();
    }

    @Override
    public void setupAnim(GedoStatueEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.35F;
        this.head.xRot = headPitch * Mth.DEG_TO_RAD * 0.35F;
        this.hat.yRot = this.head.yRot;
        this.hat.xRot = this.head.xRot;

        float swing = Mth.cos(limbSwing * 0.45F) * limbSwingAmount;
        this.rightLeg.xRot = swing * 0.35F;
        this.leftLeg.xRot = -swing * 0.35F;
        this.rightArm.xRot = -swing * 0.18F;
        this.leftArm.xRot = swing * 0.18F;
        this.body.yRot = Mth.sin(ageInTicks * 0.025F) * 0.015F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay,
            float red, float green, float blue, float alpha) {
        this.root.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
