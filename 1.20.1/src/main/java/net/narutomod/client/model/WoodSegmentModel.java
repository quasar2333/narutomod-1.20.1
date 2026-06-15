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
import net.minecraft.world.entity.Entity;
import net.narutomod.NarutomodMod;

public final class WoodSegmentModel<T extends Entity> extends EntityModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        NarutomodMod.location("wood_segment_legacy"),
        "main"
    );

    private final ModelPart root;

    public WoodSegmentModel(ModelPart root) {
        this.root = root.getChild("root");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot().addOrReplaceChild(
            "root",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-2.0F, -3.0F, -2.0F, 4.0F, 4.0F, 4.0F, CubeDeformation.NONE),
            PartPose.ZERO
        );
        PartDefinition crown = root.addOrReplaceChild(
            "crown",
            CubeListBuilder.create(),
            PartPose.offset(0.0F, 1.0F, 0.0F)
        );
        crown.addOrReplaceChild(
            "front_bark",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-2.0F, -4.0F, 0.0F, 4.0F, 4.0F, 0.0F, CubeDeformation.NONE),
            PartPose.offsetAndRotation(0.0F, -4.0F, -2.0F, 0.5236F, 3.1416F, 0.0F)
        );
        crown.addOrReplaceChild(
            "back_bark",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-2.0F, -4.0F, 0.0F, 4.0F, 4.0F, 0.0F, CubeDeformation.NONE),
            PartPose.offsetAndRotation(0.0F, -4.0F, 2.0F, 0.5236F, 0.0F, 0.0F)
        );
        crown.addOrReplaceChild(
            "left_bark",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-2.0F, -4.0F, 0.0F, 4.0F, 4.0F, 0.0F, CubeDeformation.NONE),
            PartPose.offsetAndRotation(-2.0F, -4.0F, 0.0F, 0.0F, -1.5708F, 0.5236F)
        );
        crown.addOrReplaceChild(
            "right_bark",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .mirror()
                .addBox(-2.0F, -4.0F, 0.0F, 4.0F, 4.0F, 0.0F, CubeDeformation.NONE)
                .mirror(false),
            PartPose.offsetAndRotation(2.0F, -4.0F, 0.0F, 0.0F, 1.5708F, -0.5236F)
        );
        return LayerDefinition.create(mesh, 16, 16);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay,
            float red, float green, float blue, float alpha) {
        this.root.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
