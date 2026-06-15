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
import net.narutomod.entity.EightyGodsEntity;

public final class EightyGodsModel extends EntityModel<EightyGodsEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            NarutomodMod.location("eighty_gods_fist"),
            "main");

    private final ModelPart fist;

    public EightyGodsModel(ModelPart root) {
        this.fist = root.getChild("fist");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild(
                "fist",
                CubeListBuilder.create()
                        .texOffs(0, 12)
                        .addBox(-2.0F, -4.0F, -1.0F, 4.0F, 4.0F, 8.0F, CubeDeformation.NONE)
                        .texOffs(0, 12)
                        .addBox(-2.0F, -4.0F, -2.0F, 4.0F, 4.0F, 8.0F, new CubeDeformation(0.1F))
                        .texOffs(0, 12)
                        .addBox(-2.0F, -4.0F, -3.0F, 4.0F, 4.0F, 8.0F, new CubeDeformation(0.2F))
                        .texOffs(0, 0)
                        .addBox(-2.0F, -4.0F, -4.0F, 4.0F, 4.0F, 8.0F, new CubeDeformation(0.3F)),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(EightyGodsEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay,
            float red, float green, float blue, float alpha) {
        this.fist.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
