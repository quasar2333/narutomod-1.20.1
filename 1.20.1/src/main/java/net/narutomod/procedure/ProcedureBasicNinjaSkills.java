package net.narutomod.procedure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.narutomod.registry.ModBlocks;

public final class ProcedureBasicNinjaSkills {
    private ProcedureBasicNinjaSkills() {
    }

    public static void apply(Entity entity) {
        if (!entity.level().isClientSide && entity instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 2, 1, false, false));
            living.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 2, 1, false, false));
            living.addEffect(new MobEffectInstance(MobEffects.JUMP, 2, 1, false, false));
        }
        applyWaterStanding(entity);
        applyWallClimb(entity);
        if (entity.fallDistance > 4.0F) {
            entity.fallDistance -= 0.75F;
        }
    }

    private static void applyWaterStanding(Entity entity) {
        if (entity.isInWaterOrBubble() && !entity.isShiftKeyDown()) {
            Vec3 motion = entity.getDeltaMovement();
            entity.setDeltaMovement(motion.x(), 0.01D, motion.z());
            entity.setOnGround(true);
            entity.hasImpulse = true;
        }
    }

    private static void applyWallClimb(Entity entity) {
        BlockHitResult hit = ProcedureUtils.raytraceBlocks(entity, 1.0D);
        if (entity.onGround()
                || entity.getXRot() >= 0.0F
                || hit.getType() != HitResult.Type.BLOCK
                || !isFullCollisionBlock(entity, hit.getBlockPos())) {
            return;
        }
        Vec3 motion = entity.getDeltaMovement();
        if (entity.isInWaterOrBubble() || entity.isInLava()) {
            entity.setDeltaMovement(motion.x(), 0.3D, motion.z());
        } else if (!entity.level().getBlockState(entity.blockPosition()).is(ModBlocks.MUD.get())) {
            float friction = entity.level().getBlockState(hit.getBlockPos()).getBlock().getFriction();
            entity.setDeltaMovement(motion.x(), 0.6D - (friction - 0.6D) * 2.0D, motion.z());
        }
        entity.hasImpulse = true;
    }

    private static boolean isFullCollisionBlock(Entity entity, BlockPos pos) {
        BlockState state = entity.level().getBlockState(pos);
        return state.isCollisionShapeFullBlock(entity.level(), pos);
    }
}
