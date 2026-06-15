package net.narutomod.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.narutomod.entity.ThrownSpecialWeaponEntity;
import net.narutomod.registry.ModItems;

public final class BlackReceiverProjectileRenderer extends FlightAlignedItemProjectileRenderer<ThrownSpecialWeaponEntity> {
    public BlackReceiverProjectileRenderer(EntityRendererProvider.Context context) {
        super(context, ModItems.BLACK_RECEIVER.get(), 0.0F);
    }
}
