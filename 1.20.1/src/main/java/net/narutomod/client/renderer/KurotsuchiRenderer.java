package net.narutomod.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.KurotsuchiModel;
import net.narutomod.entity.NinjaMobEntity;

public final class KurotsuchiRenderer extends MobRenderer<NinjaMobEntity, KurotsuchiModel> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/kurotsuchi.png");

    public KurotsuchiRenderer(EntityRendererProvider.Context context) {
        super(context, new KurotsuchiModel(context.bakeLayer(KurotsuchiModel.LAYER_LOCATION)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(NinjaMobEntity entity) {
        return TEXTURE;
    }
}
