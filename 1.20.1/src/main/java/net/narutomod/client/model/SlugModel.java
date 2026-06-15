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

public final class SlugModel<T extends AbstractSummonAnimalEntity> extends EntityModel<T> {
    private static final float BASE_HEIGHT = 0.75F;

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            NarutomodMod.location("slug_legacy"),
            "main"
    );

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart bodyFront;
    private final ModelPart head;
    private final ModelPart eyeRight;
    private final ModelPart hornRightTip;
    private final ModelPart eyeLeft;
    private final ModelPart hornLeftTip;
    private final ModelPart tail0;
    private final ModelPart tail1;
    private final ModelPart tail2;

    public SlugModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.bodyFront = this.body.getChild("bodyFront");
        this.head = this.bodyFront.getChild("head");
        ModelPart hornRoot = this.head.getChild("bone3");
        this.hornRightTip = hornRoot.getChild("HornRight").getChild("Horn0_1").getChild("Horn0_2").getChild("Horn0_3");
        this.eyeRight = this.hornRightTip.getChild("eyeRight");
        this.hornLeftTip = hornRoot.getChild("HornLeft").getChild("Horn0_5").getChild("Horn0_6").getChild("Horn0_7");
        this.eyeLeft = this.hornLeftTip.getChild("eyeLeft");
        this.tail0 = this.body.getChild("tail_0");
        this.tail1 = this.tail0.getChild("tail_1");
        this.tail2 = this.tail1.getChild("tail_2");
    }

    public static LayerDefinition createBodyLayer() {
        return LegacyModelLayerDefinitions.EntitySlug_ModelSlug_331();
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root.getAllParts().forEach(ModelPart::resetPose);

        this.body.xRot = Mth.HALF_PI;
        this.eyeRight.xRot = headPitch * 0.5F * Mth.DEG_TO_RAD;
        this.hornRightTip.xRot = -0.0873F + headPitch * 0.5F * Mth.DEG_TO_RAD;
        this.eyeLeft.xRot = headPitch * Mth.DEG_TO_RAD;
        this.hornLeftTip.xRot = -0.0873F + headPitch * 0.5F * Mth.DEG_TO_RAD;
        this.bodyFront.zRot = -netHeadYaw * 0.5F * Mth.DEG_TO_RAD;
        this.head.zRot = -netHeadYaw * 0.5F * Mth.DEG_TO_RAD;

        float scaledSwing = limbSwing * BASE_HEIGHT / Math.max(entity.getBbHeight(), 0.001F);
        this.tail0.zRot = tailSwing(scaledSwing, 0, 3.0F);
        this.tail1.zRot = tailSwing(scaledSwing, 1, 2.0F);
        this.tail2.zRot = tailSwing(scaledSwing, 2, 1.0F);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay,
            float red, float green, float blue, float alpha) {
        this.root.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    private static float tailSwing(float limbSwing, int index, float amplitude) {
        return Mth.cos(limbSwing * 0.9F + index * 0.15F * Mth.PI) * Mth.PI * 0.02F * amplitude;
    }
}
