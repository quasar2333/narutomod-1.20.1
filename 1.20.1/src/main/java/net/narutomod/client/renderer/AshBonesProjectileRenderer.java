package net.narutomod.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.narutomod.entity.ThrownSpecialWeaponEntity;
import net.narutomod.registry.ModItems;

public final class AshBonesProjectileRenderer extends FlightAlignedItemProjectileRenderer<ThrownSpecialWeaponEntity> {
    public AshBonesProjectileRenderer(EntityRendererProvider.Context context) {
        super(context, ModItems.ASHBONES.get(), 0.1F);
    }
}
