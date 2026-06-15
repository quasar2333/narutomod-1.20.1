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
import net.narutomod.entity.AbstractSummonAnimalEntity;

public final class ToadModel<T extends AbstractSummonAnimalEntity> extends EntityModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            NarutomodMod.location("toad_legacy"),
            "main"
    );

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart pipe;
    private final ModelPart body;
    private final ModelPart armRight;
    private final ModelPart forearmRight;
    private final ModelPart handRight;
    private final ModelPart armLeft;
    private final ModelPart legRight;
    private final ModelPart legLowerRight;
    private final ModelPart footRight;
    private final ModelPart legLeft;
    private final ModelPart legLowerLeft;
    private final ModelPart footLeft;
    private boolean pipeVisible = true;

    public ToadModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.pipe = this.head.getChild("pipe");
        this.body = root.getChild("body");
        this.armRight = this.body.getChild("armRight");
        this.forearmRight = this.armRight.getChild("forearmRight");
        this.handRight = this.forearmRight.getChild("handRight");
        this.armLeft = this.body.getChild("armLeft");
        this.legRight = root.getChild("legRight");
        this.legLowerRight = this.legRight.getChild("legLowerRight");
        this.footRight = this.legLowerRight.getChild("footRight");
        this.legLeft = root.getChild("legLeft");
        this.legLowerLeft = this.legLeft.getChild("legLowerLeft");
        this.footLeft = this.legLowerLeft.getChild("footLeft");
    }

    public static LayerDefinition createBodyLayer() {
        return LegacyModelLayerDefinitions.EntityToad_ModelToad_565();
    }

    public void setPipeVisible(boolean pipeVisible) {
        this.pipeVisible = pipeVisible;
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root.getAllParts().forEach(ModelPart::resetPose);
        this.pipe.visible = this.pipeVisible;

        this.head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        this.head.xRot = headPitch * Mth.DEG_TO_RAD;
        this.jaw.xRot = this.attackTime > 0.0F ? 0.5236F : 0.0873F;

        this.body.yRot = 0.0F;
        this.armRight.xRot = -0.5236F;
        this.armRight.zRot = 0.3491F;
        this.forearmRight.zRot = -0.5236F;
        this.handRight.zRot = 0.0F;

        if (this.attackTime > 0.0F) {
            float swing = this.attackTime;
            this.body.yRot = Mth.sin(Mth.sqrt(swing) * (Mth.PI * 2.0F)) * 0.1F;
            if (swing < 0.3333F) {
                float progress = swing / 0.3333F;
                this.armRight.xRot = -0.5236F - 1.5708F * progress;
                this.armRight.zRot = 0.3491F - 0.8727F * progress;
            } else if (swing < 0.6667F) {
                float progress = (swing - 0.3333F) / 0.3333F;
                this.armRight.xRot = -2.0944F + 1.5708F * progress;
                this.armRight.zRot = -0.5236F + 0.5236F * progress;
                this.forearmRight.zRot = -0.5236F + 0.2618F * progress;
                this.handRight.zRot = 0.5236F * progress;
            } else {
                float progress = (swing - 0.6667F) / 0.3333F;
                this.armRight.zRot = 0.3491F * progress;
                this.forearmRight.zRot = -0.2618F - 0.2618F * progress;
                this.handRight.zRot = 0.5236F - 0.5236F * progress;
            }
        }

        this.armRight.zRot += Mth.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
        this.armLeft.zRot = -0.3491F - Mth.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
        this.armRight.xRot += Mth.sin(ageInTicks * 0.067F) * 0.05F;
        this.armLeft.xRot = -0.5236F - Mth.sin(ageInTicks * 0.067F) * 0.05F;

        float jumpProgress = entity.onGround() ? 0.0F : 1.0F;
        this.legRight.xRot = 0.2618F + jumpProgress * 1.5708F;
        this.legLowerRight.xRot = -0.5236F - jumpProgress * 1.5708F;
        this.footRight.xRot = 0.2182F + jumpProgress * 1.0908F;
        this.legLeft.xRot = 0.2618F + jumpProgress * 1.5708F;
        this.legLowerLeft.xRot = -0.5236F - jumpProgress * 1.5708F;
        this.footLeft.xRot = 0.2182F + jumpProgress * 1.0908F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay,
            float red, float green, float blue, float alpha) {
        this.root.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
