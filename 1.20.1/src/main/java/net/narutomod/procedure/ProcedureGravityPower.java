package net.narutomod.procedure;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.ForgeEventFactory;
import net.narutomod.entity.EarthBlocksEntity;
import net.narutomod.registry.ModEntityTypes;

public final class ProcedureGravityPower {
    private static final int MAX_BLOCKS = 8192;

    private ProcedureGravityPower() {
    }

    @Nullable
    public static EarthBlocksEntity dislodgeBlocks(Level level, BlockPos center, int size, @Nullable Entity owner) {
        if (!(level instanceof ServerLevel serverLevel) || size <= 0
                || !ForgeEventFactory.getMobGriefingEvent(serverLevel, owner)) {
            return null;
        }

        List<BlockPos> positions = collectBlocks(level, center, size);
        if (positions.isEmpty()) {
            return null;
        }
        EarthBlocksEntity entity = ModEntityTypes.EARTH_BLOCKS.get().create(serverLevel);
        if (entity == null) {
            return null;
        }

        entity.configureFromBlocks(boundingOrigin(positions), positions);
        entity.setMovementEnabled(true);
        entity.setDeltaMovement(0.0D, 0.1D, 0.0D);
        if (!serverLevel.addFreshEntity(entity)) {
            return null;
        }
        for (BlockPos pos : positions) {
            level.removeBlock(pos, false);
        }
        return entity;
    }

    private static List<BlockPos> collectBlocks(Level level, BlockPos center, int size) {
        List<BlockPos> positions = new ArrayList<>();
        int min = -size / 2;
        int max = size / 2 + (size % 2);
        for (int y = min; y < max; y++) {
            for (int z = min; z < max; z++) {
                for (int x = min; x < max; x++) {
                    BlockPos pos = center.offset(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    float hardness = state.getDestroySpeed(level, pos);
                    if (!state.isAir() && state.getFluidState().isEmpty() && hardness >= 0.0F) {
                        positions.add(pos.immutable());
                        if (positions.size() >= MAX_BLOCKS) {
                            return positions;
                        }
                    }
                }
            }
        }
        return positions;
    }

    private static BlockPos boundingOrigin(List<BlockPos> positions) {
        int minX = positions.get(0).getX();
        int minY = positions.get(0).getY();
        int minZ = positions.get(0).getZ();
        for (BlockPos pos : positions) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
        }
        return new BlockPos(minX, minY, minZ);
    }
}
