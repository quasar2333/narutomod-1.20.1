package net.narutomod.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.GaaraModel;
import net.narutomod.entity.NinjaMobEntity;

public final class GaaraRenderer extends HumanoidMobRenderer<NinjaMobEntity, GaaraModel> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/gaara.png");

    public GaaraRenderer(EntityRendererProvider.Context context) {
        super(context, new GaaraModel(context.bakeLayer(GaaraModel.LAYER_LOCATION)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(NinjaMobEntity entity) {
        return TEXTURE;
    }
}
