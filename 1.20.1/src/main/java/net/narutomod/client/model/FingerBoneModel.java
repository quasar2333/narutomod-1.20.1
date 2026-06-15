package net.narutomod.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.generated.LegacyModelLayerDefinitions;
import net.narutomod.entity.FingerBoneEntity;

public final class FingerBoneModel extends EntityModel<FingerBoneEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            NarutomodMod.location("finger_bone_legacy"),
            "main"
    );

    private final ModelPart bone2;
    private final ModelPart bone;

    public FingerBoneModel(ModelPart root) {
        this.bone2 = root.getChild("bone2");
        this.bone = root.getChild("bone");
    }

    public static LayerDefinition createBodyLayer() {
        return LegacyModelLayerDefinitions.EntityFingerBone_ModelFingerBone_186();
    }

    @Override
    public void setupAnim(FingerBoneEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay,
            float red, float green, float blue, float alpha) {
        this.bone2.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        this.bone.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
