package net.narutomod.item;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.narutomod.Chakra;
import net.narutomod.NarutomodMod;
import net.narutomod.NarutomodModVariables;
import net.narutomod.PlayerTracker;
import net.narutomod.entity.EightTrigramsEntity;
import net.narutomod.entity.HakkeshoKeitenEntity;
import net.narutomod.network.ByakuganViewSyncMessage;
import net.narutomod.network.NetworkHandler;
import net.narutomod.particle.NarutoParticleKind;
import net.narutomod.procedure.ProcedureAirPunch;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModDamageTypes;
import net.narutomod.registry.ModItems;
import net.narutomod.registry.ModParticleTypes;
import net.narutomod.registry.ModSounds;

@Mod.EventBusSubscriber(modid = NarutomodMod.MODID)
public final class ByakuganHandler {
    public static final String BYAKUGAN_ACTIVATED_TAG = "byakugan_activated";
    public static final String BYAKUGAN_FOV_TAG = "byakugan_fov";
    public static final double BYAKUGAN_CHAKRA_USAGE = 10.0D;
    public static final float DEFAULT_FOV = 110.0F;
    public static final float MIN_FOV = 1.0F;
    public static final float MAX_FOV = 110.0F;
    public static final String HAKKE_ROKUJUUYONSHOU_COOLDOWN_TAG = "HakkeRokujuuyonshouCD";
    public static final String HAKKESHOKAITEN_COOLDOWN_TAG = "HakkeshoKaitenCD";
    public static final String RINNESHARINGAN_PRESS_TIME_TAG = "press_time";
    public static final String HAKKE_KUSHO_HOLDING_TAG = "hakke_kusho_holding";
    public static final double ROKUJUYONSHO_CHAKRA_USAGE = 100.0D;
    public static final double KAITEN_CHAKRA_USAGE_PER_TICK = HakkeshoKeitenEntity.CHAKRA_COST_PER_TICK;
    public static final double KUSHO_CHAKRA_USAGE = 0.5D;
    private static final double HAKKE_KUSHO_REQUIRED_BATTLE_XP = 500.0D;
    private static final double HAKKE_KUSHO_BREAK_BATTLE_XP = HAKKE_KUSHO_REQUIRED_BATTLE_XP + 850.0D;
    private static final int EIGHT_TRIGRAMS_REQUIRED_LEVEL = 20;
    private static final int HAKKESHOKAITEN_REQUIRED_LEVEL = 30;
    private static final int EIGHT_TRIGRAMS_COOLDOWN_BASE_TICKS = 1200;
    private static final double RINNESHARINGAN_MAX_PRESS_TICKS = 200.0D;
    private static final ProcedureAirPunch HAKKE_KUSHO = new HakkeKushoAirPunch();

    private ByakuganHandler() {
    }

