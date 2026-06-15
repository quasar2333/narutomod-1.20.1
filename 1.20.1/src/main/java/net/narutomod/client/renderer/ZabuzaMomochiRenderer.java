package net.narutomod.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.ZabuzaMomochiModel;
import net.narutomod.entity.NinjaMobEntity;

public final class ZabuzaMomochiRenderer extends MobRenderer<NinjaMobEntity, ZabuzaMomochiModel> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/zabuzamomochi.png");

    public ZabuzaMomochiRenderer(EntityRendererProvider.Context context) {
        super(context, new ZabuzaMomochiModel(context.bakeLayer(ZabuzaMomochiModel.LAYER_LOCATION)), 0.5F);
        this.addLayer(new ZabuzaStoredItemLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(NinjaMobEntity entity) {
        return TEXTURE;
    }
}
