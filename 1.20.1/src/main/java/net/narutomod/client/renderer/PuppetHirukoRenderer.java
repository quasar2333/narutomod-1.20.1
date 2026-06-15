package net.narutomod.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.PuppetHirukoModel;
import net.narutomod.entity.PuppetHirukoEntity;

public final class PuppetHirukoRenderer extends MobRenderer<PuppetHirukoEntity, PuppetHirukoModel> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/hiruko.png");

    public PuppetHirukoRenderer(EntityRendererProvider.Context context) {
        super(context, new PuppetHirukoModel(context.bakeLayer(PuppetHirukoModel.LAYER_LOCATION)), 0.5F);
        this.addLayer(new PuppetChakraStringsLayer<>(this));
    }

    @Override
    public ResourceLocation getTextureLocation(PuppetHirukoEntity entity) {
        return TEXTURE;
    }
}
