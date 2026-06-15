package net.narutomod.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.narutomod.particle.NarutoParticleKind;
import net.narutomod.registry.ModBlockEntities;
import net.narutomod.registry.ModBlocks;
import net.narutomod.registry.ModParticleTypes;

public final class PortalBlockEntity extends BlockEntity {
    private static final int INITIAL_LIFETIME = 100;
    private static final int USED_LIFETIME = 200;
    private static final int TELEPORT_COOLDOWN = 20;

    @Nullable
    private BlockPos pairPos;
    private int lifetime = INITIAL_LIFETIME;
    private int teleportCooldown;

    public PortalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PORTALBLOCK.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PortalBlockEntity portal) {
        if (portal.teleportCooldown > 0) {
            portal.teleportCooldown--;
        }
        if (portal.lifetime-- <= 0) {
            level.removeBlock(pos, false);
            return;
        }
        if (level instanceof ServerLevel serverLevel && level.getGameTime() % 5L == 0L) {
            serverLevel.sendParticles(
                    ModParticleTypes.options(NarutoParticleKind.PORTAL_SPIRAL, 2, 0xCCB080FF, 10),
                    pos.getX() + 0.5D,
                    pos.getY() + 0.5D,
                    pos.getZ() + 0.5D,
                    1,
                    0.15D,
                    0.35D,
                    0.15D,
                    0.0D);
        }
    }

    public void setPair(BlockPos pairPos) {
        this.pairPos = pairPos.immutable();
        setChanged();
    }

    public void teleportEntity(Entity entity) {
        if (this.level == null || this.level.isClientSide || this.pairPos == null || this.teleportCooldown > 0) {
            return;
        }
        if (!(this.level.getBlockEntity(this.pairPos) instanceof PortalBlockEntity pairPortal)) {
            return;
        }
        BlockState pairState = this.level.getBlockState(this.pairPos);
        if (!pairState.hasProperty(PortalBlock.FACING)) {
            return;
        }
        this.teleportCooldown = TELEPORT_COOLDOWN;
        pairPortal.teleportCooldown = TELEPORT_COOLDOWN;
        this.lifetime = Math.max(this.lifetime, USED_LIFETIME);
        pairPortal.lifetime = Math.max(pairPortal.lifetime, USED_LIFETIME);
        var facing = pairState.getValue(PortalBlock.FACING);
        double x = this.pairPos.getX() + 0.5D + facing.getStepX();
        double y = this.pairPos.getY() - (this.level.getBlockState(this.pairPos.below()).is(ModBlocks.PORTALBLOCK.get()) ? 1.0D : 0.0D);
        double z = this.pairPos.getZ() + 0.5D + facing.getStepZ();
        float yaw = facing.toYRot();
        entity.fallDistance = 0.0F;
        entity.setDeltaMovement(Vec3.ZERO);
        if (entity instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.teleport(x, y, z, yaw, entity.getXRot());
        } else {
            entity.moveTo(x, y, z, yaw, entity.getXRot());
        }
        entity.setYRot(yaw);
        if (entity instanceof LivingEntity living) {
            living.setYHeadRot(yaw);
            living.setYBodyRot(yaw);
        }
        entity.hasImpulse = true;
        setChanged();
        pairPortal.setChanged();
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("PairX")) {
            this.pairPos = new BlockPos(tag.getInt("PairX"), tag.getInt("PairY"), tag.getInt("PairZ"));
        }
        this.lifetime = tag.contains("Lifetime") ? tag.getInt("Lifetime") : INITIAL_LIFETIME;
        this.teleportCooldown = tag.getInt("TeleportCooldown");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (this.pairPos != null) {
            tag.putInt("PairX", this.pairPos.getX());
            tag.putInt("PairY", this.pairPos.getY());
            tag.putInt("PairZ", this.pairPos.getZ());
        }
        tag.putInt("Lifetime", this.lifetime);
        tag.putInt("TeleportCooldown", this.teleportCooldown);
    }
}
