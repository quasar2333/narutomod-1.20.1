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

public final class OneTailModel extends HumanoidModel<TailedBeastEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            NarutomodMod.location("one_tail"),
            "main"
    );

    private final ModelPart root;
    private final ModelPart jaw;
    private final ModelPart[] tail = new ModelPart[10];
    private final float[] tailSwayX = new float[10];
    private final float[] tailSwayY = new float[10];
    private final float[] tailSwayZ = new float[10];

    public OneTailModel(ModelPart root) {
        super(root);
        this.root = root;
        this.jaw = this.head.getChild("jaw");

        ModelPart segment = this.body.getChild("Tail_0");
        this.tail[0] = segment;
        for (int j = 1; j < this.tail.length; j++) {
            segment = segment.getChild("Tail_" + j);
            this.tail[j] = segment;
        }

        Random random = new Random(0L);
        for (int j = 1; j < this.tail.length; j++) {
            this.tailSwayX[j] = (random.nextFloat() * 0.1745F + 0.1745F)
                    * (random.nextBoolean() ? -1.0F : 1.0F);
            this.tailSwayZ[j] = (random.nextFloat() * 0.1745F + 0.1745F)
                    * (random.nextBoolean() ? -1.0F : 1.0F);
            this.tailSwayY[j] = random.nextFloat() * 0.0873F + 0.0873F;
        }
    }

    public static LayerDefinition createBodyLayer() {
        return LegacyModelLayerDefinitions.EntityOneTail_ModelOneTail_224();
    }

    @Override
    public void setupAnim(TailedBeastEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
        this.root.getAllParts().forEach(ModelPart::resetPose);
        float scaledSwing = limbSwing * 2.0F / Math.max(entity.getBbHeight(), 0.001F);
        super.setupAnim(entity, scaledSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        this.head.y += 11.0F;
        this.hat.y += 11.0F;
        this.rightArm.z += -1.5F;
        this.rightArm.x += 1.0F;
        this.leftArm.z += -1.5F;
        this.leftArm.x += -1.0F;
        this.rightLeg.y += 6.0F;
        this.leftLeg.y += 6.0F;

        for (int j = 1; j < this.tail.length; j++) {
            this.tail[j].xRot = 0.2618F + Mth.sin((ageInTicks - j) * 0.05F) * this.tailSwayX[j];
            this.tail[j].yRot = Mth.sin((ageInTicks - j) * 0.1F) * this.tailSwayY[j];
            this.tail[j].zRot = Mth.cos((ageInTicks - j) * 0.05F) * this.tailSwayZ[j];
        }

        if (entity.isShooting()) {
            this.head.xRot += -0.5236F;
            this.hat.xRot += -0.5236F;
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
        if (hatVisible) {
            this.hat.render(poseStack, consumer, LightTexture.FULL_BRIGHT, packedOverlay, red, green, blue, alpha);
        }
    }
}
