package net.narutomod.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.narutomod.network.BurningAshIgniteMessage;
import net.narutomod.network.NetworkHandler;

final class BurningAshParticle extends SmokeAtlasParticle {
    private final int excludeEntityId;

    BurningAshParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            int excludeEntityId
    ) {
        super(
                level,
                x,
                y,
                z,
                xSpeed,
                ySpeed,
                zSpeed,
                0xFF606060,
                5.0F + (float)Math.random() * 5.0F,
                100,
                0,
                -1,
                0.0D
        );
        this.excludeEntityId = excludeEntityId;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.removed) {
            for (Entity entity : this.level.getEntities((Entity)null, this.getBoundingBox().inflate(1.0D), entity -> entity instanceof LivingEntity)) {
                if (entity.getId() != this.excludeEntityId) {
                    NetworkHandler.sendToServer(new BurningAshIgniteMessage(entity.getId(), 10));
                }
            }
        }
    }
}
