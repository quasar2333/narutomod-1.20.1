package net.narutomod.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.narutomod.NarutomodMod;
import net.narutomod.entity.IceSpearEntity;

public final class IceSpearRenderer extends LegacySpikeModelRenderer<IceSpearEntity> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/spike_ice.png");
    private static final int COLOR = 0xC0FFFFFF;

    public IceSpearRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected float getScale(IceSpearEntity entity) {
        return entity.getScale();
    }

    @Override
    protected int getColor(IceSpearEntity entity) {
        return COLOR;
    }

    @Override
    public ResourceLocation getTextureLocation(IceSpearEntity entity) {
        return TEXTURE;
    }
}
