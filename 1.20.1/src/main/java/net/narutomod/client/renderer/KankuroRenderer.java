package net.narutomod.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.KankuroAnimatedModel;
import net.narutomod.entity.NinjaMobEntity;

public final class KankuroRenderer extends MobRenderer<NinjaMobEntity, KankuroAnimatedModel> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/kankuro.png");

    public KankuroRenderer(EntityRendererProvider.Context context) {
        super(context, new KankuroAnimatedModel(context.bakeLayer(KankuroAnimatedModel.LAYER_LOCATION)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(NinjaMobEntity entity) {
        return TEXTURE;
    }
}
