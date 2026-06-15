package net.narutomod.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.AnbuModel;
import net.narutomod.entity.NinjaMobEntity;

public final class AnbuRenderer extends MobRenderer<NinjaMobEntity, AnbuModel> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/ninja_anbu.png");

    public AnbuRenderer(EntityRendererProvider.Context context) {
        super(context, new AnbuModel(context.bakeLayer(AnbuModel.LAYER_LOCATION)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(NinjaMobEntity entity) {
        return TEXTURE;
    }
}
