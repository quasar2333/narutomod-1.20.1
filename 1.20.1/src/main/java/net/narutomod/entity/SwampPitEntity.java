package net.narutomod.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.particle.NarutoParticleKind;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModBlocks;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModParticleTypes;
import net.narutomod.registry.ModSounds;

public final class SwampPitEntity extends Entity {
    private static final int MAX_RADIUS = 32;
    private BlockPos center;
    private int radius;
    private int offsetY;

    public SwampPitEntity(EntityType<? extends SwampPitEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public void configure(BlockPos centerPos, int radius) {
        this.radius = Mth.clamp(radius, 1, MAX_RADIUS);
        this.center = centerPos.offset(0, this.radius, 0);
        this.offsetY = computeOffsetY();
        this.moveTo(centerPos.getX() + 0.5D, centerPos.getY(), centerPos.getZ() + 0.5D, 0.0F, 0.0F);
    }

    public static boolean hasBlockTarget(LivingEntity owner) {
        HitResult result = ProcedureUtils.raytraceBlocks(owner, 50.0D);
        return result instanceof BlockHitResult blockHit && blockHit.getType() != HitResult.Type.MISS;
    }

    public static boolean spawnFrom(LivingEntity owner, float power) {
        HitResult result = ProcedureUtils.raytraceBlocks(owner, 50.0D);
        if (!(result instanceof BlockHitResult blockHit) || blockHit.getType() == HitResult.Type.MISS
                || !(owner.level() instanceof ServerLevel level)) {
            return false;
        }
        SwampPitEntity pit = ModEntityTypes.SWAMP_PIT.get().create(level);
        if (pit == null) {
            return false;
        }
        pit.configure(blockHit.getBlockPos(), Math.max((int)power, 1));
        level.addFreshEntity(pit);
        level.playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                ModSounds.SOUND_YOMINUMA.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
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
        if (this.center == null || this.radius <= 0) {
            discard();
            return;
        }
        spawnMudSmoke();
        carveNextLayer();
        if (this.tickCount >= this.radius - this.offsetY) {
            discard();
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("Center")) {
            this.center = BlockPos.of(tag.getLong("Center"));
        }
        this.radius = tag.getInt("Radius");
        this.offsetY = tag.getInt("OffsetY");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.center != null) {
            tag.putLong("Center", this.center.asLong());
        }
        tag.putInt("Radius", this.radius);
        tag.putInt("OffsetY", this.offsetY);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private int computeOffsetY() {
        int y = 0;
        for (int j = 0; j > -this.radius * 2 && this.center.getY() + j > this.level().getMinBuildHeight(); j--) {
            if (j == y) {
                for (int x = -this.radius; x <= this.radius; x++) {
                    for (int z = -this.radius; z <= this.radius; z++) {
                        if (y >= j && this.level().isEmptyBlock(this.center.offset(x, j, z))) {
                            y--;
                        }
                    }
                }
            }
        }
        return y;
    }

    private void spawnMudSmoke() {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ModParticleTypes.options(NarutoParticleKind.SMOKE_COLORED, 0x801C120D, 25, 0, 0, -1, 4),
                    getX(),
                    getY() + 1.0D,
                    getZ(),
                    Math.max(this.radius * 100, 1),
                    this.radius / 2.0D,
                    0.0D,
                    this.radius / 2.0D,
                    0.0D);
        }
    }

    private void carveNextLayer() {
        int yOffset = 1 - this.tickCount;
        for (int x = -this.radius; x <= this.radius; x++) {
            for (int z = -this.radius; z <= this.radius; z++) {
                BlockPos pos = this.center.offset(x, yOffset, z);
                if (!this.level().isInWorldBounds(pos)) {
                    continue;
                }
                if (yOffset > this.offsetY) {
                    this.level().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                } else {
                    this.level().setBlock(pos, ModBlocks.MUD.get().defaultBlockState(), 3);
                }
            }
        }
    }
}
