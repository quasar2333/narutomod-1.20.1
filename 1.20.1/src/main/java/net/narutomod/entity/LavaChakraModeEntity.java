package net.narutomod.entity;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.Chakra;
import net.narutomod.NarutomodModVariables;

public final class LavaChakraModeEntity extends Entity {
    public static final String ENTITY_ID_KEY = "LavaChakraModeEntityId";
    public static final String ACTIVE_TAG = "LavaChakraModeActive";
    private static final double CHAKRA_BURN_PER_SECOND = 10.0D;
    private static final double AURA_RADIUS = 5.0D;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(LavaChakraModeEntity.class, EntityDataSerializers.INT);

    @Nullable
    private UUID ownerUuid;
    private int strengthAmplifier = 9;
    private boolean inactiveWritten;

    public LavaChakraModeEntity(EntityType<? extends LavaChakraModeEntity> entityType, Level level) {
        super(entityType, level);
        setNoGravity(true);
        this.noPhysics = true;
    }

    public void configure(LivingEntity owner) {
        setOwner(owner);
        if (owner.hasEffect(MobEffects.DAMAGE_BOOST)) {
            this.strengthAmplifier += owner.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier() + 1;
        }
        moveToOwner(owner);
        setClientActive(owner, true);
    }

    public boolean isOwnedBy(LivingEntity entity) {
        return entity.getUUID().equals(this.ownerUuid) || getOwner() == entity;
    }

    public void stopChakraMode() {
        discardMode();
    }

    @Nullable
    public LivingEntity getOwner() {
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

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        LivingEntity owner = getOwner();
        if (owner == null || !owner.isAlive()) {
            discardMode();
            return;
        }
        moveToOwner(owner);
        if (this.tickCount % 20 == 19 && !applyBuffsAndBurnChakra(owner)) {
            discardMode();
            return;
        }
        playLavaSound(owner);
        spawnLavaParticle(owner);
        burnNearby(owner);
    }

    @Override
    public void remove(RemovalReason reason) {
        writeInactive();
        super.remove(reason);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        this.strengthAmplifier = tag.contains("StrengthAmplifier") ? tag.getInt("StrengthAmplifier") : 9;
        this.inactiveWritten = tag.getBoolean("InactiveWritten");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putInt("StrengthAmplifier", this.strengthAmplifier);
        tag.putBoolean("InactiveWritten", this.inactiveWritten);
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private void setOwner(LivingEntity owner) {
        this.ownerUuid = owner.getUUID();
        this.entityData.set(OWNER_ID, owner.getId());
    }

    private boolean applyBuffsAndBurnChakra(LivingEntity owner) {
        if (!Chakra.pathway(owner).consume(CHAKRA_BURN_PER_SECOND)) {
            return false;
        }
        owner.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 21, this.strengthAmplifier, false, false));
        owner.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 21, 16, false, false));
        return true;
    }

    private void moveToOwner(LivingEntity owner) {
        setDeltaMovement(Vec3.ZERO);
        moveTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), owner.getXRot());
    }

    private void playLavaSound(LivingEntity owner) {
        if (this.random.nextInt(20) == 0) {
            this.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                    SoundEvents.LAVA_AMBIENT, SoundSource.PLAYERS, 0.5F, this.random.nextFloat() * 0.6F + 0.6F);
        }
    }

    private void spawnLavaParticle(LivingEntity owner) {
        if (this.level() instanceof ServerLevel serverLevel && this.random.nextInt(10) == 0) {
            serverLevel.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.LAVA,
                    owner.getX(),
                    owner.getY() + owner.getBbHeight() * 0.5D,
                    owner.getZ(),
                    1,
                    owner.getBbWidth() * 0.5D,
                    owner.getBbHeight() * 0.5D,
                    owner.getBbWidth() * 0.5D,
                    0.0D);
        }
    }

    private void burnNearby(LivingEntity owner) {
        this.level().getEntitiesOfClass(LivingEntity.class, owner.getBoundingBox().inflate(AURA_RADIUS),
                        target -> target.isAlive() && target != owner)
                .forEach(target -> {
                    target.hurt(this.level().damageSources().lava(), 4.0F);
                    target.setSecondsOnFire(15);
                });
    }

    private void discardMode() {
        writeInactive();
        discard();
    }

    private void writeInactive() {
        if (this.inactiveWritten || this.level().isClientSide) {
            return;
        }
        this.inactiveWritten = true;
        LivingEntity owner = getOwner();
        if (owner != null) {
            setClientActive(owner, false);
            if (owner.getPersistentData().getInt(ENTITY_ID_KEY) == getId()) {
                owner.getPersistentData().remove(ENTITY_ID_KEY);
            }
        }
    }

    private static void setClientActive(LivingEntity owner, boolean active) {
        if (owner instanceof ServerPlayer player) {
            NarutomodModVariables.get(player).putBoolean(ACTIVE_TAG, active);
            NarutomodModVariables.sync(player);
        }
    }
}
