package net.narutomod.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.TemariModel;
import net.narutomod.entity.NinjaMobEntity;

public final class TemariRenderer extends MobRenderer<NinjaMobEntity, TemariModel> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/temari.png");

    public TemariRenderer(EntityRendererProvider.Context context) {
        super(context, new TemariModel(context.bakeLayer(TemariModel.LAYER_LOCATION)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(NinjaMobEntity entity) {
        return TEXTURE;
    }
}
