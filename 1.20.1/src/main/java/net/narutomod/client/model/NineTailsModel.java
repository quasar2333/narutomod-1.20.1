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

public final class NineTailsModel extends EntityModel<TailedBeastEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            NarutomodMod.location("nine_tails"),
            "main"
    );

    private final ModelPart bodyRoot;
    private final ModelPart hat;
    private final ModelPart eyes;
    private final ModelPart head;
    private final ModelPart snout;
    private final ModelPart jaw;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart upperLegRight;
    private final ModelPart midLegRight;
    private final ModelPart rightFoot;
    private final ModelPart upperLegLeft;
    private final ModelPart midLegLeft;
    private final ModelPart leftFoot;
    private final ModelPart[][] tails = new ModelPart[9][8];
    private final float[][] tailSwayX = new float[9][2];
    private final float[][] tailSwayZ = new float[9][2];
    private float legacyAlpha = 1.0F;

    public NineTailsModel(ModelPart root) {
        this.bodyRoot = root.getChild("body");
        this.hat = root.getChild("hat");
        this.eyes = this.hat.getChild("eyes");
        this.head = this.bodyRoot.getChild("head");
        this.snout = this.head.getChild("snout");
        this.jaw = this.head.getChild("jaw");
        this.body = this.bodyRoot.getChild("body");
        this.rightArm = this.body.getChild("right_arm");
        this.leftArm = this.body.getChild("left_arm");
        this.rightLeg = this.bodyRoot.getChild("right_leg");
        this.leftLeg = this.bodyRoot.getChild("left_leg");
        this.upperLegRight = this.rightLeg.getChild("upperLegRight");
        this.midLegRight = this.upperLegRight.getChild("midLegRight");
        this.rightFoot = this.midLegRight.getChild("lowerLegRight").getChild("rightFoot");
        this.upperLegLeft = this.leftLeg.getChild("upperLegLeft");
        this.midLegLeft = this.upperLegLeft.getChild("midLegLeft");
        this.leftFoot = this.midLegLeft.getChild("lowerLegLeft").getChild("leftFoot");

        ModelPart tailsRoot = this.bodyRoot.getChild("tails");
        for (int i = 0; i < this.tails.length; i++) {
            ModelPart segment = tailsRoot.getChild("Tail_" + i + "_0");
            this.tails[i][0] = segment;
            for (int j = 1; j < this.tails[i].length; j++) {
                segment = segment.getChild("Tail_" + i + "_" + j);
                this.tails[i][j] = segment;
            }
        }

        Random random = new Random(0L);
        for (int i = 0; i < this.tailSwayX.length; i++) {
            for (int j = 0; j < this.tailSwayX[i].length; j++) {
                this.tailSwayX[i][j] = (random.nextFloat() * 0.1745F + 0.1745F) * (random.nextBoolean() ? -1.0F : 1.0F);
                this.tailSwayZ[i][j] = (random.nextFloat() * 0.1745F + 0.1745F) * (random.nextBoolean() ? -1.0F : 1.0F);
            }
        }
    }

    public static LayerDefinition createBodyLayer() {
        return LegacyModelLayerDefinitions.EntityNineTails_ModelNineTails_255();
    }

    public void setLegacyAlpha(float legacyAlpha) {
        this.legacyAlpha = legacyAlpha;
    }

    @Override
    public void setupAnim(TailedBeastEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
        this.bodyRoot.getAllParts().forEach(ModelPart::resetPose);
        this.hat.getAllParts().forEach(ModelPart::resetPose);

        applyLegacyBipedAnimation(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        for (int i = 0; i < this.tails.length; i++) {
            this.tails[i][0].visible = i < entity.getTailCount();
            for (int j = 1; j < this.tails[i].length; j++) {
                float swayX = Mth.sin((ageInTicks - j) * 0.2F) * this.tailSwayX[i][0];
                float swayZ = Mth.cos((ageInTicks - j) * 0.2F) * this.tailSwayZ[i][1];
                if (j == 4) {
                    this.tails[i][j].xRot = 0.1745F + swayX;
                    this.tails[i][j].zRot = swayZ;
                } else if (j < 4) {
                    this.tails[i][j].xRot = swayX;
                    this.tails[i][j].zRot = -0.1745F + swayZ;
                } else {
                    this.tails[i][j].xRot = swayX;
                    this.tails[i][j].zRot = 0.1745F + swayZ;
                }
            }
        }

        if (entity.onGround()) {
            setRotation(this.upperLegRight, -0.5236F, 0.0F, 1.5708F);
            setRotation(this.midLegRight, 0.0F, 0.0F, -2.3562F);
            setRotation(this.rightFoot, 0.0F, -3.1416F, 0.0F);
            setRotation(this.upperLegLeft, -0.5236F, 0.0F, -1.5708F);
            setRotation(this.midLegLeft, 0.0F, 0.0F, 2.3562F);
            setRotation(this.leftFoot, 0.0F, 3.1416F, 0.0F);
        } else {
            setRotation(this.upperLegRight, -0.5236F, 0.0F, 0.7854F);
            setRotation(this.midLegRight, 0.0F, 0.0F, -1.5708F);
            setRotation(this.rightFoot, 0.0F, 3.1416F, -0.5236F);
            setRotation(this.upperLegLeft, -0.5236F, 0.0F, -0.7854F);
            setRotation(this.midLegLeft, 0.0F, 0.0F, 1.5708F);
            setRotation(this.leftFoot, 0.0F, 3.1416F, 0.5236F);
        }

        if (entity.isShooting()) {
            this.snout.xRot = -0.2618F;
            this.jaw.xRot = 1.0472F;
        } else {
            this.snout.xRot = 0.0F;
            this.jaw.xRot = 0.0F;
        }

        this.hat.copyFrom(this.bodyRoot);
        this.eyes.copyFrom(this.head);
    }

    private void applyLegacyBipedAnimation(TailedBeastEntity entity, float limbSwing, float limbSwingAmount,
            float ageInTicks, float netHeadYaw, float headPitch) {
        float scaledSwing = limbSwing * 2.0F / Math.max(entity.getBbHeight(), 0.001F);
        this.head.setPos(0.0F, -25.0F, -4.0F);
        this.rightArm.setPos(-6.0F, -23.0F, -3.0F);
        this.leftArm.setPos(6.0F, -23.0F, -3.0F);
        this.rightLeg.setPos(-1.9F, -12.0F, 4.5F);
        this.leftLeg.setPos(1.9F, -12.0F, 4.5F);

        this.head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        this.head.xRot = headPitch * Mth.DEG_TO_RAD;
        this.body.yRot = 0.0F;

        this.rightArm.yRot = 0.0F;
        this.leftArm.yRot = 0.0F;
        this.rightArm.zRot = 0.0F;
        this.leftArm.zRot = 0.0F;
        this.rightLeg.yRot = 0.0F;
        this.leftLeg.yRot = 0.0F;
        this.rightLeg.zRot = 0.0F;
        this.leftLeg.zRot = 0.0F;

        this.rightArm.xRot = Mth.cos(scaledSwing * 0.6662F + Mth.PI) * 2.0F * limbSwingAmount * 0.5F;
        this.leftArm.xRot = Mth.cos(scaledSwing * 0.6662F) * 2.0F * limbSwingAmount * 0.5F;
        this.rightLeg.xRot = Mth.cos(scaledSwing * 0.6662F) * 1.4F * limbSwingAmount;
        this.leftLeg.xRot = Mth.cos(scaledSwing * 0.6662F + Mth.PI) * 1.4F * limbSwingAmount;

        this.rightArm.zRot += Mth.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
        this.leftArm.zRot -= Mth.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
        this.rightArm.xRot += Mth.sin(ageInTicks * 0.067F) * 0.05F;
        this.leftArm.xRot -= Mth.sin(ageInTicks * 0.067F) * 0.05F;
    }

    private static void setRotation(ModelPart part, float xRot, float yRot, float zRot) {
        part.xRot = xRot;
        part.yRot = yRot;
        part.zRot = zRot;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay,
            float red, float green, float blue, float alpha) {
        float finalAlpha = alpha * this.legacyAlpha;
        this.bodyRoot.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, finalAlpha);
        this.hat.render(poseStack, consumer, LightTexture.FULL_BRIGHT, packedOverlay, red, green, blue, finalAlpha);
    }
}
