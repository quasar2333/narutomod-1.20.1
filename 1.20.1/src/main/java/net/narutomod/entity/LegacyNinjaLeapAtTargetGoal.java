package net.narutomod.entity;

import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

final class LegacyNinjaLeapAtTargetGoal extends Goal {
    private final Mob leaper;
    private final float leapStrength;
    @Nullable
    private LivingEntity target;

    LegacyNinjaLeapAtTargetGoal(Mob leaper, float leapStrength) {
        this.leaper = leaper;
        this.leapStrength = leapStrength;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        this.target = this.leaper.getTarget();
        if (this.target == null || !this.leaper.onGround()) {
            return false;
        }
        double distance = this.leaper.distanceTo(this.target);
        return distance >= 3.0D
                && distance <= this.leapStrength * 12.0D
                && this.leaper.getRandom().nextInt(5) == 0;
    }

    @Override
    public boolean canContinueToUse() {
        return !this.leaper.onGround();
    }

    @Override
    public void start() {
        if (this.target == null) {
            return;
        }
        double dx = this.target.getX() - this.leaper.getX();
        double dz = this.target.getZ() - this.leaper.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        double dy = this.target.getY() - this.leaper.getY() + horizontal * 0.2D;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length > 1.0E-7D) {
            this.leaper.setDeltaMovement(
                    dx / length * this.leapStrength,
                    dy / length * this.leapStrength,
                    dz / length * this.leapStrength);
            this.leaper.hasImpulse = true;
        }
    }
}
