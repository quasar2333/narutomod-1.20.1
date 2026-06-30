package net.narutomod.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.narutomod.registry.ModEntityTypes;

public final class ToadSummonEntity extends AbstractSummonAnimalEntity {
    public ToadSummonEntity(EntityType<? extends ToadSummonEntity> entityType, Level level) {
        super(entityType, level);
    }

    public static boolean spawnFrom(LivingEntity owner, float scale) {
        if (!(owner.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        ToadSummonEntity entity = ModEntityTypes.TOAD_SUMMON.get().create(serverLevel);
        if (entity == null) {
            return false;
        }
        entity.configure(owner, scale);
        return scheduleDelayedSpawn(serverLevel, entity);
    }

    @Override
    public double baseRenderWidth() {
        return 0.8D;
    }

    @Override
    public double baseRenderHeight() {
        return 1.125D;
    }

    @Override
    public double baseRenderDepth() {
        return 1.15D;
    }

    @Override
    protected void applyScaledAttributes(float scale) {
        setAttributeBaseValue(Attributes.ARMOR, 5.0D * scale);
        setAttributeBaseValue(Attributes.ATTACK_DAMAGE, 3.0D * scale);
        setAttributeBaseValue(Attributes.FOLLOW_RANGE, 13.0D + 3.0D * scale);
        setAttributeBaseValue(Attributes.MAX_HEALTH, 10.0D * scale * scale);
        setAttributeBaseValue(Attributes.MOVEMENT_SPEED, 0.3D);
    }

    @Override
    public void travel(Vec3 travelVector) {
        LivingEntity rider = getControllingPassenger();
        if (isVehicle() && rider != null && canMountedRiderControl(rider)) {
            setTarget(null);
            getNavigation().stop();
            setYRot(rider.getYRot());
            yRotO = getYRot();
            setXRot(rider.getXRot() * 0.35F);
            setRot(getYRot(), getXRot());
            yBodyRot = getYRot();
            yHeadRot = rider.getYHeadRot();
            setMaxUpStep(getBbHeight() / 3.0F);

            float strafe = rider.xxa * 0.65F;
            float forward = rider.zza;
            if (forward < 0.0F) {
                forward *= 0.35F;
            }
            Vec3 input = new Vec3(strafe, 0.0D, forward);
            if (input.lengthSqr() > 1.0E-4D) {
                Vec3 direction = input.normalize().yRot(-rider.getYRot() * Mth.DEG_TO_RAD);
                double horizontal = Mth.clamp(0.22D + getSummonScale() * 0.018D, 0.26D, 0.55D);
                if (onGround()) {
                    double vertical = Mth.clamp(0.36D + getSummonScale() * 0.018D, 0.38D, 0.68D);
                    setDeltaMovement(direction.x() * horizontal, vertical, direction.z() * horizontal);
                    hasImpulse = true;
                } else {
                    Vec3 motion = getDeltaMovement();
                    double air = horizontal * 0.075D;
                    setDeltaMovement(motion.x() * 0.94D + direction.x() * air, motion.y(), motion.z() * 0.94D + direction.z() * air);
                }
            } else if (onGround()) {
                Vec3 motion = getDeltaMovement();
                setDeltaMovement(motion.x() * 0.55D, motion.y(), motion.z() * 0.55D);
            }
            super.travel(Vec3.ZERO);
            return;
        }
        super.travel(travelVector);
    }
}
