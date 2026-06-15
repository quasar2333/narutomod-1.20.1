package net.narutomod.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.narutomod.NarutomodMod;
import net.narutomod.entity.IceSpikeEntity;

public final class IceSpikeRenderer extends LegacySpikeModelRenderer<IceSpikeEntity> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/spike_ice.png");
    private static final int COLOR = 0xC0FFFFFF;

    public IceSpikeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected float getScale(IceSpikeEntity entity) {
        return entity.getCurrentScale();
    }

    @Override
    protected int getColor(IceSpikeEntity entity) {
        return COLOR;
    }

    @Override
    public ResourceLocation getTextureLocation(IceSpikeEntity entity) {
        return TEXTURE;
    }
}
