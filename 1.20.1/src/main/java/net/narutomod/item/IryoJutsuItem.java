package net.narutomod.item;

import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.narutomod.Chakra;
import net.narutomod.NarutomodMod;
import net.narutomod.PlayerTracker;
import net.narutomod.entity.CellularActivationEntity;
import net.narutomod.entity.PoisonMistEntity;
import net.narutomod.particle.NarutoParticleKind;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModEffects;
import net.narutomod.registry.ModParticleTypes;
import net.narutomod.registry.ModSounds;

public final class IryoJutsuItem extends JutsuItem {
    public static final JutsuDefinition HEALING = JutsuDefinition.ranked(0, "entity.narutomod.healingjutsu", 'A', 0.25D);
    public static final JutsuDefinition POISON_MIST = JutsuDefinition.ranked(1, "entity.narutomod.poison_mist", 'B', 20.0D);
    public static final JutsuDefinition CELLULAR_ACTIVATION = JutsuDefinition.ranked(2, "entity.narutomod.cellular_activation", 'A', 0.0D);
    public static final JutsuDefinition ENHANCED_STRENGTH = JutsuDefinition.ranked(3, "entity.narutomod.enhanced_strength", 'A', 30.0D);
    private static final JutsuDefinition[] IRYO_JUTSUS = {
            HEALING,
            POISON_MIST,
            CELLULAR_ACTIVATION,
            ENHANCED_STRENGTH
    };
    private static final String ENHANCED_STRENGTH_ACTIVE_TAG = "isChakraEnhancedStrengthActive";
    private static final int HEALING_RANGE = 3;

