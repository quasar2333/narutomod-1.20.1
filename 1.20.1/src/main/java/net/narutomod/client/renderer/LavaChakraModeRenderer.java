package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.narutomod.NarutomodMod;
import net.narutomod.client.render.NarutoRenderTypes;
import net.narutomod.entity.LavaChakraModeEntity;

public final class LavaChakraModeRenderer extends EntityRenderer<LavaChakraModeEntity> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/lavacloak1.png");

    public LavaChakraModeRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(LavaChakraModeEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        LivingEntity owner = entity.getOwner();
        if (owner == null || isFirstPersonOwner(owner) || isHandledByPlayerLayer(owner)) {
            return;
        }
        if (this.entityRenderDispatcher.getRenderer(owner) instanceof LivingEntityRenderer<?, ?> renderer) {
            renderOwnerCloak(entity, owner, renderer, partialTick, poseStack, bufferSource);
        }
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(LavaChakraModeEntity entity) {
        return TEXTURE;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void renderOwnerCloak(LavaChakraModeEntity entity, LivingEntity owner, LivingEntityRenderer renderer,
                                         float partialTick, PoseStack poseStack, MultiBufferSource bufferSource) {
        EntityModel model = renderer.getModel();
        boolean shouldSit = owner.isPassenger()
                && owner.getVehicle() != null
                && owner.getVehicle().shouldRiderSit();
        model.attackTime = owner.getAttackAnim(partialTick);
        model.riding = shouldSit;
        model.young = owner.isBaby();

        float bodyYaw = Mth.rotLerp(partialTick, owner.yBodyRotO, owner.yBodyRot);
        float headYaw = Mth.rotLerp(partialTick, owner.yHeadRotO, owner.yHeadRot);
        float netHeadYaw = Mth.wrapDegrees(headYaw - bodyYaw);
        float headPitch = Mth.lerp(partialTick, owner.xRotO, owner.getXRot());
        float ageInTicks = owner.tickCount + partialTick;
        float limbSwing = 0.0F;
        float limbSwingAmount = 0.0F;
        if (!shouldSit && owner.isAlive()) {
            limbSwingAmount = Math.min(owner.walkAnimation.speed(partialTick), 1.0F);
            limbSwing = owner.walkAnimation.position(partialTick);
            if (owner.isBaby()) {
                limbSwing *= 3.0F;
            }
        }

        Vec3 offset = interpolatedPosition(owner, partialTick).subtract(interpolatedPosition(entity, partialTick));
        poseStack.pushPose();
        poseStack.translate(offset.x(), offset.y(), offset.z());
        float ownerScale = owner.getScale();
        poseStack.scale(ownerScale, ownerScale, ownerScale);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.501F, 0.0F);

        model.prepareMobModel(owner, limbSwing, limbSwingAmount, partialTick);
        model.setupAnim(owner, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        VertexConsumer consumer = bufferSource.getBuffer(NarutoRenderTypes.scrollingTranslucent(TEXTURE, 0.01F, 0.01F));
        model.renderToBuffer(poseStack, consumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, 0.6F);
        poseStack.popPose();
    }

    private static boolean isFirstPersonOwner(LivingEntity owner) {
        Minecraft minecraft = Minecraft.getInstance();
        return owner == minecraft.getCameraEntity() && minecraft.options.getCameraType().isFirstPerson();
    }

    private static boolean isHandledByPlayerLayer(LivingEntity owner) {
        return owner instanceof AbstractClientPlayer player && LavaChakraModeLayer.isLavaChakraModeActive(player);
    }

    private static Vec3 interpolatedPosition(Entity entity, float partialTick) {
        return new Vec3(
                Mth.lerp(partialTick, entity.xOld, entity.getX()),
                Mth.lerp(partialTick, entity.yOld, entity.getY()),
                Mth.lerp(partialTick, entity.zOld, entity.getZ()));
    }
}
