package net.narutomod.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.IrukaSenseiModel;
import net.narutomod.entity.IrukaSenseiEntity;

public final class IrukaSenseiRenderer extends MobRenderer<IrukaSenseiEntity, IrukaSenseiModel> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/iruka64x64.png");

    public IrukaSenseiRenderer(EntityRendererProvider.Context context) {
        super(context, new IrukaSenseiModel(context.bakeLayer(IrukaSenseiModel.LAYER_LOCATION)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(IrukaSenseiEntity entity) {
        return TEXTURE;
    }
}
