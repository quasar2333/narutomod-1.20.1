package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.Buddha1000Model;
import net.narutomod.entity.Buddha1000Entity;

public final class Buddha1000Renderer extends EntityRenderer<Buddha1000Entity> {
    private static final float MODEL_SCALE = 20.0F;
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/budha1000.png");

    private final Buddha1000Model model;

    public Buddha1000Renderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 10.0F;
        this.model = new Buddha1000Model(context.bakeLayer(Buddha1000Model.LAYER_LOCATION));
    }

    @Override
    public void render(Buddha1000Entity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        float growthLift = fullGrowthLift(entity) * (entity.getRenderGrowth(partialTick) - 1.0F);
        float limbSwingAmount = legacyLimbSwingAmount(entity);
        float limbSwing = (entity.tickCount + partialTick) * entity.getBbHeight() * 0.04F;
        LivingEntity rider = entity.getControllingPassenger();

        poseStack.pushPose();
        poseStack.translate(0.0D, growthLift, 0.0D);
        poseStack.translate(0.0D, 1.5D, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));
        poseStack.scale(-MODEL_SCALE, -MODEL_SCALE, MODEL_SCALE);
        poseStack.translate(0.0D, -1.5D, 0.0D);

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        this.model.setFirstPersonRiderView(isFirstPersonRiderView(entity));
        this.model.attackTime = rider != null ? rider.getAttackAnim(partialTick) : 0.0F;
        this.model.setupAnim(entity, limbSwing, limbSwingAmount, entity.tickCount + partialTick, 0.0F, 0.0F);
        this.model.renderToBuffer(
            poseStack,
            consumer,
            LightTexture.FULL_BRIGHT,
            OverlayTexture.NO_OVERLAY,
            1.0F,
            1.0F,
            1.0F,
            1.0F
        );

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(Buddha1000Entity entity) {
        return TEXTURE;
    }

    private static boolean isFirstPersonRiderView(Buddha1000Entity entity) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.options.getCameraType() == CameraType.FIRST_PERSON
                && minecraft.cameraEntity == entity.getControllingPassenger();
    }

    private static float fullGrowthLift(Buddha1000Entity entity) {
        return (entity.isSitting() ? 1.0F : 1.5F) * MODEL_SCALE;
    }

    private static float legacyLimbSwingAmount(Buddha1000Entity entity) {
        if (entity.isSitting()) {
            return 0.0F;
        }
        Vec3 motion = entity.getDeltaMovement();
        return Mth.clamp((float)(motion.horizontalDistance() * 4.0D), 0.0F, 1.0F);
    }
}
