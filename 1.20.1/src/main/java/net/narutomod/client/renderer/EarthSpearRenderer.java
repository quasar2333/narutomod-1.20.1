package net.narutomod.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.narutomod.NarutomodMod;
import net.narutomod.entity.EarthSpearsEntity;

public final class EarthSpearRenderer extends LegacySpikeModelRenderer<EarthSpearsEntity> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/spike_stone.png");

    public EarthSpearRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected float getScale(EarthSpearsEntity entity) {
        return entity.getCurrentScale();
    }

    @Override
    public ResourceLocation getTextureLocation(EarthSpearsEntity entity) {
        return TEXTURE;
    }
}
