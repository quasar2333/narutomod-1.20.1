package net.narutomod.entity;

import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.narutomod.registry.ModEffects;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModItems;

public final class SusanooClothedEntity extends AbstractSusanooEntity {
    public static final float MODEL_SCALE = 4.0F;
    public static final float WIDTH = MODEL_SCALE * 0.8F;
    public static final float HALF_HEIGHT = MODEL_SCALE * 1.25F;
    public static final float FULL_HEIGHT = MODEL_SCALE * 2.0F;
    private static final int DEFAULT_LIFESPAN = Integer.MAX_VALUE;
    private static final EntityDataAccessor<Boolean> HAS_LEGS = SynchedEntityData.defineId(SusanooClothedEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SWINGING_ARMS = SynchedEntityData.defineId(SusanooClothedEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SHOW_SWORD = SynchedEntityData.defineId(SusanooClothedEntity.class, EntityDataSerializers.BOOLEAN);

    private int lifeSpan = DEFAULT_LIFESPAN;

    public SusanooClothedEntity(EntityType<? extends SusanooClothedEntity> entityType, Level level) {
        super(entityType, level);
        this.chakraUsage = 70.0D;
        this.setMaxUpStep(FULL_HEIGHT / 3.0F);
    }

    @Nullable
    public static SusanooClothedEntity spawnFrom(Player owner, boolean fullBody) {
        return spawnFrom(owner, fullBody, DEFAULT_LIFESPAN, true);
    }

    @Nullable
    public static SusanooClothedEntity spawnFrom(LivingEntity owner, boolean fullBody, int lifeSpan, boolean mountOwner) {
        if (!(owner.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        SusanooClothedEntity entity = ModEntityTypes.SUSANOOCLOTHED.get().create(serverLevel);
        if (entity == null) {
            return null;
        }
        entity.configure(owner, fullBody);
        entity.setLifeSpan(lifeSpan);
        serverLevel.addFreshEntity(entity);
        if (mountOwner) {
            owner.startRiding(entity, true);
        }
        return entity;
    }

    public boolean hasLegs() {
        return this.entityData.get(HAS_LEGS);
    }

    public boolean isSwingingArms() {
        return this.entityData.get(SWINGING_ARMS);
    }

    public void setSwingingArms(boolean swingingArms) {
        this.entityData.set(SWINGING_ARMS, swingingArms);
    }

    @Override
    public float entityModelScale() {
        return MODEL_SCALE;
    }

    @Override
    public boolean shouldShowSword() {
        return this.entityData.get(SHOW_SWORD);
    }

    @Override
    public void setShowSword(boolean show) {
        this.entityData.set(SHOW_SWORD, show);
        setAttributeBaseValue(Attributes.ATTACK_DAMAGE, baseAttackDamage() * (show ? 2.2D : 1.0D));
    }

    @Override
    public double getPassengersRidingOffset() {
        return hasLegs() ? MODEL_SCALE : super.getPassengersRidingOffset();
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.fixed(WIDTH, hasLegs() ? FULL_HEIGHT : HALF_HEIGHT);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (HAS_LEGS.equals(key)) {
            refreshDimensions();
        }
    }

    @Override
    public void tick() {
        if (hasLegs() && getBbHeight() < FULL_HEIGHT) {
            refreshDimensions();
        }
        super.tick();
        if (!this.level().isClientSide && this.lifeSpan-- <= 0) {
            discard();
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hurt = super.doHurtTarget(target);
        if (hurt && target instanceof LivingEntity living && hasMangekyo(getOwner()) && !isAlliedTo(living)) {
            living.addEffect(new MobEffectInstance(ModEffects.AMATERASUFLAME.get(), 200, hasLegs() ? 2 : 1, false, false));
        }
        return hurt;
    }

    @Override
    protected void doPush(Entity entity) {
        if (!this.level().isClientSide
                && entity instanceof LivingEntity living
                && living != getOwner()
                && !isAlliedTo(living)
                && hasMangekyo(getOwner())) {
            living.addEffect(new MobEffectInstance(ModEffects.AMATERASUFLAME.get(), 200, hasLegs() ? 2 : 1, false, false));
        }
        super.doPush(entity);
    }

    @Override
    public void remove(RemovalReason reason) {
        setSwingingArms(false);
        super.remove(reason);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(HAS_LEGS, false);
        this.entityData.define(SWINGING_ARMS, false);
        this.entityData.define(SHOW_SWORD, false);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(HAS_LEGS, tag.getBoolean("HasLegs"));
        this.entityData.set(SWINGING_ARMS, tag.getBoolean("SwingingArms"));
        this.entityData.set(SHOW_SWORD, tag.getBoolean("ShowSword"));
        this.lifeSpan = tag.contains("LifeSpan") ? tag.getInt("LifeSpan") : DEFAULT_LIFESPAN;
        refreshDimensions();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("HasLegs", hasLegs());
        tag.putBoolean("SwingingArms", isSwingingArms());
        tag.putBoolean("ShowSword", shouldShowSword());
        tag.putInt("LifeSpan", this.lifeSpan);
    }

    public void setLifeSpan(int ticks) {
        this.lifeSpan = ticks;
    }

    public boolean createMagatama(float size) {
        setSwingingArms(true);
        return YasakaMagatamaEntity.spawnFrom(this, getFlameColor(), size, false);
    }

    public boolean launchMagatama(Vec3 direction, float size) {
        setSwingingArms(false);
        return YasakaMagatamaEntity.spawnFrom(this, getFlameColor(), size, true, direction);
    }

    private void configure(LivingEntity owner, boolean fullBody) {
        this.entityData.set(HAS_LEGS, fullBody);
        this.chakraUsage = fullBody ? 60.0D : 50.0D;
        configureFromOwner(owner);
        setAttributeBaseValue(Attributes.MAX_HEALTH, getMaxHealth() * (fullBody ? 3.0D : 2.0D));
        setAttributeBaseValue(Attributes.ATTACK_DAMAGE, baseAttackDamage());
        if (fullBody) {
            setAttributeBaseValue(Attributes.MOVEMENT_SPEED, getAttributeValue(Attributes.MOVEMENT_SPEED) + 0.2D);
        }
        setHealth(getMaxHealth());
        refreshDimensions();
    }

    private double baseAttackDamage() {
        return Math.min(this.ownerBattleXp, hasLegs() ? BXP_REQUIRED_L4 : BXP_REQUIRED_L3) * 0.005D;
    }

    private static boolean hasMangekyo(@Nullable LivingEntity owner) {
        if (owner == null) {
            return false;
        }
        ItemStack head = owner.getItemBySlot(EquipmentSlot.HEAD);
        return head.is(ModItems.MANGEKYOSHARINGANHELMET.get())
                || head.is(ModItems.MANGEKYOSHARINGANETERNALHELMET.get())
                || head.is(ModItems.MANGEKYOSHARINGANOBITOHELMET.get());
    }
}
