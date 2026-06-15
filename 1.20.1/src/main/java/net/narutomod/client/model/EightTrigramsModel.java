package net.narutomod.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.generated.LegacyModelLayerDefinitions;
import net.narutomod.entity.EightTrigramsEntity;

public final class EightTrigramsModel extends EntityModel<EightTrigramsEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            NarutomodMod.location("eight_trigrams_legacy"),
            "main"
    );

    private final ModelPart main;

    public EightTrigramsModel(ModelPart root) {
        this.main = root.getChild("bb_main");
    }

    public static LayerDefinition createBodyLayer() {
        return LegacyModelLayerDefinitions.EntityEightTrigrams_ModelEightTrigrams_228();
    }

    @Override
    public void setupAnim(EightTrigramsEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay,
            float red, float green, float blue, float alpha) {
        this.main.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
