package net.narutomod.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.narutomod.entity.ThrownNinjaToolEntity;
import net.narutomod.registry.ModItems;

public final class ExplosiveKunaiProjectileRenderer extends FlightAlignedItemProjectileRenderer<ThrownNinjaToolEntity> {
    public ExplosiveKunaiProjectileRenderer(EntityRendererProvider.Context context) {
        super(context, ModItems.KUNAI_EXPLOSIVE.get(), 0.0F);
    }
}
