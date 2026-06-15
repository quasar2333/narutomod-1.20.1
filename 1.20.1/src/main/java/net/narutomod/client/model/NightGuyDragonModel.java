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
import net.narutomod.entity.NightGuyDragonEntity;

public final class NightGuyDragonModel extends EntityModel<NightGuyDragonEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            NarutomodMod.location("night_guy_dragon_legacy"),
            "main");

    private static final float[] CHARGE_SPINE_Y = {
            -0.7854F,
            0.7854F,
            0.7854F,
            0.3927F,
            -0.7854F,
            -0.7854F,
            -0.7854F,
            0.7854F
    };

    private final ModelPart head;
    private final ModelPart spine;
    private final ModelPart eyes;
    private final ModelPart[] whiskerLeft = new ModelPart[6];
    private final ModelPart[] whiskerRight = new ModelPart[6];
    private final ModelPart[] spineParts = new ModelPart[8];

    public NightGuyDragonModel(ModelPart root) {
        this.head = root.getChild("head");
        this.spine = root.getChild("spine");
        this.eyes = root.getChild("eyes");
        this.whiskerLeft[0] = this.head.getChild("whiskerLeft_0");
        this.whiskerRight[0] = this.head.getChild("whiskerRight_0");
        for (int i = 1; i < 6; i++) {
            this.whiskerLeft[i] = this.whiskerLeft[i - 1].getChild("whiskerLeft_" + i);
            this.whiskerRight[i] = this.whiskerRight[i - 1].getChild("whiskerRight_" + i);
        }
        this.spineParts[0] = this.spine;
        for (int i = 1; i < 8; i++) {
            this.spineParts[i] = this.spineParts[i - 1].getChild("spine" + (i + 1));
        }
    }

    public static LayerDefinition createBodyLayer() {
        return LegacyModelLayerDefinitions.ItemEightGates_ModelNightguyDragon_971();
    }

    @Override
    public void setupAnim(NightGuyDragonEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
        for (int i = 2; i < 6; i++) {
            this.whiskerLeft[i].zRot = 0.0873F * ageInTicks;
            this.whiskerRight[i].zRot = -0.0873F * ageInTicks;
        }
        if (entity.isLaunched()) {
            float cos = Mth.cos(limbSwing * 0.6662F);
            float sin = Mth.sin(limbSwing * 0.6662F);
            this.spineParts[0].yRot = cos * 0.1F * limbSwingAmount;
            this.spineParts[1].yRot = cos * 0.1F * limbSwingAmount;
            this.spineParts[2].yRot = sin * 0.1F * limbSwingAmount;
            this.spineParts[3].yRot = sin * 0.1F * limbSwingAmount;
            this.spineParts[4].yRot = cos * 0.1F * limbSwingAmount;
            this.spineParts[5].yRot = cos * 0.1F * limbSwingAmount;
            this.spineParts[6].yRot = sin * 0.1F * limbSwingAmount;
            this.spineParts[7].yRot = sin * 0.1F * limbSwingAmount;
        } else {
            for (int i = 0; i < this.spineParts.length; i++) {
                this.spineParts[i].yRot = CHARGE_SPINE_Y[i];
            }
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay,
            float red, float green, float blue, float alpha) {
        this.head.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.spine.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.eyes.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
