package net.narutomod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.narutomod.registry.ModBlocks;

public final class WaterStillBlock extends Block {
    public WaterStillBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        Vec3 motion = entity.getDeltaMovement();
        entity.setDeltaMovement(motion.x(), motion.y() - 1.5D, motion.z());
        entity.hasImpulse = true;
    }

    @Override
    public boolean skipRendering(BlockState state, BlockState adjacentState, net.minecraft.core.Direction direction) {
        return adjacentState.is(this) || super.skipRendering(state, adjacentState, direction);
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    public static boolean isInsideBlock(Entity entity, boolean testHead) {
        if (entity.getVehicle() instanceof Boat) {
            return false;
        }
        double y = entity.getY() + (testHead ? entity.getEyeHeight() : 0.0D);
        BlockPos pos = BlockPos.containing(entity.getX(), y, entity.getZ());
        return entity.level().getBlockState(pos).is(ModBlocks.WATER_STILL.get());
    }
}
