package net.narutomod.registry;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.narutomod.entity.IrukaSenseiEntity;
import net.narutomod.entity.ItachiEntity;
import net.narutomod.entity.MightGuyEntity;
import net.narutomod.entity.NinjaMobEntity;
import net.narutomod.entity.SakuraHarunoEntity;
import net.narutomod.entity.TentenEntity;
import net.narutomod.world.VillagePoiHelper;

public final class ModSpawnPlacements {
    private ModSpawnPlacements() {
    }

    public static void register() {
        registerLegacyMerchant(ModEntityTypes.IRUKA_SENSEI.get(), IrukaSenseiEntity.class);
        registerMightGuy(ModEntityTypes.MIGHTGUY.get());
        registerLegacyMerchant(ModEntityTypes.SAKURA_HARUNO.get(), SakuraHarunoEntity.class);
        registerLegacyMerchant(ModEntityTypes.TENTEN.get(), TentenEntity.class);
        registerItachi(ModEntityTypes.ITACHI.get());
        registerKisame(ModEntityTypes.KISAME_HOSHIGAKI.get());
        registerMonster(ModEntityTypes.WHITEZETSU.get());
        registerZabuza(ModEntityTypes.ZABUZA_MOMOCHI.get());
    }

    private static <T extends Mob> void registerAmbient(EntityType<T> type) {
        SpawnPlacements.register(
                type,
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mob::checkMobSpawnRules);
    }

    private static void registerMightGuy(EntityType<MightGuyEntity> type) {
        SpawnPlacements.register(
                type,
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                ModSpawnPlacements::checkMightGuySpawnRules);
    }

    private static <T extends Monster> void registerMonster(EntityType<T> type) {
        SpawnPlacements.register(
                type,
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules);
    }

    private static void registerItachi(EntityType<ItachiEntity> type) {
        SpawnPlacements.register(
                type,
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                ModSpawnPlacements::checkItachiSpawnRules);
    }

    private static void registerKisame(EntityType<NinjaMobEntity> type) {
        SpawnPlacements.register(
                type,
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                ModSpawnPlacements::checkKisameSpawnRules);
    }

    private static void registerZabuza(EntityType<NinjaMobEntity> type) {
        SpawnPlacements.register(
                type,
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                ModSpawnPlacements::checkZabuzaSpawnRules);
    }

    private static <T extends NinjaMobEntity> void registerLegacyMerchant(EntityType<T> type, Class<T> entityClass) {
        SpawnPlacements.register(
                type,
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (spawnType, level, spawnReason, pos, random) -> checkLegacyMerchantSpawnRules(
                        spawnType, entityClass, level, spawnReason, pos, random));
    }

    private static boolean checkItachiSpawnRules(EntityType<ItachiEntity> type, ServerLevelAccessor level,
            MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        return Monster.checkMonsterSpawnRules(type, level, spawnType, pos, random)
                && random.nextInt(5) == 0
                && level.getLevel()
                        .getEntitiesOfClass(ItachiEntity.class, new AABB(pos).inflate(128.0D))
                        .isEmpty();
    }

    private static boolean checkKisameSpawnRules(EntityType<NinjaMobEntity> type, ServerLevelAccessor level,
            MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        return Monster.checkMonsterSpawnRules(type, level, spawnType, pos, random)
                && random.nextInt(5) == 0
                && level.getLevel()
                        .getEntitiesOfClass(NinjaMobEntity.class, new AABB(pos).inflate(128.0D),
                                entity -> entity.getType() == type)
                        .isEmpty();
    }

    private static boolean checkZabuzaSpawnRules(EntityType<NinjaMobEntity> type, ServerLevelAccessor level,
            MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        return Monster.checkMonsterSpawnRules(type, level, spawnType, pos, random)
                && random.nextInt(5) == 0
                && level.getLevel()
                        .getEntitiesOfClass(NinjaMobEntity.class, new AABB(pos).inflate(128.0D),
                                entity -> entity.getType() == type)
                        .isEmpty();
    }

    private static <T extends NinjaMobEntity> boolean checkLegacyMerchantSpawnRules(EntityType<T> type, Class<T> entityClass, ServerLevelAccessor level,
            MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        return Mob.checkMobSpawnRules(type, level, spawnType, pos, random)
                && random.nextInt(10) == 0
                && VillagePoiHelper.findLegacyMerchantContext(level.getLevel(), pos)
                        .filter(context -> countLegacyMerchants(level, context.center()) < 2)
                        .isPresent()
                && level.getLevel()
                        .getEntitiesOfClass(entityClass, new AABB(pos).inflate(128.0D, 16.0D, 128.0D),
                                Mob::isAlive)
                        .isEmpty();
    }

    private static boolean checkMightGuySpawnRules(EntityType<MightGuyEntity> type, ServerLevelAccessor level,
            MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        return Mob.checkMobSpawnRules(type, level, spawnType, pos, random)
                && VillagePoiHelper.findNaturalMightGuyContext(level.getLevel(), pos).isPresent()
                && level.getLevel()
                        .getEntitiesOfClass(MightGuyEntity.class, new AABB(pos).inflate(512.0D, 256.0D, 512.0D),
                                MightGuyEntity::isAlive)
                        .isEmpty();
    }

    private static int countLegacyMerchants(ServerLevelAccessor level, BlockPos center) {
        return level.getLevel()
                .getEntitiesOfClass(NinjaMobEntity.class, new AABB(center).inflate(96.0D, 10.0D, 96.0D),
                        entity -> entity.isAlive() && isLegacyMerchantType(entity.getType()))
                .size();
    }

    private static boolean isLegacyMerchantType(EntityType<?> type) {
        return type == ModEntityTypes.IRUKA_SENSEI.get()
                || type == ModEntityTypes.SAKURA_HARUNO.get()
                || type == ModEntityTypes.TENTEN.get()
                || type == ModEntityTypes.MIGHTGUY.get();
    }
}
