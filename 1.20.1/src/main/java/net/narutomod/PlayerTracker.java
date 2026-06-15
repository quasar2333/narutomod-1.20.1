package net.narutomod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Team;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public final class PlayerTracker {
    public static final UUID NINJA_HEALTH = UUID.fromString("68f3f7c8-dbd6-43ec-b4d6-d64dd24f4d6a");
    private static final double MAX_BATTLE_XP = 100000.0D;

    private PlayerTracker() {
    }

    public static boolean isNinja(Player player) {
        return NarutomodModVariables.isNinja(player);
    }

    public static double getBattleXp(Player player) {
        return NarutomodModVariables.getBattleExperience(player);
    }

    public static double getNinjaLevel(Player player) {
        return NarutomodModVariables.getNinjaLevel(player);
    }

    public static void addBattleXp(ServerPlayer player, double xp) {
        NarutomodModVariables.setBattleExperience(player, Math.min(getBattleXp(player) + xp, MAX_BATTLE_XP));
    }

    public static void logBattleExp(Player player, double xp) {
        if (player instanceof ServerPlayer serverPlayer && xp > 0.0D) {
            addBattleXp(serverPlayer, xp);
        }
    }

    private static void updateNinjaHealth(Player player) {
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }

        double amount = getBattleXp(player) * 0.005D;
        AttributeModifier current = maxHealth.getModifier(NINJA_HEALTH);
        if (amount <= 0.0D) {
            if (current != null) {
                maxHealth.removeModifier(NINJA_HEALTH);
            }
            return;
        }
        if (current == null || (int) current.getAmount() / 2 != (int) amount / 2) {
            if (current != null) {
                maxHealth.removeModifier(NINJA_HEALTH);
            }
            maxHealth.addTransientModifier(new AttributeModifier(
                    NINJA_HEALTH,
                    "ninja.maxhealth",
                    amount,
                    AttributeModifier.Operation.ADDITION));
            player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + 0.1F));
        }
    }

    public static final class Deaths {
        private static final List<DeathRecord> DEAD_PLAYERS = new ArrayList<>();

        private Deaths() {
        }

        public static void log(Player player) {
            DEAD_PLAYERS.removeIf(record -> record.playerId.equals(player.getUUID()));
            DEAD_PLAYERS.add(new DeathRecord(player));
        }

        public static void clear() {
            DEAD_PLAYERS.clear();
        }

        public static boolean hasRecentNearby(Player player, double distance, double timeframe) {
            return hasRecentNearby(player, distance, timeframe, true);
        }

        public static boolean hasRecentNearby(Player player, double distance, double timeframe, boolean checkTeam) {
            prune(player.level().getGameTime(), timeframe);
            double maxDistanceSqr = distance * distance;
            for (DeathRecord record : DEAD_PLAYERS) {
                if (record.playerId.equals(player.getUUID())) {
                    continue;
                }
                double dx = record.x - player.getX();
                double dy = record.y - player.getY();
                double dz = record.z - player.getZ();
                if (dx * dx + dy * dy + dz * dz < maxDistanceSqr
                        && (!checkTeam || record.team != null && player.isAlliedTo(record.team))) {
                    return true;
                }
            }
            return false;
        }

        public static boolean hasRecentMatching(Player player, double timeframe) {
            prune(player.level().getGameTime(), timeframe);
            return DEAD_PLAYERS.stream().anyMatch(record -> record.playerId.equals(player.getUUID()));
        }

        public static long mostRecentTime(Player player) {
            for (int index = DEAD_PLAYERS.size() - 1; index >= 0; index--) {
                DeathRecord record = DEAD_PLAYERS.get(index);
                if (record.playerId.equals(player.getUUID())) {
                    return record.time;
                }
            }
            return 0L;
        }

        public static double getXpBeforeDeath(Player player) {
            for (int index = DEAD_PLAYERS.size() - 1; index >= 0; index--) {
                DeathRecord record = DEAD_PLAYERS.get(index);
                if (record.playerId.equals(player.getUUID())) {
                    return record.lastXp;
                }
            }
            return 0.0D;
        }

        private static void prune(long now, double timeframe) {
            Iterator<DeathRecord> iterator = DEAD_PLAYERS.iterator();
            while (iterator.hasNext()) {
                if (now - iterator.next().time > timeframe) {
                    iterator.remove();
                }
            }
        }
    }

    private static final class DeathRecord {
        private final UUID playerId;
        private final double x;
        private final double y;
        private final double z;
        private final long time;
        private final Team team;
        private final double lastXp;

        private DeathRecord(Player player) {
            playerId = player.getUUID();
            x = player.getX();
            y = player.getY();
            z = player.getZ();
            time = player.level().getGameTime();
            team = player.getTeam();
            lastXp = getBattleXp(player);
        }
    }

    @Mod.EventBusSubscriber(modid = NarutomodMod.MODID)
    public static final class ForgeEvents {
        private ForgeEvents() {
        }

        @SubscribeEvent
        public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide()) {
                updateNinjaHealth(event.player);
            }
        }

        @SubscribeEvent
        public static void onLivingDeath(LivingDeathEvent event) {
            if (event.getEntity() instanceof Player player) {
                Deaths.log(player);
            }
        }

        @SubscribeEvent
        public static void onLivingDamage(LivingDamageEvent event) {
            DamageSource source = event.getSource();
            if (event.getAmount() <= 0.0F || source.getEntity() == event.getEntity()) {
                return;
            }

            if (event.getEntity() instanceof Player damaged && event.getAmount() < damaged.getHealth()) {
                double battleXp = getBattleXp(damaged);
                logBattleExp(damaged, battleXp < 1.0D ? 1.0D : event.getAmount() / Math.sqrt(Math.sqrt(battleXp)));
            }

            if (source.getEntity() instanceof Player attacker) {
                LivingEntity target = event.getEntity();
                double resistance = target.getMaxHealth();
                double xp = Math.min(Math.sqrt(Math.max(resistance, 1.0D)) * Math.min(event.getAmount() / target.getMaxHealth(), 1.0F) * 0.5D, 50.0D);
                logBattleExp(attacker, xp);
            }
        }
    }
}
