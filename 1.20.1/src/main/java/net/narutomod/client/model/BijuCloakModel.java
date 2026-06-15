package net.narutomod.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Random;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.generated.LegacyModelLayerDefinitions;

public final class BijuCloakModel<T extends LivingEntity> extends HumanoidModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            NarutomodMod.location("biju_cloak_legacy"),
            "main"
    );
    private static final int[] TAIL_SHOW_MAP = {0, 1, 6, 0x19, 0x1E, 0x1F, 0x1F8, 0x7F, 0x1FE, 0x1FF};

    private final ModelPart[] earLeft = new ModelPart[6];
    private final ModelPart[] earRight = new ModelPart[6];
    private final ModelPart[][] tail = new ModelPart[9][8];
    private final ModelPart bipedBodyWear;
    private final ModelPart bipedRightArmWear;
    private final ModelPart bipedLeftArmWear;
    private final ModelPart bipedRightLegWear;
    private final ModelPart bipedLeftLegWear;
    private final float[][] tailSwayX = new float[9][8];
    private final float[][] tailSwayZ = new float[9][8];
    private final float[] leftEarSwayX = new float[6];
    private final float[] leftEarSwayZ = new float[6];
    private final float[] rightEarSwayX = new float[6];
    private final float[] rightEarSwayZ = new float[6];

    public BijuCloakModel(ModelPart root) {
        super(root);
        this.earLeft[0] = this.head.getChild("earLeft_0");
        this.earRight[0] = this.head.getChild("earRight_0");
        for (int i = 1; i < this.earLeft.length; i++) {
            this.earLeft[i] = this.earLeft[i - 1].getChild("earLeft_" + i);
            this.earRight[i] = this.earRight[i - 1].getChild("earRight_" + i);
        }
        for (int i = 0; i < this.tail.length; i++) {
            this.tail[i][0] = this.body.getChild("tail_" + i + "_0");
            for (int j = 1; j < this.tail[i].length; j++) {
                this.tail[i][j] = this.tail[i][j - 1].getChild("tail_" + i + "_" + j);
            }
        }
        this.bipedBodyWear = root.getChild("bipedBodyWear");
        this.bipedRightArmWear = root.getChild("bipedRightArmWear");
        this.bipedLeftArmWear = root.getChild("bipedLeftArmWear");
        this.bipedRightLegWear = root.getChild("bipedRightLegWear");
        this.bipedLeftLegWear = root.getChild("bipedLeftLegWear");
        initializeSwayTables();
    }

    public static LayerDefinition createBodyLayer() {
        return LegacyModelLayerDefinitions.ItemBijuCloak_ModelBijuCloak_413();
    }

    public void configureTailVisibility(int numberOfTails) {
        int tails = Mth.clamp(numberOfTails, 0, 9);
        int mask = TAIL_SHOW_MAP[tails];
        setAllVisible(true);
        for (int i = 0; i < this.tail.length; i++) {
            this.tail[i][0].visible = (mask & (1 << i)) != 0;
        }
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        float swaySin = Mth.sin(ageInTicks * 0.15F);
        float swayCos = Mth.cos(ageInTicks * 0.15F);
        for (int i = 1; i < 6; i++) {
            this.earLeft[i].xRot = -0.1745F + swaySin * this.leftEarSwayX[i];
            this.earLeft[i].zRot = swayCos * this.leftEarSwayZ[i];
            this.earRight[i].xRot = 0.1745F + swaySin * this.rightEarSwayX[i];
            this.earRight[i].zRot = swayCos * this.rightEarSwayZ[i];
        }
        for (int i = 0; i < this.tail.length; i++) {
            for (int j = 2; j < this.tail[i].length; j++) {
                this.tail[i][j].xRot = 0.2618F + swaySin * this.tailSwayX[i][j];
                this.tail[i][j].zRot = swayCos * this.tailSwayZ[i][j];
            }
        }
        syncOverlayParts();
    }

    @Override
    public void setAllVisible(boolean visible) {
        super.setAllVisible(visible);
        for (int i = 0; i < this.tail.length; i++) {
            this.tail[i][0].visible = visible;
        }
        this.bipedBodyWear.visible = visible;
        this.bipedRightArmWear.visible = visible;
        this.bipedLeftArmWear.visible = visible;
        this.bipedRightLegWear.visible = visible;
        this.bipedLeftLegWear.visible = visible;
    }

    public void renderBase(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay,
                           float red, float green, float blue, float alpha) {
        boolean headwearVisible = this.hat.visible;
        this.hat.visible = false;
        super.renderToBuffer(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.hat.visible = headwearVisible;
    }

    public void renderOverlay(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay,
                              float red, float green, float blue, float alpha) {
        syncOverlayParts();
        poseStack.pushPose();
        if (this.crouching) {
            poseStack.translate(0.0D, 0.2D, 0.0D);
        }
        this.hat.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.bipedBodyWear.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.bipedRightArmWear.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.bipedLeftArmWear.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.bipedRightLegWear.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.bipedLeftLegWear.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        poseStack.popPose();
    }

    private void initializeSwayTables() {
        Random random = new Random(0L);
        for (int i = 1; i < 6; i++) {
            this.leftEarSwayX[i] = (random.nextFloat() * 0.2618F + 0.0873F) * randomSign(random);
            this.leftEarSwayZ[i] = (random.nextFloat() * 0.2618F + 0.0873F) * randomSign(random);
            this.rightEarSwayX[i] = (random.nextFloat() * 0.2618F + 0.0873F) * randomSign(random);
            this.rightEarSwayZ[i] = (random.nextFloat() * 0.2618F + 0.0873F) * randomSign(random);
        }
        for (int i = 0; i < this.tail.length; i++) {
            for (int j = 1; j < this.tail[i].length; j++) {
                this.tailSwayX[i][j] = (random.nextFloat() * 0.1745F + 0.1745F) * randomSign(random);
                this.tailSwayZ[i][j] = (random.nextFloat() * 0.2618F + 0.2618F) * randomSign(random);
            }
        }
    }

    private void syncOverlayParts() {
        this.hat.copyFrom(this.head);
        this.bipedBodyWear.copyFrom(this.body);
        this.bipedRightArmWear.copyFrom(this.rightArm);
        this.bipedLeftArmWear.copyFrom(this.leftArm);
        this.bipedRightLegWear.copyFrom(this.rightLeg);
        this.bipedLeftLegWear.copyFrom(this.leftLeg);
    }

    private static float randomSign(Random random) {
        return random.nextBoolean() ? -1.0F : 1.0F;
    }
}