    public static boolean handleSpecialJutsuKey(ServerPlayer player, int key, boolean pressed) {
        if ((key < 1 || key > 3) || player.isSpectator() || !player.isAlive()) {
            return false;
        }
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!isByakuganHead(head)) {
            return false;
        }
        return switch (key) {
            case 1 -> handleActivationKey(player, pressed);
            case 2 -> handleEightTrigramsKey(player, head, pressed);
            case 3 -> handleHakkeshoKaitenKey(player, head, pressed);
            default -> false;
        };
    }

    private static boolean handleActivationKey(ServerPlayer player, boolean pressed) {
        if (player.getPersistentData().getBoolean(HAKKE_KUSHO_HOLDING_TAG) || player.isShiftKeyDown()) {
            return handleHakkeKusho(player, pressed);
        }
        if (pressed) {
            if (!isActive(player) && Chakra.pathway(player).getAmount() >= getByakuganChakraUsage(player) * 2.0D) {
                setActive(player, true, DEFAULT_FOV);
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        ModSounds.SOUND_BYAKUGAN.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
            }
            return true;
        }
        if (isActive(player)) {
            setActive(player, false, 0.0F);
        }
        return true;
    }

    private static boolean handleEightTrigramsKey(ServerPlayer player, ItemStack head, boolean pressed) {
        NarutomodModVariables.get(player).putBoolean(NarutomodModVariables.JUTSU_KEY_2_PRESSED, pressed);
        NarutomodModVariables.sync(player);
        if (pressed) {
            return true;
        }
        if (isRinnesharinganActivated(head)) {
            if (player.level() instanceof ServerLevel serverLevel) {
                AdvancedNatureJutsuItem.placeYomotsuHirasakaPortals(serverLevel, player, true);
            }
            return true;
        }
        triggerEightTrigrams64Palms(player, true);
        return true;
    }

    private static boolean handleHakkeshoKaitenKey(ServerPlayer player, ItemStack head, boolean pressed) {
        if (isRinnesharinganActivated(head)) {
            handleRinnesharinganShockwave(player, pressed);
            return true;
        }
        handleHakkeshoKaiten(player, pressed, true);
        return true;
    }

    public static boolean handlePowerIncreaseKey(ServerPlayer player, boolean pressed) {
        if (player.isSpectator() || !player.isAlive()
                || !isByakuganHead(player.getItemBySlot(EquipmentSlot.HEAD))
                || !isActive(player)) {
            return false;
        }
        if (pressed) {
            setFov(player, getFov(player) - 1.0F);
        }
        return true;
    }

    public static void tick(ServerPlayer player) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!isByakuganHead(head)) {
            player.getPersistentData().putBoolean(HAKKE_KUSHO_HOLDING_TAG, false);
            ProcedureAirPunch.resetPressDuration(player);
            if (isActive(player)) {
                setActive(player, false, 0.0F);
            }
            return;
        }
        if (player.isCreative() && ProcedureUtils.getOwnerId(head) == null) {
            ProcedureUtils.setOriginalOwner(player, head);
        }
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 210, 0, false, false));
        NarutomodModVariables.get(player).putLong(NarutomodModVariables.MOST_RECENT_WORN_DOJUTSU_TIME, player.level().getGameTime());
        if (player.getPersistentData().getBoolean(HAKKE_KUSHO_HOLDING_TAG)) {
            tickHakkeKusho(player);
        }
        if (!isActive(player)) {
            return;
        }
        float fov = getFov(player);
        if (fov < MIN_FOV || fov > MAX_FOV) {
            setFov(player, DEFAULT_FOV);
        }
        if (player.tickCount % 10 == 0) {
            Chakra.pathway(player).consume(getByakuganChakraUsage(player));
        }
    }

    public static boolean isByakuganHead(ItemStack stack) {
        return ByakuganHelmetItem.isByakuganStack(stack);
    }

    public static boolean isRinnesharinganActivated(ItemStack stack) {
        return ByakuganHelmetItem.isRinnesharinganStack(stack);
    }

    public static boolean isActive(ServerPlayer player) {
        return player.getPersistentData().getBoolean(BYAKUGAN_ACTIVATED_TAG);
    }

    public static float getFov(ServerPlayer player) {
        return player.getPersistentData().contains(BYAKUGAN_FOV_TAG)
                ? player.getPersistentData().getFloat(BYAKUGAN_FOV_TAG)
                : DEFAULT_FOV;
    }

    public static double projectedCameraDistance(float fov, float ninjaLevel) {
        double scaledNinjaLevel = ninjaLevel / 3.0D;
        return ((DEFAULT_FOV - fov) * Math.min(scaledNinjaLevel, 70.0D) / 10.0D) + 1.0D;
    }

    public static int targetRenderDistance(float ninjaLevel) {
        int scaledNinjaLevel = (int)(ninjaLevel / 3.0F);
        return Mth.clamp(scaledNinjaLevel * 11 / 16, 16, 32);
    }

    public static double getByakuganChakraUsage(ServerPlayer player) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!isByakuganHead(head)) {
            return Double.MAX_VALUE * 0.001D;
        }
        return player.isCreative() || ProcedureUtils.isOriginalOwner(player, head)
                ? BYAKUGAN_CHAKRA_USAGE
                : BYAKUGAN_CHAKRA_USAGE * 2.0D;
    }

    public static double getRokujuyonshoChakraUsage(ServerPlayer player) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        return player.isCreative() || isByakuganHead(head) && ProcedureUtils.isOriginalOwner(player, head)
                ? ROKUJUYONSHO_CHAKRA_USAGE
                : Double.MAX_VALUE * 0.001D;
    }

    public static double getKaitenChakraUsage(ServerPlayer player) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        return player.isCreative() || isByakuganHead(head) && ProcedureUtils.isOriginalOwner(player, head)
                ? KAITEN_CHAKRA_USAGE_PER_TICK
                : Double.MAX_VALUE * 0.001D;
    }

    public static double getKushoChakraUsage(ServerPlayer player) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        return player.isCreative() || isByakuganHead(head) && ProcedureUtils.isOriginalOwner(player, head)
                ? KUSHO_CHAKRA_USAGE
                : Double.MAX_VALUE * 0.001D;
    }

    public static long getCooldownTicks(ServerPlayer player, String tagName) {
        return Math.max(0L, getCooldownUntil(player.getItemBySlot(EquipmentSlot.HEAD), tagName) - player.level().getGameTime());
    }

    public static void clearTechniqueCooldowns(ItemStack head) {
        CompoundTag tag = head.getTag();
        if (tag != null) {
            tag.remove(HAKKE_ROKUJUUYONSHOU_COOLDOWN_TAG);
            tag.remove(HAKKESHOKAITEN_COOLDOWN_TAG);
        }
    }

    public static double getRinnesharinganPressTime(ServerPlayer player) {
        return player.getPersistentData().getDouble(RINNESHARINGAN_PRESS_TIME_TAG);
    }

    public static boolean triggerEightTrigrams64Palms(ServerPlayer player, boolean showMessages) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!canUseOwnedByakuganTechnique(player, head, EIGHT_TRIGRAMS_REQUIRED_LEVEL, showMessages)) {
            return false;
        }
        long now = player.level().getGameTime();
        long cooldownUntil = getCooldownUntil(head, HAKKE_ROKUJUUYONSHOU_COOLDOWN_TAG);
        if (!player.isCreative() && cooldownUntil > now) {
            showCooldown(player, "Eight Trigrams Sixty-Four Palms", cooldownUntil - now, showMessages);
            return false;
        }
        double chakraCost = getRokujuyonshoChakraUsage(player);
        if (!consumeChakra(player, chakraCost)) {
            return false;
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.SOUND_HAKKEROKUJUUYONSHOU.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 240, 3, false, false));
        if (!EightTrigramsEntity.spawnFrom(player)) {
            refundChakra(player, chakraCost);
            if (showMessages) {
                player.displayClientMessage(Component.literal("Could not start Eight Trigrams Sixty-Four Palms."), true);
            }
            return false;
        }
        if (!player.isCreative()) {
            long cooldownTicks = Math.round(ProcedureUtils.getCooldownModifier(player) * EIGHT_TRIGRAMS_COOLDOWN_BASE_TICKS);
            setCooldownUntil(head, HAKKE_ROKUJUUYONSHOU_COOLDOWN_TAG, now + cooldownTicks);
        }
        return true;
    }

    public static boolean handleHakkeshoKaiten(ServerPlayer player, boolean pressed, boolean showMessages) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!canUseOwnedByakuganTechnique(player, head, HAKKESHOKAITEN_REQUIRED_LEVEL, showMessages)) {
            return false;
        }
        if (pressed) {
            return startHakkeshoKaiten(player, head, showMessages);
        }
        return releaseHakkeshoKaiten(player, head);
    }

    private static boolean startHakkeshoKaiten(ServerPlayer player, ItemStack head, boolean showMessages) {
        if (HakkeshoKeitenEntity.hasActiveFor(player)) {
            return HakkeshoKeitenEntity.spawnFrom(player) || player.getVehicle() instanceof HakkeshoKeitenEntity;
        }
        long now = player.level().getGameTime();
        long cooldownUntil = getCooldownUntil(head, HAKKESHOKAITEN_COOLDOWN_TAG);
        if (!player.isCreative() && cooldownUntil > now) {
            showCooldown(player, "Hakkesho Kaiten", cooldownUntil - now, showMessages);
            return false;
        }
        if (!player.isCreative() && Chakra.pathway(player).getAmount() < getKaitenChakraUsage(player)) {
            Chakra.pathway(player).warningDisplay();
            return false;
        }
        return HakkeshoKeitenEntity.spawnFrom(player);
    }

    private static boolean releaseHakkeshoKaiten(ServerPlayer player, ItemStack head) {
        int activeTicks = HakkeshoKeitenEntity.releaseOwnedBy(player);
        if (activeTicks <= 0) {
            return false;
        }
        if (!player.isCreative()) {
            long cooldownTicks = Math.round(ProcedureUtils.getCooldownModifier(player) * activeTicks * 5.0D);
            setCooldownUntil(head, HAKKESHOKAITEN_COOLDOWN_TAG, player.level().getGameTime() + cooldownTicks);
            int foodCost = activeTicks / 60 + 1;
            player.getFoodData().setFoodLevel(Math.max(0, player.getFoodData().getFoodLevel() - foodCost));
        }
        return true;
    }

    private static boolean handleHakkeKusho(ServerPlayer player, boolean pressed) {
        if (!canUseHakkeKusho(player, true)) {
            player.getPersistentData().putBoolean(HAKKE_KUSHO_HOLDING_TAG, false);
            ProcedureAirPunch.resetPressDuration(player);
            return true;
        }
        if (pressed) {
            player.getPersistentData().putBoolean(HAKKE_KUSHO_HOLDING_TAG, true);
            tickHakkeKusho(player);
            return true;
        }
        releaseHakkeKusho(player);
        return true;
    }

    private static void tickHakkeKusho(ServerPlayer player) {
        if (!canUseHakkeKusho(player, false)) {
            player.getPersistentData().putBoolean(HAKKE_KUSHO_HOLDING_TAG, false);
            ProcedureAirPunch.resetPressDuration(player);
            return;
        }
        int nextDuration = ProcedureAirPunch.getPressDuration(player) + 1;
        double chakraCost = getKushoChakraUsage(player) * nextDuration;
        if (!player.isCreative() && Chakra.pathway(player).getAmount() < chakraCost) {
            Chakra.pathway(player).warningDisplay();
            player.getPersistentData().putBoolean(HAKKE_KUSHO_HOLDING_TAG, false);
            ProcedureAirPunch.resetPressDuration(player);
            return;
        }
        HAKKE_KUSHO.execute(true, player);
    }

    private static void releaseHakkeKusho(ServerPlayer player) {
        int duration = ProcedureAirPunch.getPressDuration(player);
        player.getPersistentData().putBoolean(HAKKE_KUSHO_HOLDING_TAG, false);
        if (duration <= 0) {
            return;
        }
        double chakraCost = getKushoChakraUsage(player) * duration;
        if (!player.isCreative() && !Chakra.pathway(player).consume(chakraCost)) {
            ProcedureAirPunch.resetPressDuration(player);
            return;
        }
        player.swing(InteractionHand.MAIN_HAND, true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.SOUND_HAKKEKUSHO.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        HAKKE_KUSHO.execute(false, player);
    }

    private static boolean canUseHakkeKusho(ServerPlayer player, boolean showMessages) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!isByakuganHead(head)) {
            return false;
        }
        if (player.isCreative()) {
            return true;
        }
        if (!ProcedureUtils.isOriginalOwner(player, head)) {
            if (showMessages) {
                player.displayClientMessage(Component.literal("This Byakugan belongs to another player."), true);
            }
            return false;
        }
        if (PlayerTracker.getBattleXp(player) < HAKKE_KUSHO_REQUIRED_BATTLE_XP) {
            if (showMessages) {
                player.displayClientMessage(Component.literal("Requires battle XP "
                        + (int)HAKKE_KUSHO_REQUIRED_BATTLE_XP + "."), true);
            }
            return false;
        }
        return true;
    }

    public static boolean handleRinnesharinganShockwave(ServerPlayer player, boolean pressed) {
        if (pressed) {
            double pressTime = Math.min(getRinnesharinganPressTime(player) + 1.0D, RINNESHARINGAN_MAX_PRESS_TICKS);
            player.getPersistentData().putDouble(RINNESHARINGAN_PRESS_TIME_TAG, pressTime);
            player.displayClientMessage(Component.literal("Power: " + formatPower((pressTime - 1.0D) / 2.0D)), true);
            return true;
        }
        double pressTime = getRinnesharinganPressTime(player);
        double radius = Mth.clamp(pressTime / 2.0D, 0.0D, RINNESHARINGAN_MAX_PRESS_TICKS / 2.0D);
        player.getPersistentData().putDouble(RINNESHARINGAN_PRESS_TIME_TAG, 0.0D);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.SOUND_DOJUTSU_ACTIVATE.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ModParticleTypes.options(NarutoParticleKind.SMOKE_COLORED, 0x10FFFFFF, 30, 0, 0, -1, 4),
                    player.getX(),
                    player.getY() + 1.4D,
                    player.getZ(),
                    1000,
                    1.0D,
                    0.0D,
                    1.0D,
                    1.0D);
        }
        knockbackAround(player, radius);
        ProcedureUtils.purgeHarmfulEffects(player);
        player.clearFire();
        return true;
    }

    private static void knockbackAround(ServerPlayer player, double radius) {
        if (radius <= 0.0D) {
            return;
        }
        AABB box = player.getBoundingBox().inflate(radius);
        Vec3 center = player.position();
        for (Entity target : player.level().getEntities(player, box, entity -> isShockwaveTarget(player, entity, radius))) {
            ProcedureUtils.pushEntity(center, target, radius, 3.0F);
        }
    }

    private static boolean isShockwaveTarget(ServerPlayer player, Entity entity, double radius) {
        if (!entity.isAlive() || entity.isSpectator() || entity == player || entity.getRootVehicle() == player.getRootVehicle()) {
            return false;
        }
        if (entity.getPersistentData().getBoolean("kamui_intangible")) {
            return false;
        }
        return entity.distanceToSqr(player) <= radius * radius;
    }

    private static boolean canUseOwnedByakuganTechnique(ServerPlayer player, ItemStack head, int requiredLevel, boolean showMessages) {
        if (!isByakuganHead(head)) {
            return false;
        }
        if (player.isCreative()) {
            return true;
        }
        if (!ProcedureUtils.isOriginalOwner(player, head)) {
            if (showMessages) {
                player.displayClientMessage(Component.literal("This Byakugan belongs to another player."), true);
            }
            return false;
        }
        if (player.experienceLevel < requiredLevel) {
            if (showMessages) {
                player.displayClientMessage(Component.literal("Requires level " + requiredLevel + "."), true);
            }
            return false;
        }
        return true;
    }

    private static void setActive(ServerPlayer player, boolean active, float fov) {
        player.getPersistentData().putBoolean(BYAKUGAN_ACTIVATED_TAG, active);
        player.getPersistentData().putFloat(BYAKUGAN_FOV_TAG, active ? Mth.clamp(fov, MIN_FOV, MAX_FOV) : 0.0F);
        sync(player);
    }

    private static void setFov(ServerPlayer player, float fov) {
        player.getPersistentData().putFloat(BYAKUGAN_FOV_TAG, Mth.clamp(fov, MIN_FOV, MAX_FOV));
        sync(player);
    }

    private static void sync(ServerPlayer player) {
        NetworkHandler.sendToPlayer(player, new ByakuganViewSyncMessage(
                isActive(player),
                getFov(player),
                (float)PlayerTracker.getNinjaLevel(player)));
    }

    private static boolean consumeChakra(ServerPlayer player, double amount) {
        return player.isCreative() || Chakra.pathway(player).consume(amount);
    }

    private static void refundChakra(ServerPlayer player, double amount) {
        if (!player.isCreative()) {
            Chakra.pathway(player).consume(-amount, true);
        }
    }

    private static long getCooldownUntil(ItemStack stack, String tagName) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(tagName) ? tag.getLong(tagName) : 0L;
    }

    private static void setCooldownUntil(ItemStack stack, String tagName, long cooldownUntil) {
        stack.getOrCreateTag().putLong(tagName, Math.max(0L, cooldownUntil));
    }

    private static void showCooldown(ServerPlayer player, String name, long remainingTicks, boolean showMessages) {
        if (showMessages) {
            player.displayClientMessage(Component.literal(name + " cooldown " + String.format("%.1f", remainingTicks / 20.0D) + "s"), true);
        }
    }

    private static String formatPower(double power) {
        if (power == (long)power) {
            return Long.toString((long)power);
        }
        return String.format("%.1f", power);
    }

    private static final class HakkeKushoAirPunch extends ProcedureAirPunch {
        @Override
        protected double getRange(int duration) {
            return duration / 3.0D + 5.0D;
        }

        @Override
        protected double getFarRadius(int duration) {
            return duration / 20.0D;
        }

        @Override
        protected void attackEntityFrom(LivingEntity user, Entity target, double distanceToCenter) {
            super.attackEntityFrom(user, target, distanceToCenter);
            if (user instanceof Player player && target instanceof LivingEntity living) {
                int strength = ProcedureAirPunch.getPressDuration(user);
                double distance = Math.sqrt(Math.max(user.distanceToSqr(target), 0.001D));
                float damage = (float)((strength * player.experienceLevel / 100.0D + 10.0D) / distance);
                living.invulnerableTime = 0;
                living.hurt(ModDamageTypes.ninjutsu(user.level(), user, user), damage);
            }
        }

        @Override
        protected boolean processAffectedBlock(LivingEntity user, BlockPos pos, double range,
                double distanceToCenter, RandomSource random) {
            if (user.level() instanceof ServerLevel level
                    && ForgeEventFactory.getMobGriefingEvent(level, user)
                    && level.getBlockState(pos.above()).isAir()) {
                BlockState state = level.getBlockState(pos);
                if (!state.isAir() && state.getDestroySpeed(level, pos) >= 0.0F) {
                    FallingBlockEntity falling = FallingBlockEntity.fall(level, pos, state);
                    falling.setDeltaMovement(falling.getDeltaMovement().add(0.0D, 0.45D, 0.0D));
                    falling.hasImpulse = true;
                    return true;
                }
            }
            return super.processAffectedBlock(user, pos, range, distanceToCenter, random);
        }

        @Override
        protected float getBreakChance(LivingEntity user, BlockPos pos, double range, double distanceToCenter) {
            if (user instanceof Player player
                    && user.level() instanceof ServerLevel level
                    && ForgeEventFactory.getMobGriefingEvent(level, user)
                    && PlayerTracker.getBattleXp(player) >= HAKKE_KUSHO_BREAK_BATTLE_XP) {
                return (float)(1.0D - ((Math.sqrt(distanceToCenter) - 4.0D) / Mth.clamp(range, 0.0D, 30.0D)));
            }
            return 0.0F;
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
            tick(player);
        }
    }
}
