package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.TentenModel;
import net.narutomod.entity.TentenEntity;

public final class TentenRenderer extends MobRenderer<TentenEntity, TentenModel> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/tenten.png");

    public TentenRenderer(EntityRendererProvider.Context context) {
        super(context, new TentenModel(context.bakeLayer(TentenModel.LAYER_LOCATION)), 0.5F);
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    protected void scale(TentenEntity entity, PoseStack poseStack, float partialTickTime) {
        poseStack.scale(0.875F, 0.875F, 0.875F);
    }

    @Override
    public ResourceLocation getTextureLocation(TentenEntity entity) {
        return TEXTURE;
    }
}
