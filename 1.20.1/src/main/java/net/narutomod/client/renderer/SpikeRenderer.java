package net.narutomod.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.narutomod.NarutomodMod;
import net.narutomod.entity.SpikeEntity;

public final class SpikeRenderer extends LegacySpikeModelRenderer<SpikeEntity> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/spike.png");

    public SpikeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected float getScale(SpikeEntity entity) {
        return entity.getEntityScale();
    }

    @Override
    protected int getColor(SpikeEntity entity) {
        return entity.getColor();
    }

    @Override
    public ResourceLocation getTextureLocation(SpikeEntity entity) {
        return TEXTURE;
    }
}
