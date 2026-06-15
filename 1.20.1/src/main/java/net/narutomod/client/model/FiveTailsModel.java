package net.narutomod.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Random;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.generated.LegacyModelLayerDefinitions;
import net.narutomod.entity.TailedBeastEntity;

public final class FiveTailsModel extends EntityModel<TailedBeastEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            NarutomodMod.location("five_tails"),
            "main"
    );

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart eyes;
    private final ModelPart leg1;
    private final ModelPart leg2;
    private final ModelPart leg3;
    private final ModelPart leg4;
    private final ModelPart[][] tails = new ModelPart[5][8];
    private final float[][] tailSwayX = new float[5][8];
    private final float[][] tailSwayZ = new float[5][8];

    public FiveTailsModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.jaw = this.head.getChild("Jaw");
        this.eyes = root.getChild("Eyes");
        this.leg1 = root.getChild("leg1");
        this.leg2 = root.getChild("leg2");
        this.leg3 = root.getChild("leg3");
        this.leg4 = root.getChild("leg4");

        ModelPart body = root.getChild("body");
        for (int i = 0; i < this.tails.length; i++) {
            ModelPart segment = body.getChild("Tail_" + i + "_0");
            this.tails[i][0] = segment;
            for (int j = 1; j < this.tails[i].length; j++) {
                segment = segment.getChild("Tail_" + i + "_" + j);
                this.tails[i][j] = segment;
            }
        }

        Random random = new Random(0L);
        for (int i = 0; i < this.tailSwayX.length; i++) {
            for (int j = 1; j < this.tailSwayX[i].length; j++) {
                this.tailSwayX[i][j] = (random.nextFloat() * 0.2618F + 0.2618F)
                        * (random.nextBoolean() ? -1.0F : 1.0F);
                this.tailSwayZ[i][j] = (random.nextFloat() * 0.1745F + 0.1745F)
                        * (random.nextBoolean() ? -1.0F : 1.0F);
            }
        }
    }

    public static LayerDefinition createBodyLayer() {
        return LegacyModelLayerDefinitions.EntityFiveTails_ModelFiveTails_222();
    }

    @Override
    public void setupAnim(TailedBeastEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
        this.root.getAllParts().forEach(ModelPart::resetPose);

        float scaledSwing = limbSwing * 2.0F / Math.max(entity.getBbHeight(), 0.001F);
        this.head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        this.head.xRot = headPitch * Mth.DEG_TO_RAD;
        this.leg1.xRot = Mth.cos(scaledSwing * 0.6662F) * 1.4F * limbSwingAmount;
        this.leg2.xRot = Mth.cos(scaledSwing * 0.6662F + Mth.PI) * 1.4F * limbSwingAmount;
        this.leg3.xRot = Mth.cos(scaledSwing * 0.6662F + Mth.PI) * 1.4F * limbSwingAmount;
        this.leg4.xRot = Mth.cos(scaledSwing * 0.6662F) * 1.4F * limbSwingAmount;

        for (int i = 0; i < this.tails.length; i++) {
            this.tails[i][0].visible = i < entity.getTailCount();
            for (int j = 1; j < this.tails[i].length; j++) {
                this.tails[i][j].xRot = Mth.sin((ageInTicks - j) * 0.2F) * this.tailSwayX[i][j];
                this.tails[i][j].zRot = Mth.cos((ageInTicks - j) * 0.2F) * this.tailSwayZ[i][j];
            }
        }

        if (entity.isShooting()) {
            this.head.xRot += -0.1745F;
            this.jaw.xRot = 0.7854F;
        } else {
            this.jaw.xRot = 0.0F;
        }
        this.eyes.xRot = this.head.xRot;
        this.eyes.yRot = this.head.yRot;
        this.eyes.zRot = this.head.zRot;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay,
            float red, float green, float blue, float alpha) {
        boolean eyesVisible = this.eyes.visible;
        this.eyes.visible = false;
        this.root.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.eyes.visible = eyesVisible;
        if (eyesVisible) {
            this.eyes.render(poseStack, consumer, LightTexture.FULL_BRIGHT, packedOverlay, red, green, blue, alpha);
        }
    }
}
