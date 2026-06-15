package net.narutomod.world;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

public final class VillagePoiHelper {
    public static final int QUEST_SEARCH_RADIUS = 48;
    public static final int NATURAL_SEARCH_RADIUS = 48;
    public static final int LEGACY_MERCHANT_SEARCH_RADIUS = 32;
    public static final int LEGACY_MERCHANT_HOME_SEARCH_RADIUS = 48;
    public static final int DEFAULT_SIEGE_RADIUS = 32;
    public static final int MAX_SIEGE_RADIUS = 64;
    public static final int MIN_QUEST_VILLAGERS = 1;
    public static final int MIN_QUEST_MEETING_POIS = 1;
    public static final int MIN_NATURAL_VILLAGERS = 10;
    public static final int MIN_NATURAL_VILLAGE_POIS = 20;

    private VillagePoiHelper() {
    }

    public static Optional<Context> findQuestContext(ServerLevel level, BlockPos origin) {
        Context context = contextAround(level, origin, QUEST_SEARCH_RADIUS);
        if (context.meetingPoiCount() < MIN_QUEST_MEETING_POIS && context.villagerCount() < MIN_QUEST_VILLAGERS) {
            return Optional.empty();
        }
        return Optional.of(context);
    }

    public static Optional<Context> findNaturalMightGuyContext(ServerLevel level, BlockPos origin) {
        Context context = contextAround(level, origin, NATURAL_SEARCH_RADIUS);
        if (context.villagePoiCount() < MIN_NATURAL_VILLAGE_POIS || context.villagerCount() < MIN_NATURAL_VILLAGERS) {
            return Optional.empty();
        }
        return Optional.of(context);
    }

    public static Optional<Context> findLegacyMerchantContext(ServerLevel level, BlockPos origin) {
        return findLegacyMerchantContext(level, origin, LEGACY_MERCHANT_SEARCH_RADIUS);
    }

    public static Optional<Context> findLegacyMerchantHomeContext(ServerLevel level, BlockPos origin) {
        return findLegacyMerchantContext(level, origin, LEGACY_MERCHANT_HOME_SEARCH_RADIUS);
    }

    private static Optional<Context> findLegacyMerchantContext(ServerLevel level, BlockPos origin, int searchRadius) {
        Context context = contextAround(level, origin, searchRadius);
        if (context.meetingPoiCount() < 1 && context.villagerCount() < 1) {
            return Optional.empty();
        }
        return Optional.of(context);
    }

    public static Optional<Context> findSiegeContext(ServerLevel level, BlockPos origin, int radius) {
        int searchRadius = Mth.clamp(radius, DEFAULT_SIEGE_RADIUS, MAX_SIEGE_RADIUS);
        Context context = contextAround(level, origin, searchRadius);
        if (context.meetingPoiCount() < MIN_QUEST_MEETING_POIS && context.villagerCount() < MIN_QUEST_VILLAGERS) {
            return Optional.empty();
        }
        return Optional.of(context);
    }

    public static boolean isSavannaOrTaiga(ServerLevel level, BlockPos pos) {
        return level.getBiome(pos).is(BiomeTags.IS_SAVANNA) || level.getBiome(pos).is(BiomeTags.IS_TAIGA);
    }

    public static int countVillagers(ServerLevel level, BlockPos center, int radius) {
        AABB area = new AABB(center).inflate(radius, 16.0D, radius);
        return level.getEntitiesOfClass(Villager.class, area, Villager::isAlive).size();
    }

    public static Optional<BlockPos> findNaturalSpawnPos(ServerLevel level, Context context, RandomSource random) {
        return findSurfaceSpawnPos(level, context.center(), context.radius(), random, 24, true);
    }

    public static Optional<BlockPos> findSiegeSpawnPos(ServerLevel level, Context context, int radius, RandomSource random) {
        int clampedRadius = Mth.clamp(Math.min(radius, context.radius()), 1, MAX_SIEGE_RADIUS);
        for (int i = 0; i < 8; i++) {
            double distance = (random.nextDouble() * 0.6D + 0.5D) * clampedRadius;
            double angle = Math.PI * random.nextGaussian();
            int x = Mth.floor(context.center().getX() + Math.cos(angle) * distance);
            int z = Mth.floor(context.center().getZ() + Math.sin(angle) * distance);
            Optional<BlockPos> spawnPos = surfaceSpawnPos(level, x, z, true);
            if (spawnPos.isPresent() && spawnPos.get().distSqr(context.center()) <= (double) clampedRadius * clampedRadius) {
                return spawnPos;
            }
        }
        return Optional.empty();
    }

    private static Optional<BlockPos> findSurfaceSpawnPos(ServerLevel level, BlockPos center, int radius,
            RandomSource random, int attempts, boolean avoidWater) {
        int clampedRadius = Mth.clamp(radius, 1, MAX_SIEGE_RADIUS);
        for (int i = 0; i < attempts; i++) {
            int x = center.getX() + random.nextInt(clampedRadius * 2 + 1) - clampedRadius;
            int z = center.getZ() + random.nextInt(clampedRadius * 2 + 1) - clampedRadius;
            Optional<BlockPos> spawnPos = surfaceSpawnPos(level, x, z, avoidWater);
            if (spawnPos.isPresent() && spawnPos.get().distSqr(center) <= (double) clampedRadius * clampedRadius) {
                return spawnPos;
            }
        }
        return Optional.empty();
    }

    private static Optional<BlockPos> surfaceSpawnPos(ServerLevel level, int x, int z, boolean avoidWater) {
        BlockPos height = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
        if (height.getY() <= level.getMinBuildHeight() || height.getY() >= level.getMaxBuildHeight() - 1) {
            return Optional.empty();
        }
        BlockPos floor = height.below();
        if (avoidWater && !level.getFluidState(floor).isEmpty()) {
            return Optional.empty();
        }
        if (!level.getBlockState(height).isAir() || !level.getBlockState(height.above()).isAir()) {
            return Optional.empty();
        }
        return Optional.of(height.immutable());
    }

    private static Context contextAround(ServerLevel level, BlockPos origin, int searchRadius) {
        PoiManager poiManager = level.getPoiManager();
        Optional<BlockPos> meetingCenter = poiManager.findClosest(
                poiType -> poiType.is(PoiTypes.MEETING),
                origin,
                searchRadius,
                PoiManager.Occupancy.ANY);
        Optional<BlockPos> anyPoiCenter = meetingCenter.isPresent()
                ? Optional.empty()
                : poiManager.findClosest(poiType -> true, origin, searchRadius, PoiManager.Occupancy.ANY);
        BlockPos center = meetingCenter.or(() -> anyPoiCenter).orElse(origin).immutable();
        long meetingPois = poiManager.getCountInRange(
                poiType -> poiType.is(PoiTypes.MEETING),
                center,
                searchRadius,
                PoiManager.Occupancy.ANY);
        long villagePois = poiManager.getCountInRange(
                poiType -> true,
                center,
                searchRadius,
                PoiManager.Occupancy.ANY);
        int villagerCount = countVillagers(level, center, searchRadius);
        int radius = Mth.clamp(DEFAULT_SIEGE_RADIUS + (int) Math.min(16L, Math.max(meetingPois, villagePois / 2L)),
                DEFAULT_SIEGE_RADIUS, MAX_SIEGE_RADIUS);
        return new Context(center, radius, villagerCount, meetingPois, villagePois);
    }

    public record Context(BlockPos center, int radius, int villagerCount, long meetingPoiCount, long villagePoiCount) {
    }
}
