package net.narutomod.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Random;
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
import net.narutomod.entity.SandLevitationEntity;

public final class SandLevitationModel extends EntityModel<SandLevitationEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        NarutomodMod.location("sand_levitation_legacy"),
        "main"
    );

    private static final String BOX_COORDINATES = """
        -1,-3,7;0,-3,7;1,-3,7;-2,-3,7;-3,-3,6;-4,-3,6;-5,-3,5;-8,-3,1;-7,-3,2;-7,-3,3;-6,-3,4;7,-3,1;
        7,-3,0;7,-3,-1;7,-3,-2;1,-3,-8;0,-3,-8;-1,-3,-8;-2,-3,-8;-8,-3,0;-8,-3,-1;-8,-3,-2;-7,-3,-3;-7,-3,-4;
        -6,-3,-5;-5,-3,-6;-4,-3,-7;-3,-3,-7;2,-3,-7;3,-3,-7;4,-3,-6;5,-3,-5;6,-3,-4;6,-3,-3;6,-3,2;6,-3,3;
        5,-3,4;4,-3,5;3,-3,6;2,-3,6;2,-4,5;1,-4,6;0,-4,6;-1,-4,6;-2,-4,6;-3,-4,5;-4,-4,5;-5,-4,4;
        -6,-4,3;-6,-4,2;-7,-4,1;-7,-4,0;-7,-4,-1;-7,-4,-2;-6,-4,-3;-6,-4,-4;-5,-4,-5;-4,-4,-6;-3,-4,-6;-2,-4,-7;
        -1,-4,-7;0,-4,-7;1,-4,-7;2,-4,-6;3,-4,-6;4,-4,-5;5,-4,-4;5,-4,-3;6,-4,-1.9;6,-4,-1;6,-4,0;6,-4,1;
        5,-4,2;5,-4,3;4,-4,4;3,-4,5;1,-4,5;0,-4,5;-1,-4,5;-2,-4,5;3,-4,4;2,-4,4;1,-4,4;0,-4,4;
        -1,-4,4;-2,-4,4;-3,-4,4;-4,-4,4;-5,-4,3;-4,-4,3;-3,-4,3;-2,-4,3;-1,-4,3;0,-4,3;1,-4,3;2,-4,3;
        3,-4,3;4,-4,3;4,-4,2;3,-4,2;2,-4,2;1,-4,2;0,-4,2;-1,-4,2;-2,-4,2;-3,-4,2;-4,-4,2;-5,-4,2;
        -6,-4,1;-5,-4,1;-4,-4,1;-3,-4,1;-2,-4,1;-1,-4,1;0,-4,1;1,-4,1;2,-4,1;3,-4,1;4,-4,1;5,-4,1;
        5,-4,0;4,-4,0;3,-4,0;2,-4,0;1,-4,0;0,-4,0;-1,-4,0;-2,-4,0;-3,-4,0;-4,-4,0;-5,-4,0;-6,-4,0;
        -6,-4,-1;-5,-4,-1;-4,-4,-1;-3,-4,-1;-2,-4,-1;-1,-4,-1;0,-4,-1;1,-4,-1;2,-4,-1;3,-4,-1;4,-4,-1;5,-4,-1;
        5,-4,-2;4,-4,-2;3,-4,-2;2,-4,-2;1,-4,-2;0,-4,-2;-1,-4,-2;-5,-4,-2;-2,-4,-2;-3,-4,-2;-4,-4,-2;-6,-4,-2;
        -5,-4,-3;-4,-4,-3;-3,-4,-3;-2,-4,-3;-1,-4,-3;1,-4,-3;0,-4,-3;2,-4,-3;3,-4,-3;4,-4,-3;-5,-4,-4;-4,-4,-4;
        -3,-4,-4;-2,-4,-4;-1,-4,-4;0,-4,-4;1,-4,-4;2,-4,-4;3,-4,-4;4,-4,-4;-4,-4,-5;-3,-4,-5;-2,-4,-5;-1,-4,-5;
        0,-4,-5;1,-4,-5;2,-4,-5;3,-4,-5;-2,-4,-6;-1,-4,-6;0,-4,-6;1,-4,-6;1,-2,0;6,-2,1;6,-2,0;6,-2,-1;
        6,-2,-1.9;5,-2,-4;5,-2,-3;5,-2,-2;5,-2,-1;5,-2,0;5,-2,1;5,-2,2;5,-2,3;4,-2,4;4,-2,3;4,-2,2;
        4,-2,1;4,-2,0;4,-2,-1;4,-2,-2;4,-2,-3;4,-2,-4;4,-2,-5;3,-2,-6;3,-2,-5;3,-2,-4;3,-2,-3;3,-2,-2;
        3,-2,-1;3,-2,0;3,-2,1;3,-2,2;3,-2,3;3,-2,4;3,-2,5;2,-2,5;2,-2,4;2,-2,3;2,-2,2;2,-2,1;
        2,-2,0;2,-2,-1;2,-2,-2;2,-2,-3;2,-2,-4;2,-2,-5;2,-2,-6;1,-2,-7;1,-2,-6;1,-2,-5;1,-2,-4;1,-2,-3;
        1,-2,-2;1,-2,-1;1,-2,1;1,-2,2;1,-2,3;1,-2,4;1,-2,5;1,-2,6;0,-2,6;0,-2,5;0,-2,4;0,-2,3;
        0,-2,2;0,-2,1;0,-2,0;0,-2,-1;0,-2,-2;0,-2,-3;0,-2,-4;0,-2,-5;0,-2,-6;0,-2,-7;-1,-2,-7;-1,-2,-6;
        -1,-2,-5;-1,-2,-4;-1,-2,-3;-1,-2,-2;-1,-2,-1;-1,-2,0;-1,-2,1;-1,-2,2;-1,-2,3;-1,-2,4;-1,-2,5;-1,-2,6;
        -2,-2,6;-2,-2,5;-2,-2,4;-2,-2,3;-2,-2,2;-2,-2,1;-2,-2,-1;-2,-2,0;-2,-2,-2;-2,-2,-3;-2,-2,-4;-2,-2,-5;
        -2,-2,-6;-2,-2,-7;-3,-2,-6;-3,-2,-5;-3,-2,-4;-3,-2,-3;-3,-2,-2;-3,-2,-1;-3,-2,0;-3,-2,1;-3,-2,2;-3,-2,3;
        -3,-2,4;-3,-2,5;-4,-2,5;-4,-2,4;-4,-2,3;-4,-2,2;-4,-2,1;-4,-2,0;-4,-2,-1;-4,-2,-2;-4,-2,-3;-4,-2,-4;
        -4,-2,-5;-4,-2,-6;-5,-2,-5;-5,-2,-4;-5,-2,-3;-5,-2,-2;-5,-2,0;-5,-2,-1;-5,-2,1;-5,-2,2;-5,-2,3;-5,-2,4;
        -6,-2,3;-6,-2,2;-6,-2,1;-6,-2,0;-6,-2,-1;-6,-2,-2;-6,-2,-3;-6,-2,-4;-7,-2,-2;-7,-2,-1;-7,-2,0;-7,-2,1;
        -1,-1,-1;0,-1,-1;0,-1,0;-1,-1,0;-3,-1,0;-2,-1,0;-4,-1,0;-5,-1,0;-5,-1,-1;-4,-1,-1;-3,-1,-1;-2,-1,-1;
        4,-1,-1;3,-1,-1;2,-1,-1;1,-1,-1;4,-1,0;3,-1,0;2,-1,0;1,-1,0;0,-1,1;0,-1,3;0,-1,2;0,-1,4;
        -1,-1,1;-1,-1,2;-1,-1,3;-1,-1,4;-1,-1,-5;-1,-1,-4;-1,-1,-3;-1,-1,-2;0,-1,-5;0,-1,-4;0,-1,-3;0,-1,-2;
        1,-1,1;1,-1,2;2,-1,1;2,-1,2;3,-1,1;1,-1,3;-2,-1,2;-3,-1,2;-2,-1,1;-3,-1,1;-2,-1,-2;-3,-1,-2;
        -2,-1,-3;-3,-1,-3;2,-1,-2;1,-1,-2;2,-1,-3;1,-1,-3;1,-1,-4;3,-1,-2;-4,-1,-2;-2,-1,-4;-2,-1,3;-4,-1,1
        """;

    private final ModelPart root;

    public SandLevitationModel(ModelPart root) {
        this.root = root.getChild("cloud");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        CubeListBuilder boxes = CubeListBuilder.create();
        Random random = new Random(0L);
        for (String entry : BOX_COORDINATES.trim().split(";")) {
            String[] parts = entry.trim().split(",");
            if (parts.length != 3) {
                continue;
            }
            float x = Float.parseFloat(parts[0].trim());
            float y = Float.parseFloat(parts[1].trim());
            float z = Float.parseFloat(parts[2].trim());
            boxes.texOffs(random.nextInt(13), random.nextInt(15))
                .addBox(x, y, z, 1.0F, 1.0F, 1.0F, CubeDeformation.NONE);
        }
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("cloud", boxes, PartPose.ZERO);
        return LayerDefinition.create(mesh, 16, 16);
    }

    @Override
    public void setupAnim(SandLevitationEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay,
            float red, float green, float blue, float alpha) {
        this.root.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
