package net.narutomod.entity;

import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.level.Level;

public final class HakuEntity extends NinjaMobEntity implements RangedAttackMob {
    private static final int LEGACY_NINJA_LEVEL = 60;
    private static final double LEGACY_MAX_HEALTH = 50.0D + 0.005D * LEGACY_NINJA_LEVEL * LEGACY_NINJA_LEVEL;
    private static final double LEGACY_ARMOR = 40.0D;
    private static final double LEGACY_MOVEMENT_SPEED = 0.6D;
    private static final double LEGACY_ATTACK_DAMAGE = 10.0D;
    private static final double LEGACY_FOLLOW_RANGE = 48.0D;
    private static final double INITIAL_CHAKRA = 4000.0D;
    private static final double ICE_SPEARS_CHAKRA = 20.0D;
    private static final double ICE_DOME_CHAKRA = 5.0D;
    private static final int ICE_SPEARS_COOLDOWN_TICKS = 80;
    private static final int DOME_SPEARS_COOLDOWN_TICKS = 160;
    private static final int DOME_COOLDOWN_TICKS = 500;
    private static final int MAX_DOME_USAGE_TICKS = DOME_SPEARS_COOLDOWN_TICKS + 180;
    private static final float ICE_SPEARS_RANGE = 10.0F;
    private static final float ICE_SPEARS_POWER = 2.0F;
    private static final double LEGACY_MELEE_DISTANCE_SQR = 9.0D;

    private double chakra = INITIAL_CHAKRA;
    private int peacefulTicks;
    @Nullable
    private IceDomeEntity domeEntity;
    private int domeSpearsLastUsed;
    private int domeLastUsed;
    @Nullable
    private LivingEntity leader;

    public HakuEntity(EntityType<? extends HakuEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = LEGACY_NINJA_LEVEL;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return NinjaMobEntity.createAttributes(
                LEGACY_MAX_HEALTH,
                LEGACY_ARMOR,
                LEGACY_MOVEMENT_SPEED,
                LEGACY_ATTACK_DAMAGE,
                LEGACY_FOLLOW_RANGE);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.5D, true) {
            @Override
            public boolean canUse() {
                return HakuEntity.this.getLastHurtByMob() == null && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                LivingEntity target = HakuEntity.this.getTarget();
                return target != null
                        && HakuEntity.this.distanceToSqr(target) <= LEGACY_MELEE_DISTANCE_SQR
                        && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(2, new RangedAttackGoal(this, 1.25D, ICE_SPEARS_COOLDOWN_TICKS, ICE_SPEARS_RANGE));
        this.goalSelector.addGoal(3, new FollowLeaderGoal(this, 0.5D, 4.0F));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
    }

    public void setLeader(@Nullable LivingEntity leader) {
        this.leader = leader;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            tickLegacyIceDomeSupport();
            tickLegacyChakraRecovery();
        }
    }

    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        this.swing(InteractionHand.MAIN_HAND);
        if (this.level().isClientSide || !consumeLegacyChakra(ICE_SPEARS_CHAKRA)) {
            return;
        }
        IceSpearEntity.spawnAtTarget(this, target, ICE_SPEARS_POWER);
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.chakra = tag.contains("HakuChakra") ? tag.getDouble("HakuChakra") : INITIAL_CHAKRA;
        this.domeSpearsLastUsed = tag.getInt("HakuDomeSpearsLastUsed");
        this.domeLastUsed = tag.getInt("HakuDomeLastUsed");
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putDouble("HakuChakra", this.chakra);
        tag.putInt("HakuDomeSpearsLastUsed", this.domeSpearsLastUsed);
        tag.putInt("HakuDomeLastUsed", this.domeLastUsed);
    }

    @Override
    public float getVoicePitch() {
        return (this.getRandom().nextFloat() - this.getRandom().nextFloat()) * 0.2F + 2.4F;
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
    }

    boolean consumeLegacyChakra(double amount) {
        if (this.chakra < amount) {
            return false;
        }
        this.chakra -= amount;
        return true;
    }

    private void tickLegacyIceDomeSupport() {
        LivingEntity target = getTarget();
        if (target != null && target.isAlive()
                && shouldSpawnLegacyDome()
                && !domeActive()
                && this.tickCount > this.domeLastUsed + DOME_COOLDOWN_TICKS
                && this.chakra > ICE_DOME_CHAKRA * 5.0D) {
            this.domeEntity = IceDomeEntity.spawnAt(this, target.getX(), target.getY() - 0.1D, target.getZ());
            if (this.domeEntity != null) {
                this.domeSpearsLastUsed = this.tickCount - DOME_SPEARS_COOLDOWN_TICKS + 60;
                this.domeLastUsed = this.tickCount;
            }
        }
        if (!domeActive()) {
            return;
        }
        if (this.domeEntity.tickCount > MAX_DOME_USAGE_TICKS) {
            this.domeEntity.discard();
            this.domeEntity = null;
        } else if (this.tickCount > this.domeSpearsLastUsed + DOME_SPEARS_COOLDOWN_TICKS) {
            this.domeEntity.shootSpears();
            this.domeSpearsLastUsed = this.tickCount;
        }
    }

    private boolean shouldSpawnLegacyDome() {
        return (this.leader != null && this.leader.isAlive() && this.leader.getHealth() < this.leader.getMaxHealth() * 0.25F)
                || getHealth() < getMaxHealth() * 0.5F;
    }

    private boolean domeActive() {
        return this.domeEntity != null && this.domeEntity.isAlive();
    }

    private void tickLegacyChakraRecovery() {
        LivingEntity target = getTarget();
        LivingEntity attacker = getLastHurtByMob();
        boolean peaceful = (target == null || !target.isAlive()) && (attacker == null || !attacker.isAlive());
        if (!peaceful) {
            this.peacefulTicks = 0;
            return;
        }
        this.peacefulTicks++;
        if (this.peacefulTicks % 20 == 19) {
            this.chakra = Math.min(INITIAL_CHAKRA, this.chakra + INITIAL_CHAKRA * 0.04D);
            if (getHealth() < getMaxHealth()) {
                heal(1.0F);
            }
        }
    }

    private static final class FollowLeaderGoal extends Goal {
        private final HakuEntity haku;
        private final double speed;
        private final float stopRange;
        @Nullable
        private LivingEntity followingEntity;

        private FollowLeaderGoal(HakuEntity haku, double speed, float stopRange) {
            this.haku = haku;
            this.speed = speed;
            this.stopRange = stopRange;
        }

        @Override
        public boolean canUse() {
            LivingEntity leader = this.haku.leader;
            if (leader == null || !leader.isAlive()) {
                return false;
            }
            if (leader instanceof Player player && player.isSpectator()) {
                return false;
            }
            if (this.haku.distanceToSqr(leader) < this.stopRange * this.stopRange) {
                return false;
            }
            this.followingEntity = leader;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return this.followingEntity != null
                    && this.followingEntity.isAlive()
                    && this.haku.distanceToSqr(this.followingEntity) >= this.stopRange * this.stopRange;
        }

        @Override
        public void stop() {
            this.followingEntity = null;
            this.haku.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (this.followingEntity != null) {
                this.haku.getNavigation().moveTo(this.followingEntity, getSpeed());
            }
        }

        private double getSpeed() {
            if (this.followingEntity instanceof NinjaMobEntity ninja && ninja.getTarget() != null) {
                return this.speed * 2.0D;
            }
            return this.speed;
        }
    }
}
