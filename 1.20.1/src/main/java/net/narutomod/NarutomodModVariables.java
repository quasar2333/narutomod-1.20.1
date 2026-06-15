package net.narutomod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.narutomod.network.PlayerVariablesSyncMessage;

public final class NarutomodModVariables {
    public static final String BATTLEXP = "battle_experience";
    public static final String RINNESHARINGAN_ACTIVATED = "RinneSharinganActivated";
    public static final String INVULNERABLE_TIME = "invulnerableTime";
    public static final String DEATH_ANIMATION_TIME = "deathAnimationTime";
    public static final String NO_CLIP_FLAG = "noClipFlag";
    public static final String FIRST_GOT_NINJUTSU = "FirstGotNinjutsuFlag";
    public static final String JINCHURIKI_TAILS = "JinchurikiTails";
    public static final String MOST_RECENT_WORN_DOJUTSU_TIME = "mostRecentlyHadAnyDojutsu";
    public static final String JUTSU_KEY_2_PRESSED = "JutsuKey2Pressed";
    public static final String CTRL_PRESSED = "CTRL_pressed";
    public static final String FORCE_BOW_POSE = "ForceBipedBowPose";
    public static final String TENSEIGAN_EVOLVED_TIME = "TenseiganEvolvedTime";
    public static final String CHAKRA_PATHWAY_SYSTEM = "ChakraPathwaySystem";
    public static final String KEEP_NINJA_XP_RULE_NAME = "keepNinjaXp";

    public static final GameRules.Key<GameRules.BooleanValue> KEEP_NINJA_XP = GameRules.register(
            KEEP_NINJA_XP_RULE_NAME,
            GameRules.Category.PLAYER,
            GameRules.BooleanValue.create(false));

    public static final Capability<PlayerVariables> PLAYER_VARIABLES = CapabilityManager.get(new CapabilityToken<>() {
    });
    private static final Map<UUID, PlayerVariables> FALLBACK_PLAYER_VARIABLES = new ConcurrentHashMap<>();

    private NarutomodModVariables() {
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(PlayerVariables.class);
    }

    public static LazyOptional<PlayerVariables> getOptional(Player player) {
        return player.getCapability(PLAYER_VARIABLES);
    }

    public static PlayerVariables get(Player player) {
        return getOptional(player).orElseGet(() ->
                FALLBACK_PLAYER_VARIABLES.computeIfAbsent(player.getUUID(), uuid -> new PlayerVariables()));
    }

    public static double getBattleExperience(Player player) {
        return get(player).getDouble(BATTLEXP);
    }

    public static double getNinjaLevel(Player player) {
        return Math.sqrt(getBattleExperience(player));
    }

    public static boolean isNinja(Player player) {
        return getBattleExperience(player) > 0.0D;
    }

    public static void setBattleExperience(ServerPlayer player, double value) {
        PlayerVariables variables = get(player);
        variables.putDouble(BATTLEXP, Math.min(Math.max(value, 0.0D), 100000.0D));
        sync(player);
    }

    public static void addBattleExperience(ServerPlayer player, double value) {
        setBattleExperience(player, getBattleExperience(player) + value);
    }

    public static void sync(ServerPlayer player) {
        player.getCapability(PLAYER_VARIABLES).ifPresent(variables ->
                PlayerVariablesSyncMessage.sendTrackingAndSelf(player, variables.save()));
    }

    public static final class PlayerVariables {
        private CompoundTag values = new CompoundTag();

        public CompoundTag save() {
            return values.copy();
        }

        public void load(CompoundTag tag) {
            values = tag.copy();
        }

        public void copyFrom(PlayerVariables old, boolean keepBattleExperience) {
            values = old.save();
            if (!keepBattleExperience) {
                putDouble(BATTLEXP, 0.0D);
            }
        }

        public double getDouble(String key) {
            return values.getDouble(key);
        }

        public void putDouble(String key, double value) {
            values.putDouble(key, value);
        }

        public boolean getBoolean(String key) {
            return values.getBoolean(key);
        }

        public void putBoolean(String key, boolean value) {
            values.putBoolean(key, value);
        }

        public int getInt(String key) {
            return values.getInt(key);
        }

        public void putInt(String key, int value) {
            values.putInt(key, value);
        }

        public long getLong(String key) {
            return values.getLong(key);
        }

        public void putLong(String key, long value) {
            values.putLong(key, value);
        }

        public boolean contains(String key) {
            return values.contains(key);
        }

        public void remove(String key) {
            values.remove(key);
        }
    }

    private static final class PlayerVariablesProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
        private final PlayerVariables backend = new PlayerVariables();
        private final LazyOptional<PlayerVariables> optional = LazyOptional.of(() -> backend);

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
            return capability == PLAYER_VARIABLES ? optional.cast() : LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            return backend.save();
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            backend.load(tag);
        }

        private void invalidate() {
            optional.invalidate();
        }
    }

    public static final class ForgeEvents {
        private ForgeEvents() {
        }

        @SubscribeEvent
        public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
            if (event.getObject() instanceof Player) {
                PlayerVariablesProvider provider = new PlayerVariablesProvider();
                event.addCapability(NarutomodMod.location("player_variables"), provider);
                event.addListener(provider::invalidate);
            }
        }

        @SubscribeEvent
        public static void onPlayerClone(PlayerEvent.Clone event) {
            event.getOriginal().reviveCaps();
            boolean keepBattleExperience = !event.isWasDeath()
                    || event.getOriginal().level().getGameRules().getBoolean(KEEP_NINJA_XP);
            event.getOriginal().getCapability(PLAYER_VARIABLES).ifPresent(oldVariables ->
                    event.getEntity().getCapability(PLAYER_VARIABLES).ifPresent(newVariables ->
                            newVariables.copyFrom(oldVariables, keepBattleExperience)));
            event.getOriginal().invalidateCaps();

            if (event.getEntity() instanceof ServerPlayer player) {
                sync(player);
            }
        }

        @SubscribeEvent
        public static void onLivingDeath(LivingDeathEvent event) {
            if (event.getEntity() instanceof ServerPlayer player
                    && !player.level().getGameRules().getBoolean(KEEP_NINJA_XP)) {
                player.getCapability(PLAYER_VARIABLES).ifPresent(variables -> variables.putDouble(BATTLEXP, 0.0D));
                sync(player);
            }
        }

        @SubscribeEvent
        public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                sync(player);
            }
        }

        @SubscribeEvent
        public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                sync(player);
            }
        }

        @SubscribeEvent
        public static void onStartTracking(PlayerEvent.StartTracking event) {
            if (event.getTarget() instanceof ServerPlayer tracked && event.getEntity() instanceof ServerPlayer watcher) {
                tracked.getCapability(PLAYER_VARIABLES).ifPresent(variables ->
                        PlayerVariablesSyncMessage.sendTo(watcher, tracked.getId(), variables.save()));
            }
        }
    }
}