    public IryoJutsuItem() {
        super(JutsuType.IRYO, IRYO_JUTSUS);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        JutsuDefinition currentJutsu = getCurrentJutsu(stack);
        if (isJutsu(currentJutsu, HEALING)) {
            if (!canActivateIryo(level, player, stack, HEALING, true)) {
                return InteractionResultHolder.fail(stack);
            }
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        if (isJutsu(currentJutsu, POISON_MIST)) {
            if (!canActivateIryo(level, player, stack, POISON_MIST, true)) {
                return InteractionResultHolder.fail(stack);
            }
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        if (isJutsu(currentJutsu, CELLULAR_ACTIVATION)) {
            if (!level.isClientSide) {
                toggleCellularActivation(level, player, stack);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (isJutsu(currentJutsu, ENHANCED_STRENGTH)) {
            if (!level.isClientSide) {
                toggleEnhancedStrength(level, player, stack);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (!level.isClientSide) {
            player.displayClientMessage(Component.literal("This Iryo jutsu is not ported yet."), true);
        }
        return InteractionResultHolder.fail(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        super.onUseTick(level, livingEntity, stack, remainingUseDuration);
        if (!(livingEntity instanceof Player player) || level.isClientSide) {
            return;
        }
        JutsuDefinition currentJutsu = getCurrentJutsu(stack);
        if (isJutsu(currentJutsu, HEALING)) {
            float power = getHealingPower(player, stack);
            double cost = HEALING.chakraUsage() * power;
            if (power <= 0.0F || (!player.isCreative() && !Chakra.pathway(player).consume(cost))) {
                player.stopUsingItem();
                return;
            }
            LivingEntity target = findHealingTarget(player);
            healTarget((ServerLevel) level, player, target, power);
        } else if (isJutsu(currentJutsu, POISON_MIST)) {
            tickPoisonMistCharge((ServerLevel) level, player, stack, remainingUseDuration);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int remainingUseDuration) {
        if (level.isClientSide || !(livingEntity instanceof Player player)) {
            return;
        }
        JutsuDefinition currentJutsu = getCurrentJutsu(stack);
        if (isJutsu(currentJutsu, HEALING)) {
            player.displayClientMessage(Component.literal("Healing Jutsu ended."), true);
        } else if (isJutsu(currentJutsu, POISON_MIST)) {
            activatePoisonMist(level, player, stack, remainingUseDuration);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (!level.isClientSide && entity instanceof Player player && isEnhancedStrengthActive(stack)) {
            if (!isOwnedByOrUnbound(player, stack)) {
                setEnhancedStrengthActive(stack, false);
                return;
            }
            int amplifier = Math.max((int)(getLegacyXpRatio(player, stack, ENHANCED_STRENGTH) * Chakra.getLevel(player) / 2.0D), 1);
            player.addEffect(new MobEffectInstance(ModEffects.CHAKRA_ENHANCED_STRENGTH.get(), 3, amplifier, true, false));
        }
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    public static boolean isEnhancedStrengthActive(ItemStack stack) {
        return stack.getTag() != null && stack.getTag().getBoolean(ENHANCED_STRENGTH_ACTIVE_TAG);
    }

    public static void setEnhancedStrengthActive(ItemStack stack, boolean active) {
        stack.getOrCreateTag().putBoolean(ENHANCED_STRENGTH_ACTIVE_TAG, active);
    }

    private void toggleEnhancedStrength(Level level, Player player, ItemStack stack) {
        if (isEnhancedStrengthActive(stack)) {
            setEnhancedStrengthActive(stack, false);
            player.removeEffect(ModEffects.CHAKRA_ENHANCED_STRENGTH.get());
            player.displayClientMessage(Component.literal("Chakra Enhanced Strength ended."), true);
            return;
        }
        if (!canActivateIryo(level, player, stack, ENHANCED_STRENGTH, true)) {
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(ENHANCED_STRENGTH.chakraUsage())) {
            return;
        }
        setEnhancedStrengthActive(stack, true);
        addJutsuXp(stack, ENHANCED_STRENGTH, 1);
        player.displayClientMessage(Component.literal("Chakra Enhanced Strength activated."), true);
    }

    private void tickPoisonMistCharge(ServerLevel level, Player player, ItemStack stack, int remainingUseDuration) {
        int usedTicks = getUseDuration(stack) - remainingUseDuration;
        float power = getPoisonMistPower(stack, player, remainingUseDuration);
        if (usedTicks % 20 == 0) {
            player.displayClientMessage(Component.literal(String.format("Poison Mist power %.1f", power)), true);
        }
        if (usedTicks % 5 == 0) {
            level.sendParticles(
                    ModParticleTypes.options(NarutoParticleKind.SMOKE_COLORED, 0x40630065, 40, 5, 0xF0, player.getId(), 4),
                    player.getX(),
                    player.getY() + 0.9D,
                    player.getZ(),
                    10,
                    0.2D,
                    0.0D,
                    0.2D,
                    0.05D);
        }
        if (usedTicks % 10 == 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.SOUND_CHARGING_CHAKRA.get(), SoundSource.PLAYERS, 0.05F, level.random.nextFloat() + 0.5F);
        }
    }

    private void activatePoisonMist(Level level, Player player, ItemStack stack, int remainingUseDuration) {
        if (!canActivateIryo(level, player, stack, POISON_MIST, true)) {
            return;
        }
        float power = getPoisonMistPower(stack, player, remainingUseDuration);
        double chakraCost = POISON_MIST.chakraUsage() * power;
        if (power <= 0.0F || (!player.isCreative() && !Chakra.pathway(player).consume(chakraCost))) {
            return;
        }
        if (PoisonMistEntity.spawnFrom(player, power)) {
            addJutsuXp(stack, POISON_MIST, 1);
        }
    }

    private void toggleCellularActivation(Level level, Player player, ItemStack stack) {
        if (!canActivateIryo(level, player, stack, CELLULAR_ACTIVATION, true)) {
            return;
        }
        boolean active = CellularActivationEntity.toggleFor(player);
        if (active) {
            addJutsuXp(stack, CELLULAR_ACTIVATION, 1);
            player.displayClientMessage(Component.literal("Cellular Activation activated."), true);
        } else {
            player.displayClientMessage(Component.literal("Cellular Activation ended."), true);
        }
    }

    private boolean canActivateIryo(Level level, Player player, ItemStack stack, JutsuDefinition definition, boolean report) {
        if (!player.isCreative() && !PlayerTracker.isNinja(player)) {
            if (report && !level.isClientSide) {
                player.displayClientMessage(Component.literal("You need ninja experience to use Medical Ninjutsu."), true);
            }
            return false;
        }
        if (!player.isCreative() && !isOwnedByOrUnbound(player, stack)) {
            if (report && !level.isClientSide) {
                player.displayClientMessage(Component.literal("This Medical Ninjutsu item belongs to another player."), true);
            }
            return false;
        }
        if (!level.isClientSide) {
            setOwnerIfMissing(stack, player);
        }
        if (!player.isCreative() && !isJutsuEnabled(stack, definition)) {
            if (report && !level.isClientSide) {
                player.displayClientMessage(Component.literal("You need to learn this Medical Ninjutsu from a scroll."), true);
            }
            return false;
        }
        if (!player.isCreative() && !hasEnoughJutsuXp(stack, definition)) {
            if (report && !level.isClientSide) {
                player.displayClientMessage(Component.literal("Iryo XP " + getJutsuXp(stack, definition)
                        + "/" + getRequiredXp(stack, definition)), true);
            }
            return false;
        }
        long cooldown = getRemainingCooldownTicks(stack, level, definition);
        if (cooldown > 0L) {
            if (report && !level.isClientSide) {
                player.displayClientMessage(Component.literal("Cooldown " + cooldown / 20L + "s"), true);
            }
            return false;
        }
        if (!player.isCreative() && Chakra.pathway(player).getAmount() < definition.chakraUsage()) {
            Chakra.pathway(player).warningDisplay();
            return false;
        }
        return true;
    }

    private float getHealingPower(Player player, ItemStack stack) {
        return (float)(Chakra.getLevel(player) * getLegacyXpRatio(player, stack, HEALING) / 15.0D);
    }

    private float getPoisonMistPower(ItemStack stack, Player player, int remainingUseDuration) {
        float power = getChargingPower(stack, player, remainingUseDuration, 5.0F, 15.0F);
        if (player.isCreative()) {
            return power;
        }
        float maxPower = (float)(Chakra.pathway(player).getAmount() / POISON_MIST.chakraUsage() * 0.9999D);
        return Math.min(power, Math.max(maxPower, 0.0F));
    }

    private double getLegacyXpRatio(Player player, ItemStack stack, JutsuDefinition definition) {
        if (player.isCreative()) {
            return 1.0D;
        }
        int required = Math.max(getRequiredXp(stack, definition), 1);
        return Mth.clamp((double) getJutsuXp(stack, definition) / (double) required, 0.0D, 1.0D);
    }

    private static LivingEntity findHealingTarget(Player player) {
        HitResult hit = ProcedureUtils.objectEntityLookingAt(player, HEALING_RANGE, 0.15D, true, false,
                target -> target instanceof LivingEntity);
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity target) {
            return target;
        }
        return player;
    }

    private static void healTarget(ServerLevel level, Player healer, LivingEntity target, float power) {
        if (healer.tickCount % 3 == 0) {
            level.playSound(null, target.getX(), target.getY(), target.getZ(),
                    ModSounds.SOUND_WINDECHO.get(), SoundSource.NEUTRAL, 0.5F,
                    Mth.sin((float) healer.tickCount * 0.1F) * 0.8F + 1.5F);
        }
        level.sendParticles(
                ModParticleTypes.options(NarutoParticleKind.SMOKE_COLORED,
                        0x2000FFF6 | (level.random.nextInt(0x20) << 24),
                        10 + level.random.nextInt(25),
                        0,
                        0xF0,
                        -1,
                        0),
                target.getX(),
                target.getY() + target.getBbHeight() * 0.5D,
                target.getZ(),
                10,
                0.0D,
                0.0D,
                0.0D,
                0.0D);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 6, false, false));
        target.heal(power * 0.01F);
    }

    private static boolean isJutsu(JutsuDefinition first, JutsuDefinition second) {
        return first.index() == second.index() && first.translationKey().equals(second.translationKey());
    }

    @Mod.EventBusSubscriber(modid = NarutomodMod.MODID)
    public static final class ForgeEvents {
        private ForgeEvents() {
        }

        @SubscribeEvent
        public static void onLivingHurt(LivingHurtEvent event) {
            Entity source = event.getSource().getEntity();
            if (!(source instanceof LivingEntity attacker) || !attacker.hasEffect(ModEffects.CHAKRA_ENHANCED_STRENGTH.get())) {
                return;
            }
            MobEffectInstance effect = attacker.getEffect(ModEffects.CHAKRA_ENHANCED_STRENGTH.get());
            int strength = effect == null ? 0 : Math.max(effect.getAmplifier(), 1);
            if (strength <= 0 || !Chakra.pathway(attacker).consume((double) strength)) {
                return;
            }
            LivingEntity target = event.getEntity();
            Level level = target.level();
            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.GENERIC_EXPLODE,
                    SoundSource.BLOCKS, 1.0F, (1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.2F) * 0.7F);
            ProcedureUtils.pushEntity(attacker, target, 10.0D, 0.1F * strength);
            event.setAmount(event.getAmount() + strength);
        }

        @SubscribeEvent
        public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
            Player player = event.getEntity();
            MobEffectInstance effect = player.getEffect(ModEffects.CHAKRA_ENHANCED_STRENGTH.get());
            if (effect == null || !effect.isAmbient()) {
                return;
            }
            int strength = Math.max(effect.getAmplifier(), 1);
            if (Chakra.pathway(player).consume(0.05D * strength)) {
                event.setNewSpeed(event.getOriginalSpeed() * (1.0F + 0.15F * strength));
            }
        }
    }
}
