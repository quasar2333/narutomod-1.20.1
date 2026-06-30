package net.narutomod.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.narutomod.Chakra;
import net.narutomod.NarutomodMod;
import net.narutomod.NarutomodModVariables;
import net.narutomod.NarutomodSavedData;
import net.narutomod.item.AdvancedNatureJutsuItem;
import net.narutomod.item.BijuCloakItem;
import net.narutomod.item.DotonItem;
import net.narutomod.item.FutonItem;
import net.narutomod.item.JutsuItem;
import net.narutomod.item.KatonItem;
import net.narutomod.item.SusanooPowerIncreaseHandler;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModItems;

@Mod.EventBusSubscriber(modid = NarutomodMod.MODID)
public final class BijuManager {
    public static final int MIN_TAILS = 1;
    public static final int MAX_TAILS = 10;
    public static final String JINCHURIKI_UUID_KEY = "JinchurikiUUID";
    public static final String JINCHURIKI_NAME_KEY = "JinchurikiName";
    public static final String CLOAK_XP_KEY = "JinchurikiCloakXp";
    public static final String CLOAK_COOLDOWN_KEY = "JinchurikiCloakCD";
    public static final String CLOAK_LEVEL_KEY = "JinchurikiCloakLevel";
    public static final String SPAWNED_KEY = "spawned";
    public static final String DIMENSION_KEY = "dimension";
    public static final String ENTITY_UUID_KEY = "EntityUUID";
    public static final String ENTITY_TAG_KEY = "EntityTag";
    public static final String GEDO_SEALED_KEY = "GedoSealed";
    private static final double NATURAL_GRANT_BASE_CHANCE = 0.001D;
    private static final double OLD_RANDOM_POOL_WITH_BIJU = 110.0D;
    private static final int[] ZERO_CLOAK_XP = {0, 0, 0};
    private static final Map<UUID, TailBeastBallCharge> TAIL_BEAST_BALL_CHARGE = new HashMap<>();

    private BijuManager() {
    }

    public static boolean isValidTailCount(int tails) {
        return tails >= MIN_TAILS && tails <= MAX_TAILS;
    }

