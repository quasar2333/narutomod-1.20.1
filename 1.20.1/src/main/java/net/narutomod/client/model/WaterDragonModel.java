package net.narutomod.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
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
import net.narutomod.entity.WaterDragonEntity;
import net.narutomod.procedure.ProcedureUtils;

public final class WaterDragonModel extends EntityModel<WaterDragonEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        NarutomodMod.location("water_dragon_legacy"),
        "main"
    );

    private static final float[] HORN_DEFORMATIONS = {0.8F, 0.6F, 0.4F, 0.2F, 0.0F};
    private static final float[] WHISKER_DEFORMATIONS = {0.8F, 0.7F, 0.6F, 0.5F, 0.4F, 0.2F};

    private final ModelPart head;
    private final ModelPart teethUpper;
    private final ModelPart teethLower;
    private final ModelPart jaw;
    private final ModelPart[] whiskerLeft = new ModelPart[6];
    private final ModelPart[] whiskerRight = new ModelPart[6];
    private final ModelPart[] spine = new ModelPart[WaterDragonEntity.SPINE_SEGMENTS];
    private final ModelPart eyes;

    public WaterDragonModel(ModelPart root) {
        this.head = root.getChild("head");
        this.teethUpper = this.head.getChild("teeth_upper");
        this.jaw = this.head.getChild("jaw");
        this.teethLower = this.jaw.getChild("teeth_lower");
        this.eyes = root.getChild("eyes");

        this.whiskerLeft[0] = this.head.getChild("whisker_left_0");
        this.whiskerRight[0] = this.head.getChild("whisker_right_0");
        for (int i = 1; i < this.whiskerLeft.length; i++) {
            this.whiskerLeft[i] = this.whiskerLeft[i - 1].getChild("whisker_left_" + i);
            this.whiskerRight[i] = this.whiskerRight[i - 1].getChild("whisker_right_" + i);
        }

        this.spine[0] = root.getChild("spine_0");
        for (int i = 1; i < this.spine.length; i++) {
            this.spine[i] = this.spine[i - 1].getChild("spine_" + i);
        }
    }

    public static LayerDefinition createBodyLayer() {
        return createBodyLayer(WaterDragonEntity.SPINE_SEGMENTS);
    }

    public static LayerDefinition createBodyLayer(int spineSegments) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild(
            "head",
            addBox(addBox(addBox(addBox(CubeListBuilder.create(),
                176, 44, -6.0F, 6.0F, -26.0F, 12.0F, 5.0F, 16.0F, 1.0F, false),
                112, 30, -8.0F, -1.0F, -11.0F, 16.0F, 16.0F, 16.0F, 1.0F, false),
                112, 0, -5.0F, 5.0F, -26.0F, 2.0F, 2.0F, 4.0F, 1.0F, false),
                112, 0, 3.0F, 5.0F, -26.0F, 2.0F, 2.0F, 4.0F, 1.0F, true),
            PartPose.ZERO
        );
        head.addOrReplaceChild(
            "teeth_upper",
            box(152, 146, -6.0F, -12.0F, -26.0F, 12.0F, 2.0F, 16.0F, 0.5F, false),
            PartPose.offset(0.0F, 24.0F, 0.0F)
        );
        head.addOrReplaceChild(
            "bone_right",
            box(0, 200, 0.0F, -8.0F, 0.0F, 8.0F, 16.0F, 0.0F, 0.0F, false),
            PartPose.offsetAndRotation(9.0F, 7.0F, -11.0F, 0.0F, -0.7854F, 0.0F)
        );
        head.addOrReplaceChild(
            "bone_left",
            box(0, 200, -8.0F, -8.0F, 0.0F, 8.0F, 16.0F, 0.0F, 0.0F, true),
            PartPose.offsetAndRotation(-9.0F, 7.0F, -11.0F, 0.0F, 0.7854F, 0.0F)
        );
        PartDefinition jaw = head.addOrReplaceChild(
            "jaw",
            box(176, 65, -6.0F, 0.0F, -16.75F, 12.0F, 4.0F, 16.0F, 1.0F, false),
            PartPose.offset(0.0F, 11.0F, -9.0F)
        );
        jaw.addOrReplaceChild(
            "teeth_lower",
            box(112, 144, -6.0F, -16.0F, -25.75F, 12.0F, 2.0F, 16.0F, 0.5F, false),
            PartPose.offset(0.0F, 13.0F, 9.0F)
        );

        addHorn(head, "horn_right", -6.0F, -0.5236F, 0.0873F, false);
        addHorn(head, "horn_left", 6.0F, 0.5236F, -0.0873F, true);
        addWhisker(head, "whisker_left", 6.0F, 1.0472F, -0.1745F, true);
        addWhisker(head, "whisker_right", -6.0F, -1.0472F, 0.1745F, false);
        addSpine(root, spineSegments);

        root.addOrReplaceChild(
            "eyes",
            addBox(addBox(CubeListBuilder.create(),
                130, 50, -6.6F, 2.6F, -12.1F, 3.0F, 2.0F, 0.0F, 0.0F, false),
                130, 50, 3.6F, 2.6F, -12.1F, 3.0F, 2.0F, 0.0F, 0.0F, true),
            PartPose.ZERO
        );

        return LayerDefinition.create(mesh, 256, 256);
    }

    @Override
    public void setupAnim(WaterDragonEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
        this.head.xRot = headPitch * Mth.DEG_TO_RAD;
        this.jaw.xRot = entity.isWaiting() ? 0.0F : 0.5236F;
        for (int i = 2; i < 6; i++) {
            this.whiskerLeft[i].zRot = 0.2618F * ageInTicks;
            this.whiskerRight[i].zRot = -0.2618F * ageInTicks;
        }

        List<ProcedureUtils.Vec2f> rotations = entity.getPartRotations();
        for (int i = 0; i < this.spine.length; i++) {
            if (i < rotations.size()) {
                ProcedureUtils.Vec2f rotation = rotations.get(i);
                this.spine[i].visible = true;
                this.spine[i].xRot = -rotation.y * Mth.DEG_TO_RAD;
                this.spine[i].yRot = -rotation.x * Mth.DEG_TO_RAD;
            } else {
                this.spine[i].visible = false;
            }
        }
    }

    public void setFaceDetailsVisible(boolean visible) {
        this.teethUpper.visible = visible;
        this.teethLower.visible = visible;
        this.eyes.visible = visible;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay,
            float red, float green, float blue, float alpha) {
        this.head.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.spine[0].render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.eyes.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    private static void addHorn(PartDefinition head, String name, float x, float baseYRot, float segmentYRot, boolean mirror) {
        PartDefinition current = head.addOrReplaceChild(
            name,
            box(0, 0, -1.0F, -2.0F, 0.0F, 2.0F, 4.0F, 6.0F, 1.0F, mirror),
            PartPose.offsetAndRotation(x, -2.0F, -13.0F, 0.0873F, baseYRot, 0.0F)
        );
        for (int i = 0; i < HORN_DEFORMATIONS.length; i++) {
            current = current.addOrReplaceChild(
                name + "_" + i,
                box(0, 0, -1.0F, -2.0F, 0.0F, 2.0F, 4.0F, 6.0F, HORN_DEFORMATIONS[i], mirror),
                PartPose.offsetAndRotation(0.0F, 0.0F, 7.0F, 0.0873F, segmentYRot, 0.0F)
            );
        }
    }

    private static void addWhisker(PartDefinition head, String name, float x, float baseYRot,
            float segmentYRot, boolean mirror) {
        PartDefinition current = head.addOrReplaceChild(
            name + "_0",
            box(0, 0, -1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 6.0F, WHISKER_DEFORMATIONS[0], mirror),
            PartPose.offsetAndRotation(x, 6.0F, -24.0F, 0.0F, baseYRot, 0.0F)
        );
        for (int i = 1; i < WHISKER_DEFORMATIONS.length; i++) {
            current = current.addOrReplaceChild(
                name + "_" + i,
                box(0, 0, -1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 6.0F, WHISKER_DEFORMATIONS[i], mirror),
                PartPose.offsetAndRotation(0.0F, 0.0F, 6.0F, -0.0873F, segmentYRot, 0.0F)
            );
        }
    }

    private static void addSpine(PartDefinition root, int spineSegments) {
        PartDefinition current = root;
        for (int i = 0; i < spineSegments; i++) {
            current = current.addOrReplaceChild(
                "spine_" + i,
                addBox(
                    box(192, 104, -5.0F, -4.5F, 0.0F, 10.0F, 10.0F, 10.0F, 2.0F, false),
                    48, 0, -1.0F, -10.5F, 2.0F, 2.0F, 4.0F, 6.0F, 1.0F, false
                ),
                i == 0 ? PartPose.offset(0.0F, 6.5F, 7.0F) : PartPose.offset(0.0F, 0.0F, 11.0F)
            );
        }
    }

    private static CubeListBuilder box(int u, int v, float x, float y, float z, float width, float height,
            float depth, float deformation, boolean mirror) {
        return addBox(CubeListBuilder.create(), u, v, x, y, z, width, height, depth, deformation, mirror);
    }

    private static CubeListBuilder addBox(CubeListBuilder builder, int u, int v, float x, float y, float z,
            float width, float height, float depth, float deformation, boolean mirror) {
        builder.texOffs(u, v);
        if (mirror) {
            builder.mirror();
        }
        builder.addBox(x, y, z, width, height, depth, new CubeDeformation(deformation));
        if (mirror) {
            builder.mirror(false);
        }
        return builder;
    }
}
