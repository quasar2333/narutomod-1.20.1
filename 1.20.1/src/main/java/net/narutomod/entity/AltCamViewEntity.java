package net.narutomod.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.registry.ModEntityTypes;

public final class AltCamViewEntity extends Entity {
    private static final String LIFE_TICKS_TAG = "LifeTicks";
    private int lifeTicks;

    public AltCamViewEntity(EntityType<? extends AltCamViewEntity> entityType, Level level) {
        super(entityType, level);
        setNoGravity(true);
        this.noPhysics = true;
    }

    public static AltCamViewEntity spawnDebug(ServerPlayer player) {
        AltCamViewEntity entity = ModEntityTypes.ALTCAMVIEWENTITY.get().create(player.serverLevel());
        if (entity == null) {
            return null;
        }

        Vec3 look = player.getLookAngle();
        entity.moveTo(
                player.getX() + look.x * 2.0D,
                player.getEyeY() + look.y * 2.0D,
                player.getZ() + look.z * 2.0D,
                player.getYRot(),
                player.getXRot());
        entity.lifeTicks = 200;
        player.serverLevel().addFreshEntity(entity);
        return entity;
    }

    @Override
    public void tick() {
        super.tick();
        setNoGravity(true);
        this.noPhysics = true;
        setDeltaMovement(Vec3.ZERO);
        if (!level().isClientSide && this.lifeTicks > 0 && this.tickCount > this.lifeTicks) {
            discard();
        }
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.lifeTicks = tag.contains(LIFE_TICKS_TAG) ? tag.getInt(LIFE_TICKS_TAG) : 0;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.lifeTicks > 0) {
            tag.putInt(LIFE_TICKS_TAG, this.lifeTicks);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
