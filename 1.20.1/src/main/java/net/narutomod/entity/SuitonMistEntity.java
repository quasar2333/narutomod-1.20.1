package net.narutomod.entity;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.network.EntityGlowMessage;
import net.narutomod.network.NetworkHandler;
import net.narutomod.network.SuitonMistFogMessage;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModSounds;

public final class SuitonMistEntity extends Entity {
    private static final UUID FOLLOW_MODIFIER = UUID.fromString("7c3e5536-e32d-4ef7-8cf2-e5ef57f9d48f");
    private static final int BUILD_TIME = 200;
    private static final int DISSIPATE_TIME = 120;
    private static final int FOG_SYNC_TICKS = 3;
    private static final int GLOW_SYNC_TICKS = 5;
    private static final int DRY_IDLE_TICKS = 400;
    private static final int WET_IDLE_TICKS = 1200;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(SuitonMistEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(SuitonMistEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> IDLE_UNTIL = SynchedEntityData.defineId(SuitonMistEntity.class, EntityDataSerializers.INT);

    @Nullable
    private UUID ownerUuid;

    public SuitonMistEntity(EntityType<? extends SuitonMistEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public void configure(LivingEntity owner) {
        setOwner(owner);
        float radius = owner instanceof Player player ? (float)Math.min(1.5D * player.experienceLevel, 60.0D) : 32.0F;
        this.entityData.set(RADIUS, radius);
        this.moveTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), owner.getXRot());
        int idleTicks = hasNearbyLiquid() ? WET_IDLE_TICKS : DRY_IDLE_TICKS;
        this.entityData.set(IDLE_UNTIL, BUILD_TIME + idleTicks);
    }

    public static boolean spawnFrom(LivingEntity owner) {
        SuitonMistEntity entity = ModEntityTypes.SUITONMIST.get().create(owner.level());
        if (entity == null) {
            return false;
        }
        owner.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                ModSounds.SOUND_KIRIGAKURENOJUTSU.get(), SoundSource.PLAYERS, 5.0F, 1.0F);
        entity.configure(owner);
        owner.level().addFreshEntity(entity);
        return true;
    }

    public float getRadius() {
        return this.entityData.get(RADIUS);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(RADIUS, 0.0F);
        this.entityData.define(IDLE_UNTIL, BUILD_TIME + DRY_IDLE_TICKS);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        int idleUntil = this.entityData.get(IDLE_UNTIL);
        int dissipateUntil = idleUntil + DISSIPATE_TIME;
        if (this.tickCount >= dissipateUntil) {
            discardMist();
            return;
        }
        double phase = getFogPhase(idleUntil, dissipateUntil);
        applyMistEffects(phase);
        spawnMistParticles(phase);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide && reason.shouldDestroy()) {
            clearMobFollowModifiers();
        }
        super.remove(reason);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double range = Math.max(64.0D, getRadius() + 64.0D) * getViewScale();
        return distance < range * range;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        this.entityData.set(RADIUS, tag.getFloat("Radius"));
        this.entityData.set(IDLE_UNTIL, tag.contains("IdleUntil") ? tag.getInt("IdleUntil") : BUILD_TIME + DRY_IDLE_TICKS);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putFloat("Radius", getRadius());
        tag.putInt("IdleUntil", this.entityData.get(IDLE_UNTIL));
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private void setOwner(LivingEntity owner) {
        this.ownerUuid = owner.getUUID();
        this.entityData.set(OWNER_ID, owner.getId());
    }

    @Nullable
    private LivingEntity getOwner() {
        int ownerId = this.entityData.get(OWNER_ID);
        if (ownerId >= 0 && this.level().getEntity(ownerId) instanceof LivingEntity living) {
            return living;
        }
        if (this.ownerUuid != null && this.level() instanceof ServerLevel serverLevel
                && serverLevel.getEntity(this.ownerUuid) instanceof LivingEntity living) {
            setOwner(living);
            return living;
        }
        return null;
    }

    private double getFogPhase(int idleUntil, int dissipateUntil) {
        if (this.tickCount <= BUILD_TIME) {
            return (double)this.tickCount / BUILD_TIME;
        }
        if (this.tickCount > idleUntil && this.tickCount < dissipateUntil) {
            return (double)(dissipateUntil - this.tickCount) / DISSIPATE_TIME;
        }
        return 1.0D;
    }

