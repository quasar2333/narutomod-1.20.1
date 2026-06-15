package net.narutomod.entity;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModSounds;

public final class GedoStatueEntity extends AbstractSummonAnimalEntity {
    public static final float MODEL_SCALE = 10.0F;
    public static final float BASE_WIDTH = 0.5F;
    public static final float BASE_HEIGHT = 1.9F;
    private static final int DRAGON_TARGET_COUNT = 5;
    private static final int DRAGON_SCAN_INTERVAL = 100;
    private static final double TARGET_RANGE_XZ = 20.0D;
    private static final double TARGET_RANGE_Y = 7.0D;
    private static final int ABSORB_SCAN_INTERVAL = 20;
    private static final double ABSORB_RANGE_XZ = 24.0D;
    private static final double ABSORB_RANGE_Y = 16.0D;

    @Nullable
    private UUID dragonUuid;

    public GedoStatueEntity(EntityType<? extends GedoStatueEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 100;
        setPersistenceRequired();
        setMaxUpStep(BASE_HEIGHT * MODEL_SCALE / 3.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AbstractSummonAnimalEntity.createSummonAttributes()
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Nullable
    public static GedoStatueEntity spawnFrom(LivingEntity owner) {
        if (!(owner.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        return spawnAt(serverLevel, owner.position(), owner.getYRot(), owner);
    }

    @Nullable
    public static GedoStatueEntity spawnAt(ServerLevel level, Vec3 pos, float yaw, LivingEntity owner) {
        GedoStatueEntity entity = ModEntityTypes.GEDO_STATUE.get().create(level);
        if (entity == null) {
            return null;
        }
        entity.configure(owner, MODEL_SCALE);
        entity.moveTo(pos.x(), pos.y(), pos.z(), yaw, 0.0F);
        entity.setYBodyRot(yaw);
        entity.setYHeadRot(yaw);
        if (!level.addFreshEntity(entity)) {
            return null;
        }
        return entity;
    }

    @Nullable
    public static GedoStatueEntity findLoaded(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof GedoStatueEntity gedo && gedo.isAlive()) {
                    return gedo;
                }
            }
        }
        return null;
    }

    @Override
    public double baseRenderWidth() {
        return BASE_WIDTH;
    }

    @Override
    public double baseRenderHeight() {
        return BASE_HEIGHT;
    }

    @Override
    public double baseRenderDepth() {
        return BASE_WIDTH;
    }

    @Override
    protected void applyScaledAttributes(float scale) {
        setAttributeBaseValue(Attributes.ARMOR, 100.0D);
        setAttributeBaseValue(Attributes.ATTACK_DAMAGE, 100.0D);
        setAttributeBaseValue(Attributes.FOLLOW_RANGE, 24.0D);
        setAttributeBaseValue(Attributes.MAX_HEALTH, 10000.0D);
        setAttributeBaseValue(Attributes.MOVEMENT_SPEED, 0.8D);
        setAttributeBaseValue(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
        setHealth(getMaxHealth());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2D, true) {
            @Override
            protected double getAttackReachSqr(LivingEntity target) {
                double reach = GedoStatueEntity.this.getBbWidth() * 2.0D + target.getBbWidth();
                return reach * reach;
            }
        });
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide || !isVisibleSummon()) {
            return;
        }
        tickDragonLockout();
        if (this.tickCount % ABSORB_SCAN_INTERVAL == 0 && tryAbsorbNearbyTailedBeastNow()) {
            return;
        }
        if (!isNoAi() && this.tickCount % DRAGON_SCAN_INTERVAL == 0) {
            trySpawnDragon();
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        target.invulnerableTime = 0;
        return target.hurt(this.damageSources().mobAttack(this), (float)getAttributeValue(Attributes.ATTACK_DAMAGE));
    }

    @Override
    public SoundEvent getAmbientSound() {
        return ModSounds.SOUND_MONSTERGROWL.get();
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return null;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }

