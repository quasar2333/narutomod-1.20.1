package net.narutomod.entity;

import javax.annotation.Nullable;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.narutomod.registry.ModEffects;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModItems;

public final class SusanooSkeletonEntity extends AbstractSusanooEntity {
    public static final float WIDTH = 2.4F;
    public static final float HEIGHT = 3.6F;
    private static final EntityDataAccessor<Boolean> LEGS_VISIBLE = SynchedEntityData.defineId(SusanooSkeletonEntity.class, EntityDataSerializers.BOOLEAN);

    public SusanooSkeletonEntity(EntityType<? extends SusanooSkeletonEntity> entityType, Level level) {
        super(entityType, level);
        this.chakraUsage = 50.0D;
        this.setMaxUpStep(HEIGHT / 3.0F);
    }

    @Nullable
    public static SusanooSkeletonEntity spawnFrom(Player owner) {
        if (!(owner.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        SusanooSkeletonEntity entity = ModEntityTypes.SUSANOOSKELETON.get().create(serverLevel);
        if (entity == null) {
            return null;
        }
        entity.configure(owner);
        serverLevel.addFreshEntity(entity);
        owner.startRiding(entity, true);
        return entity;
    }

    public boolean legsVisible() {
        return this.entityData.get(LEGS_VISIBLE);
    }

    @Override
    public float entityModelScale() {
        return 1.0F;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(LEGS_VISIBLE, false);
    }

    @Override
    protected void configureFromOwner(LivingEntity owner) {
        super.configureFromOwner(owner);
        setAttributeBaseValue(Attributes.ATTACK_DAMAGE, Math.min(this.ownerBattleXp, BXP_REQUIRED_L2) * 0.005D);
        setHealth(getMaxHealth());
    }

    private void configure(Player owner) {
        configureFromOwner(owner);
        this.entityData.set(LEGS_VISIBLE, false);
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
        return head.is(ModItems.MANGEKYOSHARINGANHELMET.get())
                || head.is(ModItems.MANGEKYOSHARINGANETERNALHELMET.get())
                || head.is(ModItems.MANGEKYOSHARINGANOBITOHELMET.get());
    }
}
