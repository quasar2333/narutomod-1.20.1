package net.narutomod.client.model;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.LivingEntity;
import net.narutomod.NarutomodMod;

public final class AsuraPathArmorModel<T extends LivingEntity> extends HumanoidModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            NarutomodMod.location("asura_path_armor_legacy"),
            "main"
    );

    private final ModelPart tail;

    public AsuraPathArmorModel(ModelPart root) {
        super(root);
        this.tail = this.body.getChild("tail");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild(
                "body",
                CubeListBuilder.create()
                        .texOffs(16, 16)
                        .addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.3F)),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );

        PartDefinition tail = body.addOrReplaceChild(
                "tail",
                CubeListBuilder.create()
                        .texOffs(48, 0)
                        .addBox(-4.0F, -4.0F, 0.0F, 8.0F, 4.0F, 0.0F, new CubeDeformation(0.1F)),
                PartPose.offsetAndRotation(0.0F, 9.0F, 2.0F, -0.7854F, 0.0F, 0.0F)
        );
        PartDefinition tail2 = addTailSegment(tail, "tail2", 48, 0, true);
        PartDefinition tail3 = addTailSegment(tail2, "tail3", 48, 0, true);
        PartDefinition tail4 = addTailSegment(tail3, "tail4", 48, 0, true);
        PartDefinition tail5 = addTailSegment(tail4, "tail5", 48, 0, true);
        PartDefinition tail6 = addTailSegment(tail5, "tail6", 48, 0, true);
        PartDefinition tail7 = addTailSegment(tail6, "tail7", 48, 0, true);
        PartDefinition tail8 = addTailSegment(tail7, "tail8", 48, 0, true);
        PartDefinition tail9 = addTailSegment(tail8, "tail9", 48, 0, true);
        PartDefinition tail10 = addTailSegment(tail9, "tail10", 48, 0, true);
        addTailSegment(tail10, "tail11", 48, 4, false);

        root.addOrReplaceChild(
                "right_arm",
                CubeListBuilder.create()
                        .texOffs(40, 16)
                        .addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.3F)),
                PartPose.offset(-5.0F, 2.0F, 0.0F)
        );

        root.addOrReplaceChild(
                "left_arm",
                CubeListBuilder.create()
                        .texOffs(40, 16)
                        .mirror()
                        .addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.3F))
                        .mirror(false),
                PartPose.offset(5.0F, 2.0F, 0.0F)
        );

        return LayerDefinition.create(mesh, 64, 32);
    }

    public void configureForChestplate() {
        this.setAllVisible(false);
        this.body.visible = true;
        this.rightArm.visible = true;
        this.leftArm.visible = true;
        this.tail.visible = true;
    }

    private static PartDefinition addTailSegment(PartDefinition parent, String name, int u, int v, boolean rotated) {
        PartPose pose = rotated
                ? PartPose.offsetAndRotation(0.0F, -4.0F, 0.0F, 0.2618F, 0.0F, 0.0F)
                : PartPose.offset(0.0F, -4.0F, 0.0F);
        return parent.addOrReplaceChild(
                name,
                CubeListBuilder.create()
                        .texOffs(u, v)
                        .addBox(-4.0F, -4.0F, 0.0F, 8.0F, 4.0F, 0.0F, new CubeDeformation(0.1F)),
                pose
        );
    }
}
