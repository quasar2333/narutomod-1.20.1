package net.narutomod.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModSounds;

public final class SnakeSummonEntity extends AbstractSummonAnimalEntity {
    public SnakeSummonEntity(EntityType<? extends SnakeSummonEntity> entityType, Level level) {
        super(entityType, level);
    }

    public static boolean spawnFrom(LivingEntity owner, float scale) {
        if (!(owner.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        SnakeSummonEntity entity = ModEntityTypes.SNAKE_SUMMON.get().create(serverLevel);
        if (entity == null) {
            return false;
        }
        entity.configure(owner, scale);
        return scheduleDelayedSpawn(serverLevel, entity);
    }

    @Override
    public double baseRenderWidth() {
        return 0.3D;
    }

    @Override
    public double baseRenderHeight() {
        return 0.25D;
    }

    @Override
    public double baseRenderDepth() {
        return 3.0D;
    }

    @Override
    protected void applyScaledAttributes(float scale) {
        setAttributeBaseValue(Attributes.ARMOR, 5.0D * scale);
        setAttributeBaseValue(Attributes.ATTACK_DAMAGE, 2.6667D * scale);
        setAttributeBaseValue(Attributes.FOLLOW_RANGE, 13.0D + 3.0D * scale);
        setAttributeBaseValue(Attributes.MAX_HEALTH, 8.0D * scale * scale);
        setAttributeBaseValue(Attributes.MOVEMENT_SPEED, 0.25D + scale * 0.05D);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.SOUND_SNAKE_HISS.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return net.minecraft.sounds.SoundEvents.GENERIC_HURT;
    }

    @Override
    public void travel(Vec3 travelVector) {
        LivingEntity rider = getControllingPassenger();
        if (isVehicle() && rider != null && canMountedRiderControl(rider)) {
            setTarget(null);
            getNavigation().stop();
            float yaw = rider.getYRot();
            setYRot(yaw);
            yRotO = yaw;
            setXRot(rider.getXRot() * 0.1F);
            setRot(getYRot(), getXRot());
            yBodyRot = yaw;
            yHeadRot = yaw;
            yHeadRotO = yaw;
            setMaxUpStep(Math.max(1.0F, getBbHeight() * 0.45F));

            float strafe = rider.xxa * 0.52F;
            float forward = rider.zza;
            if (forward < 0.0F) {
                forward *= 0.3F;
            }
            Vec3 input = new Vec3(strafe, 0.0D, forward);
            if (input.lengthSqr() > 1.0E-4D) {
                Vec3 direction = input.normalize().yRot(-yaw * Mth.DEG_TO_RAD);
                double mountedSpeed = Mth.clamp(0.105D + getSummonScale() * 0.009D, 0.14D, 0.3D);
                Vec3 motion = getDeltaMovement();
                double blend = onGround() ? 0.72D : 0.28D;
                setDeltaMovement(
                        motion.x() * (1.0D - blend) + direction.x() * mountedSpeed * blend,
                        motion.y(),
                        motion.z() * (1.0D - blend) + direction.z() * mountedSpeed * blend);
                hasImpulse = true;
            } else if (onGround()) {
                Vec3 motion = getDeltaMovement();
                setDeltaMovement(motion.x() * 0.62D, motion.y(), motion.z() * 0.62D);
            }
            super.travel(Vec3.ZERO);
            return;
        }
        super.travel(travelVector);
    }

    @Override
    public double getPassengersRidingOffset() {
        return 0.0D;
    }

    public boolean shouldRiderSit() {
        return false;
    }
}
