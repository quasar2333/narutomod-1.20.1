package net.narutomod.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.KakashiModel;
import net.narutomod.entity.NinjaMobEntity;

public final class KakashiRenderer extends MobRenderer<NinjaMobEntity, KakashiModel> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/kakashi.png");

    public KakashiRenderer(EntityRendererProvider.Context context) {
        super(context, new KakashiModel(context.bakeLayer(KakashiModel.LAYER_LOCATION)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(NinjaMobEntity entity) {
        return TEXTURE;
    }
}
