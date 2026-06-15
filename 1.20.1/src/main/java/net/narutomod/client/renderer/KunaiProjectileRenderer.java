package net.narutomod.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.narutomod.entity.ThrownNinjaToolEntity;
import net.narutomod.registry.ModItems;

public final class KunaiProjectileRenderer extends FlightAlignedItemProjectileRenderer<ThrownNinjaToolEntity> {
    public KunaiProjectileRenderer(EntityRendererProvider.Context context) {
        super(context, ModItems.KUNAI.get(), 0.0F);
    }
}
