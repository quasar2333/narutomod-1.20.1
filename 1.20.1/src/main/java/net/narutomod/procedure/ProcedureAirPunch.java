package net.narutomod.procedure;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ProcedureAirPunch {
    private static final String PRESS_DURATION_TAG = "air_punch_press_duration";
    private static final double STEP = 1.0D;

    public static int getPressDuration(Entity entity) {
        return entity.getPersistentData().getInt(PRESS_DURATION_TAG);
    }

    public static void resetPressDuration(Entity entity) {
        entity.getPersistentData().putInt(PRESS_DURATION_TAG, 0);
    }

    public void execute(boolean pressed, LivingEntity user) {
        if (pressed) {
            int duration = Math.min(getPressDuration(user) + 1, 200);
            user.getPersistentData().putInt(PRESS_DURATION_TAG, duration);
            if (user.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.SMOKE, user.getX(), user.getY() + 1.0D, user.getZ(),
                        duration, 0.25D, 0.0D, 0.25D, 0.15D);
            }
            if (user instanceof Player player) {
                player.displayClientMessage(Component.literal("Power range: " + (int)getRange(duration)), true);
            }
            return;
        }

        int duration = getPressDuration(user);
        if (duration > 0) {
            execute(user, getRange(duration), getFarRadius(duration), getNearRadius(duration), user.getRandom());
        }
        resetPressDuration(user);
    }

    protected void execute(LivingEntity user, double range, double farRadius, double nearRadius, RandomSource random) {
        if (!(user.level() instanceof ServerLevel level) || range <= 0.0D) {
            return;
        }
        Vec3 start = user.getEyePosition();
        Vec3 look = user.getLookAngle().normalize();
        List<AffectedTrace> affected = collectAffected(user, range, farRadius, nearRadius);
        for (double distance = STEP; distance <= range; distance += STEP) {
            Vec3 pos = start.add(look.scale(distance));
            level.sendParticles(ParticleTypes.EXPLOSION, pos.x(), pos.y(), pos.z(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        for (AffectedTrace trace : affected) {
            if (trace.entity() != null) {
                attackEntityFrom(user, trace.entity(), trace.distanceToCenter());
            } else if (trace.blockPos() != null) {
                processAffectedBlock(user, trace.blockPos(), range, trace.distanceToCenter(), random);
            }
        }
    }

    protected double getRange(int duration) {
        return duration;
    }

    protected double getFarRadius(int duration) {
        return Math.max(1.0D, duration / 8.0D);
    }

    protected double getNearRadius(int duration) {
        return 0.5D;
    }

    protected void attackEntityFrom(LivingEntity user, Entity target, double distanceToCenter) {
        ProcedureUtils.pushEntity(user, target, Math.max(getRange(getPressDuration(user)), 1.0D), 2.0F);
    }

    protected boolean processAffectedBlock(LivingEntity user, BlockPos pos, double range, double distanceToCenter, RandomSource random) {
        float chance = getBreakChance(user, pos, range, distanceToCenter);
        return ProcedureUtils.breakBlockAndDropWithChance(user.level(), pos, 10.0F, chance, 0.0F);
    }

    protected float getBreakChance(LivingEntity user, BlockPos pos, double range, double distanceToCenter) {
        return 0.0F;
    }

    private List<AffectedTrace> collectAffected(LivingEntity user, double range, double farRadius, double nearRadius) {
        Level level = user.level();
        Vec3 start = user.getEyePosition();
        Vec3 look = user.getLookAngle().normalize();
        List<AffectedTrace> affected = new ArrayList<>();
        Set<BlockPos> seenBlocks = new HashSet<>();
        Set<Entity> seenEntities = new HashSet<>();
        for (double distance = STEP; distance <= range; distance += STEP) {
            double radius = nearRadius + (farRadius - nearRadius) * distance / range;
            Vec3 center = start.add(look.scale(distance));
            AABB box = new AABB(center, center).inflate(radius);
            collectEntities(user, affected, seenEntities, center, box, radius, distance);
            collectBlocks(level, affected, seenBlocks, center, box, radius, distance);
        }
        affected.sort(Comparator.comparingDouble(AffectedTrace::distanceAlongLook));
        return affected;
    }

    private static void collectEntities(LivingEntity user, List<AffectedTrace> affected, Set<Entity> seenEntities,
            Vec3 center, AABB box, double radius, double distanceAlongLook) {
        for (Entity target : user.level().getEntities(user, box, entity ->
                entity.isAlive()
                        && !entity.isSpectator()
                        && entity.getRootVehicle() != user.getRootVehicle()
                        && entity.distanceToSqr(center) <= radius * radius)) {
            if (seenEntities.add(target)) {
                affected.add(AffectedTrace.entity(target, target.getBoundingBox().getCenter().distanceTo(center), distanceAlongLook));
            }
        }
    }

    private static void collectBlocks(Level level, List<AffectedTrace> affected, Set<BlockPos> seenBlocks,
            Vec3 center, AABB box, double radius, double distanceAlongLook) {
        for (BlockPos pos : BlockPos.betweenClosed(
                (int)Math.floor(box.minX), (int)Math.floor(box.minY), (int)Math.floor(box.minZ),
                (int)Math.floor(box.maxX), (int)Math.floor(box.maxY), (int)Math.floor(box.maxZ))) {
            BlockPos immutable = pos.immutable();
            if (!seenBlocks.add(immutable) || level.getBlockState(immutable).isAir()) {
                continue;
            }
            double distance = Vec3.atCenterOf(immutable).distanceTo(center);
            if (distance <= radius) {
                affected.add(AffectedTrace.block(immutable, distance, distanceAlongLook));
            }
        }
    }

    private record AffectedTrace(Entity entity, BlockPos blockPos, double distanceToCenter, double distanceAlongLook) {
        private static AffectedTrace entity(Entity entity, double distanceToCenter, double distanceAlongLook) {
            return new AffectedTrace(entity, null, distanceToCenter, distanceAlongLook);
        }

        private static AffectedTrace block(BlockPos pos, double distanceToCenter, double distanceAlongLook) {
            return new AffectedTrace(null, pos, distanceToCenter, distanceAlongLook);
        }
    }
}
