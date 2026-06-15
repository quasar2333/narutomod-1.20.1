package net.narutomod.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.narutomod.NarutomodMod;
import net.narutomod.entity.BrackenDanceEntity;

public final class BrackenDanceRenderer extends LegacySpikeModelRenderer<BrackenDanceEntity> {
    private static final ResourceLocation TEXTURE = NarutomodMod.location("textures/spike_bone.png");

    public BrackenDanceRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected float getScale(BrackenDanceEntity entity) {
        return entity.getCurrentScale();
    }

    @Override
    public ResourceLocation getTextureLocation(BrackenDanceEntity entity) {
        return TEXTURE;
    }
}