    @Override
    protected float getSoundVolume() {
        return 10.0F;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return false;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Dragon")) {
            this.dragonUuid = tag.getUUID("Dragon");
        } else {
            this.dragonUuid = null;
        }
        setMaxUpStep(BASE_HEIGHT * MODEL_SCALE / 3.0F);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.dragonUuid != null) {
            tag.putUUID("Dragon", this.dragonUuid);
        }
    }

    private void tickDragonLockout() {
        if (this.dragonUuid == null) {
            return;
        }
        Entity dragon = this.level() instanceof ServerLevel serverLevel ? serverLevel.getEntity(this.dragonUuid) : null;
        if (dragon != null && dragon.isAlive()) {
            setNoAi(true);
            return;
        }
        this.dragonUuid = null;
        setNoAi(false);
    }

    public boolean trySpawnDragonNow() {
        if (this.level().isClientSide) {
            return false;
        }
        tickDragonLockout();
        if (this.dragonUuid != null) {
            return false;
        }
        return trySpawnDragon();
    }

    public boolean tryAbsorbNearbyTailedBeastNow() {
        if (this.level().isClientSide) {
            return false;
        }
        return tryCompleteTenTailsActivation() || tryAbsorbNearbyTailedBeast();
    }

    private boolean tryAbsorbNearbyTailedBeast() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        List<TailedBeastEntity> targets = serverLevel.getEntitiesOfClass(
                TailedBeastEntity.class,
                getBoundingBox().inflate(ABSORB_RANGE_XZ, ABSORB_RANGE_Y, ABSORB_RANGE_XZ),
                BijuManager::canSealTailedBeastIntoGedo);
        targets.sort(Comparator.comparingDouble(this::distanceToSqr));
        if (targets.isEmpty()) {
            return false;
        }
        TailedBeastEntity target = targets.get(0);
        Vec3 effectPos = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        if (!BijuManager.sealTailedBeastIntoGedo(target)) {
            return false;
        }
        playAbsorbEffects(serverLevel, effectPos);
        tryCompleteTenTailsActivation();
        return true;
    }

    private boolean tryCompleteTenTailsActivation() {
        if (!(this.level() instanceof ServerLevel serverLevel)
                || !BijuManager.canActivateTenTailsFromGedo(serverLevel.getServer())) {
            return false;
        }
        TenTailsEntity tenTails = TenTailsEntity.spawnAt(serverLevel, position(), getYRot(), getOwner());
        if (tenTails == null) {
            return false;
        }
        playTenTailsActivationEffects(serverLevel, tenTails.position(), tenTails.getBbHeight());
        discard();
        return true;
    }

    private void playAbsorbEffects(ServerLevel level, Vec3 pos) {
        level.playSound(null, pos.x(), pos.y(), pos.z(), ModSounds.SOUND_KAMUISFX.get(), SoundSource.HOSTILE, 3.0F, 0.8F);
        level.sendParticles(ParticleTypes.SMOKE,
                pos.x(), pos.y(), pos.z(),
                120, 2.0D, 2.0D, 2.0D, 0.05D);
    }

    private void playTenTailsActivationEffects(ServerLevel level, Vec3 pos, double height) {
        level.playSound(null, pos.x(), pos.y(), pos.z(), ModSounds.SOUND_MONSTERGROWL.get(), SoundSource.HOSTILE, 6.0F, 0.6F);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                pos.x(), pos.y() + height * 0.5D, pos.z(),
                6, 8.0D, Math.max(height, 1.0D), 8.0D, 1.0D);
    }

    private boolean trySpawnDragon() {
        List<LivingEntity> targets = findDragonTargets();
        if (targets.size() < DRAGON_TARGET_COUNT) {
            return false;
        }
        PurpleDragonEntity dragon = PurpleDragonEntity.spawnFrom(this, targets.subList(0, DRAGON_TARGET_COUNT));
        if (dragon != null) {
            this.dragonUuid = dragon.getUUID();
            setNoAi(true);
            return true;
        }
        return false;
    }

    private List<LivingEntity> findDragonTargets() {
        LivingEntity owner = getOwner();
        AABB box = getBoundingBox().inflate(TARGET_RANGE_XZ, TARGET_RANGE_Y, TARGET_RANGE_XZ);
        return this.level().getEntitiesOfClass(LivingEntity.class, box, living -> {
                    if (!living.isAlive() || living == this || living == owner) {
                        return false;
                    }
                    if (owner != null && (living.isAlliedTo(owner) || owner.isAlliedTo(living))) {
                        return false;
                    }
                    return living.getAttribute(Attributes.ATTACK_DAMAGE) != null;
                })
                .stream()
                .sorted(Comparator.comparingDouble(this::distanceToSqr))
                .toList();
    }
}
