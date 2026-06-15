package net.narutomod;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.narutomod.procedure.ProcedureBasicNinjaSkills;
import net.narutomod.procedure.ProcedurePlayerLegacyDojutsuPostTick;
import net.narutomod.procedure.ProcedurePlayerLegacyNinjutsuGrant;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.network.ChakraWarningMessage;
import net.narutomod.network.NetworkHandler;

public final class Chakra {
    private static final Map<LivingEntity, Pathway> LIVING_PATHWAYS = new WeakHashMap<>();
    private static final Map<Player, TickState> PLAYER_TICK_STATES = new WeakHashMap<>();

    private Chakra() {
    }

    public static double getLevel(LivingEntity entity) {
        Pathway pathway = pathway(entity);
        return Math.sqrt(Math.max(pathway.getAmount(), pathway.getMax()));
    }

    public static double getChakraModifier(LivingEntity entity) {
        // Faithful to 1.12.2 Chakra.getChakraModifier: 1 / (0.5 + 0.02 * chakraLevel),
        // via the shared ProcedureUtils.getCDModifier curve. Scales jutsu charge speed.
        return ProcedureUtils.getCDModifier(getLevel(entity));
    }

    public static Pathway pathway(LivingEntity entity) {
        if (entity instanceof Player player) {
            return new PlayerPathway(player);
        }
        return LIVING_PATHWAYS.computeIfAbsent(entity, Pathway::new);
    }

    public static class Pathway {
        protected final LivingEntity user;
        private double amount;
        private double max;

        private Pathway(LivingEntity user) {
            this.user = user;
        }

        public double getMax() {
            return max;
        }

        public void setMax(double max) {
            this.max = Math.max(max, 0.0D);
            if (amount > this.max) {
                amount = this.max;
            }
        }

        public double getAmount() {
            return amount;
        }

        protected void set(double amount) {
            this.amount = Math.max(amount, 0.0D);
        }

        public boolean consume(double amount) {
            return consume(amount, false);
        }

        public boolean consume(double amount, boolean ignoreMax) {
            double current = getAmount();
            double max = getMax();
            double next = current - amount;
            double clamped = next > max
                    ? (ignoreMax ? next : amount > 0.0D ? next : current > max ? current : max)
                    : next > 0.0D ? next : current;
            if (Double.compare(current, clamped) != 0) {
                set(clamped);
                return true;
            }
            if (amount > 0.0D) {
                warningDisplay();
            }
            return false;
        }

        public void consume(float percent) {
            consume(percent, false);
        }

        public void consume(float percent, boolean ignoreMax) {
            consume(getMax() * percent, ignoreMax);
        }

        public void clear() {
            set(0.0D);
        }

        public boolean isFull() {
            return getAmount() >= getMax();
        }

        public void warningDisplay() {
        }

        protected void serverTick(TickState state) {
            if (getAmount() > getMax() && user.tickCount % 20 == 0) {
                consume(10.0D);
            }
            if (getAmount() < 10.0D && getMax() > 150.0D && !(user instanceof Player player && player.isCreative())) {
                user.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 3));
                user.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3));
                user.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 3));
            }

            state.update(user);
            if (state.motionlessTicks > 100 && user.tickCount % 80 == 0) {
                consume(-0.006F);
            }
        }

        @Override
        public String toString() {
            return "Chakra:{amount:" + getAmount() + ",max:" + getMax() + "," + user.getName().getString() + "}";
        }
    }

    private static final class PlayerPathway extends Pathway {
        private final Player player;

        private PlayerPathway(Player player) {
            super(player);
            this.player = player;
        }

        @Override
        public double getMax() {
            return NarutomodModVariables.getBattleExperience(player) * 0.5D;
        }

        @Override
        public void setMax(double max) {
        }

        @Override
        public double getAmount() {
            return NarutomodModVariables.get(player).getDouble(NarutomodModVariables.CHAKRA_PATHWAY_SYSTEM);
        }

        @Override
        protected void set(double amount) {
            NarutomodModVariables.PlayerVariables variables = NarutomodModVariables.get(player);
            variables.putDouble(NarutomodModVariables.CHAKRA_PATHWAY_SYSTEM, Math.max(amount, 0.0D));
            if (player instanceof ServerPlayer serverPlayer) {
                NarutomodModVariables.sync(serverPlayer);
            }
        }

        @Override
        public void warningDisplay() {
            if (player instanceof ServerPlayer serverPlayer) {
                NetworkHandler.sendToPlayer(serverPlayer, new ChakraWarningMessage(60));
            }
        }
    }

    private static final class TickState {
        private int motionlessTicks;
        private double previousX;
        private double previousZ;

        private TickState(LivingEntity entity) {
            previousX = entity.getX();
            previousZ = entity.getZ();
        }

        private void update(LivingEntity entity) {
            boolean moved = entity.getX() != previousX || entity.getZ() != previousZ;
            if (moved || !entity.onGround()) {
                motionlessTicks = 0;
            } else {
                motionlessTicks++;
            }
            previousX = entity.getX();
            previousZ = entity.getZ();
        }
    }

    @Mod.EventBusSubscriber(modid = NarutomodMod.MODID)
    public static final class ForgeEvents {
        private ForgeEvents() {
        }

        @SubscribeEvent
        public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
                return;
            }
            Player player = event.player;
            ProcedurePlayerLegacyDojutsuPostTick.apply(player);
            if (!NarutomodModVariables.isNinja(player)) {
                return;
            }

            PlayerPathway pathway = new PlayerPathway(player);
            pathway.serverTick(PLAYER_TICK_STATES.computeIfAbsent(player, TickState::new));
            if (player.experienceLevel >= 10) {
                ProcedurePlayerLegacyNinjutsuGrant.apply(player);
                ProcedureBasicNinjaSkills.apply(player);
            }
            if (player instanceof ServerPlayer serverPlayer
                    && player.isSleeping()
                    && serverPlayer.serverLevel().players().stream().allMatch(Player::isSleeping)
                    && player.tickCount % 20 == 0) {
                pathway.consume(-0.6F);
            }
        }

        @SubscribeEvent
        public static void onLivingDeath(LivingDeathEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                NarutomodModVariables.get(player).putDouble(NarutomodModVariables.CHAKRA_PATHWAY_SYSTEM, 10.0D);
                PLAYER_TICK_STATES.remove(player);
                NarutomodModVariables.sync(player);
            }
        }

        @SubscribeEvent
        public static void onClone(PlayerEvent.Clone event) {
            PLAYER_TICK_STATES.remove(event.getOriginal());
            if (event.getEntity() instanceof ServerPlayer player) {
                NarutomodModVariables.sync(player);
            }
        }
    }
}
