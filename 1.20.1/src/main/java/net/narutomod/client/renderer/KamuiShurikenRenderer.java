package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.narutomod.entity.KamuiShurikenEntity;
import net.narutomod.registry.ModItems;

public final class KamuiShurikenRenderer extends EntityRenderer<KamuiShurikenEntity> {
    private final ItemRenderer itemRenderer;
    private final ItemStack fallbackStack;

    public KamuiShurikenRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.fallbackStack = new ItemStack(ModItems.KAMUISHURIKEN.get());
    }

    @Override
    public void render(KamuiShurikenEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        float age = entity.tickCount + partialTick;
        float scale = entity.getScale();
        ItemStack stack = entity.getItem().isEmpty() ? this.fallbackStack : entity.getItem();

        poseStack.pushPose();
        poseStack.translate(0.0D, 0.125F * scale, 0.0D);
        poseStack.scale(scale, scale, scale);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(5.0F * age));
        poseStack.mulPose(Axis.XP.rotationDegrees(-60.0F * age));
        this.itemRenderer.renderStatic(
                stack,
                ItemDisplayContext.GROUND,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                bufferSource,
                entity.level(),
                entity.getId());
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(KamuiShurikenEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
