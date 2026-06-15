package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.WoodGolemModel;
import net.narutomod.entity.WoodGolemEntity;

public final class WoodGolemRenderer extends MobRenderer<WoodGolemEntity, WoodGolemModel> {
    private static final float MODEL_SCALE = 8.0F;
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/woodgolem.png");

    public WoodGolemRenderer(EntityRendererProvider.Context context) {
        super(context, new WoodGolemModel(context.bakeLayer(WoodGolemModel.LAYER_LOCATION)), 0.5F * MODEL_SCALE);
    }

    @Override
    public void render(WoodGolemEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        this.model.setFirstPersonRiderView(isFirstPersonRiderView(entity));
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    protected void scale(WoodGolemEntity entity, PoseStack poseStack, float partialTick) {
        float growth = entity.getRenderGrowth(partialTick);
        poseStack.translate(0.0F, 1.5F - 1.5F * MODEL_SCALE * growth, 0.0F);
        poseStack.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
        this.shadowRadius = 0.5F * MODEL_SCALE * growth;
    }

    @Override
    protected float getAttackAnim(WoodGolemEntity entity, float partialTick) {
        LivingEntity rider = entity.getControllingPassenger();
        if (rider != null) {
            return rider.getAttackAnim(partialTick);
        }
        return super.getAttackAnim(entity, partialTick);
    }

    @Override
    public ResourceLocation getTextureLocation(WoodGolemEntity entity) {
        return TEXTURE;
    }

    private static boolean isFirstPersonRiderView(WoodGolemEntity entity) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.options.getCameraType() == CameraType.FIRST_PERSON
                && minecraft.cameraEntity == entity.getControllingPassenger();
    }
}
