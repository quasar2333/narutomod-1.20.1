package net.narutomod.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.generated.LegacyModelLayerDefinitions;
import net.narutomod.entity.PretaShieldEntity;

public final class PretaShieldModel extends EntityModel<PretaShieldEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            NarutomodMod.location("preta_shield_legacy"),
            "main"
    );

    private static final float LEGACY_CENTER_Y = 7.0F / 16.0F;

    private final ModelPart bone;

    public PretaShieldModel(ModelPart root) {
        this.bone = root.getChild("bone");
    }

    public static LayerDefinition createBodyLayer() {
        return LegacyModelLayerDefinitions.EntityPretaShield_ModelPretaShield_285();
    }

    @Override
    public void setupAnim(PretaShieldEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay,
            float red, float green, float blue, float alpha) {
        this.bone.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    public void renderCentered(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay,
            float red, float green, float blue, float alpha) {
        poseStack.pushPose();
        poseStack.translate(0.0D, -LEGACY_CENTER_Y, 0.0D);
        renderToBuffer(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        poseStack.popPose();
    }
}
