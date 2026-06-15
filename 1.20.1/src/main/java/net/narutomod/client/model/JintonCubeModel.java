package net.narutomod.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.generated.LegacyModelLayerDefinitions;
import net.narutomod.entity.JintonCubeEntity;

public final class JintonCubeModel extends EntityModel<JintonCubeEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            NarutomodMod.location("jinton_cube_legacy"),
            "main"
    );

    private static final float LEGACY_CENTER_Y = 4.0F / 16.0F;

    private final ModelPart core;
    private final ModelPart shell;

    public JintonCubeModel(ModelPart root) {
        this.core = root.getChild("bone");
        this.shell = root.getChild("bone2");
    }

    public static LayerDefinition createBodyLayer() {
        return LegacyModelLayerDefinitions.ItemJinton_ModelCube_488();
    }

    @Override
    public void setupAnim(JintonCubeEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay,
            float red, float green, float blue, float alpha) {
        renderCore(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        renderShell(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha * 0.3F);
    }

    public void renderCore(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay,
            float red, float green, float blue, float alpha) {
        poseStack.pushPose();
        poseStack.translate(0.0D, LEGACY_CENTER_Y, 0.0D);
        this.core.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        poseStack.popPose();
    }

    public void renderShell(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay,
            float red, float green, float blue, float alpha) {
        poseStack.pushPose();
        poseStack.translate(0.0D, LEGACY_CENTER_Y, 0.0D);
        this.shell.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        poseStack.popPose();
    }
}
