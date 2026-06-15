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

public final class TwoTailsModel extends EntityModel<TailedBeastEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            NarutomodMod.location("two_tails"),
            "main"
    );

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart jaw;
    private final ModelPart eyes;
    private final ModelPart leg1;
    private final ModelPart leg2;
    private final ModelPart leg3;
    private final ModelPart leg4;
    private final ModelPart headFlamed;
    private final ModelPart bodyFlamed;
    private final ModelPart jawFlamed;
    private final ModelPart leg1Flamed;
    private final ModelPart leg2Flamed;
    private final ModelPart leg3Flamed;
    private final ModelPart leg4Flamed;
    private final ModelPart[][] tails = new ModelPart[2][8];
    private final ModelPart[][] tailsFlamed = new ModelPart[2][8];
    private final float[][] tailSwayX = new float[2][8];
    private final float[][] tailSwayY = new float[2][8];
    private final float[][] tailSwayZ = new float[2][8];

    public TwoTailsModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.jaw = this.head.getChild("Jaw");
        this.eyes = root.getChild("eyes");
        this.leg1 = root.getChild("leg1");
        this.leg2 = root.getChild("leg2");
        this.leg3 = root.getChild("leg3");
        this.leg4 = root.getChild("leg4");
        this.headFlamed = root.getChild("headFlamed");
        this.bodyFlamed = root.getChild("bodyFlamed");
        this.jawFlamed = this.headFlamed.getChild("Jaw2");
        this.leg1Flamed = root.getChild("leg1Flamed");
        this.leg2Flamed = root.getChild("leg2Flamed");
        this.leg3Flamed = root.getChild("leg3Flamed");
        this.leg4Flamed = root.getChild("leg4Flamed");

        for (int i = 0; i < this.tails.length; i++) {
            ModelPart segment = this.body.getChild("Tail_" + i + "_0");
            this.tails[i][0] = segment;
            for (int j = 1; j < this.tails[i].length; j++) {
                segment = segment.getChild("Tail_" + i + "_" + j);
                this.tails[i][j] = segment;
            }

            ModelPart flamedSegment = this.bodyFlamed.getChild("TailFlamed_" + i + "_0");
            this.tailsFlamed[i][0] = flamedSegment;
            for (int j = 1; j < this.tailsFlamed[i].length; j++) {
                flamedSegment = flamedSegment.getChild("TailFlamed_" + i + "_" + j);
                this.tailsFlamed[i][j] = flamedSegment;
            }
        }

        Random random = new Random(0L);
        for (int i = 0; i < this.tailSwayX.length; i++) {
            for (int j = 1; j < this.tailSwayX[i].length; j++) {
                this.tailSwayX[i][j] = (random.nextFloat() * 0.2618F + 0.1745F)
                        * (random.nextBoolean() ? -1.0F : 1.0F);
                this.tailSwayZ[i][j] = (random.nextFloat() * 0.1745F + 0.1745F)
                        * (random.nextBoolean() ? -1.0F : 1.0F);
                this.tailSwayY[i][j] = random.nextFloat() * 0.1745F + 0.1745F;
            }
        }
    }

    public static LayerDefinition createBodyLayer() {
        return LegacyModelLayerDefinitions.EntityTwoTails_ModelTwoTails_298();
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
            boolean visible = i < entity.getTailCount();
            this.tails[i][0].visible = visible;
            this.tailsFlamed[i][0].visible = visible;
            for (int j = 1; j < this.tails[i].length; j++) {
                float xRot = Mth.sin((ageInTicks - j) * 0.1F) * this.tailSwayX[i][j];
                float yRot = Mth.sin((ageInTicks - j) * 0.1F) * this.tailSwayY[i][j];
                float zRot = Mth.cos((ageInTicks - j) * 0.1F) * this.tailSwayZ[i][j];
                applyRotation(this.tails[i][j], xRot, yRot, zRot);
                applyRotation(this.tailsFlamed[i][j], xRot, yRot, zRot);
            }
        }

        if (entity.isShooting()) {
            this.head.xRot += -0.1745F;
            this.jaw.xRot = 0.7854F;
        } else {
            this.jaw.xRot = 0.0F;
        }

        copyRotations(this.head, this.headFlamed);
        copyRotations(this.body, this.bodyFlamed);
        copyRotations(this.leg1, this.leg1Flamed);
        copyRotations(this.leg2, this.leg2Flamed);
        copyRotations(this.leg3, this.leg3Flamed);
        copyRotations(this.leg4, this.leg4Flamed);
        this.jawFlamed.xRot = this.jaw.xRot;
        this.eyes.xRot = this.head.xRot;
        this.eyes.yRot = this.head.yRot;
        this.eyes.zRot = this.head.zRot;
        setFlamedVisible(false);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay,
            float red, float green, float blue, float alpha) {
        setFlamedVisible(false);
        boolean eyesVisible = this.eyes.visible;
        this.eyes.visible = false;
        this.root.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.eyes.visible = eyesVisible;
        if (eyesVisible) {
            this.eyes.render(poseStack, consumer, LightTexture.FULL_BRIGHT, packedOverlay, red, green, blue, alpha);
        }
    }

    public void renderFlameToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay,
            float red, float green, float blue, float alpha) {
        setFlamedVisible(true);
        this.root.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        setFlamedVisible(false);
    }

    private void setFlamedVisible(boolean visible) {
        this.headFlamed.visible = visible;
        this.bodyFlamed.visible = visible;
        this.leg1Flamed.visible = visible;
        this.leg2Flamed.visible = visible;
        this.leg3Flamed.visible = visible;
        this.leg4Flamed.visible = visible;
        this.head.visible = !visible;
        this.body.visible = !visible;
        this.leg1.visible = !visible;
        this.leg2.visible = !visible;
        this.leg3.visible = !visible;
        this.leg4.visible = !visible;
        this.eyes.visible = !visible;
    }

    private static void applyRotation(ModelPart part, float xRot, float yRot, float zRot) {
        part.xRot = xRot;
        part.yRot = yRot;
        part.zRot = zRot;
    }

    private static void copyRotations(ModelPart source, ModelPart target) {
        target.xRot = source.xRot;
        target.yRot = source.yRot;
        target.zRot = source.zRot;
    }
}
