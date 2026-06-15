package net.narutomod.entity;

import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.narutomod.item.JutsuItem;
import net.narutomod.item.NinjaToolItem;
import net.narutomod.item.SusanooPowerIncreaseHandler;
import net.narutomod.particle.NarutoParticleKind;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModEffects;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModItems;
import net.narutomod.registry.ModParticleTypes;
import net.narutomod.registry.ModSounds;

public final class ItachiEntity extends NinjaMobEntity implements RangedAttackMob {
    private static final double INITIAL_CHAKRA = 4000.0D;
    private static final double GENJUTSU_CHAKRA = 100.0D;
    private static final double FIREBALL_CHAKRA = 50.0D;
    private static final double AMATERASU_CHAKRA = 50.0D;
    private static final double INVISIBILITY_CHAKRA = 20.0D;
    private static final double SUSANOO_CHAKRA = 300.0D;
    private static final int GENJUTSU_DURATION = 200;
    private static final int GENJUTSU_LOOK_TICKS = 5;
    private static final double GENJUTSU_LOOK_RANGE = 24.0D;
    private static final int AMATERASU_DURATION = 1200;
    private static final int RANGED_ATTACK_INTERVAL_TICKS = 50;
    private static final float RANGED_ATTACK_RADIUS = 16.0F;
    private static final double LEGACY_MELEE_DISTANCE = 4.0D;
    private static final float LEGACY_FIREBALL_SCALE = 5.0F;
    private static final float LEGACY_KUNAI_SPEED = 1.0F;
    private static final int LEGACY_ITACHI_ENTITY_ID = 117;
    private static final int CROW_ESCAPE_COOLDOWN_TICKS = 200;
    private static final int CROW_ESCAPE_COUNT = 100;
    private static final int CROW_ESCAPE_INVISIBILITY_TICKS = 200;
    private static final int SUSANOO_GUARD_COOLDOWN_TICKS = 600;
    private static final int SUSANOO_GUARD_LIFESPAN_TICKS = 600;
    private static final int PEACEFUL_TARGET_RESET_TICKS = 200;
    private static final double INVISIBLE_TARGET_RANGE_SQR = 400.0D;

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            getDisplayName(),
            BossEvent.BossBarColor.BLUE,
            BossEvent.BossBarOverlay.PROGRESS);
    private int lookedAtTime;
    private int lastGenjutsuTime = -GENJUTSU_DURATION;
    private int lastInvisibilityTime = -CROW_ESCAPE_COOLDOWN_TICKS;
    private int lastSusanooTime = -SUSANOO_GUARD_COOLDOWN_TICKS;
    private int peacefulTicks;
    private double chakra = INITIAL_CHAKRA;
    private boolean real;

    public ItachiEntity(EntityType<? extends ItachiEntity> entityType, Level level) {
        super(entityType, level);
        setReal(this.random.nextInt(5) == 0);
        equipLegacyLoadout();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new RangedAttackGoal(this, 1.0D, RANGED_ATTACK_INTERVAL_TICKS, RANGED_ATTACK_RADIUS) {
            @Override
            public boolean canUse() {
                return ItachiEntity.this.shouldUseLegacyRangedAttack() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return ItachiEntity.this.shouldUseLegacyRangedAttack() && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(2, new LeapAtTargetGoal(this, 1.0F) {
            @Override
            public boolean canUse() {
                LivingEntity target = ItachiEntity.this.getTarget();
                return target != null
                        && !ItachiEntity.this.isPassenger()
                        && target.getY() - ItachiEntity.this.getY() > 3.0D
                        && super.canUse();
            }
        });
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0D, true) {
            @Override
            public boolean canUse() {
                LivingEntity target = ItachiEntity.this.getTarget();
                return target != null && ItachiEntity.this.isWithinLegacyMeleeReach(target) && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                LivingEntity target = ItachiEntity.this.getTarget();
                return target != null && ItachiEntity.this.isWithinLegacyMeleeReach(target) && super.canContinueToUse();
            }

            @Override
            protected double getAttackReachSqr(LivingEntity target) {
                double reach = ItachiEntity.this.legacyMeleeReach();
                return reach * reach;
            }
        });
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.3D));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
                this,
                Player.class,
                10,
                true,
                false,
                ItachiEntity::isPreferredLegacyTarget));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            syncLegacyHeadwear();
            tickLegacyPeacefulState();
            tickLegacyGenjutsu();
            trackLegacyBossPlayers();
            this.bossEvent.setProgress(Mth.clamp(getHealth() / getMaxHealth(), 0.0F, 1.0F));
        }
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
            @Nullable SpawnGroupData spawnData, @Nullable CompoundTag dataTag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, spawnData, dataTag);
        equipLegacyLoadout();
        return result;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypes.FALL)) {
            return false;
        }
        if (!this.level().isClientSide) {
            Entity attacker = source.getEntity();
            if (this.random.nextInt(3) <= 1) {
                teleportLegacyDodge(attacker);
                return false;
            }
            if (shouldTriggerLegacySusanooGuard(amount)) {
                SusanooClothedEntity susanoo = spawnLegacySusanooGuard(source, attacker);
                if (susanoo != null) {
                    susanoo.hurt(source, amount);
                    return false;
                }
            }
            if (this.tickCount > this.lastInvisibilityTime + CROW_ESCAPE_COOLDOWN_TICKS
                    && consumeChakra(INVISIBILITY_CHAKRA)) {
                spawnLegacyCrowEscape();
                this.lastInvisibilityTime = this.tickCount;
            }
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        SusanooClothedEntity susanoo = activeLegacySusanoo();
        if (susanoo != null) {
            susanoo.swing(InteractionHand.MAIN_HAND);
            return susanoo.doHurtTarget(target);
        }
        return super.doHurtTarget(target);
    }

    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        int chance = this.random.nextInt(12);
        SusanooClothedEntity susanoo = activeLegacySusanoo();
        if (susanoo != null) {
            if (chance < 5) {
                susanoo.launchMagatama(directionTo(target, susanoo), SusanooClothedEntity.MODEL_SCALE * 0.5F);
            }
            return;
        }
        if (chance == 0 && distanceFactor > 0.3333F && consumeChakra(AMATERASU_CHAKRA)) {
            applyLegacyAmaterasu(target);
        } else if (chance <= 2 && distanceFactor >= 0.5333F && consumeChakra(FIREBALL_CHAKRA)) {
            launchLegacyFireball(target);
        } else if (!isPassenger()) {
            launchLegacyKunai(target);
        }
    }

    @Override
    public boolean isAlliedTo(Entity entity) {
        return entity instanceof ItachiEntity
                || entity.getType() == ModEntityTypes.KISAME_HOSHIGAKI.get()
                || super.isAlliedTo(entity);
    }

    @Override
    public boolean hasLineOfSight(Entity entity) {
        return super.hasLineOfSight(entity)
                && (!entity.isInvisible() || distanceToSqr(entity) <= INVISIBLE_TARGET_RANGE_SQR);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.lookedAtTime = tag.getInt("LookedAtTime");
        this.lastGenjutsuTime = tag.contains("LastGenjutsuTime") ? tag.getInt("LastGenjutsuTime") : -GENJUTSU_DURATION;
        this.lastInvisibilityTime = tag.contains("LastInvisibilityTime")
                ? tag.getInt("LastInvisibilityTime")
                : -CROW_ESCAPE_COOLDOWN_TICKS;
        this.lastSusanooTime = tag.contains("LastSusanooTime") ? tag.getInt("LastSusanooTime") : -SUSANOO_GUARD_COOLDOWN_TICKS;
        this.chakra = tag.contains("ItachiChakra") ? tag.getDouble("ItachiChakra") : INITIAL_CHAKRA;
        setReal(tag.contains("RealItachi") ? tag.getBoolean("RealItachi") : this.real);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("LookedAtTime", this.lookedAtTime);
        tag.putInt("LastGenjutsuTime", this.lastGenjutsuTime);
        tag.putInt("LastInvisibilityTime", this.lastInvisibilityTime);
        tag.putInt("LastSusanooTime", this.lastSusanooTime);
        tag.putDouble("ItachiChakra", this.chakra);
        tag.putBoolean("RealItachi", this.real);
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        this.spawnAtLocation(new ItemStack(ModItems.KUNAI.get()));
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ILLUSIONER_DEATH;
    }

    private void equipLegacyLoadout() {
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.KUNAI.get()));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        syncLegacyHeadwear();
    }

    private void syncLegacyHeadwear() {
        LivingEntity target = getTarget();
        boolean combatHead = target != null && target.isAlive();
        ItemStack head = getItemBySlot(EquipmentSlot.HEAD);
        if (combatHead) {
            if (head.getItem() != ModItems.MANGEKYOSHARINGANHELMET.get()) {
                head = new ItemStack(ModItems.MANGEKYOSHARINGANHELMET.get());
                setItemSlot(EquipmentSlot.HEAD, head);
            }
            JutsuItem.setOwner(head, this);
        } else if (head.getItem() != ModItems.AKATSUKI_ROBEHELMET.get()) {
            setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.AKATSUKI_ROBEHELMET.get()));
        }
        setDropChance(EquipmentSlot.HEAD, this.real ? 1.0F : 0.0F);
    }

    private void setReal(boolean real) {
        this.real = real;
        setDropChance(EquipmentSlot.HEAD, this.real ? 1.0F : 0.0F);
    }

    private boolean shouldUseLegacyRangedAttack() {
        LivingEntity target = getTarget();
        return target != null && target.isAlive() && !isWithinLegacyMeleeReach(target);
    }

    private boolean isWithinLegacyMeleeReach(LivingEntity target) {
        double reach = legacyMeleeReach();
        return distanceToSqr(target) <= reach * reach;
    }

    private double legacyMeleeReach() {
        SusanooClothedEntity susanoo = activeLegacySusanoo();
        return susanoo != null ? ProcedureUtils.getReachDistance(susanoo) : LEGACY_MELEE_DISTANCE;
    }

    @Nullable
    private SusanooClothedEntity activeLegacySusanoo() {
        return getVehicle() instanceof SusanooClothedEntity susanoo && susanoo.isAlive() ? susanoo : null;
    }

    private void applyLegacyAmaterasu(LivingEntity target) {
        this.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                ModSounds.SOUND_SHARINGANSFX.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        target.addEffect(new MobEffectInstance(ModEffects.AMATERASUFLAME.get(), AMATERASU_DURATION, 1, false, false));
    }

    private void launchLegacyFireball(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        KatonFireballEntity fireball = ModEntityTypes.KATONFIREBALL.get().create(level);
        if (fireball == null) {
            return;
        }
        fireball.configure(this, directionTo(target, this), LEGACY_FIREBALL_SCALE);
        level.addFreshEntity(fireball);
        level.playSound(null, getX(), getY(), getZ(), ModSounds.SOUND_FLAMETHROW.get(),
                SoundSource.NEUTRAL, 1.0F, 0.8F + this.random.nextFloat() * 0.4F);
    }

    private void launchLegacyKunai(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        ThrownNinjaToolEntity kunai = ModEntityTypes.ENTITYBULLETKUNAI.get().create(level);
        if (kunai == null) {
            return;
        }
        kunai.configure(this, (NinjaToolItem) ModItems.KUNAI.get(), false);
        kunai.setKnockbackStrength(1);
        kunai.moveTo(getX(), getEyeY() - 0.1D, getZ(), getYRot(), getXRot());
        Vec3 direction = target.getEyePosition().subtract(kunai.position());
        double horizontal = Math.sqrt(direction.x() * direction.x() + direction.z() * direction.z());
        kunai.shoot(direction.x(), direction.y() + horizontal * 0.2D, direction.z(), LEGACY_KUNAI_SPEED, 0.0F);
        level.addFreshEntity(kunai);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.ARROW_SHOOT, SoundSource.NEUTRAL,
                1.0F, 1.0F / (this.random.nextFloat() * 0.5F + 1.0F) + 0.25F);
    }

    private static Vec3 directionTo(LivingEntity target, Entity origin) {
        return target.getEyePosition().subtract(origin.getX(), origin.getEyeY(), origin.getZ());
    }

    private void tickLegacyPeacefulState() {
        LivingEntity target = getTarget();
        LivingEntity attacker = getLastHurtByMob();
        boolean peaceful = (target == null || !target.isAlive()) && (attacker == null || !attacker.isAlive());
        if (peaceful) {
            this.peacefulTicks++;
            if (this.peacefulTicks % 20 == 19) {
                this.chakra = Math.min(INITIAL_CHAKRA, this.chakra + INITIAL_CHAKRA * 0.04D);
                if (getHealth() < getMaxHealth()) {
                    heal(1.0F);
                }
            }
            if (this.peacefulTicks > PEACEFUL_TARGET_RESET_TICKS) {
                setTarget(null);
            }
        } else {
            this.peacefulTicks = 0;
        }
    }

    private void trackLegacyBossPlayers() {
        Entity entity = getLastHurtByMob();
        if (!(entity instanceof ServerPlayer)) {
            entity = getTarget();
        }
        if (entity instanceof ServerPlayer player) {
            this.bossEvent.addPlayer(player);
        } else {
            this.bossEvent.removeAllPlayers();
        }
    }

    private void tickLegacyGenjutsu() {
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            this.lookedAtTime = 0;
            return;
        }
        if (isTargetLookingAtItachi(target)) {
            this.lookedAtTime++;
        } else {
            this.lookedAtTime = 0;
        }
        if (this.lookedAtTime >= GENJUTSU_LOOK_TICKS
                && this.tickCount > this.lastGenjutsuTime + GENJUTSU_DURATION
                && consumeChakra(GENJUTSU_CHAKRA)) {
            applyLegacyGenjutsu(target);
            this.lastGenjutsuTime = this.tickCount;
            this.lookedAtTime = 0;
        }
    }

    private boolean isTargetLookingAtItachi(LivingEntity target) {
        HitResult result = ProcedureUtils.objectEntityLookingAt(target, GENJUTSU_LOOK_RANGE);
        return result instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() == this;
    }

    private void applyLegacyGenjutsu(LivingEntity target) {
        if (target instanceof ServerPlayer serverTarget && this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    serverTarget,
                    ModParticleTypes.options(NarutoParticleKind.MOB_APPEARANCE, LEGACY_ITACHI_ENTITY_ID),
                    true,
                    target.getX(),
                    target.getY() + target.getBbHeight() * 0.5D,
                    target.getZ(),
                    1,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.0D);
        }
        this.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                ModSounds.SOUND_GENJUTSU.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        target.addEffect(new MobEffectInstance(ModEffects.PARALYSIS.get(), GENJUTSU_DURATION, 1, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, GENJUTSU_DURATION + 40, 0, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, true));
    }

    private boolean consumeChakra(double amount) {
        if (this.chakra < amount) {
            return false;
        }
        this.chakra -= amount;
        return true;
    }

    private void teleportLegacyDodge(@Nullable Entity attacker) {
        teleportTo(
                getX() + (this.random.nextDouble() - 0.5D) * 2.0D,
                getY(),
                getZ() + (this.random.nextDouble() - 0.5D) * 2.0D);
        if (attacker instanceof LivingEntity living) {
            setTarget(living);
            setLastHurtByMob(living);
        }
    }

    private boolean shouldTriggerLegacySusanooGuard(float incomingDamage) {
        return this.real
                && getHealth() > 0.0F
                && getHealth() - incomingDamage <= getMaxHealth() / 3.0F
                && !isPassenger()
                && this.tickCount > this.lastSusanooTime + SUSANOO_GUARD_COOLDOWN_TICKS
                && this.chakra >= SUSANOO_CHAKRA;
    }

    @Nullable
    private SusanooClothedEntity spawnLegacySusanooGuard(DamageSource source, @Nullable Entity attacker) {
        if (!(this.level() instanceof ServerLevel)) {
            return null;
        }
        SusanooClothedEntity susanoo = SusanooClothedEntity.spawnFrom(this, false, SUSANOO_GUARD_LIFESPAN_TICKS, true);
        if (susanoo == null) {
            return null;
        }
        consumeChakra(SUSANOO_CHAKRA);
        this.lastSusanooTime = this.tickCount;
        if (attacker instanceof LivingEntity living) {
            setTarget(living);
            setLastHurtByMob(living);
        }
        return susanoo;
    }

    private void spawnLegacyCrowEscape() {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, CROW_ESCAPE_INVISIBILITY_TICKS, 1, false, false));
        for (int i = 0; i < CROW_ESCAPE_COUNT; i++) {
            CrowEntity crow = ModEntityTypes.CROW.get().create(level);
            if (crow != null) {
                crow.moveTo(getX(), getY() + 1.4D, getZ(), this.random.nextFloat() * 360.0F, 0.0F);
                level.addFreshEntity(crow);
            }
        }
        teleportTo(
                getX() + (this.random.nextDouble() - 0.5D) * 6.0D,
                getY() + 1.0D,
                getZ() + (this.random.nextDouble() - 0.5D) * 6.0D);
    }

    private static boolean isPreferredLegacyTarget(@Nullable LivingEntity target) {
        if (!(target instanceof Player player)) {
            return false;
        }
        return SusanooPowerIncreaseHandler.isSharinganHead(player.getItemBySlot(EquipmentSlot.HEAD))
                || BijuManager.isJinchuriki(player);
    }
}
