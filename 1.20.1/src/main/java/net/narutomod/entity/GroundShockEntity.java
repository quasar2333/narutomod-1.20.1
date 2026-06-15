package net.narutomod.entity;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.registry.ModEntityTypes;

public final class GroundShockEntity extends Entity {
    private static final int AFFECTED_EFFECT_TIME = 200;
    private static final double ENTITY_LAUNCH_Y = 0.9D;
    private static final double BLOCK_LAUNCH_Y = 0.4D;

    private final Set<Integer> affectedEntityIds = new HashSet<>();
    private int radius;

    public GroundShockEntity(EntityType<? extends GroundShockEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public static boolean spawnFrom(Level level, BlockPos origin, int radius) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        GroundShockEntity entity = ModEntityTypes.GROUND_SHOCK.get().create(serverLevel);
        if (entity == null) {
            return false;
        }
        entity.configure(origin, radius);
        return serverLevel.addFreshEntity(entity);
    }

    public void configure(BlockPos origin, int radius) {
        moveTo(origin.getX() + 0.5D, origin.getY(), origin.getZ() + 0.5D);
        this.radius = Math.max(radius, 0);
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        if (this.tickCount <= this.radius) {
            runWave();
        } else {
            discard();
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.radius = tag.getInt("Radius");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Radius", this.radius);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private void runWave() {
        BlockPos center = blockPosition();
        int wave = this.tickCount + 1;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-wave, -5, -wave), center.offset(wave, 3, wave))) {
            BlockState state = this.level().getBlockState(pos);
            BlockPos above = pos.above();
            double dx = pos.getX() - center.getX();
            double dz = pos.getZ() - center.getZ();
            double distance = Mth.sqrt((float)(dx * dx + dz * dz));
            if ((int)distance == wave && state.isFaceSturdy(this.level(), pos, Direction.UP) && this.level().isEmptyBlock(above)) {
                affectEntitiesAt(above);
                launchBlock(pos.immutable(), state);
            }
        }
    }

    private void affectEntitiesAt(BlockPos pos) {
        for (Entity entity : this.level().getEntities(this, new AABB(pos))) {
            if (entity instanceof FallingBlockEntity || !this.affectedEntityIds.add(entity.getId())) {
                continue;
            }
            Vec3 velocity = entity.getDeltaMovement();
            entity.setDeltaMovement(velocity.x(), velocity.y() + ENTITY_LAUNCH_Y, velocity.z());
            entity.hasImpulse = true;
            entity.hurtMarked = true;
            if (entity instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.CONFUSION, AFFECTED_EFFECT_TIME, 0, false, false));
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, AFFECTED_EFFECT_TIME, 1, false, false));
            }
        }
    }

    private void launchBlock(BlockPos pos, BlockState state) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        FallingBlockEntity falling = FallingBlockEntity.fall(serverLevel, pos, state);
        // Legacy Ground Shock duplicated a visual falling block without removing the source terrain block.
        serverLevel.setBlock(pos, state, 3);
        falling.setDeltaMovement(0.0D, BLOCK_LAUNCH_Y, 0.0D);
        falling.hasImpulse = true;
    }
}
