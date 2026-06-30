package net.narutomod.entity;

import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.narutomod.registry.ModEffects;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModItems;

public final class SusanooSkeletonEntity extends AbstractSusanooEntity {
    public static final float WIDTH = 2.4F;
    public static final float HALF_HEIGHT = 2.4F;
    public static final float HEIGHT = 3.6F;
    private static final EntityDataAccessor<Boolean> FULL_BODY = SynchedEntityData.defineId(SusanooSkeletonEntity.class, EntityDataSerializers.BOOLEAN);

    public SusanooSkeletonEntity(EntityType<? extends SusanooSkeletonEntity> entityType, Level level) {
        super(entityType, level);
        this.chakraUsage = 30.0D;
        this.setMaxUpStep(HEIGHT / 3.0F);
    }

    @Nullable
    public static SusanooSkeletonEntity spawnFrom(Player owner) {
        return spawnFrom(owner, false);
    }

    @Nullable
    public static SusanooSkeletonEntity spawnFrom(Player owner, boolean fullBody) {
        if (!(owner.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        SusanooSkeletonEntity entity = ModEntityTypes.SUSANOOSKELETON.get().create(serverLevel);
        if (entity == null) {
            return null;
        }
        entity.configure(owner, fullBody);
        serverLevel.addFreshEntity(entity);
        owner.startRiding(entity, true);
        return entity;
    }

    public boolean legsVisible() {
        return false;
    }

    public boolean isFullBody() {
        return this.entityData.get(FULL_BODY);
    }

    @Override
    public float entityModelScale() {
        return 1.0F;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.fixed(WIDTH, isFullBody() ? HEIGHT : HALF_HEIGHT);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (FULL_BODY.equals(key)) {
            refreshDimensions();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.tickCount % 20 == 1) {
            addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 22, 2, false, false));
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(FULL_BODY, false);
    }

    @Override
    protected void configureFromOwner(LivingEntity owner) {
        super.configureFromOwner(owner);
        setAttributeBaseValue(Attributes.ATTACK_DAMAGE, Math.min(this.ownerBattleXp, BXP_REQUIRED_L2) * 0.003D);
        setReachDistance(isFullBody() ? BASE_REACH_DISTANCE : 0.0D);
        setHealth(getMaxHealth());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(FULL_BODY, tag.getBoolean("FullBody"));
        refreshDimensions();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("FullBody", isFullBody());
    }

    private void configure(Player owner, boolean fullBody) {
        this.entityData.set(FULL_BODY, fullBody);
        configureFromOwner(owner);
        this.setMaxUpStep((fullBody ? HEIGHT : HALF_HEIGHT) / 3.0F);
        refreshDimensions();
    }

    @Override
    protected void doPush(Entity entity) {
        if (!this.level().isClientSide
                && getOwner() != null
                && entity instanceof LivingEntity living
                && living != getOwner()
                && !isAlliedTo(living)
                && hasMangekyo(getOwner())) {
            living.addEffect(new MobEffectInstance(ModEffects.AMATERASUFLAME.get(), 200, 0, false, false));
        }
        super.doPush(entity);
    }

    private static boolean hasMangekyo(@Nullable LivingEntity owner) {
        if (owner == null) {
            return false;
        }
        ItemStack head = owner.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
        return head.is(ModItems.MANGEKYOSHARINGANHELMET.get());
    }
}
