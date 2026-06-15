package net.narutomod.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.util.Mth;
import net.narutomod.NarutomodMod;
import net.narutomod.entity.PurpleDragonEntity;
import net.narutomod.procedure.ProcedureUtils;

public final class PurpleDragonModel extends EntityModel<PurpleDragonEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        NarutomodMod.location("purple_dragon_legacy"),
        "main"
    );

    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart[] whiskerLeft = new ModelPart[6];
    private final ModelPart[] whiskerRight = new ModelPart[6];
    private final ModelPart[] spine = new ModelPart[PurpleDragonEntity.SPINE_SEGMENTS];
    private final ModelPart eyes;

    public PurpleDragonModel(ModelPart root) {
        this.head = root.getChild("head");
        this.jaw = this.head.getChild("jaw");
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
        return WaterDragonModel.createBodyLayer(PurpleDragonEntity.SPINE_SEGMENTS);
    }

    @Override
    public void setupAnim(PurpleDragonEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
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

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay,
            float red, float green, float blue, float alpha) {
        this.head.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.spine[0].render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.eyes.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
