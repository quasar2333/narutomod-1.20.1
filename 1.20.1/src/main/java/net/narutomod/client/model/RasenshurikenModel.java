package net.narutomod.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.narutomod.NarutomodMod;
import net.narutomod.entity.RasenshurikenEntity;

public final class RasenshurikenModel extends EntityModel<RasenshurikenEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        NarutomodMod.location("rasenshuriken_legacy"),
        "main"
    );

    private static final float HALF_PI = 1.5708F;
    private static final float[] BALL_ROTATIONS = {
        0.0F, -0.2618F, -0.5236F, -0.7854F, -1.0472F, -1.309F, -1.8326F, -2.0944F, -2.3562F,
        -2.618F, -2.8798F, 2.8798F, 2.618F, 2.3562F, 2.0944F, 1.8326F, 1.309F, 1.0472F,
        0.7854F, 0.5236F, 0.2618F
    };

    private final ModelPart flaps;
    private final ModelPart ball;

    public RasenshurikenModel(ModelPart root) {
        this.flaps = root.getChild("flaps");
        this.ball = root.getChild("ball");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition flaps = root.addOrReplaceChild("flaps", CubeListBuilder.create(), PartPose.ZERO);
        addFlapGroup(flaps, "flap_base", 0.0F, 0.0F, 0.0F);
        addFlapGroup(flaps, "flap_tilt_negative", -0.0436F, -0.0436F, 0.0436F);
        addFlapGroup(flaps, "flap_tilt_positive", 0.0436F, 0.0436F, -0.0436F);

        PartDefinition ball = root.addOrReplaceChild("ball", CubeListBuilder.create(), PartPose.ZERO);
        for (int i = 0; i < BALL_ROTATIONS.length; i++) {
            float rotation = BALL_ROTATIONS[i];
            ball.addOrReplaceChild(
                "ball_shell_" + i,
                CubeListBuilder.create()
                    .texOffs(0, 0)
                    .addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, rotation, rotation, -rotation)
            );
        }
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(RasenshurikenEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
        this.ball.xRot = ageInTicks * 0.6F;
        this.ball.yRot = -ageInTicks * 0.8F;
        this.flaps.xRot = flapPulse(ageInTicks);
        this.flaps.yRot = ageInTicks * 0.6F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay,
            float red, float green, float blue, float alpha) {
        renderBall(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        renderFlaps(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    public void renderBall(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay,
            float red, float green, float blue, float alpha) {
        this.ball.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    public void renderFlaps(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay,
            float red, float green, float blue, float alpha) {
        this.flaps.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    public static float flapScale(float ageInTicks) {
        return 1.5F + flapPulse(ageInTicks) * 2.0F;
    }

    private static float flapPulse(float ageInTicks) {
        return Mth.sin(ageInTicks * 0.2F) * 0.1F;
    }

    private static void addFlapGroup(PartDefinition parent, String name, float xRot, float yRot, float zRot) {
        PartDefinition group = parent.addOrReplaceChild(
            name,
            CubeListBuilder.create(),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, xRot, yRot, zRot)
        );
        addFlap(group, name + "_north", 0.0F);
        addFlap(group, name + "_down", -HALF_PI);
        addFlap(group, name + "_south", Mth.PI);
        addFlap(group, name + "_up", HALF_PI);
    }

    private static void addFlap(PartDefinition parent, String name, float xRot) {
        parent.addOrReplaceChild(
            name,
            CubeListBuilder.create()
                .texOffs(0, 6)
                .addBox(0.0F, -16.0F, -5.0F, 0.0F, 16.0F, 10.0F, CubeDeformation.NONE),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, xRot, 0.0F, HALF_PI)
        );
    }
}
