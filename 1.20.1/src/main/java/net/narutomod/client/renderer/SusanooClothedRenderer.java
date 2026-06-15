package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.SusanooClothedModel;
import net.narutomod.client.render.NarutoRenderTypes;
import net.narutomod.entity.SusanooClothedEntity;

public final class SusanooClothedRenderer extends MobRenderer<SusanooClothedEntity, SusanooClothedModel> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/susanoo_clothed.png");
    private static final ResourceLocation FLAME_TEXTURE = NarutomodMod.location("textures/gas256.png");

    public SusanooClothedRenderer(EntityRendererProvider.Context context) {
        super(context, new SusanooClothedModel(context.bakeLayer(SusanooClothedModel.LAYER_LOCATION)), 0.6F * SusanooClothedEntity.MODEL_SCALE);
        addLayer(new FlameLayer(this));
    }

    @Override
    public void render(SusanooClothedEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        copyRiderSwing(entity);
        boolean bodyVisible = this.model.body.visible;
        boolean headVisible = this.model.head.visible;
        boolean hatVisible = this.model.hat.visible;
        if (shouldHideBodyForFirstPersonRider(entity)) {
            this.model.body.visible = false;
            this.model.head.visible = false;
            this.model.hat.visible = false;
        }
        try {
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, LightTexture.FULL_BRIGHT);
        } finally {
            this.model.body.visible = bodyVisible;
            this.model.head.visible = headVisible;
            this.model.hat.visible = hatVisible;
        }
    }

    @Override
    protected void scale(SusanooClothedEntity entity, PoseStack poseStack, float partialTick) {
        float divisor = entity.hasLegs() ? 1.0F : 2.0F;
        poseStack.translate(0.0F, 1.5F - 1.5F * SusanooClothedEntity.MODEL_SCALE / divisor, 0.0F);
        poseStack.scale(SusanooClothedEntity.MODEL_SCALE, SusanooClothedEntity.MODEL_SCALE, SusanooClothedEntity.MODEL_SCALE);
    }

    @Override
    public ResourceLocation getTextureLocation(SusanooClothedEntity entity) {
        return TEXTURE;
    }

    private static void copyRiderSwing(SusanooClothedEntity entity) {
        if (entity.getControllingPassenger() instanceof AbstractClientPlayer rider) {
            entity.attackAnim = rider.attackAnim;
            entity.oAttackAnim = rider.oAttackAnim;
            entity.swingTime = rider.swingTime;
            entity.swinging = rider.swinging;
            entity.swingingArm = rider.swingingArm;
        }
    }

    private static boolean shouldHideBodyForFirstPersonRider(SusanooClothedEntity entity) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.getCameraEntity() == entity.getControllingPassenger()
                && minecraft.options.getCameraType().isFirstPerson();
    }

    private static final class FlameLayer extends RenderLayer<SusanooClothedEntity, SusanooClothedModel> {
        private static final float MAX_ALPHA = 0.5F;

        private FlameLayer(RenderLayerParent<SusanooClothedEntity, SusanooClothedModel> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, SusanooClothedEntity entity,
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
