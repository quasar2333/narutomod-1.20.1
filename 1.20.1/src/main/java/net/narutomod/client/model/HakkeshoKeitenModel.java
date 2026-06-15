package net.narutomod.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.generated.LegacyModelLayerDefinitions;
import net.narutomod.entity.HakkeshoKeitenEntity;

public final class HakkeshoKeitenModel extends EntityModel<HakkeshoKeitenEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            NarutomodMod.location("hakkesho_keiten_legacy"),
            "main"
    );

    private final ModelPart shell;

    public HakkeshoKeitenModel(ModelPart root) {
        this.shell = root.getChild("shell");
    }

    public static LayerDefinition createBodyLayer() {
        return LegacyModelLayerDefinitions.EntityHakkeshoKeiten_ModelKaiten_183();
    }

    @Override
    public void setupAnim(HakkeshoKeitenEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay,
            float red, float green, float blue, float alpha) {
        this.shell.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
