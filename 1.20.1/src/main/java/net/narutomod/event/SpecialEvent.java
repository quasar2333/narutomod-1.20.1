package net.narutomod.event;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.narutomod.NarutomodMod;
import net.narutomod.NarutomodSavedData;
import net.narutomod.network.CameraShakeMessage;
import net.narutomod.network.NetworkHandler;
import net.narutomod.particle.NarutoParticleKind;
import net.narutomod.registry.ModBlocks;
import net.narutomod.registry.ModParticleTypes;
import net.narutomod.registry.ModSounds;
import net.narutomod.world.VillagePoiHelper;

@Mod.EventBusSubscriber(modid = NarutomodMod.MODID)
public final class SpecialEvent {
    public static final String EVENTS_TAG = "SpecialEvents";
    public static final int CYLINDRICAL_EXPLOSION_TYPE = 1;
    public static final int DELAYED_SPAWN_TYPE = 3;
    public static final int VILLAGE_SIEGE_TYPE = 4;
    public static final int SPHERICAL_EXPLOSION_TYPE = 2;
    public static final int METEOR_SHOWER_TYPE = 5;
    public static final int SET_BLOCKS_TYPE = 6;
    public static final int VANILLA_EXPLOSION_TYPE = 7;
    public static final int DELAYED_CALLBACK_TYPE = 8;

    private static final int NO_EVENT_TYPE = 0;
    private static final String TYPE_TAG = "Type";
    private static final String ID_TAG = "ID";
    private static final String ENTITY_UUID_TAG = "EntityUUID";
    private static final String LEGACY_WORLD_TAG = "World";
    private static final String DIMENSION_TAG = "Dimension";
    private static final String START_TAG = "Start";
    private static final String TICK_TAG = "Tick";
    private static final String X_TAG = "x0";
    private static final String Y_TAG = "y0";
    private static final String Z_TAG = "z0";
    private static final String ENTITY_TYPE_TAG = "EntityType";
    private static final String ENTITY_TAG = "EntityTag";
    private static final String VANILLA_ENTITY_ID_TAG = "id";
    private static final String BLOCKS_LIST_TAG = "blocksList";
    private static final String BLOCK_STATE_TAG = "state";
    private static final String LEGACY_BLOCK_STATE_TAG = "blockstate";
    private static final String LIFESPAN_TAG = "lifespan";
    private static final String ADD_INDEX_TAG = "addIndex";
    private static final String REMOVE_INDEX_TAG = "removeIndex";
    private static final String SOUND_TAG = "Sound";
    private static final String PARTICLES_TAG = "Particles";
    private static final String STRENGTH_TAG = "strength";
    private static final String FLAMING_TAG = "flaming";
    private static final String DAMAGES_TERRAIN_TAG = "damagesTerrain";
    private static final String RADIUS_TAG = "radius";
    private static final String HEIGHT_TAG = "height";
    private static final String TX_TAG = "tx";
    private static final String TY_TAG = "ty";
    private static final String TZ_TAG = "tz";
    private static final String TR_TAG = "tr";
    private static final String MOB_GRIEFING_TAG = "mobGriefing";
    private static final String USE_BLOCK_EXPLOSION_RESISTANCE_TAG = "useBlockExplosionResistance";
    private static final String FIRE_CHANCE_TAG = "fireChance";
    private static final String STRIKE_INTERVAL_TAG = "strikeInterval";
    private static final String DURATION_TAG = "duration";
    private static final String ALL_PLAYERS_TAG = "allPlayers";
    private static final String SPAWN_INTERVAL_TAG = "spawnInterval";
    private static final String CALLBACK_ID_TAG = "callbackID";
    private static final int SMOKE_BOMB_CALLBACK_ID = 681;
    private static final int SET_BLOCKS_BUDGET_PER_TICK = 64;
    private static final int CYLINDRICAL_EXPLOSION_BLOCK_BUDGET_PER_TICK = 1024;
    private static final int SPHERICAL_EXPLOSION_BLOCK_BUDGET_PER_TICK = 1024;

    private SpecialEvent() {
    }

