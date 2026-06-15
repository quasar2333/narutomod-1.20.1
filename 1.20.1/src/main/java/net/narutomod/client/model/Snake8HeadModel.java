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
import net.narutomod.entity.Snake8HeadEntity;

public final class Snake8HeadModel extends EntityModel<Snake8HeadEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            NarutomodMod.location("snake_8_head1"),
            "main"
    );

    private final ModelPart root;
    private final ModelPart head;

    public Snake8HeadModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("neck",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 13.0F, CubeDeformation.NONE)
                        .texOffs(0, 15)
                        .addBox(-1.25F, -1.25F, 5.0F, 2.5F, 2.5F, 8.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.0F, 21.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(34, 0)
                        .addBox(-2.5F, -2.4F, -5.0F, 5.0F, 4.8F, 6.0F, CubeDeformation.NONE)
                        .texOffs(56, 0)
                        .addBox(-1.8F, 0.7F, -8.0F, 3.6F, 1.6F, 3.5F, CubeDeformation.NONE)
                        .texOffs(68, 0)
                        .addBox(-1.9F, -3.6F, -1.3F, 1.0F, 2.4F, 1.0F, CubeDeformation.NONE)
                        .texOffs(72, 0)
                        .addBox(0.9F, -3.6F, -1.3F, 1.0F, 2.4F, 1.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, 21.0F, 0.0F));
        return LayerDefinition.create(mesh, 96, 64);
    }

    @Override
    public void setupAnim(Snake8HeadEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.head.xRot = headPitch * Mth.DEG_TO_RAD * 0.35F + Mth.sin(ageInTicks * 0.2F) * 0.04F;
        this.head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.2F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay,
            float red, float green, float blue, float alpha) {
        this.root.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
