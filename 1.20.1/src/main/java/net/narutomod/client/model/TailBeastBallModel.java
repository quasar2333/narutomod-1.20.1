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
import net.narutomod.entity.TailBeastBallEntity;

public final class TailBeastBallModel extends EntityModel<TailBeastBallEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            NarutomodMod.location("tail_beast_ball_legacy"),
            "main"
    );

    private static final float ROTATION_45 = 0.7854F;

    private final ModelPart core;
    private final ModelPart shell;

    public TailBeastBallModel(ModelPart root) {
        this.core = root.getChild("core");
        this.shell = root.getChild("shell");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        addSquareBallPart(root, "core", CubeDeformation.NONE);
        addSquareBallPart(root, "shell", new CubeDeformation(0.1F));
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(TailBeastBallEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay,
            float red, float green, float blue, float alpha) {
        renderCore(poseStack, consumer, packedLight, packedOverlay, 0.0F, 0.0F, 0.0F, alpha);
        renderShell(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha * 0.15F);
    }

    public void renderCore(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay,
            float red, float green, float blue, float alpha) {
        this.core.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    public void renderShell(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay,
            float red, float green, float blue, float alpha) {
        this.shell.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    private static void addSquareBallPart(PartDefinition root, String name, CubeDeformation deformation) {
        PartDefinition part = root.addOrReplaceChild(name, CubeListBuilder.create(), PartPose.ZERO);
        addCube(part, "cube0", PartPose.ZERO, deformation);
        addCube(part, "cube1", PartPose.rotation(0.0F, 0.0F, ROTATION_45), deformation);
        addCube(part, "cube2", PartPose.rotation(0.0F, -ROTATION_45, 0.0F), deformation);
        addCube(part, "cube3", PartPose.rotation(-ROTATION_45, 0.0F, 0.0F), deformation);
    }

    private static void addCube(PartDefinition parent, String name, PartPose pose, CubeDeformation deformation) {
        parent.addOrReplaceChild(
                name,
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-2.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F, deformation),
                pose);
    }
}