    private void applyMistEffects(double phase) {
        LivingEntity owner = getOwner();
        double radius = getRadius();
        AABB box = getBoundingBox().inflate(radius + 100.0D);
        for (LivingEntity living : this.level().getEntitiesOfClass(LivingEntity.class, box)) {
            if (living == owner && !(owner instanceof Player)) {
                continue;
            }
            double distanceOutside = distanceTo(living) - radius;
            if (living instanceof Player player) {
                syncPlayerFog(player, phase, distanceOutside);
            } else if (living instanceof Mob mob) {
                applyFollowRangeReduction(mob, phase, distanceOutside);
            }
            syncOwnerTargetGlow(owner, living, true);
        }
    }

    private static void syncPlayerFog(Player player, double phase, double distanceOutside) {
        if (!(player instanceof ServerPlayer serverPlayer) || phase <= 0.0D) {
            return;
        }
        float density = (float)(phase / Math.max(distanceOutside, 1.0D));
        NetworkHandler.sendToPlayer(serverPlayer, new SuitonMistFogMessage(density, FOG_SYNC_TICKS));
    }

    private static void syncOwnerTargetGlow(@Nullable LivingEntity owner, LivingEntity target, boolean glow) {
        if (!(owner instanceof ServerPlayer ownerPlayer) || target == owner) {
            return;
        }
        int duration = glow ? GLOW_SYNC_TICKS : 0;
        NetworkHandler.sendToPlayer(ownerPlayer, new EntityGlowMessage(target.getId(), glow, duration));
    }

    private void applyFollowRangeReduction(Mob mob, double phase, double distanceOutside) {
        AttributeInstance followRange = mob.getAttribute(Attributes.FOLLOW_RANGE);
        if (followRange == null) {
            return;
        }
        followRange.removeModifier(FOLLOW_MODIFIER);
        double adjustableRange = Math.max(followRange.getValue() - 2.0D, 0.0D);
        double reduction = phase * adjustableRange - Mth.clamp(distanceOutside, 0.0D, adjustableRange);
        if (reduction > 0.0D) {
            followRange.addTransientModifier(new AttributeModifier(
                    FOLLOW_MODIFIER,
                    "suiton.followModifier",
                    -reduction,
                    AttributeModifier.Operation.ADDITION));
        }
    }

    private void clearMobFollowModifiers() {
        AABB box = getBoundingBox().inflate(255.0D);
        for (Mob mob : this.level().getEntitiesOfClass(Mob.class, box)) {
            AttributeInstance followRange = mob.getAttribute(Attributes.FOLLOW_RANGE);
            if (followRange != null) {
                followRange.removeModifier(FOLLOW_MODIFIER);
            }
        }
    }

    private void spawnMistParticles(double phase) {
        if (!(this.level() instanceof ServerLevel level) || phase <= 0.0D) {
            return;
        }
        int count = Mth.clamp((int)(getRadius() * phase), 4, 80);
        level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 1.0D, getZ(), count,
                Math.max(0.25D, getRadius() * 0.35D),
                1.5D,
                Math.max(0.25D, getRadius() * 0.35D),
                0.01D);
    }

    private boolean hasNearbyLiquid() {
        if (!(this.level() instanceof ServerLevel level)) {
            return false;
        }
        BlockPos min = BlockPos.containing(getX() - 20.0D, getY() - 10.0D, getZ() - 20.0D);
        BlockPos max = BlockPos.containing(getX() + 20.0D, getY() + 10.0D, getZ() + 20.0D);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (!level.getFluidState(pos).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void discardMist() {
        clearMobFollowModifiers();
        clearOwnerTargetGlow();
        discard();
    }

    private void clearOwnerTargetGlow() {
        LivingEntity owner = getOwner();
        if (!(owner instanceof ServerPlayer)) {
            return;
        }
        AABB box = getBoundingBox().inflate(255.0D);
        for (LivingEntity living : this.level().getEntitiesOfClass(LivingEntity.class, box)) {
            syncOwnerTargetGlow(owner, living, false);
        }
    }
}
