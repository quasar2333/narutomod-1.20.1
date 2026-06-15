package net.narutomod.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import net.narutomod.registry.ModBlocks;

public final class KamuiChunkGenerator extends ChunkGenerator {
    public static final Codec<KamuiChunkGenerator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(RegistryOps.retrieveElement(Biomes.THE_VOID))
                    .apply(instance, instance.stable(KamuiChunkGenerator::new)));

    private static final int LEGACY_DIMENSION_ID = 3;
    private static final int SEA_LEVEL = 63;
    private static final int GEN_DEPTH = 256;
    private static final int ISLAND_MIN_Y = 55;
    private static final int ISLAND_Y_SPREAD = 16;
    private static final int ISLAND_MIN_RADIUS = 10;
    private static final int ISLAND_RADIUS_SPREAD = 5;
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    public KamuiChunkGenerator(Holder.Reference<Biome> biome) {
        super(new FixedBiomeSource(biome));
    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunk) {
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
        ChunkPos chunkPos = chunk.getPos();
        for (int sourceX = chunkPos.x - 2; sourceX <= chunkPos.x + 1; sourceX++) {
            for (int sourceZ = chunkPos.z - 2; sourceZ <= chunkPos.z + 1; sourceZ++) {
                for (Island island : plannedIslands(level.getSeed(), sourceX, sourceZ)) {
                    placeIslandSlice(level, chunkPos, island);
                }
            }
        }
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager, RandomState randomState, ChunkAccess chunk) {
    }

    @Override
    public void applyCarvers(
            WorldGenRegion level,
            long seed,
            RandomState randomState,
            BiomeManager biomeManager,
            StructureManager structureManager,
            ChunkAccess chunk,
            GenerationStep.Carving carving) {
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion level) {
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState randomState) {
        return level.getMinBuildHeight();
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState randomState) {
        BlockState[] states = new BlockState[Math.max(0, level.getHeight())];
        for (int i = 0; i < states.length; i++) {
            states[i] = AIR;
        }
        return new NoiseColumn(level.getMinBuildHeight(), states);
    }

    @Override
    public void addDebugScreenInfo(List<String> lines, RandomState randomState, BlockPos pos) {
        lines.add("Kamui terrain: legacy populate islands");
    }

    @Override
    public int getSpawnHeight(LevelHeightAccessor level) {
        return Math.min(level.getMaxBuildHeight(), KamuiDimension.PLATFORM_Y + 1);
    }

    @Override
    public int getMinY() {
        return 0;
    }

    @Override
    public int getGenDepth() {
        return GEN_DEPTH;
    }

    @Override
    public int getSeaLevel() {
        return SEA_LEVEL;
    }

    public static List<Island> plannedIslands(long worldSeed, int chunkX, int chunkZ) {
        SimplexNoise islandNoise = new SimplexNoise(RandomSource.create(worldSeed - LEGACY_DIMENSION_ID));
        float islandHeight = getIslandHeightValue(islandNoise, chunkX, chunkZ, 1, 1);
        Random random = new Random((long) chunkX * 535358712L + (long) chunkZ * 347539041L);
        if (islandHeight >= -10.0F || random.nextInt(4) != 0) {
            return List.of();
        }

        List<Island> islands = new ArrayList<>(2);
        islands.add(nextIsland(chunkX, chunkZ, random));
        if (random.nextInt(4) == 0) {
            islands.add(nextIsland(chunkX, chunkZ, random));
        }
        return islands;
    }

    public static int countPlannedIslandChunks(long worldSeed, ChunkPos center, int radius) {
        int count = 0;
        for (int x = center.x - radius; x <= center.x + radius; x++) {
            for (int z = center.z - radius; z <= center.z + radius; z++) {
                if (!plannedIslands(worldSeed, x, z).isEmpty()) {
                    count++;
                }
            }
        }
        return count;
    }

    private static Island nextIsland(int chunkX, int chunkZ, Random random) {
        int centerX = chunkX * 16 + random.nextInt(16) + 8;
        int centerY = ISLAND_MIN_Y + random.nextInt(ISLAND_Y_SPREAD);
        int centerZ = chunkZ * 16 + random.nextInt(16) + 8;
        int radius = ISLAND_MIN_RADIUS + random.nextInt(ISLAND_RADIUS_SPREAD);
        return new Island(centerX, centerY, centerZ, radius);
    }

    private static float getIslandHeightValue(SimplexNoise islandNoise, int chunkX, int chunkZ, int localX, int localZ) {
        float x = chunkX * 2.0F + localX;
        float z = chunkZ * 2.0F + localZ;
        float height = 100.0F - Mth.sqrt(x * x + z * z) * 8.0F;
        height = Mth.clamp(height, -100.0F, 80.0F);
        for (int offsetX = -12; offsetX <= 12; offsetX++) {
            for (int offsetZ = -12; offsetZ <= 12; offsetZ++) {
                long islandChunkX = chunkX + offsetX;
                long islandChunkZ = chunkZ + offsetZ;
                if (islandChunkX * islandChunkX + islandChunkZ * islandChunkZ <= 4096L
                        || islandNoise.getValue(islandChunkX, islandChunkZ) >= -0.8999999761581421D) {
                    continue;
                }
                float radiusFactor = (Mth.abs((float) islandChunkX) * 3439.0F + Mth.abs((float) islandChunkZ) * 147.0F) % 13.0F + 9.0F;
                x = localX - offsetX * 2.0F;
                z = localZ - offsetZ * 2.0F;
                float nearbyHeight = 100.0F - Mth.sqrt(x * x + z * z) * radiusFactor;
                nearbyHeight = Mth.clamp(nearbyHeight, -100.0F, 80.0F);
                if (nearbyHeight > height) {
                    height = nearbyHeight;
                }
            }
        }
        return height;
    }

    private static void placeIslandSlice(WorldGenLevel level, ChunkPos targetChunk, Island island) {
        BlockState state = ModBlocks.KAMUIBLOCK.get().defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minX = Math.max(targetChunk.getMinBlockX(), island.centerX() - island.radius());
        int maxX = Math.min(targetChunk.getMaxBlockX(), island.centerX() + island.radius());
        int minZ = Math.max(targetChunk.getMinBlockZ(), island.centerZ() - island.radius());
        int maxZ = Math.min(targetChunk.getMaxBlockZ(), island.centerZ() + island.radius());
        if (minX > maxX || minZ > maxZ) {
            return;
        }
        int minY = Math.max(level.getMinBuildHeight() + 1, 1);
        int maxY = Math.min(island.centerY(), level.getMaxBuildHeight() - 1);
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = maxY; y >= minY; y--) {
                    level.setBlock(pos.set(x, y, z), state, Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    public record Island(int centerX, int centerY, int centerZ, int radius) {
    }
}
