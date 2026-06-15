package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.TenTailsModel;
import net.narutomod.entity.TenTailsEntity;

public final class TenTailsRenderer extends MobRenderer<TenTailsEntity, TenTailsModel> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/tentailsl1.png");

    public TenTailsRenderer(EntityRendererProvider.Context context) {
        super(context, new TenTailsModel(context.bakeLayer(TenTailsModel.LAYER_LOCATION)), TenTailsEntity.MODEL_SCALE * 0.5F);
    }

    @Override
    protected void scale(TenTailsEntity entity, PoseStack poseStack, float partialTick) {
        float scale = TenTailsEntity.MODEL_SCALE;
        poseStack.translate(0.0F, 1.5F - 1.5F * scale, 0.375F * scale);
        poseStack.scale(scale, scale, scale);
        this.shadowRadius = scale * 0.5F;
    }

    @Override
    public ResourceLocation getTextureLocation(TenTailsEntity entity) {
        return TEXTURE;
    }
}