    public static boolean isJinchuriki(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            return getAssignedTail(serverPlayer) > 0;
        }
        return NarutomodModVariables.get(player).getInt(NarutomodModVariables.JINCHURIKI_TAILS) > 0;
    }

    public static boolean isJinchurikiOf(Player player, int tails) {
        if (!isValidTailCount(tails)) {
            return false;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            return getAssignedTail(serverPlayer) == tails;
        }
        return NarutomodModVariables.get(player).getInt(NarutomodModVariables.JINCHURIKI_TAILS) == tails;
    }

    public static int getAssignedTail(ServerPlayer player) {
        return getAssignedTail(player.getServer(), player.getUUID());
    }

    public static int getAssignedTail(MinecraftServer server, UUID playerUuid) {
        for (int tails = MIN_TAILS; tails <= MAX_TAILS; tails++) {
            UUID holder = getJinchurikiUuid(server, tails);
            if (playerUuid.equals(holder)) {
                return tails;
            }
        }
        return 0;
    }

    public static boolean setPlayerAsJinchuriki(ServerPlayer player, int tails) {
        if (!isValidTailCount(tails)) {
            return false;
        }
        MinecraftServer server = player.getServer();
        revokePlayer(player, false);
        revokeByTail(server, tails, true);
        discardLoadedTailedBeast(server, tails);
        clearSavedSpawnedTailedBeast(server, tails);
        if (tails == MAX_TAILS) {
            discardLoadedTenTailsExcept(server, null);
        }
        if (tails <= 9) {
            clearGedoSealedTail(server, tails);
        }

        NarutomodSavedData.TailedBeast data = data(server, tails);
        CompoundTag tag = data.data();
        tag.putUUID(JINCHURIKI_UUID_KEY, player.getUUID());
        tag.putString(JINCHURIKI_NAME_KEY, player.getScoreboardName());
        tag.putIntArray(CLOAK_XP_KEY, ZERO_CLOAK_XP);
        tag.putLong(CLOAK_COOLDOWN_KEY, 0L);
        tag.putInt(CLOAK_LEVEL_KEY, 0);
        data.setDirty();
        setSyncedTail(player, tails);
        grantTailSideEffects(player, tails);
        return true;
    }

    public static boolean sealTailedBeastIntoPlayer(TailedBeastEntity beast, ServerPlayer player) {
        if (!(beast.level() instanceof ServerLevel level) || !beast.isAlive() || player.isSpectator() || !player.isAlive()) {
            return false;
        }
        int tails = beast.getTailCount();
        MinecraftServer server = level.getServer();
        if (tails < MIN_TAILS
                || tails > 9
                || getAssignedTail(player) > 0
                || getJinchurikiUuid(server, tails) != null) {
            return false;
        }
        Component beastName = beast.getDisplayName();
        if (!setPlayerAsJinchuriki(player, tails)) {
            return false;
        }
        clearSavedSpawnedTailedBeast(server, tails);
        beast.discard();
        server.getPlayerList().broadcastSystemMessage(
                Component.translatable("chattext.tentails.sealedintoplayer", beastName, player.getScoreboardName()),
                false);
        return true;
    }

    public static boolean sealTailedBeastIntoGedo(TailedBeastEntity beast) {
        if (!(beast.level() instanceof ServerLevel level) || !beast.isAlive()) {
            return false;
        }
        int tails = beast.getTailCount();
        MinecraftServer server = level.getServer();
        if (tails < MIN_TAILS
                || tails > 9
                || isGedoSealedTail(server, tails)
                || getJinchurikiUuid(server, tails) != null) {
            return false;
        }
        Component beastName = beast.getDisplayName();
        if (!setGedoSealedTail(server, tails, true)) {
            return false;
        }
        if (beast.isAlive()) {
            beast.discard();
        }
        server.getPlayerList().broadcastSystemMessage(
                Component.literal("Gedo Statue absorbed " + beastName.getString() + "."),
                false);
        return true;
    }

    public static boolean sealTenTailsIntoPlayer(TenTailsEntity beast, ServerPlayer player) {
        if (!(beast.level() instanceof ServerLevel level) || !beast.isAlive() || player.isSpectator() || !player.isAlive()) {
            return false;
        }
        MinecraftServer server = level.getServer();
        if (getAssignedTail(player) > 0 || getJinchurikiUuid(server, MAX_TAILS) != null) {
            return false;
        }
        Component beastName = beast.getDisplayName();
        if (!setPlayerAsJinchuriki(player, MAX_TAILS)) {
            return false;
        }
        clearSavedSpawnedTailedBeast(server, MAX_TAILS);
        beast.discard();
        server.getPlayerList().broadcastSystemMessage(
                Component.translatable("chattext.tentails.sealedintoplayer", beastName, player.getScoreboardName()),
                false);
        return true;
    }

    public static boolean revokePlayer(ServerPlayer player) {
        return revokePlayer(player, true);
    }

    public static boolean revokeByTail(MinecraftServer server, int tails) {
        return revokeByTail(server, tails, true);
    }

    public static int revokeAll(MinecraftServer server) {
        int revoked = 0;
        for (int tails = MIN_TAILS; tails <= MAX_TAILS; tails++) {
            if (revokeByTail(server, tails, true)) {
                revoked++;
            }
        }
        return revoked;
    }

    public static int availableTailedBeasts(MinecraftServer server) {
        int available = 0;
        for (int tails = MIN_TAILS; tails <= 9; tails++) {
            if (isOrdinaryBijuAvailable(server, tails)) {
                available++;
            }
        }
        return available;
    }

    public static int getRandomAvailableTailedBeast(MinecraftServer server) {
        List<Integer> available = new ArrayList<>();
        for (int tails = MIN_TAILS; tails <= 9; tails++) {
            if (isOrdinaryBijuAvailable(server, tails)) {
                available.add(tails);
            }
        }
        if (available.isEmpty()) {
            return 0;
        }
        return available.get(server.overworld().random.nextInt(available.size()));
    }

    public static int tryNaturalJinchurikiGrant(ServerPlayer player, boolean force) {
        if (force ? !canForceJinchurikiGrant(player) : !isNaturalJinchurikiCandidate(player)) {
            return 0;
        }
        if (!force) {
            if (player.getRandom().nextDouble() > NATURAL_GRANT_BASE_CHANCE || !fallsThroughOldNaturalAbilityRoll(player)) {
                return 0;
            }
        }
        int tails = getRandomAvailableTailedBeast(player.getServer());
        if (tails <= 0 || !setPlayerAsJinchuriki(player, tails)) {
            return 0;
        }
        player.getServer().getPlayerList().broadcastSystemMessage(
                Component.translatable(
                        "chattext.biju.playerisjinchuriki",
                        player.getScoreboardName(),
                        Component.translatable("entity.narutomod." + displayName(tails))),
                false);
        return tails;
    }

    public static int assignedJinchurikiCount(MinecraftServer server) {
        int assigned = 0;
        for (int tails = MIN_TAILS; tails <= MAX_TAILS; tails++) {
            if (getJinchurikiUuid(server, tails) != null) {
                assigned++;
            }
        }
        return assigned;
    }

    public static int savedSpawnedTailedBeastCount(MinecraftServer server) {
        int spawned = 0;
        for (int tails = MIN_TAILS; tails <= MAX_TAILS; tails++) {
            if (hasSavedSpawnedTailedBeast(server, tails)) {
                spawned++;
            }
        }
        return spawned;
    }

    public static boolean hasSavedSpawnedTailedBeast(MinecraftServer server, int tails) {
        if (!isValidTailCount(tails)) {
            return false;
        }
        return data(server, tails).data().getBoolean(SPAWNED_KEY);
    }

    public static boolean isGedoSealedTail(MinecraftServer server, int tails) {
        return tails >= MIN_TAILS && tails <= 9 && data(server, tails).data().getBoolean(GEDO_SEALED_KEY);
    }

    public static boolean canSealTailedBeastIntoGedo(TailedBeastEntity beast) {
        if (!(beast.level() instanceof ServerLevel level) || !beast.isAlive()) {
            return false;
        }
        int tails = beast.getTailCount();
        return tails >= MIN_TAILS
                && tails <= 9
                && getJinchurikiUuid(level.getServer(), tails) == null
                && !isGedoSealedTail(level.getServer(), tails);
    }

    public static boolean setGedoSealedTail(MinecraftServer server, int tails, boolean sealed) {
        if (tails < MIN_TAILS || tails > 9) {
            return false;
        }
        if (!sealed) {
            return clearGedoSealedTail(server, tails);
        }
        NarutomodSavedData.TailedBeast data = data(server, tails);
        CompoundTag tag = data.data();
        if (tag.getBoolean(GEDO_SEALED_KEY)) {
            return false;
        }
        revokeByTail(server, tails, true);
        discardLoadedTailedBeast(server, tails);
        clearSavedSpawnedTailedBeast(server, tails);
        tag.putBoolean(GEDO_SEALED_KEY, true);
        data.setDirty();
        return true;
    }

    public static int clearGedoSealedTails(MinecraftServer server) {
        int cleared = 0;
        for (int tails = MIN_TAILS; tails <= 9; tails++) {
            if (clearGedoSealedTail(server, tails)) {
                cleared++;
            }
        }
        return cleared;
    }

    public static int countGedoSealedTails(MinecraftServer server) {
        int count = 0;
        for (int tails = MIN_TAILS; tails <= 9; tails++) {
            if (isGedoSealedTail(server, tails)) {
                count++;
            }
        }
        return count;
    }

    public static boolean canActivateTenTailsFromGedo(MinecraftServer server) {
        return countGedoSealedTails(server) >= 9
                && getJinchurikiUuid(server, MAX_TAILS) == null
                && !hasSavedSpawnedTailedBeast(server, MAX_TAILS)
                && countLoadedTenTails(server) == 0;
    }

    public static String listGedoSealedTails(MinecraftServer server) {
        List<String> sealed = new ArrayList<>();
        for (int tails = MIN_TAILS; tails <= 9; tails++) {
            if (isGedoSealedTail(server, tails)) {
                sealed.add(tails + ":" + displayName(tails));
            }
        }
        return sealed.isEmpty() ? "none" : String.join(",", sealed);
    }

    public static boolean saveSpawnedTailedBeast(TailedBeastEntity beast) {
        if (!(beast.level() instanceof ServerLevel level) || !beast.isAlive()) {
            return false;
        }
        int tails = beast.getTailCount();
        if (tails < MIN_TAILS || tails > 9 || isGedoSealedTail(level.getServer(), tails)) {
            return false;
        }
        NarutomodSavedData.TailedBeast data = data(level.getServer(), tails);
        CompoundTag tag = data.data();
        CompoundTag entityTag = new CompoundTag();
        beast.saveWithoutId(entityTag);
        tag.putBoolean(SPAWNED_KEY, true);
        tag.putString(DIMENSION_KEY, level.dimension().location().toString());
        tag.putUUID(ENTITY_UUID_KEY, beast.getUUID());
        tag.put(ENTITY_TAG_KEY, entityTag);
        data.setDirty();
        return true;
    }

    public static boolean saveSpawnedTenTails(TenTailsEntity beast) {
        if (!(beast.level() instanceof ServerLevel level) || !beast.isAlive()) {
            return false;
        }
        NarutomodSavedData.TailedBeast data = data(level.getServer(), MAX_TAILS);
        CompoundTag tag = data.data();
        CompoundTag entityTag = new CompoundTag();
        beast.saveWithoutId(entityTag);
        tag.putBoolean(SPAWNED_KEY, true);
        tag.putString(DIMENSION_KEY, level.dimension().location().toString());
        tag.putUUID(ENTITY_UUID_KEY, beast.getUUID());
        tag.put(ENTITY_TAG_KEY, entityTag);
        data.setDirty();
        discardLoadedTenTailsExcept(level.getServer(), beast.getUUID());
        return true;
    }

    public static boolean clearSpawnedTailedBeast(TailedBeastEntity beast) {
        if (!(beast.level() instanceof ServerLevel level)) {
            return false;
        }
        int tails = beast.getTailCount();
        if (tails < MIN_TAILS || tails > 9) {
            return false;
        }
        CompoundTag tag = data(level.getServer(), tails).data();
        if (tag.hasUUID(ENTITY_UUID_KEY) && !beast.getUUID().equals(tag.getUUID(ENTITY_UUID_KEY))) {
            return false;
        }
        return clearSavedSpawnedTailedBeast(level.getServer(), tails);
    }

    public static boolean clearSpawnedTenTails(TenTailsEntity beast) {
        if (!(beast.level() instanceof ServerLevel level)) {
            return false;
        }
        CompoundTag tag = data(level.getServer(), MAX_TAILS).data();
        if (tag.hasUUID(ENTITY_UUID_KEY) && !beast.getUUID().equals(tag.getUUID(ENTITY_UUID_KEY))) {
            return false;
        }
        return clearSavedSpawnedTailedBeast(level.getServer(), MAX_TAILS);
    }

    public static boolean clearSavedSpawnedTailedBeast(MinecraftServer server, int tails) {
        if (!isValidTailCount(tails)) {
            return false;
        }
        NarutomodSavedData.TailedBeast data = data(server, tails);
        CompoundTag tag = data.data();
        boolean changed = tag.contains(SPAWNED_KEY)
                || tag.contains(DIMENSION_KEY)
                || tag.contains(ENTITY_UUID_KEY)
                || tag.contains(ENTITY_TAG_KEY, Tag.TAG_COMPOUND);
        if (changed) {
            tag.remove(SPAWNED_KEY);
            tag.remove(DIMENSION_KEY);
            tag.remove(ENTITY_UUID_KEY);
            tag.remove(ENTITY_TAG_KEY);
            data.setDirty();
        }
        return changed;
    }

    public static int restoreSpawnedTailedBeasts(MinecraftServer server) {
        int restored = 0;
        for (int tails = MIN_TAILS; tails <= MAX_TAILS; tails++) {
            if (restoreSpawnedTailedBeast(server, tails)) {
                restored++;
            }
        }
        return restored;
    }

    public static boolean restoreSpawnedTailedBeast(MinecraftServer server, int tails) {
        if (tails == MAX_TAILS) {
            return restoreSpawnedTenTails(server);
        }
        if (isGedoSealedTail(server, tails)) {
            clearSavedSpawnedTailedBeast(server, tails);
            return false;
        }
        if (!hasSavedSpawnedTailedBeast(server, tails)) {
            return false;
        }
        CompoundTag tag = data(server, tails).data();
        UUID savedUuid = tag.hasUUID(ENTITY_UUID_KEY) ? tag.getUUID(ENTITY_UUID_KEY) : null;
        TailedBeastEntity loaded = findLoadedTailedBeast(server, tails, savedUuid);
        if (loaded != null) {
            saveSpawnedTailedBeast(loaded);
            return false;
        }
        ResourceLocation dimensionId = ResourceLocation.tryParse(tag.getString(DIMENSION_KEY));
        if (dimensionId == null) {
            return false;
        }
        ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, dimensionId);
        ServerLevel level = server.getLevel(dimensionKey);
        if (level == null || !tag.contains(ENTITY_TAG_KEY, Tag.TAG_COMPOUND)) {
            return false;
        }
        TailedBeastEntity beast = TailedBeastEntity.Variant.byTailCount(tails).entityType().get().create(level);
        if (beast == null) {
            return false;
        }
        beast.load(tag.getCompound(ENTITY_TAG_KEY));
        if (savedUuid != null) {
            beast.setUUID(savedUuid);
        }
        if (!level.addFreshEntity(beast)) {
            beast.discard();
            return false;
        }
        saveSpawnedTailedBeast(beast);
        return true;
    }

    public static boolean restoreSpawnedTenTails(MinecraftServer server) {
        if (!hasSavedSpawnedTailedBeast(server, MAX_TAILS)) {
            return false;
        }
        CompoundTag tag = data(server, MAX_TAILS).data();
        UUID savedUuid = tag.hasUUID(ENTITY_UUID_KEY) ? tag.getUUID(ENTITY_UUID_KEY) : null;
        TenTailsEntity loaded = findLoadedTenTails(server, savedUuid);
        if (loaded != null) {
            saveSpawnedTenTails(loaded);
            return false;
        }
        TenTailsEntity existing = findAnyLoadedTenTails(server, null);
        if (existing != null) {
            saveSpawnedTenTails(existing);
            return false;
        }
        ResourceLocation dimensionId = ResourceLocation.tryParse(tag.getString(DIMENSION_KEY));
        if (dimensionId == null) {
            return false;
        }
        ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, dimensionId);
        ServerLevel level = server.getLevel(dimensionKey);
        if (level == null || !tag.contains(ENTITY_TAG_KEY, Tag.TAG_COMPOUND)) {
            return false;
        }
        TenTailsEntity beast = ModEntityTypes.TEN_TAILS.get().create(level);
        if (beast == null) {
            return false;
        }
        beast.load(tag.getCompound(ENTITY_TAG_KEY));
        if (savedUuid != null) {
            beast.setUUID(savedUuid);
        }
        if (!level.addFreshEntity(beast)) {
            beast.discard();
            return false;
        }
        saveSpawnedTenTails(beast);
        return true;
    }

    public static int countLoadedTenTails(MinecraftServer server) {
        int count = 0;
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof TenTailsEntity beast && beast.isAlive()) {
                    count++;
                }
            }
        }
        return count;
    }

    public static int discardLoadedTenTailsExcept(MinecraftServer server, @Nullable UUID keepUuid) {
        int discarded = 0;
        for (ServerLevel level : server.getAllLevels()) {
            List<TenTailsEntity> loaded = new ArrayList<>();
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof TenTailsEntity beast
                        && beast.isAlive()
                        && (keepUuid == null || !keepUuid.equals(beast.getUUID()))) {
                    loaded.add(beast);
                }
            }
            for (TenTailsEntity beast : loaded) {
                beast.discard();
                discarded++;
            }
        }
        return discarded;
    }

    public static List<String> listSpawnedTailedBeasts(MinecraftServer server) {
        List<String> rows = new ArrayList<>();
        for (int tails = MIN_TAILS; tails <= MAX_TAILS; tails++) {
            CompoundTag tag = data(server, tails).data();
            String dimension = tag.getString(DIMENSION_KEY);
            String uuid = tag.hasUUID(ENTITY_UUID_KEY) ? tag.getUUID(ENTITY_UUID_KEY).toString() : "none";
            rows.add(tails + ":" + displayName(tails)
                    + "/spawned=" + tag.getBoolean(SPAWNED_KEY)
                    + "/gedo_sealed=" + (tails <= 9 && tag.getBoolean(GEDO_SEALED_KEY))
                    + "/dim=" + (dimension.isEmpty() ? "none" : dimension)
                    + "/uuid=" + uuid);
        }
        return rows;
    }

    public static void resolvePlayer(ServerPlayer player) {
        int assignedTail = getAssignedTail(player);
        int syncedTail = NarutomodModVariables.get(player).getInt(NarutomodModVariables.JINCHURIKI_TAILS);
        if (assignedTail > 0) {
            NarutomodSavedData.TailedBeast data = data(player.getServer(), assignedTail);
            data.data().putString(JINCHURIKI_NAME_KEY, player.getScoreboardName());
            data.setDirty();
            grantTailSideEffects(player, assignedTail);
        }
        if (assignedTail != syncedTail) {
            setSyncedTail(player, assignedTail);
        }
    }

    @Nullable
    public static UUID getJinchurikiUuid(MinecraftServer server, int tails) {
        if (!isValidTailCount(tails)) {
            return null;
        }
        CompoundTag tag = data(server, tails).data();
        return tag.hasUUID(JINCHURIKI_UUID_KEY) ? tag.getUUID(JINCHURIKI_UUID_KEY) : null;
    }

    public static String getJinchurikiName(MinecraftServer server, int tails) {
        if (!isValidTailCount(tails)) {
            return "";
        }
        CompoundTag tag = data(server, tails).data();
        return tag.getString(JINCHURIKI_NAME_KEY);
    }

    public static int getCloakLevel(MinecraftServer server, int tails) {
        return isValidTailCount(tails) ? data(server, tails).data().getInt(CLOAK_LEVEL_KEY) : 0;
    }

    public static int[] getCloakXp(MinecraftServer server, int tails) {
        if (!isValidTailCount(tails)) {
            return ZERO_CLOAK_XP.clone();
        }
        int[] xp = data(server, tails).data().getIntArray(CLOAK_XP_KEY);
        if (xp.length == 3) {
            return xp;
        }
        return ZERO_CLOAK_XP.clone();
    }

    public static long getCloakCooldown(MinecraftServer server, int tails) {
        return isValidTailCount(tails) ? data(server, tails).data().getLong(CLOAK_COOLDOWN_KEY) : 0L;
    }

    public static int getCloakLevel(ServerPlayer player) {
        int tails = getAssignedTail(player);
        return tails > 0 ? getCloakLevel(player.getServer(), tails) : 0;
    }

    public static int getCurrentCloakXp(ServerPlayer player) {
        int tails = getAssignedTail(player);
        int level = getCloakLevel(player);
        return tails > 0 && level >= 1 && level <= 3 ? getCloakXp(player.getServer(), tails)[level - 1] : 0;
    }

    public static void setCloakXp(ServerPlayer player, int level, int xp) {
        int tails = getAssignedTail(player);
        if (tails <= 0 || level < 1 || level > 3) {
            return;
        }
        setCloakXp(player.getServer(), tails, level, xp);
    }

    public static void addCloakXp(ServerPlayer player, int xp) {
        int level = getCloakLevel(player);
        if (level < 1 || level > 3) {
            return;
        }
        setCloakXp(player, level, getCurrentCloakXp(player) + Math.max(xp, 0));
    }

    public static boolean toggleBijuCloak(ServerPlayer player) {
        int tails = getAssignedTail(player);
        if (tails <= 0) {
            player.displayClientMessage(Component.literal("No tailed beast is sealed in this player."), true);
            return false;
        }
        int cloakLevel = getCloakLevel(player.getServer(), tails);
        if (cloakLevel <= 0) {
            long now = player.level().getGameTime();
            long cooldown = getCloakCooldown(player.getServer(), tails);
            if (now < cooldown && !player.isCreative()) {
                player.displayClientMessage(Component.literal("Biju cloak cooldown " + ((cooldown - now) / 20L) + "s"), true);
                return false;
            }
            SusanooPowerIncreaseHandler.deactivate(player, false);
            setCloakCooldown(player.getServer(), tails, now);
            setCloakLevel(player.getServer(), tails, 1);
            BijuCloakItem.setWearingTicks(player, 0);
            Chakra.pathway(player).consume(-5000.0D, true);
            equipCloakSet(player, tails, 1);
            return true;
        }

        saveAndResetWearingTicks(player, cloakLevel);
        setCloakLevel(player.getServer(), tails, 0);
        removeCloakSet(player);
        discardLoadedTailedBeast(player.getServer(), tails);
        clearSavedSpawnedTailedBeast(player.getServer(), tails);
        Chakra.Pathway pathway = Chakra.pathway(player);
        if (pathway.getAmount() > pathway.getMax()) {
            pathway.consume(pathway.getAmount() - pathway.getMax());
        }
        return true;
    }

    public static int increaseCloakLevel(ServerPlayer player) {
        int tails = getAssignedTail(player);
        int cloakLevel = getCloakLevel(player);
        if (tails <= 0 || cloakLevel <= 0) {
            return 0;
        }
        if (cloakLevel < 3) {
            int xp = getCloakXp(player.getServer(), tails)[cloakLevel - 1];
            boolean canIncrease = cloakLevel == 1 && xp > 3600 || cloakLevel == 2 && xp > 4800;
            if (!canIncrease && !player.isCreative()) {
                player.displayClientMessage(Component.literal("Biju cloak XP " + xp + "/" + (cloakLevel == 1 ? 3600 : 4800)), true);
                return cloakLevel;
            }
            saveAndResetWearingTicks(player, cloakLevel);
            Chakra.pathway(player).consume(-10000.0D, true);
            cloakLevel++;
            setCloakLevel(player.getServer(), tails, cloakLevel);
            equipCloakSet(player, tails, cloakLevel);
        }
        if (cloakLevel == 3 && tails <= 9) {
            TailedBeastEntity beast = TailedBeastEntity.spawnFrom(player, TailedBeastEntity.Variant.byTailCount(tails), true);
            if (beast != null) {
                beast.setKcm(tails == 9);
                beast.setLifeSpan(getCloakXp(player.getServer(), tails)[2] * 5 + 200);
                saveSpawnedTailedBeast(beast);
                removeCloakSet(player);
            }
        } else if (cloakLevel == 3 && tails == MAX_TAILS) {
            TenTailsEntity beast = TenTailsEntity.spawnFrom(player, true);
            if (beast != null) {
                beast.setLifeSpan(getCloakXp(player.getServer(), tails)[2] * 5 + 200);
                saveSpawnedTenTails(beast);
                removeCloakSet(player);
            }
        }
        return cloakLevel;
    }

    public static boolean handleSpecialJutsuKey(ServerPlayer player, int key, boolean pressed) {
        if (player.isSpectator() || !player.level().isLoaded(player.blockPosition())) {
            return false;
        }
        if (key == 2) {
            NarutomodModVariables.get(player).putBoolean(NarutomodModVariables.JUTSU_KEY_2_PRESSED, pressed);
            NarutomodModVariables.sync(player);
            if (!pressed && isJinchuriki(player)) {
                return toggleBijuCloak(player);
            }
            return false;
        }
        if (key == 3) {
            int cloakLevel = getCloakLevel(player);
            if (cloakLevel == 2) {
                return handleTailBeastBallCharge(player, pressed);
            }
            if (cloakLevel == 3 && !pressed) {
                return fireMountedTailedBeastBall(player);
            }
        }
        return false;
    }

    public static List<String> listAssignments(MinecraftServer server) {
        List<String> rows = new ArrayList<>();
        for (int tails = MIN_TAILS; tails <= MAX_TAILS; tails++) {
            UUID uuid = getJinchurikiUuid(server, tails);
            String holder = uuid == null ? "unsealed" : holderName(server, tails, uuid);
            int[] xp = getCloakXp(server, tails);
            rows.add(tails + ":" + displayName(tails)
                    + "=" + holder
                    + "/cloak=" + getCloakLevel(server, tails)
                    + "/xp=" + xp[0] + "," + xp[1] + "," + xp[2]
                    + "/cd=" + getCloakCooldown(server, tails));
        }
        return rows;
    }

    public static String displayName(int tails) {
        if (tails == 10) {
            return "ten_tails";
        }
        try {
            return TailedBeastEntity.Variant.byTailCount(tails).registryName();
        } catch (IllegalArgumentException exception) {
            return "unknown";
        }
    }

    private static boolean isOrdinaryBijuAvailable(MinecraftServer server, int tails) {
        return getJinchurikiUuid(server, tails) == null && !isGedoSealedTail(server, tails);
    }

    private static boolean isNaturalJinchurikiCandidate(ServerPlayer player) {
        return canForceJinchurikiGrant(player)
                && availableTailedBeasts(player.getServer()) > 0
                && NarutomodModVariables.getBattleExperience(player) >= 300.0D
                && NarutomodModVariables.getNinjaLevel(player) >= 10.0D
                && !hasNaturalAbilityMarker(player);
    }

    private static boolean canForceJinchurikiGrant(ServerPlayer player) {
        return !player.isSpectator()
                && player.isAlive()
                && availableTailedBeasts(player.getServer()) > 0
                && getAssignedTail(player) <= 0;
    }

    private static boolean hasNaturalAbilityMarker(ServerPlayer player) {
        return hasAnyItem(player,
                ModItems.BYAKUGANHELMET.get(),
                ModItems.SHARINGANHELMET.get(),
                ModItems.MANGEKYOSHARINGANHELMET.get(),
                ModItems.MANGEKYOSHARINGANETERNALHELMET.get(),
                ModItems.MANGEKYOSHARINGANOBITOHELMET.get(),
                ModItems.TENSEIGANHELMET.get(),
                ModItems.RINNEGANHELMET.get(),
                ModItems.BYAKURINNESHARINGANHELMET.get(),
                ModItems.YOOTON.get(),
                ModItems.RANTON.get(),
                ModItems.HYOTON.get(),
                ModItems.JITON.get(),
                ModItems.SHAKUTON.get(),
                ModItems.BAKUTON.get(),
                ModItems.JINTON.get(),
                ModItems.FUTTON.get(),
                ModItems.SHIKOTSUMYAKU.get());
    }

    private static boolean fallsThroughOldNaturalAbilityRoll(ServerPlayer player) {
        double rngbase = OLD_RANDOM_POOL_WITH_BIJU;
        if (roll(player, 10.0D / rngbase)) {
            return false;
        }
        if (roll(player, 10.0D / (rngbase - 10.0D))) {
            return false;
        }
        if (roll(player, 10.0D / (rngbase - 20.0D))) {
            return false;
        }
        if (hasEither(player, ModItems.DOTON.get(), ModItems.KATON.get()) && roll(player, (10.0D / (rngbase - 30.0D)) / 0.4D)) {
            return false;
        }
        if (hasEither(player, ModItems.FUTON.get(), ModItems.KATON.get()) && roll(player, (10.0D / (rngbase - 40.0D)) / 0.4D)) {
            return false;
        }
        if (hasEither(player, ModItems.FUTON.get(), ModItems.SUITON.get()) && roll(player, (10.0D / (rngbase - 50.0D)) / 0.4D)) {
            return false;
        }
        if (hasEither(player, ModItems.FUTON.get(), ModItems.DOTON.get()) && roll(player, (10.0D / (rngbase - 60.0D)) / 0.4D)) {
            return false;
        }
        if (hasEither(player, ModItems.RAITON.get(), ModItems.DOTON.get()) && roll(player, (10.0D / (rngbase - 70.0D)) / 0.4D)) {
            return false;
        }
        if (hasEither(player, ModItems.RAITON.get(), ModItems.SUITON.get()) && roll(player, (10.0D / (rngbase - 80.0D)) / 0.4D)) {
            return false;
        }
        if (hasEither(player, ModItems.SUITON.get(), ModItems.KATON.get()) && roll(player, (10.0D / (rngbase - 90.0D)) / 0.4D)) {
            return false;
        }
        return !hasAnyItem(player, ModItems.KATON.get(), ModItems.DOTON.get(), ModItems.FUTON.get())
                || !roll(player, (5.0D / (rngbase - 100.0D)) / 0.6D);
    }

    private static boolean roll(ServerPlayer player, double chance) {
        return player.getRandom().nextDouble() <= Math.min(Math.max(chance, 0.0D), 1.0D);
    }

    private static boolean hasEither(ServerPlayer player, Item first, Item second) {
        return hasAnyItem(player, first, second);
    }

    private static boolean hasAnyItem(ServerPlayer player, Item... items) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            for (Item item : items) {
                if (stack.is(item)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void grantTailSideEffects(ServerPlayer player, int tails) {
        if (tails == 1) {
            grantAdvancedNatureItem(player, ModItems.JITON.get(), AdvancedNatureJutsuItem.AdvancedNatureKind.JITON, -1);
            grantJutsuItem(player, ModItems.FUTON.get(), FutonItem.VACUUM);
            grantJutsuItem(player, ModItems.DOTON.get(), DotonItem.EARTH_WALL);
        } else if (tails == 4) {
            grantAdvancedNatureItem(player, ModItems.YOOTON.get(), AdvancedNatureJutsuItem.AdvancedNatureKind.YOOTON, 2);
            grantJutsuItem(player, ModItems.KATON.get(), KatonItem.GREAT_FIREBALL);
            grantJutsuItem(player, ModItems.DOTON.get(), DotonItem.EARTH_WALL);
        }
    }

    private static void grantJutsuItem(ServerPlayer player, Item item, JutsuItem.JutsuDefinition primaryJutsu) {
        ItemStack stack = findItemStack(player, item);
        boolean missing = stack == null;
        if (missing) {
            stack = new ItemStack(item);
        }
        if (stack.getItem() instanceof JutsuItem jutsuItem) {
            JutsuItem.setOwnerIfMissing(stack, player);
            jutsuItem.enableJutsu(stack, primaryJutsu, true);
            jutsuItem.setJutsuXp(stack, primaryJutsu, jutsuItem.getRequiredXp(stack, primaryJutsu));
        }
        if (missing) {
            giveOrDrop(player, stack);
        }
    }

    private static void grantAdvancedNatureItem(
            ServerPlayer player,
            Item item,
            AdvancedNatureJutsuItem.AdvancedNatureKind kind,
            int primaryJutsuIndex) {
        ItemStack stack = findItemStack(player, item);
        boolean missing = stack == null;
        if (missing) {
            stack = new ItemStack(item);
        }
        if (stack.getItem() instanceof AdvancedNatureJutsuItem advancedNatureItem) {
            JutsuItem.setOwnerIfMissing(stack, player);
            if (primaryJutsuIndex >= 0) {
                JutsuItem.JutsuDefinition definition = kind.definitionByIndex(primaryJutsuIndex);
                advancedNatureItem.enableJutsu(stack, definition, true);
                advancedNatureItem.setJutsuXp(stack, definition, advancedNatureItem.getRequiredXp(stack, definition));
                stack.getOrCreateTag().putInt(JutsuItem.JUTSU_INDEX_TAG, primaryJutsuIndex);
            }
        }
        if (missing) {
            giveOrDrop(player, stack);
        }
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack.copy(), false);
        }
    }

    @Nullable
    private static ItemStack findItemStack(ServerPlayer player, Item item) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(item)) {
                return stack;
            }
        }
        return null;
    }

    private static void setCloakLevel(MinecraftServer server, int tails, int level) {
        if (!isValidTailCount(tails)) {
            return;
        }
        NarutomodSavedData.TailedBeast data = data(server, tails);
        data.data().putInt(CLOAK_LEVEL_KEY, Math.max(Math.min(level, 3), 0));
        data.setDirty();
    }

    private static void setCloakXp(MinecraftServer server, int tails, int level, int xp) {
        if (!isValidTailCount(tails) || level < 1 || level > 3) {
            return;
        }
        NarutomodSavedData.TailedBeast data = data(server, tails);
        int[] values = getCloakXp(server, tails);
        values[level - 1] = Math.max(xp, 0);
        data.data().putIntArray(CLOAK_XP_KEY, values);
        data.setDirty();
    }

    private static void setCloakCooldown(MinecraftServer server, int tails, long cooldown) {
        if (!isValidTailCount(tails)) {
            return;
        }
        NarutomodSavedData.TailedBeast data = data(server, tails);
        data.data().putLong(CLOAK_COOLDOWN_KEY, Math.max(cooldown, 0L));
        data.setDirty();
    }

    private static void saveAndResetWearingTicks(ServerPlayer player, int cloakLevel) {
        if (cloakLevel < 1 || cloakLevel > 3) {
            return;
        }
        int wearingTicks = BijuCloakItem.getWearingTicks(player);
        if (wearingTicks <= 0) {
            return;
        }
        addCloakXp(player, wearingTicks / 20);
        long cooldown = player.level().getGameTime() + wearingTicks + computeCloakCooldown(cloakLevel, wearingTicks, getCurrentCloakXp(player));
        setCloakCooldown(player.getServer(), getAssignedTail(player), cooldown);
        BijuCloakItem.setWearingTicks(player, 0);
    }

    private static int computeCloakCooldown(int level, int wearingTicks, int xp) {
        double divisor = Math.max(Math.sqrt(Math.sqrt(Math.max(xp, 1))) - 3.0D, 1.0D);
        return (int)(level * 2.0D * wearingTicks / divisor);
    }

    private static void equipCloakSet(ServerPlayer player, int tails, int cloakLevel) {
        int xp = getCloakXp(player.getServer(), tails)[Math.max(Math.min(cloakLevel, 3), 1) - 1];
        equipCloakPiece(player, EquipmentSlot.HEAD, BijuCloakItem.createStack(ModItems.BIJU_CLOAKHELMET.get(), tails, cloakLevel, xp));
        equipCloakPiece(player, EquipmentSlot.CHEST, BijuCloakItem.createStack(ModItems.BIJU_CLOAKBODY.get(), tails, cloakLevel, xp));
        equipCloakPiece(player, EquipmentSlot.LEGS, BijuCloakItem.createStack(ModItems.BIJU_CLOAKLEGS.get(), tails, cloakLevel, xp));
    }

    private static void equipCloakPiece(ServerPlayer player, EquipmentSlot slot, ItemStack stack) {
        ItemStack current = player.getItemBySlot(slot);
        if (!current.isEmpty() && !BijuCloakItem.isBijuCloak(current)) {
            if (!player.getInventory().add(current.copy())) {
                player.drop(current.copy(), false);
            }
        }
        player.setItemSlot(slot, stack);
    }

    private static void removeCloakSet(ServerPlayer player) {
        removeCloakPiece(player, EquipmentSlot.HEAD);
        removeCloakPiece(player, EquipmentSlot.CHEST);
        removeCloakPiece(player, EquipmentSlot.LEGS);
    }

    private static void removeCloakPiece(ServerPlayer player, EquipmentSlot slot) {
        if (BijuCloakItem.isBijuCloak(player.getItemBySlot(slot))) {
            player.setItemSlot(slot, ItemStack.EMPTY);
        }
    }

    private static boolean handleTailBeastBallCharge(ServerPlayer player, boolean pressed) {
        TailBeastBallCharge charge = TAIL_BEAST_BALL_CHARGE.computeIfAbsent(player.getUUID(), uuid -> new TailBeastBallCharge());
        if (pressed) {
            if (player.tickCount < charge.cooldownUntilTick) {
                if (player.tickCount % 20 == 0) {
                    player.displayClientMessage(Component.literal("Tail Beast Ball cooldown "
                            + ((charge.cooldownUntilTick - player.tickCount) / 20) + "s"), true);
                }
                return false;
            }
            charge.power = Math.min(charge.power + 0.01F, 14.0F);
            if (player.tickCount % 10 == 0) {
                player.displayClientMessage(Component.literal(String.format("Tail Beast Ball %.1f", charge.power)), true);
            }
            return true;
        }

        if (charge.power <= 0.0F || player.tickCount < charge.cooldownUntilTick) {
            charge.power = 0.0F;
            return false;
        }
        float power = Math.max(charge.power, 0.1F);
        if (!player.isCreative() && !Chakra.pathway(player).consume(100.0D * power)) {
            charge.power = 0.0F;
            return false;
        }
        if (TailBeastBallEntity.spawnFrom(player, power, power * 70.0F)) {
            charge.cooldownUntilTick = player.tickCount + (int)(power * 300.0F);
            charge.power = 0.0F;
            return true;
        }
        if (!player.isCreative()) {
            Chakra.pathway(player).consume(-100.0D * power, true);
        }
        charge.power = 0.0F;
        return false;
    }

    private static boolean fireMountedTailedBeastBall(ServerPlayer player) {
        if (player.getVehicle() instanceof TailedBeastEntity beast && beast.getTailCount() == getAssignedTail(player)) {
            beast.performRangedAttack(player, 0.0F);
            return true;
        }
        if (getAssignedTail(player) == MAX_TAILS && player.getVehicle() instanceof TenTailsEntity beast) {
            beast.performRangedAttack(player, 0.0F);
            return true;
        }
        player.displayClientMessage(Component.literal("Ride your tailed beast to fire its Tail Beast Ball."), true);
        return false;
    }

    private static boolean revokePlayer(ServerPlayer player, boolean syncPlayer) {
        MinecraftServer server = player.getServer();
        boolean changed = false;
        UUID uuid = player.getUUID();
        for (int tails = MIN_TAILS; tails <= MAX_TAILS; tails++) {
            UUID holder = getJinchurikiUuid(server, tails);
            if (uuid.equals(holder)) {
                clearTailData(server, tails);
                changed = true;
            }
        }
        if (changed || NarutomodModVariables.get(player).getInt(NarutomodModVariables.JINCHURIKI_TAILS) != 0) {
            removeCloakSet(player);
            BijuCloakItem.setWearingTicks(player, 0);
            NarutomodModVariables.get(player).putInt(NarutomodModVariables.JINCHURIKI_TAILS, 0);
            if (syncPlayer) {
                NarutomodModVariables.sync(player);
            }
        }
        return changed;
    }

    private static boolean revokeByTail(MinecraftServer server, int tails, boolean syncPlayer) {
        if (!isValidTailCount(tails)) {
            return false;
        }
        UUID holder = getJinchurikiUuid(server, tails);
        boolean changed = clearTailData(server, tails);
        if (holder != null && syncPlayer) {
            ServerPlayer player = server.getPlayerList().getPlayer(holder);
            if (player != null) {
                removeCloakSet(player);
                BijuCloakItem.setWearingTicks(player, 0);
                NarutomodModVariables.get(player).putInt(NarutomodModVariables.JINCHURIKI_TAILS, 0);
                NarutomodModVariables.sync(player);
            }
        }
        return changed;
    }

    private static boolean clearTailData(MinecraftServer server, int tails) {
        NarutomodSavedData.TailedBeast data = data(server, tails);
        CompoundTag tag = data.data();
        boolean changed = tag.contains(JINCHURIKI_UUID_KEY)
                || tag.contains(JINCHURIKI_NAME_KEY)
                || tag.contains(CLOAK_XP_KEY, Tag.TAG_INT_ARRAY)
                || tag.contains(CLOAK_COOLDOWN_KEY)
                || tag.contains(CLOAK_LEVEL_KEY);
        if (changed) {
            tag.remove(JINCHURIKI_UUID_KEY);
            tag.remove(JINCHURIKI_NAME_KEY);
            tag.remove(CLOAK_XP_KEY);
            tag.remove(CLOAK_COOLDOWN_KEY);
            tag.remove(CLOAK_LEVEL_KEY);
            data.setDirty();
        }
        return changed;
    }

    private static boolean clearGedoSealedTail(MinecraftServer server, int tails) {
        if (tails < MIN_TAILS || tails > 9) {
            return false;
        }
        NarutomodSavedData.TailedBeast data = data(server, tails);
        CompoundTag tag = data.data();
        if (!tag.contains(GEDO_SEALED_KEY)) {
            return false;
        }
        tag.remove(GEDO_SEALED_KEY);
        data.setDirty();
        return true;
    }

    @Nullable
    private static TailedBeastEntity findLoadedTailedBeast(MinecraftServer server, int tails, @Nullable UUID uuid) {
        if (uuid == null) {
            return null;
        }
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(uuid);
            if (entity instanceof TailedBeastEntity beast && beast.getTailCount() == tails && beast.isAlive()) {
                return beast;
            }
        }
        return null;
    }

    private static boolean discardLoadedTailedBeast(MinecraftServer server, int tails) {
        if (tails == MAX_TAILS) {
            return discardLoadedTenTailsExcept(server, null) > 0;
        }
        if (tails < MIN_TAILS || tails > 9) {
            return false;
        }
        CompoundTag tag = data(server, tails).data();
        UUID uuid = tag.hasUUID(ENTITY_UUID_KEY) ? tag.getUUID(ENTITY_UUID_KEY) : null;
        TailedBeastEntity beast = findLoadedTailedBeast(server, tails, uuid);
        if (beast == null) {
            return false;
        }
        beast.discard();
        return true;
    }

    @Nullable
    private static TenTailsEntity findLoadedTenTails(MinecraftServer server, @Nullable UUID uuid) {
        if (uuid == null) {
            return null;
        }
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(uuid);
            if (entity instanceof TenTailsEntity beast && beast.isAlive()) {
                return beast;
            }
        }
        return null;
    }

    @Nullable
    private static TenTailsEntity findAnyLoadedTenTails(MinecraftServer server, @Nullable UUID skipUuid) {
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof TenTailsEntity beast
                        && beast.isAlive()
                        && (skipUuid == null || !skipUuid.equals(beast.getUUID()))) {
                    return beast;
                }
            }
        }
        return null;
    }

    private static NarutomodSavedData.TailedBeast data(MinecraftServer server, int tails) {
        return NarutomodSavedData.tailedBeast(server, NarutomodSavedData.TAILED_BEAST_DATA_NAMES[tails - 1]);
    }

    private static void setSyncedTail(ServerPlayer player, int tails) {
        NarutomodModVariables.get(player).putInt(NarutomodModVariables.JINCHURIKI_TAILS, tails);
        NarutomodModVariables.sync(player);
    }

    private static String holderName(MinecraftServer server, int tails, UUID uuid) {
        ServerPlayer player = server.getPlayerList().getPlayer(uuid);
        if (player != null) {
            return player.getScoreboardName();
        }
        String storedName = getJinchurikiName(server, tails);
        return storedName.isEmpty() ? uuid.toString() : storedName;
    }

    private static final class TailBeastBallCharge {
        private int cooldownUntilTick;
        private float power;
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            NarutomodSavedData.loadAll(player.getServer());
            restoreSpawnedTailedBeasts(player.getServer());
            resolvePlayer(player);
        }
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        NarutomodSavedData.loadAll(event.getServer());
        restoreSpawnedTailedBeasts(event.getServer());
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath() && event.getEntity() instanceof ServerPlayer player) {
            resolvePlayer(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player && player.tickCount % 20 == 0) {
            tryNaturalJinchurikiGrant(player, false);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        int tails = getAssignedTail(player);
        if (tails <= 0) {
            return;
        }
        revokePlayer(player);
        player.displayClientMessage(Component.literal("Released " + displayName(tails) + " from its jinchuriki."), true);
    }
}
