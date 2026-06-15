package net.narutomod.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.narutomod.network.AcidSpitCorrosionMessage;
import net.narutomod.network.NetworkHandler;
import net.narutomod.particle.NarutoParticleOptions;

final class AcidSpitParticle extends SmokeAtlasParticle {
    private static int nextBreakProgressId = -1;

    private final int breakProgressId = nextBreakProgressId--;
    private final int excludedEntityId;
    private LivingEntity affectedEntity;
    private double heightOffset;

    AcidSpitParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            NarutoParticleOptions options
    ) {
        super(
                level,
                x,
                y,
                z,
                xSpeed,
                ySpeed,
                zSpeed,
                options.arg(1, 0x80FFD6BA),
                0.5F + (float)Math.random() * 4.5F,
                0,
                0,
                -1,
                -0.005D
        );
        this.excludedEntityId = options.arg(0, -1);
        this.setSize(this.bbWidth * this.legacyScale, this.bbHeight * this.legacyScale);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.removed) {
            return;
        }

        if (this.affectedEntity == null) {
            for (Entity entity : this.level.getEntities((Entity)null, this.getBoundingBox(), entity -> entity instanceof LivingEntity)) {
                if (entity.getId() != this.excludedEntityId && entity instanceof LivingEntity living) {
                    NetworkHandler.sendToServer(new AcidSpitCorrosionMessage(living.getId(), this.lifetime - this.age));
                    this.affectedEntity = living;
                    this.heightOffset = this.y - living.getY();
                    break;
                }
            }
        } else if (this.affectedEntity.isAlive()) {
            this.setPos(this.affectedEntity.getX(), this.affectedEntity.getY() + this.heightOffset, this.affectedEntity.getZ());
            this.heightOffset -= 0.005D;
        }

        BlockPos breakPos = nearestNonAirBlock(this.getBoundingBox().inflate(0.01D));
        if (breakPos != null) {
            Minecraft.getInstance().levelRenderer.destroyBlockProgress(this.breakProgressId, breakPos, 5);
        }
    }

    private BlockPos nearestNonAirBlock(AABB box) {
        int minX = Mth.floor(box.minX);
        int minY = Mth.floor(box.minY);
        int minZ = Mth.floor(box.minZ);
        int maxX = Mth.floor(box.maxX);
        int maxY = Mth.floor(box.maxY);
        int maxZ = Mth.floor(box.maxZ);

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!this.level.getBlockState(pos).isAir()) {
                        return pos;
                    }
                }
            }
        }
        return null;
    }
}