    public static boolean setDelayedSpawnEvent(Level level, Entity entity, int xOffset, int yOffset, int zOffset, long timeToSpawn) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        return setDelayedSpawnEvent(serverLevel, entity, xOffset, yOffset, zOffset, timeToSpawn);
    }

    public static boolean setDelayedSpawnEvent(ServerLevel level, Entity entity, int xOffset, int yOffset, int zOffset, long timeToSpawn) {
        if (level == null || entity == null) {
            return false;
        }
        ResourceLocation entityTypeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (entityTypeId == null) {
            return false;
        }
        CompoundTag entityTag = new CompoundTag();
        if (!entity.saveAsPassenger(entityTag)) {
            return false;
        }
        entityTag.putString(VANILLA_ENTITY_ID_TAG, entityTypeId.toString());

        MinecraftServer server = level.getServer();
        long now = eventTime(server);
        int x = Mth.floor(entity.getX()) + xOffset;
        int y = Mth.floor(entity.getY()) + yOffset;
        int z = Mth.floor(entity.getZ()) + zOffset;

        CompoundTag eventTag = new CompoundTag();
        eventTag.putInt(TYPE_TAG, DELAYED_SPAWN_TYPE);
        eventTag.putInt(ID_TAG, ThreadLocalRandom.current().nextInt());
        eventTag.putInt(LEGACY_WORLD_TAG, legacyDimensionId(level));
        eventTag.putString(DIMENSION_TAG, level.dimension().location().toString());
        eventTag.putLong(START_TAG, Math.max(timeToSpawn, now));
        eventTag.putInt(TICK_TAG, 0);
        eventTag.putInt(X_TAG, x);
        eventTag.putInt(Y_TAG, y);
        eventTag.putInt(Z_TAG, z);
        eventTag.putString(ENTITY_TYPE_TAG, entityTypeId.toString());
        eventTag.put(ENTITY_TAG, entityTag);
        appendEvent(server, eventTag);
        return true;
    }

    public static boolean setDelayedCallbackEvent(Level level, int x, int y, int z, long startTime, int callbackId) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        return setDelayedCallbackEvent(serverLevel, x, y, z, startTime, callbackId);
    }

    public static boolean setDelayedCallbackEvent(ServerLevel level, int x, int y, int z, long startTime, int callbackId) {
        if (level == null) {
            return false;
        }
        MinecraftServer server = level.getServer();
        long now = eventTime(server);

        CompoundTag eventTag = new CompoundTag();
        eventTag.putInt(TYPE_TAG, DELAYED_CALLBACK_TYPE);
        eventTag.putInt(ID_TAG, ThreadLocalRandom.current().nextInt());
        eventTag.putInt(LEGACY_WORLD_TAG, legacyDimensionId(level));
        eventTag.putString(DIMENSION_TAG, level.dimension().location().toString());
        eventTag.putLong(START_TAG, Math.max(startTime, now));
        eventTag.putInt(TICK_TAG, 0);
        eventTag.putInt(X_TAG, x);
        eventTag.putInt(Y_TAG, y);
        eventTag.putInt(Z_TAG, z);
        eventTag.putInt(CALLBACK_ID_TAG, callbackId);
        appendEvent(server, eventTag);
        return true;
    }

    public static int setSmokeBombCallbackEvents(Level level, int x, int y, int z, long startTime, int ticks) {
        if (!(level instanceof ServerLevel serverLevel) || ticks <= 0) {
            return 0;
        }
        int scheduled = 0;
        for (int i = 0; i < ticks; i++) {
            if (setDelayedCallbackEvent(serverLevel, x, y, z, startTime + i, SMOKE_BOMB_CALLBACK_ID)) {
                scheduled++;
            }
        }
        return scheduled;
    }

    public static boolean setMassExplosionEvent(Level level, int x, int yTop, int z, int yBottom, int radius) {
        return setCylindricalExplosionEvent(level, x, yTop, z, yBottom, radius);
    }

    public static boolean setCylindricalExplosionEvent(Level level, int x, int yTop, int z, int yBottom, int radius) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        return setCylindricalExplosionEvent(
                serverLevel,
                x,
                yTop,
                z,
                yBottom,
                radius,
                serverLevel.getServer().overworld().getGameTime(),
                true,
                true,
                ForgeEventFactory.getMobGriefingEvent(serverLevel, null));
    }

    public static boolean setMassExplosionEvent(ServerLevel level, int x, int yTop, int z, int yBottom, int radius,
            long startTime, boolean particles, boolean sounds, boolean mobGriefing) {
        return setCylindricalExplosionEvent(level, x, yTop, z, yBottom, radius, startTime, particles, sounds, mobGriefing);
    }

    public static boolean setCylindricalExplosionEvent(ServerLevel level, int x, int yTop, int z, int yBottom, int radius,
            long startTime, boolean particles, boolean sounds, boolean mobGriefing) {
        if (level == null || radius <= 0) {
            return false;
        }
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight() - 1;
        int bottom = Mth.clamp(Math.min(yTop, yBottom), minY, maxY);
        int top = Mth.clamp(Math.max(yTop, yBottom), minY, maxY);
        MinecraftServer server = level.getServer();
        long now = eventTime(server);

        CompoundTag eventTag = new CompoundTag();
        eventTag.putInt(TYPE_TAG, CYLINDRICAL_EXPLOSION_TYPE);
        eventTag.putInt(ID_TAG, ThreadLocalRandom.current().nextInt());
        eventTag.putInt(LEGACY_WORLD_TAG, legacyDimensionId(level));
        eventTag.putString(DIMENSION_TAG, level.dimension().location().toString());
        eventTag.putLong(START_TAG, Math.max(startTime, now));
        eventTag.putInt(TICK_TAG, 0);
        eventTag.putInt(X_TAG, x);
        eventTag.putInt(Y_TAG, bottom);
        eventTag.putInt(Z_TAG, z);
        eventTag.putBoolean(SOUND_TAG, sounds);
        eventTag.putBoolean(PARTICLES_TAG, particles);
        eventTag.putInt(RADIUS_TAG, radius);
        eventTag.putInt(HEIGHT_TAG, top - bottom);
        eventTag.putInt(TX_TAG, 0);
        eventTag.putInt(TY_TAG, top);
        eventTag.putInt(TZ_TAG, 0);
        eventTag.putInt(TR_TAG, 0);
        eventTag.putBoolean(MOB_GRIEFING_TAG, mobGriefing);
        appendEvent(server, eventTag);
        return true;
    }

    public static boolean setSphericalExplosionEvent(Level level, int x, int y, int z, int radius, Entity excludeEntity) {
        return setSphericalExplosionEvent(level, x, y, z, radius, excludeEntity, 0.0F);
    }

    public static boolean setSphericalExplosionEvent(Level level, int x, int y, int z, int radius, Entity excludeEntity, float fireChance) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        return setSphericalExplosionEvent(
                serverLevel,
                excludeEntity,
                x,
                y,
                z,
                radius,
                serverLevel.getServer().overworld().getGameTime(),
                true,
                fireChance,
                true,
                true,
                ForgeEventFactory.getMobGriefingEvent(serverLevel, excludeEntity));
    }

    public static boolean setSphericalExplosionEvent(ServerLevel level, Entity excludeEntity, int x, int y, int z, int radius,
            long startTime, boolean useBlockExplosionResistance, float fireChance, boolean particles, boolean sounds, boolean mobGriefing) {
        if (level == null || radius <= 0) {
            return false;
        }
        MinecraftServer server = level.getServer();
        long now = eventTime(server);

        CompoundTag eventTag = new CompoundTag();
        eventTag.putInt(TYPE_TAG, SPHERICAL_EXPLOSION_TYPE);
        eventTag.putInt(ID_TAG, ThreadLocalRandom.current().nextInt());
        if (excludeEntity != null && !excludeEntity.isRemoved() && excludeEntity.level() == level) {
            eventTag.putUUID(ENTITY_UUID_TAG, excludeEntity.getUUID());
        }
        eventTag.putInt(LEGACY_WORLD_TAG, legacyDimensionId(level));
        eventTag.putString(DIMENSION_TAG, level.dimension().location().toString());
        eventTag.putLong(START_TAG, Math.max(startTime, now));
        eventTag.putInt(TICK_TAG, 0);
        eventTag.putInt(X_TAG, x);
        eventTag.putInt(Y_TAG, y);
        eventTag.putInt(Z_TAG, z);
        eventTag.putBoolean(SOUND_TAG, sounds);
        eventTag.putBoolean(PARTICLES_TAG, particles);
        eventTag.putInt(RADIUS_TAG, radius);
        eventTag.putInt(TX_TAG, 0);
        eventTag.putInt(TY_TAG, radius);
        eventTag.putInt(TZ_TAG, 0);
        eventTag.putInt(TR_TAG, 0);
        eventTag.putBoolean(MOB_GRIEFING_TAG, mobGriefing);
        eventTag.putBoolean(USE_BLOCK_EXPLOSION_RESISTANCE_TAG, useBlockExplosionResistance);
        eventTag.putFloat(FIRE_CHANCE_TAG, Math.max(0.0F, fireChance));
        appendEvent(server, eventTag);
        return true;
    }

    public static boolean setVillageSiegeEvent(Level level, int centerX, int centerY, int centerZ, long startTime,
            int radius, Entity mob, int spawnInterval) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        return setVillageSiegeEvent(serverLevel, centerX, centerY, centerZ, startTime, radius, mob, spawnInterval);
    }

    public static boolean setVillageSiegeEvent(ServerLevel level, int centerX, int centerY, int centerZ, long startTime,
            int radius, Entity mob, int spawnInterval) {
        if (level == null || radius <= 0 || spawnInterval <= 0) {
            return false;
        }
        MinecraftServer server = level.getServer();
        long now = eventTime(server);

        CompoundTag eventTag = new CompoundTag();
        eventTag.putInt(TYPE_TAG, VILLAGE_SIEGE_TYPE);
        eventTag.putInt(ID_TAG, ThreadLocalRandom.current().nextInt());
        if (mob != null) {
            ResourceLocation mobType = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
            if (mobType != null) {
                eventTag.putString(ENTITY_TYPE_TAG, mobType.toString());
            }
        }
        eventTag.putInt(LEGACY_WORLD_TAG, legacyDimensionId(level));
        eventTag.putString(DIMENSION_TAG, level.dimension().location().toString());
        eventTag.putLong(START_TAG, Math.max(startTime, now));
        eventTag.putInt(TICK_TAG, 0);
        eventTag.putInt(X_TAG, centerX);
        eventTag.putInt(Y_TAG, centerY);
        eventTag.putInt(Z_TAG, centerZ);
        eventTag.putInt(RADIUS_TAG, radius);
        eventTag.putInt(SPAWN_INTERVAL_TAG, spawnInterval);
        appendEvent(server, eventTag);
        return true;
    }

    public static boolean setMeteorShowerEvent(Level level, int centerX, int centerY, int centerZ, long startTime,
            int radius, int strikeInterval, int duration) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        return setMeteorShowerEvent(serverLevel, null, centerX, centerY, centerZ, startTime, radius, strikeInterval, duration, false);
    }

    public static boolean setMeteorShowerEvent(Level level, Entity centerAround, long startTime,
            int radius, int strikeInterval, int duration) {
        if (!(level instanceof ServerLevel serverLevel) || centerAround == null) {
            return false;
        }
        return setMeteorShowerEvent(serverLevel, centerAround, Mth.floor(centerAround.getX()), Mth.floor(centerAround.getY()),
                Mth.floor(centerAround.getZ()), startTime, radius, strikeInterval, duration, false);
    }

    public static boolean setMeteorShowerForAllPlayersEvent(Level level, long startTime, int radius, int strikeInterval, int duration) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        return setMeteorShowerEvent(serverLevel, null, 0, 0, 0, startTime, radius, strikeInterval, duration, true);
    }

    public static boolean setMeteorShowerEvent(ServerLevel level, Entity centerAround, int centerX, int centerY, int centerZ,
            long startTime, int radius, int strikeInterval, int duration, boolean allPlayers) {
        if (level == null || radius <= 0 || strikeInterval <= 0 || duration <= 0) {
            return false;
        }
        MinecraftServer server = level.getServer();
        long now = eventTime(server);

        CompoundTag eventTag = new CompoundTag();
        eventTag.putInt(TYPE_TAG, METEOR_SHOWER_TYPE);
        eventTag.putInt(ID_TAG, ThreadLocalRandom.current().nextInt());
        if (centerAround != null && !centerAround.isRemoved() && centerAround.level() == level) {
            eventTag.putUUID(ENTITY_UUID_TAG, centerAround.getUUID());
        }
        eventTag.putInt(LEGACY_WORLD_TAG, legacyDimensionId(level));
        eventTag.putString(DIMENSION_TAG, level.dimension().location().toString());
        eventTag.putLong(START_TAG, Math.max(startTime, now));
        eventTag.putInt(TICK_TAG, 0);
        eventTag.putInt(X_TAG, centerX);
        eventTag.putInt(Y_TAG, centerY);
        eventTag.putInt(Z_TAG, centerZ);
        eventTag.putInt(RADIUS_TAG, radius);
        eventTag.putInt(STRIKE_INTERVAL_TAG, strikeInterval);
        eventTag.putInt(DURATION_TAG, duration);
        eventTag.putBoolean(ALL_PLAYERS_TAG, allPlayers);
        appendEvent(server, eventTag);
        return true;
    }

    public static boolean setBlocksEvent(Level level, Map<BlockPos, BlockState> blocks, long startTime, int lifespan) {
        return setBlocksEvent(level, blocks, startTime, lifespan, true, true);
    }

    public static boolean setBlocksEvent(Level level, Map<BlockPos, BlockState> blocks, long startTime, int lifespan,
            boolean particles, boolean sounds) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        return setBlocksEvent(serverLevel, blocks, startTime, lifespan, particles, sounds);
    }

    public static boolean setBlocksEvent(ServerLevel level, Map<BlockPos, BlockState> blocks, long startTime, int lifespan,
            boolean particles, boolean sounds) {
        if (level == null || blocks == null || blocks.isEmpty()) {
            return false;
        }
        MinecraftServer server = level.getServer();
        long now = eventTime(server);

        ListTag blockList = new ListTag();
        BlockPos firstPos = null;
        for (Map.Entry<BlockPos, BlockState> entry : blocks.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            CompoundTag blockTag = new CompoundTag();
            BlockPos pos = entry.getKey();
            BlockState state = entry.getValue();
            if (firstPos == null) {
                firstPos = pos;
            }
            blockTag.putInt("x", pos.getX());
            blockTag.putInt("y", pos.getY());
            blockTag.putInt("z", pos.getZ());
            blockTag.putInt(LEGACY_BLOCK_STATE_TAG, Block.getId(state));
            blockTag.put(BLOCK_STATE_TAG, NbtUtils.writeBlockState(state));
            blockList.add(blockTag);
        }
        if (blockList.isEmpty()) {
            return false;
        }

        CompoundTag eventTag = new CompoundTag();
        eventTag.putInt(TYPE_TAG, SET_BLOCKS_TYPE);
        eventTag.putInt(ID_TAG, ThreadLocalRandom.current().nextInt());
        eventTag.putInt(LEGACY_WORLD_TAG, legacyDimensionId(level));
        eventTag.putString(DIMENSION_TAG, level.dimension().location().toString());
        eventTag.putLong(START_TAG, Math.max(startTime, now));
        eventTag.putInt(TICK_TAG, 0);
        eventTag.putInt(X_TAG, firstPos.getX());
        eventTag.putInt(Y_TAG, firstPos.getY());
        eventTag.putInt(Z_TAG, firstPos.getZ());
        eventTag.putBoolean(SOUND_TAG, sounds);
        eventTag.putBoolean(PARTICLES_TAG, particles);
        eventTag.putInt(LIFESPAN_TAG, Math.max(0, lifespan));
        eventTag.putInt(ADD_INDEX_TAG, 0);
        eventTag.putInt(REMOVE_INDEX_TAG, 0);
        eventTag.put(BLOCKS_LIST_TAG, blockList);
        appendEvent(server, eventTag);
        return true;
    }

    public static boolean setVanillaExplosionEvent(Level level, Entity entity, int x, int y, int z, float strength, long startTime, boolean flames) {
        if (!(level instanceof ServerLevel serverLevel) || strength <= 0.0F) {
            return false;
        }
        boolean damagesTerrain = ForgeEventFactory.getMobGriefingEvent(serverLevel, entity);
        return setVanillaExplosionEvent(serverLevel, entity, x, y, z, strength, startTime, flames, damagesTerrain);
    }

    public static boolean setVanillaExplosionEvent(ServerLevel level, Entity entity, int x, int y, int z, float strength,
            long startTime, boolean flames, boolean damagesTerrain) {
        if (level == null || strength <= 0.0F) {
            return false;
        }
        MinecraftServer server = level.getServer();
        long now = eventTime(server);

        CompoundTag eventTag = new CompoundTag();
        eventTag.putInt(TYPE_TAG, VANILLA_EXPLOSION_TYPE);
        eventTag.putInt(ID_TAG, ThreadLocalRandom.current().nextInt());
        if (entity != null && !entity.isRemoved() && entity.level() == level) {
            eventTag.putUUID(ENTITY_UUID_TAG, entity.getUUID());
        }
        eventTag.putInt(LEGACY_WORLD_TAG, legacyDimensionId(level));
        eventTag.putString(DIMENSION_TAG, level.dimension().location().toString());
        eventTag.putLong(START_TAG, Math.max(startTime, now));
        eventTag.putInt(TICK_TAG, 0);
        eventTag.putInt(X_TAG, x);
        eventTag.putInt(Y_TAG, y);
        eventTag.putInt(Z_TAG, z);
        eventTag.putFloat(STRENGTH_TAG, strength);
        eventTag.putBoolean(FLAMING_TAG, flames);
        eventTag.putBoolean(DAMAGES_TERRAIN_TAG, damagesTerrain);
        appendEvent(server, eventTag);
        return true;
    }

    public static int pendingCount(MinecraftServer server) {
        return events(server).size();
    }

    public static int pendingCount(MinecraftServer server, int type) {
        int count = 0;
        ListTag events = events(server);
        for (int i = 0; i < events.size(); i++) {
            if (events.getCompound(i).getInt(TYPE_TAG) == type) {
                count++;
            }
        }
        return count;
    }

    public static String debugSummary(MinecraftServer server) {
        ListTag events = events(server);
        long now = eventTime(server);
        int delayed = 0;
        int cylindricalExplosion = 0;
        int sphericalExplosion = 0;
        int villageSiege = 0;
        int meteorShower = 0;
        int setBlocks = 0;
        int vanillaExplosion = 0;
        int delayedCallback = 0;
        int unsupported = 0;
        int due = 0;
        long nextStart = Long.MAX_VALUE;
        for (int i = 0; i < events.size(); i++) {
            CompoundTag eventTag = events.getCompound(i);
            int type = eventTag.getInt(TYPE_TAG);
            long start = eventTag.contains(START_TAG, Tag.TAG_LONG) ? eventTag.getLong(START_TAG) : now;
            if (type == DELAYED_SPAWN_TYPE) {
                delayed++;
                if (start <= now) {
                    due++;
                }
                nextStart = Math.min(nextStart, start);
            } else if (type == CYLINDRICAL_EXPLOSION_TYPE) {
                cylindricalExplosion++;
                if (start <= now) {
                    due++;
                }
                nextStart = Math.min(nextStart, start);
            } else if (type == SPHERICAL_EXPLOSION_TYPE) {
                sphericalExplosion++;
                if (start <= now) {
                    due++;
                }
                nextStart = Math.min(nextStart, start);
            } else if (type == VILLAGE_SIEGE_TYPE) {
                villageSiege++;
                if (start <= now) {
                    due++;
                }
                nextStart = Math.min(nextStart, start);
            } else if (type == METEOR_SHOWER_TYPE) {
                meteorShower++;
                if (start <= now) {
                    due++;
                }
                nextStart = Math.min(nextStart, start);
            } else if (type == SET_BLOCKS_TYPE) {
                setBlocks++;
                if (start <= now) {
                    due++;
                }
                nextStart = Math.min(nextStart, start);
            } else if (type == VANILLA_EXPLOSION_TYPE) {
                vanillaExplosion++;
                if (start <= now) {
                    due++;
                }
                nextStart = Math.min(nextStart, start);
            } else if (type == DELAYED_CALLBACK_TYPE) {
                delayedCallback++;
                if (start <= now) {
                    due++;
                }
                nextStart = Math.min(nextStart, start);
            } else if (type != NO_EVENT_TYPE) {
                unsupported++;
                nextStart = Math.min(nextStart, start);
            }
        }
        String next = nextStart == Long.MAX_VALUE ? "none" : String.valueOf(nextStart);
        return "pending=" + events.size()
                + ", delayed_spawn=" + delayed
                + ", cylindrical_explosion=" + cylindricalExplosion
                + ", spherical_explosion=" + sphericalExplosion
                + ", village_siege=" + villageSiege
                + ", meteor_shower=" + meteorShower
                + ", set_blocks=" + setBlocks
                + ", vanilla_explosion=" + vanillaExplosion
                + ", delayed_callback=" + delayedCallback
                + ", unsupported=" + unsupported
                + ", due=" + due
                + ", time=" + now
                + ", next_start=" + next;
    }

    public static int executeDueEvents(MinecraftServer server) {
        if (server == null) {
            return 0;
        }
        NarutomodSavedData.SpecialEvents data = NarutomodSavedData.specialEvents(server);
        CompoundTag root = data.data();
        if (!root.contains(EVENTS_TAG, Tag.TAG_LIST)) {
            return 0;
        }

        ListTag current = root.getList(EVENTS_TAG, Tag.TAG_COMPOUND);
        if (current.isEmpty()) {
            return 0;
        }

        long now = eventTime(server);
        ListTag kept = new ListTag();
        boolean changed = false;
        int executed = 0;
        for (int i = 0; i < current.size(); i++) {
            CompoundTag eventTag = current.getCompound(i).copy();
            int type = eventTag.getInt(TYPE_TAG);
            if (type == NO_EVENT_TYPE) {
                changed = true;
                continue;
            }
            if (type == DELAYED_SPAWN_TYPE && isDue(eventTag, now)) {
                if (executeDelayedSpawn(server, eventTag)) {
                    executed++;
                }
                changed = true;
                continue;
            }
            if (type == CYLINDRICAL_EXPLOSION_TYPE && isDue(eventTag, now)) {
                CylindricalExplosionTickResult result = tickCylindricalExplosion(server, eventTag);
                executed += result.touchedBlocks() > 0 ? 1 : 0;
                changed |= result.changed() || result.clear();
                if (!result.clear()) {
                    kept.add(result.eventTag());
                }
                continue;
            }
            if (type == SPHERICAL_EXPLOSION_TYPE && isDue(eventTag, now)) {
                SphericalExplosionTickResult result = tickSphericalExplosion(server, eventTag);
                executed += result.touchedBlocks() > 0 ? 1 : 0;
                changed |= result.changed() || result.clear();
                if (!result.clear()) {
                    kept.add(result.eventTag());
                }
                continue;
            }
            if (type == VILLAGE_SIEGE_TYPE && isDue(eventTag, now)) {
                VillageSiegeTickResult result = tickVillageSiege(server, eventTag);
                executed += result.spawnedMobs();
                changed |= result.changed() || result.clear();
                if (!result.clear()) {
                    kept.add(result.eventTag());
                }
                continue;
            }
            if (type == METEOR_SHOWER_TYPE && isDue(eventTag, now)) {
                MeteorShowerTickResult result = tickMeteorShower(server, eventTag, now);
                executed += result.spawnedMeteors();
                changed |= result.changed() || result.clear();
                if (!result.clear()) {
                    kept.add(result.eventTag());
                }
                continue;
            }
            if (type == SET_BLOCKS_TYPE && isDue(eventTag, now)) {
                SetBlocksTickResult result = tickSetBlocks(server, eventTag, now);
                executed += result.touchedBlocks() > 0 ? 1 : 0;
                changed |= result.changed() || result.clear();
                if (!result.clear()) {
                    kept.add(result.eventTag());
                }
                continue;
            }
            if (type == VANILLA_EXPLOSION_TYPE && isDue(eventTag, now)) {
                if (executeVanillaExplosion(server, eventTag)) {
                    executed++;
                }
                changed = true;
                continue;
            }
            if (type == DELAYED_CALLBACK_TYPE && isDue(eventTag, now)) {
                if (executeDelayedCallback(server, eventTag)) {
                    executed++;
                }
                changed = true;
                continue;
            }
            kept.add(eventTag);
        }

        if (changed) {
            CompoundTag updated = root.copy();
            if (kept.isEmpty()) {
                updated.remove(EVENTS_TAG);
            } else {
                updated.put(EVENTS_TAG, kept);
            }
            data.replace(updated);
        }
        return executed;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            executeDueEvents(server);
        }
    }

    private static boolean executeDelayedSpawn(MinecraftServer server, CompoundTag eventTag) {
        ServerLevel level = levelForEvent(server, eventTag);
        if (level == null || !eventTag.contains(ENTITY_TAG, Tag.TAG_COMPOUND)) {
            return false;
        }

        CompoundTag entityTag = eventTag.getCompound(ENTITY_TAG).copy();
        if (!entityTag.contains(VANILLA_ENTITY_ID_TAG, Tag.TAG_STRING)) {
            String entityType = eventTag.getString(ENTITY_TYPE_TAG);
            if (entityType.isEmpty()) {
                return false;
            }
            entityTag.putString(VANILLA_ENTITY_ID_TAG, entityType);
        }

        Entity entity = EntityType.loadEntityRecursive(entityTag, level, loaded -> loaded);
        if (entity == null) {
            return false;
        }

        double x = eventTag.getInt(X_TAG);
        double y = eventTag.getInt(Y_TAG);
        double z = eventTag.getInt(Z_TAG);
        level.getChunk(BlockPos.containing(x, y, z));
        entity.moveTo(x, y, z, entity.getYRot(), entity.getXRot());
        if (!level.addFreshEntity(entity)) {
            entity.discard();
            return false;
        }
        return true;
    }

    private static CylindricalExplosionTickResult tickCylindricalExplosion(MinecraftServer server, CompoundTag eventTag) {
        ServerLevel level = levelForEvent(server, eventTag);
        if (level == null) {
            return new CylindricalExplosionTickResult(eventTag, false, true, 0);
        }
        CompoundTag updated = eventTag.copy();
        int radius = updated.getInt(RADIUS_TAG);
        if (radius <= 0) {
            return new CylindricalExplosionTickResult(updated, false, true, 0);
        }

        int yBottom = Mth.clamp(updated.getInt(Y_TAG), level.getMinBuildHeight(), level.getMaxBuildHeight() - 1);
        int height = Math.max(0, updated.getInt(HEIGHT_TAG));
        int yTop = Mth.clamp(yBottom + height, level.getMinBuildHeight(), level.getMaxBuildHeight() - 1);
        int tick = updated.getInt(TICK_TAG) + 1;
        updated.putInt(TICK_TAG, tick);
        boolean sounds = !updated.contains(SOUND_TAG, Tag.TAG_BYTE) || updated.getBoolean(SOUND_TAG);
        boolean particles = !updated.contains(PARTICLES_TAG, Tag.TAG_BYTE) || updated.getBoolean(PARTICLES_TAG);
        if (sounds && tick % 10 == 0) {
            level.playSound(null, updated.getInt(X_TAG), yBottom, updated.getInt(Z_TAG),
                    ModSounds.SOUND_GROUND_CHARGE.get(), SoundSource.NEUTRAL,
                    50.0F, level.random.nextFloat() * 0.7F + 0.3F);
        }

        int x0 = updated.getInt(X_TAG);
        int z0 = updated.getInt(Z_TAG);
        int tx = updated.getInt(TX_TAG);
        int ty = updated.contains(TY_TAG, Tag.TAG_INT) ? updated.getInt(TY_TAG) : yTop;
        int tz = updated.getInt(TZ_TAG);
        int tr = updated.getInt(TR_TAG);
        boolean mobGriefing = !updated.contains(MOB_GRIEFING_TAG, Tag.TAG_BYTE) || updated.getBoolean(MOB_GRIEFING_TAG);
        int touched = 0;
        int scanned = 0;
        boolean clear = false;
        long maxScans = Math.max(64L, (long) (yTop - yBottom + 1) * Math.max(1L, (long) radius * Math.max(1, radius)) * 16L);
        while (touched < CYLINDRICAL_EXPLOSION_BLOCK_BUDGET_PER_TICK && !clear) {
            for (BlockPos pos : cylindricalMirrorPositions(x0, ty, z0, tx, tz)) {
                if (pos.getY() < level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight()) {
                    continue;
                }
                BlockState state = level.getBlockState(pos);
                if (!state.isAir()) {
                    if (mobGriefing) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    }
                    if (particles) {
                        spawnCylindricalExplosionParticles(level, pos);
                    }
                    touched++;
                    if (touched >= CYLINDRICAL_EXPLOSION_BLOCK_BUDGET_PER_TICK) {
                        break;
                    }
                }
            }
            scanned++;
            ty--;
            if (ty < yBottom) {
                ty = yTop;
                tz++;
                tx = (int) Math.round(Math.sqrt(Math.max(0.0D, tr * tr - tz * tz)));
            }
            if (tz > (int) Math.round(tr / 1.41421356D)) {
                tz = 0;
                tx = ++tr;
            }
            if (tr > radius) {
                clear = true;
            }
            if (scanned > maxScans) {
                clear = true;
            }
        }

        if (clear) {
            return new CylindricalExplosionTickResult(updated, true, true, touched);
        }
        updated.putInt(TX_TAG, tx);
        updated.putInt(TY_TAG, ty);
        updated.putInt(TZ_TAG, tz);
        updated.putInt(TR_TAG, tr);
        return new CylindricalExplosionTickResult(updated, true, false, touched);
    }

    private static Set<BlockPos> cylindricalMirrorPositions(int x0, int y, int z0, int tx, int tz) {
        Set<BlockPos> positions = new HashSet<>();
        positions.add(new BlockPos(x0 + tx, y, z0 + tz));
        positions.add(new BlockPos(x0 - tx, y, z0 + tz));
        positions.add(new BlockPos(x0 + tx, y, z0 - tz));
        positions.add(new BlockPos(x0 - tx, y, z0 - tz));
        positions.add(new BlockPos(x0 + tz, y, z0 + tx));
        positions.add(new BlockPos(x0 + tz, y, z0 - tx));
        positions.add(new BlockPos(x0 - tz, y, z0 + tx));
        positions.add(new BlockPos(x0 - tz, y, z0 - tx));
        positions.add(new BlockPos(x0 + tx - 1, y, z0 + tz));
        positions.add(new BlockPos(x0 - tx + 1, y, z0 + tz));
        positions.add(new BlockPos(x0 + tx - 1, y, z0 - tz));
        positions.add(new BlockPos(x0 - tx + 1, y, z0 - tz));
        positions.add(new BlockPos(x0 + tz, y, z0 + tx - 1));
        positions.add(new BlockPos(x0 + tz, y, z0 - tx + 1));
        positions.add(new BlockPos(x0 - tz, y, z0 + tx - 1));
        positions.add(new BlockPos(x0 - tz, y, z0 - tx + 1));
        return positions;
    }

    private static void spawnCylindricalExplosionParticles(ServerLevel level, BlockPos pos) {
        level.sendParticles(ParticleTypes.EXPLOSION, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                1, 1.0D, 1.0D, 1.0D, 3.0D);
        level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                1, 1.0D, 1.0D, 1.0D, 0.0D);
    }

    private static SphericalExplosionTickResult tickSphericalExplosion(MinecraftServer server, CompoundTag eventTag) {
        ServerLevel level = levelForEvent(server, eventTag);
        if (level == null) {
            return new SphericalExplosionTickResult(eventTag, false, true, 0);
        }
        CompoundTag updated = eventTag.copy();
        int radius = updated.getInt(RADIUS_TAG);
        if (radius <= 0) {
            return new SphericalExplosionTickResult(updated, false, true, 0);
        }

        int tick = updated.getInt(TICK_TAG) + 1;
        updated.putInt(TICK_TAG, tick);
        boolean sounds = !updated.contains(SOUND_TAG, Tag.TAG_BYTE) || updated.getBoolean(SOUND_TAG);
        boolean particles = !updated.contains(PARTICLES_TAG, Tag.TAG_BYTE) || updated.getBoolean(PARTICLES_TAG);
        if (sounds) {
            playSphericalExplosionSound(level, updated, tick);
        }

        int x0 = updated.getInt(X_TAG);
        int y0 = updated.getInt(Y_TAG);
        int z0 = updated.getInt(Z_TAG);
        int tx = updated.getInt(TX_TAG);
        int ty = updated.contains(TY_TAG, Tag.TAG_INT) ? updated.getInt(TY_TAG) : radius;
        int tz = updated.getInt(TZ_TAG);
        int tr = updated.getInt(TR_TAG);
        boolean mobGriefing = !updated.contains(MOB_GRIEFING_TAG, Tag.TAG_BYTE) || updated.getBoolean(MOB_GRIEFING_TAG);
        boolean useResistance = !updated.contains(USE_BLOCK_EXPLOSION_RESISTANCE_TAG, Tag.TAG_BYTE)
                || updated.getBoolean(USE_BLOCK_EXPLOSION_RESISTANCE_TAG);
        int touched = 0;
        int scanned = 0;
        boolean clear = false;
        sendSphericalExplosionCameraShake(level, x0, y0, z0, radius, tr);
        while (touched < SPHERICAL_EXPLOSION_BLOCK_BUDGET_PER_TICK && !clear) {
            for (BlockPos pos : sphericalMirrorPositions(x0, y0, z0, tx, ty, tz)) {
                BlockState state = level.getBlockState(pos);
                if (!state.isAir() && level.random.nextFloat() <= 1.75F - (float) tr / radius) {
                    if (mobGriefing && canSphericalExplosionDestroy(level, pos, state, radius, useResistance)) {
                        maybeDropExplosionBlock(level, pos, state, radius);
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    }
                    if (particles) {
                        spawnSphericalExplosionParticles(level, pos);
                    }
                    touched++;
                    if (touched >= SPHERICAL_EXPLOSION_BLOCK_BUDGET_PER_TICK) {
                        break;
                    }
                }
            }
            scanned++;
            int maxAtRing = (int) Math.round(Math.sqrt(Math.max(0.0D, radius * radius - tr * tr)));
            ty--;
            if (y0 + ty >= level.getMaxBuildHeight()) {
                continue;
            }
            if (ty < -maxAtRing || y0 + ty < level.getMinBuildHeight()) {
                ty = maxAtRing;
                tz++;
                tx = (int) Math.round(Math.sqrt(Math.max(0.0D, tr * tr - tz * tz)));
            }
            if (tz > (int) Math.round(tr / 1.41421356D)) {
                tz = 0;
                tx = ++tr;
            }
            if (tr > radius) {
                clear = true;
            }
            if (scanned > Math.max(64, radius * radius * Math.max(2, radius) * 8)) {
                clear = true;
            }
        }

        if (clear) {
            if (mobGriefing && updated.getFloat(FIRE_CHANCE_TAG) > 0.01F) {
                igniteAfterSphericalExplosion(level, x0, y0, z0, radius, updated.getFloat(FIRE_CHANCE_TAG));
            }
            return new SphericalExplosionTickResult(updated, true, true, touched);
        }
        updated.putInt(TX_TAG, tx);
        updated.putInt(TY_TAG, ty);
        updated.putInt(TZ_TAG, tz);
        updated.putInt(TR_TAG, tr);
        return new SphericalExplosionTickResult(updated, true, false, touched);
    }

    private static void playSphericalExplosionSound(ServerLevel level, CompoundTag eventTag, int tick) {
        int x = eventTag.getInt(X_TAG);
        int y = eventTag.getInt(Y_TAG);
        int z = eventTag.getInt(Z_TAG);
        if (tick == 1) {
            level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS,
                    10.0F, level.random.nextFloat() * 0.5F + 0.5F);
        } else if (tick % 40 == 10) {
            level.playSound(null, x, y, z, ModSounds.SOUND_EXPLOSION.get(), SoundSource.BLOCKS,
                    Math.max(10.0F - tick / 40.0F, 1.0F), level.random.nextFloat() * 0.5F + 0.5F);
        }
    }

    private static void sendSphericalExplosionCameraShake(ServerLevel level, int x, int y, int z, int radius, int currentRing) {
        if (radius <= 20) {
            return;
        }
        float progress = Mth.clamp(1.0F - (float) currentRing / radius, 0.0F, 1.0F);
        NetworkHandler.sendToPlayersNear(
                level,
                x,
                y,
                z,
                32.0D * progress + radius,
                new CameraShakeMessage(80, 8.0F * progress));
    }

    private static Set<BlockPos> sphericalMirrorPositions(int x0, int y0, int z0, int tx, int ty, int tz) {
        Set<BlockPos> positions = new HashSet<>();
        positions.add(new BlockPos(x0 + tx, y0 + ty, z0 + tz));
        positions.add(new BlockPos(x0 - tx, y0 + ty, z0 + tz));
        positions.add(new BlockPos(x0 + tx, y0 + ty, z0 - tz));
        positions.add(new BlockPos(x0 - tx, y0 + ty, z0 - tz));
        positions.add(new BlockPos(x0 + tz, y0 + ty, z0 + tx));
        positions.add(new BlockPos(x0 + tz, y0 + ty, z0 - tx));
        positions.add(new BlockPos(x0 - tz, y0 + ty, z0 + tx));
        positions.add(new BlockPos(x0 - tz, y0 + ty, z0 - tx));
        positions.add(new BlockPos(x0 + tx - 1, y0 + ty, z0 + tz));
        positions.add(new BlockPos(x0 - tx + 1, y0 + ty, z0 + tz));
        positions.add(new BlockPos(x0 + tx - 1, y0 + ty, z0 - tz));
        positions.add(new BlockPos(x0 - tx + 1, y0 + ty, z0 - tz));
        positions.add(new BlockPos(x0 + tz, y0 + ty, z0 + tx - 1));
        positions.add(new BlockPos(x0 + tz, y0 + ty, z0 - tx + 1));
        positions.add(new BlockPos(x0 - tz, y0 + ty, z0 + tx - 1));
        positions.add(new BlockPos(x0 - tz, y0 + ty, z0 - tx + 1));
        return positions;
    }

    private static boolean canSphericalExplosionDestroy(ServerLevel level, BlockPos pos, BlockState state, int radius, boolean useResistance) {
        float blast = radius * (0.7F + level.random.nextFloat() * 0.6F);
        float resistance = state.getBlock().getExplosionResistance();
        if (!useResistance && resistance < 3600000.0F) {
            resistance = 0.0F;
        }
        blast -= (resistance + 0.3F) * 0.3F;
        return blast > 0.0F && state.getDestroySpeed(level, pos) >= 0.0F;
    }

    private static void maybeDropExplosionBlock(ServerLevel level, BlockPos pos, BlockState state, int radius) {
        float dropChance = radius <= 0 ? 1.0F : 0.5F / radius;
        if (level.random.nextFloat() <= dropChance) {
            Block.dropResources(state, level, pos);
        }
    }

    private static void spawnSphericalExplosionParticles(ServerLevel level, BlockPos pos) {
        level.sendParticles(ParticleTypes.EXPLOSION, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                1, 1.0D, 1.0D, 1.0D, 0.0D);
        level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                2, 1.0D, 1.0D, 1.0D, 0.02D);
    }

    private static void igniteAfterSphericalExplosion(ServerLevel level, int x0, int y0, int z0, int radius, float fireChance) {
        int attempts = Math.min(4096, Math.max(64, radius * radius * radius));
        for (int i = 0; i < attempts; i++) {
            if (level.random.nextFloat() > fireChance) {
                continue;
            }
            int x = x0 + level.random.nextInt(radius * 2 + 1) - radius;
            int y = y0 + level.random.nextInt(radius * 2 + 1) - radius;
            int z = z0 + level.random.nextInt(radius * 2 + 1) - radius;
            BlockPos pos = new BlockPos(x, y, z);
            if (pos.distSqr(new BlockPos(x0, y0, z0)) > radius * radius * 1.21D || !level.isEmptyBlock(pos)) {
                continue;
            }
            BlockState fire = BaseFireBlock.getState(level, pos);
            if (fire.canSurvive(level, pos)) {
                level.setBlock(pos, fire, 3);
            }
        }
    }

    private static MeteorShowerTickResult tickMeteorShower(MinecraftServer server, CompoundTag eventTag, long now) {
        ServerLevel level = levelForEvent(server, eventTag);
        if (level == null) {
            return new MeteorShowerTickResult(eventTag, false, true, 0);
        }
        CompoundTag updated = eventTag.copy();
        int radius = updated.getInt(RADIUS_TAG);
        int strikeInterval = updated.getInt(STRIKE_INTERVAL_TAG);
        int duration = updated.getInt(DURATION_TAG);
        if (radius <= 0 || strikeInterval <= 0 || duration <= 0) {
            return new MeteorShowerTickResult(updated, false, true, 0);
        }
        long start = updated.contains(START_TAG, Tag.TAG_LONG) ? updated.getLong(START_TAG) : now;
        int tick = updated.getInt(TICK_TAG) + 1;
        updated.putInt(TICK_TAG, tick);
        if (now > start + duration) {
            return new MeteorShowerTickResult(updated, true, true, 0);
        }

        int interval = Math.max(1, strikeInterval + level.random.nextInt(3) - 1);
        if (tick % interval != 0) {
            return new MeteorShowerTickResult(updated, true, false, 0);
        }

        Entity centerEntity = null;
        if (updated.getBoolean(ALL_PLAYERS_TAG) && !level.players().isEmpty()) {
            centerEntity = level.players().get(level.random.nextInt(level.players().size()));
        } else {
            centerEntity = trackedEntity(level, updated);
        }
        int centerX = centerEntity != null ? Mth.floor(centerEntity.getX()) : updated.getInt(X_TAG);
        int centerZ = centerEntity != null ? Mth.floor(centerEntity.getZ()) : updated.getInt(Z_TAG);
        int x = centerX + level.random.nextInt(radius * 2) - radius;
        int z = centerZ + level.random.nextInt(radius * 2) - radius;
        int y = Mth.clamp(250, level.getMinBuildHeight(), level.getMaxBuildHeight() - 1);
        level.setBlock(new BlockPos(x, y, z), ModBlocks.METEOR.get().defaultBlockState(), 3);
        return new MeteorShowerTickResult(updated, true, false, 1);
    }

    private static VillageSiegeTickResult tickVillageSiege(MinecraftServer server, CompoundTag eventTag) {
        ServerLevel level = levelForEvent(server, eventTag);
        if (level == null) {
            return new VillageSiegeTickResult(eventTag, false, true, 0);
        }
        CompoundTag updated = eventTag.copy();
        int radius = updated.getInt(RADIUS_TAG);
        int spawnInterval = updated.getInt(SPAWN_INTERVAL_TAG);
        if (radius <= 0 || spawnInterval <= 0) {
            return new VillageSiegeTickResult(updated, false, true, 0);
        }

        int tick = updated.getInt(TICK_TAG) + 1;
        updated.putInt(TICK_TAG, tick);
        if (tick == 1) {
            server.getPlayerList().broadcastSystemMessage(Component.translatable("chattext.specialevent.villagesiege"), false);
        }
        if (isLegacyDaytime(level)) {
            return new VillageSiegeTickResult(updated, true, true, 0);
        }

        double chance = level.random.nextDouble() * 0.6D + 0.5D;
        if (tick % spawnInterval != 0 || level.random.nextDouble() > chance) {
            return new VillageSiegeTickResult(updated, true, false, 0);
        }
        BlockPos center = new BlockPos(updated.getInt(X_TAG), updated.getInt(Y_TAG), updated.getInt(Z_TAG));
        VillagePoiHelper.Context village = VillagePoiHelper.findSiegeContext(level, center, radius).orElse(null);
        if (village == null) {
            return new VillageSiegeTickResult(updated, true, false, 0);
        }
        BlockPos spawnPos = VillagePoiHelper.findSiegeSpawnPos(level, village, radius, level.random).orElse(null);
        if (spawnPos == null) {
            return new VillageSiegeTickResult(updated, true, false, 0);
        }
        EntityType<?> type = villageSiegeMobType(updated, level);
        Entity entity = type.create(level);
        if (!(entity instanceof Mob mob)) {
            return new VillageSiegeTickResult(updated, true, false, 0);
        }
        mob.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D,
                level.random.nextFloat() * 360.0F, 0.0F);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.EVENT, null, null);
        return new VillageSiegeTickResult(updated, true, false, level.addFreshEntity(mob) ? 1 : 0);
    }

    private static boolean isLegacyDaytime(ServerLevel level) {
        long dayTime = Math.floorMod(level.getDayTime(), 24000L);
        return dayTime < 13000L || dayTime > 23000L;
    }

    private static EntityType<?> villageSiegeMobType(CompoundTag eventTag, ServerLevel level) {
        if (eventTag.contains(ENTITY_TYPE_TAG, Tag.TAG_STRING)) {
            EntityType<?> type = EntityType.byString(eventTag.getString(ENTITY_TYPE_TAG)).orElse(null);
            if (type != null) {
                return type;
            }
        }
        return level.random.nextBoolean() ? EntityType.ZOMBIE : EntityType.SKELETON;
    }

    private static SetBlocksTickResult tickSetBlocks(MinecraftServer server, CompoundTag eventTag, long now) {
        ServerLevel level = levelForEvent(server, eventTag);
        if (level == null || !eventTag.contains(BLOCKS_LIST_TAG, Tag.TAG_LIST)) {
            return new SetBlocksTickResult(eventTag, false, true, 0);
        }
        CompoundTag updated = eventTag.copy();
        ListTag blocks = updated.getList(BLOCKS_LIST_TAG, Tag.TAG_COMPOUND);
        if (blocks.isEmpty()) {
            return new SetBlocksTickResult(updated, false, true, 0);
        }

        int tick = updated.getInt(TICK_TAG) + 1;
        updated.putInt(TICK_TAG, tick);

        int budget = SET_BLOCKS_BUDGET_PER_TICK;
        int touched = 0;
        int addIndex = Mth.clamp(updated.getInt(ADD_INDEX_TAG), 0, blocks.size());
        boolean particles = !updated.contains(PARTICLES_TAG, Tag.TAG_BYTE) || updated.getBoolean(PARTICLES_TAG);
        boolean sounds = !updated.contains(SOUND_TAG, Tag.TAG_BYTE) || updated.getBoolean(SOUND_TAG);
        boolean playedSound = false;
        while (addIndex < blocks.size() && budget > 0) {
            CompoundTag blockTag = blocks.getCompound(addIndex);
            BlockPos pos = blockPos(blockTag);
            BlockState state = blockState(level, blockTag);
            if (!state.isAir() || !level.isEmptyBlock(pos)) {
                level.getChunk(pos);
                if (sounds && !playedSound && tick % 5 == 1) {
                    playPlaceSound(level, pos, state);
                    playedSound = true;
                }
                if (particles) {
                    level.levelEvent(2001, pos, Block.getId(state));
                }
                if (level.setBlock(pos, state, 3)) {
                    touched++;
                }
            }
            addIndex++;
            budget--;
        }
        updated.putInt(ADD_INDEX_TAG, addIndex);
        if (budget == 0) {
            return new SetBlocksTickResult(updated, true, false, touched);
        }

        int lifespan = Math.max(0, updated.getInt(LIFESPAN_TAG));
        long start = updated.contains(START_TAG, Tag.TAG_LONG) ? updated.getLong(START_TAG) : now;
        boolean removeDue = lifespan > 0 && now > start + lifespan;
        if (removeDue) {
            int removeIndex = Mth.clamp(updated.getInt(REMOVE_INDEX_TAG), 0, blocks.size());
            while (removeIndex < blocks.size() && budget > 0) {
                CompoundTag blockTag = blocks.getCompound(removeIndex);
                BlockPos pos = blockPos(blockTag);
                if (!level.isEmptyBlock(pos)) {
                    level.getChunk(pos);
                    if (particles) {
                        level.levelEvent(2001, pos, Block.getId(level.getBlockState(pos)));
                    }
                    if (level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3)) {
                        touched++;
                    }
                }
                removeIndex++;
                budget--;
            }
            updated.putInt(REMOVE_INDEX_TAG, removeIndex);
            if (removeIndex < blocks.size()) {
                return new SetBlocksTickResult(updated, true, false, touched);
            }
        }

        boolean clear = lifespan == 0 && addIndex >= blocks.size() || removeDue && updated.getInt(REMOVE_INDEX_TAG) >= blocks.size();
        return new SetBlocksTickResult(updated, true, clear, touched);
    }

    private static BlockPos blockPos(CompoundTag blockTag) {
        return new BlockPos(blockTag.getInt("x"), blockTag.getInt("y"), blockTag.getInt("z"));
    }

    private static BlockState blockState(ServerLevel level, CompoundTag blockTag) {
        if (blockTag.contains(BLOCK_STATE_TAG, Tag.TAG_COMPOUND)) {
            return NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK), blockTag.getCompound(BLOCK_STATE_TAG));
        }
        if (blockTag.contains(LEGACY_BLOCK_STATE_TAG, Tag.TAG_INT)) {
            return Block.stateById(blockTag.getInt(LEGACY_BLOCK_STATE_TAG));
        }
        return Blocks.AIR.defaultBlockState();
    }

    private static void playPlaceSound(ServerLevel level, BlockPos pos, BlockState state) {
        SoundType soundType = state.getSoundType(level, pos, null);
        level.playSound(null, pos, soundType.getPlaceSound(), SoundSource.BLOCKS, soundType.getVolume() * 0.5F, soundType.getPitch());
    }

    private static boolean executeVanillaExplosion(MinecraftServer server, CompoundTag eventTag) {
        ServerLevel level = levelForEvent(server, eventTag);
        if (level == null) {
            return false;
        }
        float strength = eventTag.contains(STRENGTH_TAG, Tag.TAG_FLOAT) ? eventTag.getFloat(STRENGTH_TAG) : 0.0F;
        if (strength <= 0.0F) {
            return false;
        }
        Entity trackedEntity = trackedEntity(level, eventTag);
        double x = trackedEntity != null ? trackedEntity.getX() : eventTag.getInt(X_TAG);
        double y = trackedEntity != null ? trackedEntity.getY() : eventTag.getInt(Y_TAG);
        double z = trackedEntity != null ? trackedEntity.getZ() : eventTag.getInt(Z_TAG);
        boolean flames = eventTag.getBoolean(FLAMING_TAG);
        boolean damagesTerrain = eventTag.getBoolean(DAMAGES_TERRAIN_TAG);
        Level.ExplosionInteraction interaction = damagesTerrain ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE;
        level.explode(null, x, y, z, strength, flames, interaction);
        return true;
    }

    private static boolean executeDelayedCallback(MinecraftServer server, CompoundTag eventTag) {
        ServerLevel level = levelForEvent(server, eventTag);
        if (level == null || !eventTag.contains(CALLBACK_ID_TAG, Tag.TAG_INT)) {
            return false;
        }
        int callbackId = eventTag.getInt(CALLBACK_ID_TAG);
        if (callbackId == SMOKE_BOMB_CALLBACK_ID) {
            spawnDelayedCallbackSmoke(level, eventTag.getInt(X_TAG), eventTag.getInt(Y_TAG), eventTag.getInt(Z_TAG));
            return true;
        }
        return false;
    }

    private static void spawnDelayedCallbackSmoke(ServerLevel level, int x, int y, int z) {
        level.sendParticles(
                ModParticleTypes.options(NarutoParticleKind.SMOKE_COLORED,
                        0xFF101010, 8, 40 + level.random.nextInt(21), 0, -1, 1),
                x,
                y,
                z,
                200,
                2.0D,
                1.0D,
                2.0D,
                0.02D);
    }

    private static Entity trackedEntity(ServerLevel level, CompoundTag eventTag) {
        if (!eventTag.hasUUID(ENTITY_UUID_TAG)) {
            return null;
        }
        UUID uuid = eventTag.getUUID(ENTITY_UUID_TAG);
        Entity entity = level.getEntity(uuid);
        return entity != null && entity.isAlive() ? entity : null;
    }

    private static boolean isDue(CompoundTag eventTag, long now) {
        return !eventTag.contains(START_TAG, Tag.TAG_LONG) || eventTag.getLong(START_TAG) <= now;
    }

    private static void appendEvent(MinecraftServer server, CompoundTag eventTag) {
        NarutomodSavedData.SpecialEvents data = NarutomodSavedData.specialEvents(server);
        CompoundTag root = data.data().copy();
        ListTag events = copyEvents(root);
        events.add(eventTag);
        root.put(EVENTS_TAG, events);
        data.replace(root);
    }

    private static ListTag events(MinecraftServer server) {
        if (server == null) {
            return new ListTag();
        }
        return copyEvents(NarutomodSavedData.specialEvents(server).data());
    }

    private static ListTag copyEvents(CompoundTag root) {
        ListTag copy = new ListTag();
        if (!root.contains(EVENTS_TAG, Tag.TAG_LIST)) {
            return copy;
        }
        ListTag events = root.getList(EVENTS_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < events.size(); i++) {
            copy.add(events.getCompound(i).copy());
        }
        return copy;
    }

    private static ServerLevel levelForEvent(MinecraftServer server, CompoundTag eventTag) {
        ResourceLocation dimensionId = ResourceLocation.tryParse(eventTag.getString(DIMENSION_TAG));
        if (dimensionId != null) {
            ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, dimensionId));
            if (level != null) {
                return level;
            }
        }
        if (eventTag.contains(LEGACY_WORLD_TAG, Tag.TAG_INT)) {
            int legacyDimension = eventTag.getInt(LEGACY_WORLD_TAG);
            if (legacyDimension == -1) {
                return server.getLevel(Level.NETHER);
            }
            if (legacyDimension == 1) {
                return server.getLevel(Level.END);
            }
        }
        return server.overworld();
    }

    private static long eventTime(MinecraftServer server) {
        return server.overworld().getGameTime();
    }

    private static int legacyDimensionId(ServerLevel level) {
        ResourceKey<Level> dimension = level.dimension();
        if (dimension == Level.NETHER) {
            return -1;
        }
        if (dimension == Level.END) {
            return 1;
        }
        return 0;
    }

    private record SetBlocksTickResult(CompoundTag eventTag, boolean changed, boolean clear, int touchedBlocks) {
    }

    private record CylindricalExplosionTickResult(CompoundTag eventTag, boolean changed, boolean clear, int touchedBlocks) {
    }

    private record SphericalExplosionTickResult(CompoundTag eventTag, boolean changed, boolean clear, int touchedBlocks) {
    }

    private record MeteorShowerTickResult(CompoundTag eventTag, boolean changed, boolean clear, int spawnedMeteors) {
    }

    private record VillageSiegeTickResult(CompoundTag eventTag, boolean changed, boolean clear, int spawnedMobs) {
    }
}
