package net.narutomod.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.narutomod.registry.ModEntityTypes;

public final class SlugSummonEntity extends AbstractSummonAnimalEntity {
    public SlugSummonEntity(EntityType<? extends SlugSummonEntity> entityType, Level level) {
        super(entityType, level);
        this.fireImmune();
    }

    public static boolean spawnFrom(LivingEntity owner, float scale) {
        if (!(owner.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        SlugSummonEntity entity = ModEntityTypes.SLUG.get().create(serverLevel);
        if (entity == null) {
            return false;
        }
        entity.configure(owner, scale);
        return scheduleDelayedSpawn(serverLevel, entity);
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public double baseRenderWidth() {
        return 0.75D;
    }

    @Override
    public double baseRenderHeight() {
        return 0.75D;
    }

    @Override
    public double baseRenderDepth() {
        return 1.4D;
    }

    @Override
    protected void applyScaledAttributes(float scale) {
        setAttributeBaseValue(Attributes.ARMOR, 5.0D * scale);
        setAttributeBaseValue(Attributes.ATTACK_DAMAGE, 3.0D * scale);
        setAttributeBaseValue(Attributes.FOLLOW_RANGE, 13.0D + 3.0D * scale);
        setAttributeBaseValue(Attributes.MAX_HEALTH, 10.0D * scale * scale);
        setAttributeBaseValue(Attributes.MOVEMENT_SPEED, 0.25D + scale * 0.05D);
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
            setMaxUpStep(Math.max(1.0F, getBbHeight() * 0.35F));

            float strafe = rider.xxa * 0.45F;
            float forward = rider.zza;
            if (forward < 0.0F) {
                forward *= 0.3F;
            }
            Vec3 input = new Vec3(strafe, 0.0D, forward);
            if (input.lengthSqr() > 1.0E-4D) {
                Vec3 direction = input.normalize().yRot(-yaw * Mth.DEG_TO_RAD);
                double speed = Mth.clamp(0.065D + getSummonScale() * 0.006D, 0.08D, 0.2D);
                Vec3 motion = getDeltaMovement();
                double blend = onGround() ? 0.48D : 0.2D;
                setDeltaMovement(
                        motion.x() * (1.0D - blend) + direction.x() * speed * blend,
                        motion.y(),
                        motion.z() * (1.0D - blend) + direction.z() * speed * blend);
                hasImpulse = true;
            } else if (onGround()) {
                Vec3 motion = getDeltaMovement();
                setDeltaMovement(motion.x() * 0.7D, motion.y(), motion.z() * 0.7D);
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

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        if (!hasPassenger(passenger)) {
            return;
        }
        float scale = getSummonScale();
        float headYaw = Mth.wrapDegrees(yHeadRot - yBodyRot) * 0.5F;
        Vec3 seat = new Vec3(0.0D, 0.0D, 0.2113D * scale)
                .yRot(-headYaw * Mth.DEG_TO_RAD)
                .add(0.0D, 0.0D, 0.3753D * scale)
                .yRot(-headYaw * Mth.DEG_TO_RAD)
                .yRot(-yBodyRot * Mth.DEG_TO_RAD)
                .add(position());
        moveFunction.accept(passenger, seat.x(), seat.y() + 0.875D * scale + passenger.getMyRidingOffset(), seat.z());
    }
}
