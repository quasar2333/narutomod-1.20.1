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

public final class ThreeTailsModel extends HumanoidModel<TailedBeastEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            NarutomodMod.location("three_tails"),
            "main"
    );

    private final ModelPart root;
    private final ModelPart jaw;
    private final ModelPart[][] tails = new ModelPart[3][8];
    private final float[][] tailSwayX = new float[3][8];
    private final float[][] tailSwayY = new float[3][8];
    private final float[][] tailSwayZ = new float[3][8];

    public ThreeTailsModel(ModelPart root) {
        super(root);
        this.root = root;
        this.jaw = this.head.getChild("Jaw");

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
                this.tailSwayX[i][j] = (random.nextFloat() * 0.2618F + 0.2618F)
                        * (random.nextBoolean() ? -1.0F : 1.0F);
                this.tailSwayZ[i][j] = (random.nextFloat() * 0.1745F + 0.1745F)
                        * (random.nextBoolean() ? -1.0F : 1.0F);
                this.tailSwayY[i][j] = random.nextFloat() * 0.0873F + 0.0873F;
            }
        }
    }

    public static LayerDefinition createBodyLayer() {
        return LegacyModelLayerDefinitions.EntityThreeTails_ModelThreeTails_248();
    }

    @Override
    public void setupAnim(TailedBeastEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
        this.root.getAllParts().forEach(ModelPart::resetPose);
        this.rightLeg.visible = false;
        this.leftLeg.visible = false;
        float scaledSwing = limbSwing * 2.0F / Math.max(entity.getBbHeight(), 0.001F);
        super.setupAnim(entity, scaledSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        this.head.y += 16.0F;
        this.hat.y += 16.0F;
        this.rightArm.z += 0.5F;
        this.rightArm.x += -0.5F;
        this.leftArm.z += 0.5F;
        this.leftArm.x += 0.5F;

        for (int i = 0; i < this.tails.length; i++) {
            this.tails[i][0].visible = i < entity.getTailCount();
            for (int j = 1; j < this.tails[i].length; j++) {
                float baseX = j <= 4 ? 0.2618F : -0.2618F;
                this.tails[i][j].xRot = baseX + Mth.sin((ageInTicks - j) * 0.05F) * this.tailSwayX[i][j];
                this.tails[i][j].yRot = Mth.sin((ageInTicks - j) * 0.1F) * this.tailSwayY[i][j];
                this.tails[i][j].zRot = Mth.cos((ageInTicks - j) * 0.05F) * this.tailSwayZ[i][j];
            }
        }

        if (entity.isShooting()) {
            this.head.xRot += -0.1745F;
            this.hat.xRot += -0.1745F;
            this.jaw.xRot = 0.7854F;
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
