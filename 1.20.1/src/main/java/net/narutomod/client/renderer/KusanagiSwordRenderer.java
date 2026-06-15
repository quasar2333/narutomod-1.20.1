package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.narutomod.entity.KusanagiSwordEntity;
import net.narutomod.registry.ModItems;

public final class KusanagiSwordRenderer extends EntityRenderer<KusanagiSwordEntity> {
    private final ItemRenderer itemRenderer;
    private final ItemStack fallbackStack;

    public KusanagiSwordRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.fallbackStack = new ItemStack(ModItems.KUSANAGI_SWORD.get());
        this.fallbackStack.getOrCreateTag().putBoolean("inAir", true);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(KusanagiSwordEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        ItemStack stack = entity.getItem().isEmpty() ? this.fallbackStack : entity.getItem();
        float yaw = -entity.yRotO - Mth.wrapDegrees(entity.getYRot() - entity.yRotO) * partialTick + 180.0F;
        float pitch = -Mth.lerp(partialTick, entity.xRotO, entity.getXRot()) + 90.0F;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        this.itemRenderer.renderStatic(
                stack,
                ItemDisplayContext.GROUND,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                bufferSource,
                entity.level(),
                entity.getId());
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, LightTexture.FULL_BRIGHT);
    }

    @Override
    public ResourceLocation getTextureLocation(KusanagiSwordEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
