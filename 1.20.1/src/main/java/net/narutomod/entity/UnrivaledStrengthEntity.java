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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.particle.NarutoParticleKind;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModItems;
import net.narutomod.registry.ModParticleTypes;
import net.narutomod.registry.ModSounds;

public final class UnrivaledStrengthEntity extends Entity {
    private static final int SMOKE_COLOR = 0x20FFFFFF;
    private static final double PUNCH_RANGE = 5.0D;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(UnrivaledStrengthEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> POWER = SynchedEntityData.defineId(UnrivaledStrengthEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DURATION = SynchedEntityData.defineId(UnrivaledStrengthEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> STEAM_ARMOR = SynchedEntityData.defineId(UnrivaledStrengthEntity.class, EntityDataSerializers.BOOLEAN);

    @Nullable
    private UUID ownerUuid;

    public UnrivaledStrengthEntity(EntityType<? extends UnrivaledStrengthEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public void configure(LivingEntity owner, float chargedPower) {
        setOwner(owner);
        boolean steamArmor = isWearingSteamArmor(owner);
        setSteamArmor(steamArmor);
        float effectivePower = Math.max(steamArmor ? chargedPower * 2.0F : chargedPower, 0.1F);
        setPower(effectivePower);
        setDuration(Math.max((int)(effectivePower * 20.0F), 1));
        moveToOwner(owner);
        this.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                ModSounds.SOUND_KAIRIKIMUSO.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        applyBuffs(owner);
    }

    public static boolean spawnFrom(LivingEntity owner, float power) {
        UnrivaledStrengthEntity entity = ModEntityTypes.UNRIVALED_STRENGTH.get().create(owner.level());
        if (entity == null) {
            return false;
        }
        entity.configure(owner, power);
        return owner.level().addFreshEntity(entity);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(POWER, 0.1F);
        this.entityData.define(DURATION, 1);
        this.entityData.define(STEAM_ARMOR, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        LivingEntity owner = getOwner();
        if (owner == null || !owner.isAlive()) {
            discard();
            return;
        }
        moveToOwner(owner);
        if (isSteamArmor()) {
            spawnSteamArmorSmoke((ServerLevel)this.level(), owner);
        } else {
            boolean initialBurst = this.tickCount <= 10;
            spawnAuraSmoke((ServerLevel)this.level(), owner, initialBurst);
            hurtNearby(owner, initialBurst);
        }
        pushLookTarget(owner);
        playLoopSound();
        if (this.tickCount > getDuration()) {
            discard();
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double range = 96.0D * getViewScale();
        return distance < range * range;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        setPower(tag.contains("Power") ? tag.getFloat("Power") : 0.1F);
        setDuration(tag.contains("Duration") ? tag.getInt("Duration") : 1);
        setSteamArmor(tag.getBoolean("SteamArmor"));
        this.tickCount = tag.getInt("Life");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        tag.putFloat("Power", getPower());
        tag.putInt("Duration", getDuration());
        tag.putBoolean("SteamArmor", isSteamArmor());
        tag.putInt("Life", this.tickCount);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private void applyBuffs(LivingEntity owner) {
        int duration = getDuration();
        float power = getPower();
        addStackingEffect(owner, MobEffects.DAMAGE_BOOST, duration, (int)power);
        addStackingEffect(owner, MobEffects.MOVEMENT_SPEED, duration, (int)(power * 2.0F));
        addStackingEffect(owner, MobEffects.JUMP, duration, (int)(power * 0.5F));
    }

    private void addStackingEffect(LivingEntity owner, MobEffect effect, int duration, int baseAmplifier) {
        MobEffectInstance existing = owner.getEffect(effect);
        int amplifier = baseAmplifier + (existing != null ? existing.getAmplifier() : -1);
        owner.addEffect(new MobEffectInstance(effect, duration, Math.max(amplifier, 0), false, false));
    }

    private void spawnAuraSmoke(ServerLevel level, LivingEntity owner, boolean initialBurst) {
        int count = initialBurst ? 50 : 20;
        for (int i = 0; i < count; i++) {
            double motionX = this.random.nextDouble() - 0.5D;
            double motionY = initialBurst ? this.random.nextDouble() * 0.5D : 0.0D;
            double motionZ = this.random.nextDouble() - 0.5D;
            if (!initialBurst) {
                motionX *= 0.1D;
                motionZ *= 0.1D;
            }
            level.sendParticles(
                    ModParticleTypes.options(NarutoParticleKind.SMOKE_COLORED,
                            SMOKE_COLOR,
                            (initialBurst ? 20 : 10) + this.random.nextInt(11),
                            0,
                            0,
                            initialBurst ? -1 : owner.getId(),
                            4),
                    this.getX(),
                    this.getY() + 1.0D,
                    this.getZ(),
                    0,
                    motionX,
                    motionY,
                    motionZ,
                    1.0D);
        }
    }

    private void hurtNearby(LivingEntity owner, boolean initialBurst) {
        double radius = initialBurst ? 7.0D : 4.0D;
        for (LivingEntity target : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(radius),
                target -> target.isAlive() && target != owner && target.getRootVehicle() != owner.getRootVehicle())) {
            target.invulnerableTime = 0;
            target.hurt(target.damageSources().hotFloor(), 1.0F);
        }
    }

    private void spawnSteamArmorSmoke(ServerLevel level, LivingEntity owner) {
        double yawRadians = Math.toRadians(owner.yBodyRot);
        Vec3 back = new Vec3(0.5D * Math.sin(yawRadians), 0.0D, -0.5D * Math.cos(yawRadians));
        Vec3 source = owner.position().add(back).add(0.0D, 1.4D, 0.0D);
        for (int i = 0; i < 10; i++) {
            level.sendParticles(
                    ModParticleTypes.options(NarutoParticleKind.SMOKE_COLORED,
                            SMOKE_COLOR,
                            10 + this.random.nextInt(11),
                            0,
                            0,
                            owner.getId(),
                            4),
                    source.x(),
                    source.y(),
                    source.z(),
                    0,
                    (this.random.nextDouble() - 0.5D) * 0.1D,
                    0.0D,
                    (this.random.nextDouble() - 0.5D) * 0.1D,
                    1.0D);
        }
    }

    private void pushLookTarget(LivingEntity owner) {
        if (!owner.swinging) {
            return;
        }
        HitResult result = ProcedureUtils.objectEntityLookingAt(owner, PUNCH_RANGE, 0.0D, false, false,
                target -> target != this && target != owner && target.getRootVehicle() != owner.getRootVehicle());
        if (result instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() instanceof LivingEntity target) {
            ProcedureUtils.pushEntity(owner, target, 20.0D, 1.5F);
        }
    }

    private void playLoopSound() {
        if (this.tickCount % 5 == 4) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 0.2F, this.random.nextFloat() * 0.5F + 0.4F);
        }
    }

    private void moveToOwner(LivingEntity owner) {
        this.setDeltaMovement(Vec3.ZERO);
        this.moveTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), owner.getXRot());
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

    private float getPower() {
        return this.entityData.get(POWER);
    }

    private void setPower(float power) {
        this.entityData.set(POWER, Math.max(power, 0.1F));
    }

    private int getDuration() {
        return this.entityData.get(DURATION);
    }

    private void setDuration(int duration) {
        this.entityData.set(DURATION, Math.max(duration, 1));
    }

    private boolean isSteamArmor() {
        return this.entityData.get(STEAM_ARMOR);
    }

    private void setSteamArmor(boolean steamArmor) {
        this.entityData.set(STEAM_ARMOR, steamArmor);
    }

    private static boolean isWearingSteamArmor(LivingEntity entity) {
        ItemStack head = entity.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack legs = entity.getItemBySlot(EquipmentSlot.LEGS);
        return head.is(ModItems.STEAM_ARMORHELMET.get())
                && chest.is(ModItems.STEAM_ARMORBODY.get())
                && legs.is(ModItems.STEAM_ARMORLEGS.get());
    }
}
