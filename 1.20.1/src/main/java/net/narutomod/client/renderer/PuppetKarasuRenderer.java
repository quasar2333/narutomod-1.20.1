package net.narutomod.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.narutomod.NarutomodMod;
import net.narutomod.client.model.PuppetKarasuModel;
import net.narutomod.entity.PuppetKarasuEntity;

public final class PuppetKarasuRenderer extends HumanoidMobRenderer<PuppetKarasuEntity, PuppetKarasuModel> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/karasu.png");

    public PuppetKarasuRenderer(EntityRendererProvider.Context context) {
        super(context, new PuppetKarasuModel(context.bakeLayer(PuppetKarasuModel.LAYER_LOCATION)), 0.5F);
        this.addLayer(new PuppetChakraStringsLayer<>(this));
    }

    @Override
    public ResourceLocation getTextureLocation(PuppetKarasuEntity entity) {
        return TEXTURE;
    }
}
