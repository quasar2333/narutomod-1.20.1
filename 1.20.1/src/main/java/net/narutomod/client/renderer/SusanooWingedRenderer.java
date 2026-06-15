package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.SusanooWingedModel;
import net.narutomod.client.render.NarutoRenderTypes;
import net.narutomod.entity.SusanooWingedEntity;
import net.narutomod.procedure.ProcedureUtils;

public final class SusanooWingedRenderer extends MobRenderer<SusanooWingedEntity, SusanooWingedModel> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/susanoo_winged.png");
    private static final ResourceLocation FLAME_TEXTURE = NarutomodMod.location("textures/gas256.png");

    public SusanooWingedRenderer(EntityRendererProvider.Context context) {
        super(context, new SusanooWingedModel(context.bakeLayer(SusanooWingedModel.LAYER_LOCATION)), SusanooWingedEntity.MODEL_SCALE * 0.5F);
        addLayer(new FlameLayer(this));
        addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public void render(SusanooWingedEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        copyRiderSwing(entity);
        boolean headVisible = this.model.head.visible;
        boolean hatVisible = this.model.hat.visible;
        if (shouldHideHeadForFirstPersonRider(entity)) {
            this.model.head.visible = false;
            this.model.hat.visible = false;
        }
        try {
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, LightTexture.FULL_BRIGHT);
        } finally {
            this.model.head.visible = headVisible;
            this.model.hat.visible = hatVisible;
        }
    }

    @Override
    protected void setupRotations(SusanooWingedEntity entity, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick) {
        super.setupRotations(entity, poseStack, ageInTicks, rotationYaw, partialTick);
        if (!entity.onGround() && isMovingTowardsLookDirection(entity)) {
            float amount = flyingBodyRotationAmount(entity);
            float translateY = (1.0F - Mth.cos(amount * Mth.HALF_PI)) * entity.getBbHeight() * 0.75F;
            float translateZ = Mth.sin(amount * Mth.HALF_PI) * entity.getBbHeight() * 0.75F;
            poseStack.translate(0.0F, translateY, translateZ);
            poseStack.mulPose(Axis.XP.rotationDegrees(amount * -90.0F));
        }
    }

    @Override
    protected void scale(SusanooWingedEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.translate(0.0F, 1.5F - 1.5F * SusanooWingedEntity.MODEL_SCALE, 0.0F);
        poseStack.scale(SusanooWingedEntity.MODEL_SCALE, SusanooWingedEntity.MODEL_SCALE, SusanooWingedEntity.MODEL_SCALE);
    }

    @Override
    public ResourceLocation getTextureLocation(SusanooWingedEntity entity) {
        return TEXTURE;
    }

    private static void copyRiderSwing(SusanooWingedEntity entity) {
        if (entity.getControllingPassenger() instanceof AbstractClientPlayer rider) {
            entity.attackAnim = rider.attackAnim;
            entity.oAttackAnim = rider.oAttackAnim;
            entity.swingTime = rider.swingTime;
            entity.swinging = rider.swinging;
            entity.swingingArm = rider.swingingArm;
        }
    }

    private static boolean shouldHideHeadForFirstPersonRider(SusanooWingedEntity entity) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.getCameraEntity() == entity.getControllingPassenger()
                && minecraft.options.getCameraType().isFirstPerson();
    }

    private static float flyingBodyRotationAmount(SusanooWingedEntity entity) {
        return Mth.clamp((float)(entity.getSyncedMotionXZ().length() * 1.2D), 0.0F, 1.0F);
    }

    private static boolean isMovingTowardsLookDirection(SusanooWingedEntity entity) {
        Vec3 motion = entity.getSyncedMotionXZ();
        if (motion.lengthSqr() <= 1.0E-12D) {
            return false;
        }
        float motionYaw = ProcedureUtils.getYawFromVec(motion);
        float yawDelta = Math.abs(Mth.wrapDegrees(motionYaw - entity.getSyncedHeadYaw()));
        return yawDelta < 90.0F;
    }

    private static final class FlameLayer extends RenderLayer<SusanooWingedEntity, SusanooWingedModel> {
        private static final float MAX_ALPHA = 0.6F;

        private FlameLayer(RenderLayerParent<SusanooWingedEntity, SusanooWingedModel> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, SusanooWingedEntity entity,
                float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            int color = entity.getFlameColor();
            float red = (float)(color >> 16 & 0xFF) / 255.0F;
            float green = (float)(color >> 8 & 0xFF) / 255.0F;
            float blue = (float)(color & 0xFF) / 255.0F;
            float alpha = MAX_ALPHA * Math.min(ageInTicks / 60.0F, 1.0F);
            VertexConsumer consumer = bufferSource.getBuffer(NarutoRenderTypes.scrollingTranslucent(FLAME_TEXTURE, 0.0F, 0.01F));
            poseStack.pushPose();
            poseStack.scale(0.99F, 0.99F, 0.99F);
            getParentModel().renderFlameToBuffer(poseStack, consumer, LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY, red, green, blue, alpha);
            poseStack.popPose();
        }
    }
}
