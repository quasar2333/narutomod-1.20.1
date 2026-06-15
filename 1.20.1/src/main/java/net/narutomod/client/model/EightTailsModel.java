package net.narutomod.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Random;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.generated.LegacyModelLayerDefinitions;
import net.narutomod.entity.TailedBeastEntity;

public final class EightTailsModel extends HumanoidModel<TailedBeastEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            NarutomodMod.location("eight_tails"),
            "main"
    );

    private final ModelPart root;
    private final ModelPart jaw;
    private final ModelPart[][] tails = new ModelPart[8][8];
    private final float[][] tailSwayX = new float[8][8];
    private final float[][] tailSwayY = new float[8][8];
    private final float[][] tailSwayZ = new float[8][8];

    public EightTailsModel(ModelPart root) {
        super(root);
        this.root = root;
        this.jaw = this.head.getChild("jaw");

        for (int i = 0; i < this.tails.length; i++) {
            ModelPart segment = root.getChild("Tail_" + i + "_0");
            this.tails[i][0] = segment;
            for (int j = 1; j < this.tails[i].length; j++) {
                segment = segment.getChild("Tail_" + i + "_" + j);
                this.tails[i][j] = segment;
            }
        }

        Random random = new Random(0L);
        for (int i = 0; i < this.tailSwayX.length; i++) {
            for (int j = 1; j < this.tailSwayX[i].length; j++) {
                float sqrt = Mth.sqrt((float) j);
                this.tailSwayX[i][j] = (random.nextFloat() * 0.1309F + 0.1309F) * sqrt
                        * (random.nextBoolean() ? -1.0F : 1.0F);
                this.tailSwayZ[i][j] = (random.nextFloat() * 0.1309F + 0.1309F) * sqrt
                        * (random.nextBoolean() ? -1.0F : 1.0F);
                this.tailSwayY[i][j] = random.nextFloat() * 0.1745F + 0.1745F;
            }
        }
    }

    public static LayerDefinition createBodyLayer() {
        return LegacyModelLayerDefinitions.EntityEightTails_ModelEightTails_209();
    }

    @Override
    public void setupAnim(TailedBeastEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
        this.root.getAllParts().forEach(ModelPart::resetPose);
        float scaledSwing = limbSwing * 2.0F / Math.max(entity.getBbHeight(), 0.001F);
        super.setupAnim(entity, scaledSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        this.head.y += 4.0F;
        this.hat.y += 4.0F;
        this.rightArm.z += -5.0F;
        this.rightArm.x += -2.0F;
        this.leftArm.z += -5.0F;
        this.leftArm.x += 2.0F;
        this.rightLeg.visible = false;
        this.leftLeg.visible = false;

        for (int i = 0; i < this.tails.length; i++) {
            this.tails[i][0].visible = i < entity.getTailCount();
            for (int j = 1; j < this.tails[i].length; j++) {
                this.tails[i][j].xRot = 0.2618F + Mth.sin((ageInTicks - j) * 0.08F) * this.tailSwayX[i][j];
                this.tails[i][j].zRot = Mth.cos((ageInTicks - j) * 0.08F) * this.tailSwayZ[i][j];
                this.tails[i][j].yRot = Mth.sin((ageInTicks - j) * 0.08F) * this.tailSwayY[i][j];
            }
        }

        if (entity.isShooting()) {
            this.head.xRot += -0.5236F;
            this.hat.xRot += -0.5236F;
            this.jaw.xRot = 1.0472F;
        } else {
            this.jaw.xRot = 0.0F;
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay,
            float red, float green, float blue, float alpha) {
        boolean hatVisible = this.hat.visible;
        this.hat.visible = false;
        super.renderToBuffer(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.hat.visible = hatVisible;
        for (ModelPart[] tail : this.tails) {
            if (tail[0].visible) {
                tail[0].render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
            }
        }
        if (hatVisible) {
            this.hat.render(poseStack, consumer, LightTexture.FULL_BRIGHT, packedOverlay, red, green, blue, alpha);
        }
    }
}
