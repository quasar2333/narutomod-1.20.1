package net.narutomod.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.narutomod.entity.AsakujakuFireballEntity;

public final class AsakujakuFireballRenderer extends ThrownItemRenderer<AsakujakuFireballEntity> {
    public AsakujakuFireballRenderer(EntityRendererProvider.Context context) {
        super(context, 0.75F, true);
    }
}
