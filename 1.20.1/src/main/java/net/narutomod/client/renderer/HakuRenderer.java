package net.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.HakuModel;
import net.narutomod.entity.HakuEntity;

public final class HakuRenderer extends MobRenderer<HakuEntity, HakuModel> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/haku.png");

    public HakuRenderer(EntityRendererProvider.Context context) {
        super(context, new HakuModel(context.bakeLayer(HakuModel.LAYER_LOCATION)), 0.5F);
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(HakuEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(HakuEntity entity, PoseStack poseStack, float partialTickTime) {
        poseStack.scale(0.875F, 0.875F, 0.875F);
    }
}
