package net.narutomod.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.generated.LegacyModelLayerDefinitions;
import net.narutomod.entity.RasenganEntity;

public final class RasenganModel extends EntityModel<RasenganEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        NarutomodMod.location("rasengan_legacy"),
        "main"
    );

    private final ModelPart core;
    private final ModelPart shell;

    public RasenganModel(ModelPart root) {
        this.core = root.getChild("core");
        this.shell = root.getChild("shell");
    }

    public static LayerDefinition createBodyLayer() {
        return LegacyModelLayerDefinitions.EntityRasengan_ModelRasengan_406();
    }

    @Override
    public void setupAnim(RasenganEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        renderCore(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        renderShell(poseStack, consumer, packedLight, packedOverlay, 0.66F, 0.87F, 1.0F, alpha);
    }

    public void renderCore(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        this.core.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    public void renderShell(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        this.shell.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
