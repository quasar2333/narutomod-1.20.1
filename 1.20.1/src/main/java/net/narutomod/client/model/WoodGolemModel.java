package net.narutomod.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.LightTexture;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.generated.LegacyModelLayerDefinitions;
import net.narutomod.entity.WoodGolemEntity;

public final class WoodGolemModel extends HumanoidModel<WoodGolemEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            NarutomodMod.location("wood_golem_legacy"),
            "main"
    );

    private final ModelPart rightUpperArm;
    private final ModelPart rightForeArm;
    private final ModelPart leftUpperArm;
    private final ModelPart leftForeArm;
    private final ModelPart rightThigh;
    private final ModelPart rightCalf;
    private final ModelPart leftThigh;
    private final ModelPart leftCalf;
    private final ModelPart dragon;
    private boolean firstPersonRiderView;

    public WoodGolemModel(ModelPart root) {
        super(root);
        this.rightUpperArm = this.rightArm.getChild("rightUpperArm");
        this.rightForeArm = this.rightUpperArm.getChild("rightForeArm");
        this.leftUpperArm = this.leftArm.getChild("leftUpperArm");
        this.leftForeArm = this.leftUpperArm.getChild("leftForeArm");
        this.rightThigh = this.rightLeg.getChild("rightThigh");
        this.rightCalf = this.rightThigh.getChild("rightCalf");
        this.leftThigh = this.leftLeg.getChild("leftThigh");
        this.leftCalf = this.leftThigh.getChild("leftCalf");
        this.dragon = root.getChild("dragon");
    }

    public static LayerDefinition createBodyLayer() {
        return LegacyModelLayerDefinitions.EntityWoodGolem_ModelWoodGolem_230();
    }

    public void setFirstPersonRiderView(boolean firstPersonRiderView) {
        this.firstPersonRiderView = firstPersonRiderView;
    }

    @Override
    public void setupAnim(WoodGolemEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
        setAllVisible(true);
        float scaledSwing = limbSwing * 2.0F / Math.max(entity.getBbHeight(), 0.001F);
        super.setupAnim(entity, scaledSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        poseBuddhaPassenger(entity.isPassenger());
        this.dragon.visible = false;
        if (this.firstPersonRiderView) {
            this.head.visible = false;
            this.hat.visible = false;
            this.dragon.visible = false;
        }
    }

    @Override
    public void setAllVisible(boolean visible) {
        super.setAllVisible(visible);
        this.dragon.visible = visible;
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

    private void poseBuddhaPassenger(boolean ridingBuddha) {
        if (ridingBuddha) {
            this.rightArm.xRot = 0.0F;
            this.rightArm.yRot = 0.0F;
            this.rightArm.zRot = 0.0F;
            this.rightUpperArm.xRot = -1.0472F;
            this.rightUpperArm.yRot = -0.5236F;
            this.rightUpperArm.zRot = 0.2618F;
            this.rightForeArm.xRot = -0.5236F;
            this.rightForeArm.yRot = 0.0F;
            this.rightForeArm.zRot = -0.5236F;
            this.leftArm.xRot = 0.0F;
            this.leftArm.yRot = 0.0F;
            this.leftArm.zRot = 0.0F;
            this.leftUpperArm.xRot = -1.0472F;
            this.leftUpperArm.yRot = 0.5236F;
            this.leftUpperArm.zRot = -0.2618F;
            this.leftForeArm.xRot = -0.5236F;
            this.leftForeArm.yRot = 0.0F;
            this.leftForeArm.zRot = 0.5236F;
            this.rightThigh.xRot = -2.0944F;
            this.rightThigh.yRot = 0.2618F;
            this.rightThigh.zRot = -1.4835F;
            this.rightCalf.xRot = 1.309F;
            this.rightCalf.yRot = 0.0F;
            this.rightCalf.zRot = 0.0F;
            this.leftThigh.xRot = -2.0944F;
            this.leftThigh.yRot = -0.2618F;
            this.leftThigh.zRot = 1.4835F;
            this.leftCalf.xRot = 1.309F;
            this.leftCalf.yRot = 0.0F;
            this.leftCalf.zRot = 0.0F;
        } else {
            this.rightUpperArm.xRot = 0.0F;
            this.rightUpperArm.yRot = -0.5236F;
            this.rightUpperArm.zRot = 0.2618F;
            this.rightForeArm.xRot = -0.5236F;
            this.rightForeArm.yRot = 0.0F;
            this.rightForeArm.zRot = 0.0F;
            this.leftUpperArm.xRot = 0.0F;
            this.leftUpperArm.yRot = 0.5236F;
            this.leftUpperArm.zRot = -0.2618F;
            this.leftForeArm.xRot = -0.5236F;
            this.leftForeArm.yRot = 0.0F;
            this.leftForeArm.zRot = 0.0F;
            this.rightThigh.xRot = -0.1745F;
            this.rightThigh.yRot = 0.3491F;
            this.rightThigh.zRot = 0.0F;
            this.rightCalf.xRot = 0.2618F;
            this.rightCalf.yRot = 0.0F;
            this.rightCalf.zRot = 0.0F;
            this.leftThigh.xRot = -0.1745F;
            this.leftThigh.yRot = -0.3491F;
            this.leftThigh.zRot = 0.0F;
            this.leftCalf.xRot = 0.2618F;
            this.leftCalf.yRot = 0.0F;
            this.leftCalf.zRot = 0.0F;
        }
    }
}
