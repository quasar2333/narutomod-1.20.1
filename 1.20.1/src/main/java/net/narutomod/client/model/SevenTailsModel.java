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

public final class SevenTailsModel extends HumanoidModel<TailedBeastEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            NarutomodMod.location("seven_tails"),
            "main"
    );

    private final ModelPart root;
    private final ModelPart[] tailFans = new ModelPart[6];
    private final ModelPart[] tailSegments = new ModelPart[10];
    private final float[] tailSwayX = new float[10];
    private final float[] tailSwayY = new float[10];
    private final float[] tailSwayZ = new float[10];

    public SevenTailsModel(ModelPart root) {
        super(root);
        this.root = root;

        ModelPart tailRoot = this.body.getChild("stomach")
                .getChild("bone11")
                .getChild("bone10")
                .getChild("bone9")
                .getChild("bone7")
                .getChild("bone13")
                .getChild("bone14")
                .getChild("bone15")
                .getChild("bone16")
                .getChild("bone17");

        for (int i = 0; i < this.tailFans.length; i++) {
            this.tailFans[i] = tailRoot.getChild("tail_" + i);
        }

        ModelPart segment = tailRoot.getChild("tail6_0");
        this.tailSegments[0] = segment;
        for (int i = 1; i < this.tailSegments.length; i++) {
            segment = segment.getChild("tail6_" + i);
            this.tailSegments[i] = segment;
        }

        Random random = new Random(0L);
        for (int i = 0; i < this.tailSwayX.length; i++) {
            this.tailSwayX[i] = (random.nextFloat() * 0.2618F + 0.2618F)
                    * (random.nextBoolean() ? -1.0F : 1.0F);
            this.tailSwayZ[i] = (random.nextFloat() * 0.1745F + 0.1745F)
                    * (random.nextBoolean() ? -1.0F : 1.0F);
            this.tailSwayY[i] = random.nextFloat() * 0.0873F + 0.0873F;
        }
    }

    public static LayerDefinition createBodyLayer() {
        return LegacyModelLayerDefinitions.EntitySevenTails_ModelSevenTails_343();
    }

    @Override
    public void setupAnim(TailedBeastEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
        this.root.getAllParts().forEach(ModelPart::resetPose);
        this.leftLeg.visible = false;
        this.rightLeg.visible = false;
        float scaledSwing = limbSwing * 2.0F / Math.max(entity.getBbHeight(), 0.001F);
        super.setupAnim(entity, scaledSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        this.head.y += 2.5F;
        this.hat.y += 2.5F;
        this.rightArm.z += 0.75F;
        this.rightArm.x += 2.75F;
        this.leftArm.z += 0.75F;
        this.leftArm.x += -2.75F;

        this.tailFans[0].yRot = Mth.sin(ageInTicks) * 0.0873F;
        this.tailFans[1].yRot = Mth.sin(ageInTicks) * 0.0873F + 0.3927F;
        this.tailFans[2].yRot = Mth.sin(ageInTicks) * 0.0873F + 0.7854F;
        this.tailFans[3].yRot = Mth.cos(ageInTicks) * 0.0873F - 0.7854F;
        this.tailFans[4].yRot = Mth.cos(ageInTicks) * 0.0873F - 0.3927F;
        this.tailFans[5].yRot = Mth.cos(ageInTicks) * 0.0873F;

        for (int i = 1; i < this.tailSegments.length; i++) {
            this.tailSegments[i].xRot = 0.1745F + Mth.sin((ageInTicks - i) * 0.05F) * this.tailSwayX[i];
            this.tailSegments[i].yRot = Mth.sin((ageInTicks - i) * 0.05F) * this.tailSwayY[i];
            this.tailSegments[i].zRot = Mth.cos((ageInTicks - i) * 0.05F) * this.tailSwayZ[i];
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
