package net.narutomod.item;

import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.narutomod.Chakra;
import net.narutomod.NarutomodMod;
import net.narutomod.PlayerTracker;
import net.narutomod.entity.BugSwarmEntity;
import net.narutomod.entity.KageBunshinEntity;
import net.narutomod.entity.LimboCloneEntity;
import net.narutomod.entity.AbstractPuppetEntity;
import net.narutomod.entity.ReplacementCloneEntity;
import net.narutomod.entity.RasenganEntity;
import net.narutomod.entity.SealingChainsEntity;
import net.narutomod.entity.TransformationJutsuEntity;
import net.narutomod.particle.NarutoParticleKind;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModParticleTypes;
import net.narutomod.registry.ModSounds;

public class NinjutsuItem extends JutsuItem {
    public static final JutsuDefinition REPLACEMENT = JutsuDefinition.ranked(0, "replacementclone", 'D', 30.0D);
    public static final JutsuDefinition KAGE_BUNSHIN = JutsuDefinition.ranked(1, "kage_bunshin", 'B', 0.0D);
    public static final JutsuDefinition RASENGAN = JutsuDefinition.ranked(2, "rasengan", 'A', 150.0D);
    public static final JutsuDefinition LIMBO_CLONE = JutsuDefinition.ranked(3, "limbo_clone", 'S', LimboCloneEntity.CHAKRA_USAGE);
    public static final JutsuDefinition SEALING_CHAIN = JutsuDefinition.ranked(4, "sealing_chains", 'A', 50.0D);
    public static final JutsuDefinition PUPPET = JutsuDefinition.ranked(5, "tooltip.ninjutsu.puppetjutsu", 'C', 0.25D);
    public static final JutsuDefinition BUG_SWARM = JutsuDefinition.ranked(6, "bugball", 'C', 100.0D);
    public static final JutsuDefinition INVISIBILITY = JutsuDefinition.ranked(7, "tooltip.ninjutsu.hidingincamouflage", 'A', 20.0D);
    public static final JutsuDefinition TRANSFORM = JutsuDefinition.ranked(8, "transformation_jutsu", 'D', 50.0D);
    private static final JutsuDefinition[] NINJUTSU_JUTSUS = {
            REPLACEMENT,
            KAGE_BUNSHIN,
            RASENGAN,
            LIMBO_CLONE,
            SEALING_CHAIN,
            PUPPET,
            BUG_SWARM,
            INVISIBILITY,
            TRANSFORM
    };
    public static final String RASENGAN_LEARNED_TAG = "RasenganLearned";
    public static final String RASENGAN_SIZE_TAG = "RasenganSize";
    public static final String REPLACEMENT_LAST_USE_TAG = "ReplacementJutsuLastUse";
    public static final String HIDING_CAMOUFLAGE_ACTIVE_TAG = "HidingWithCamouflageActive";
    private static final int REPLACEMENT_COOLDOWN_TICKS = 100;
    private static final float BUG_SWARM_POWERUP_DELAY = 100.0F;
    private static final float BUG_SWARM_MIN_POWER = 0.1F;
    private static final RasenganProfile NINJUTSU_RASENGAN = new RasenganProfile("Rasengan", 150.0D, 0.0F, 0.5F, 3.0F, 200.0F, 40, false);
    private static final int MAX_USE_DURATION = 72000;
    private final RasenganProfile rasenganProfile;

    public NinjutsuItem() {
        this(NINJUTSU_RASENGAN);
    }

    protected NinjutsuItem(RasenganProfile rasenganProfile) {
        this(rasenganProfile, JutsuType.NINJUTSU, NINJUTSU_JUTSUS);
    }

    protected NinjutsuItem(RasenganProfile rasenganProfile, JutsuType jutsuType, JutsuDefinition... definitions) {
        super(jutsuType, definitions);
        this.rasenganProfile = rasenganProfile;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        JutsuDefinition currentJutsu = getCurrentJutsu(stack);
        if (isJutsu(currentJutsu, REPLACEMENT)) {
            return toggleReplacement(level, player, stack);
        }
        if (isJutsu(currentJutsu, KAGE_BUNSHIN)) {
            return useKageBunshin(level, player, stack);
        }
        if (isJutsu(currentJutsu, LIMBO_CLONE)) {
            return useLimboClone(level, player, stack);
        }
        if (isJutsu(currentJutsu, SEALING_CHAIN)) {
            return useSealingChains(level, player, stack);
        }
        if (isJutsu(currentJutsu, PUPPET)) {
            return usePuppet(level, player, stack);
        }
        if (isJutsu(currentJutsu, BUG_SWARM)) {
            return beginBugSwarmCharge(level, player, hand, stack);
        }
        if (isJutsu(currentJutsu, INVISIBILITY)) {
            return toggleHidingInCamouflage(level, player, stack);
        }
        if (isJutsu(currentJutsu, TRANSFORM)) {
            return useTransformation(level, player, stack);
        }
        if (!isJutsu(currentJutsu, RASENGAN)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("This Ninjutsu jutsu is not ported yet."), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        if (!canBeginCharge(level, player, stack)) {
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (level.isClientSide || !(entity instanceof LivingEntity living) || !isHidingInCamouflageActive(stack)) {
            return;
        }
        living.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 21, 0, false, false));
        if (living.tickCount % 20 != 0) {
            return;
        }
        if (!Chakra.pathway(living).consume(INVISIBILITY.chakraUsage())) {
            setHidingInCamouflageActive(stack, false);
        }
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        if (!(livingEntity instanceof Player player) || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        int usedTicks = getUseDuration(stack) - remainingUseDuration;
        if (usedTicks % 5 == 0) {
            serverLevel.sendParticles(
                    ModParticleTypes.options(NarutoParticleKind.SMOKE_COLORED, 0x106AD1FF, 40, 5, 0xF0, player.getId(), 4),
                    player.getX(),
                    player.getY() + 0.9D,
                    player.getZ(),
                    12,
                    0.2D,
                    0.0D,
                    0.2D,
                    0.05D
            );
        }
        if (usedTicks % 10 == 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.SOUND_CHARGING_CHAKRA.get(), SoundSource.PLAYERS, 0.05F, level.random.nextFloat() + 0.5F);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int remainingUseDuration) {
        if (!(livingEntity instanceof Player player) || level.isClientSide) {
            return;
        }
        JutsuDefinition currentJutsu = getCurrentJutsu(stack);
        if (isJutsu(currentJutsu, BUG_SWARM)) {
            releaseBugSwarm(stack, level, player, remainingUseDuration);
            return;
        }
        if (!isJutsu(currentJutsu, RASENGAN)) {
            return;
        }

        float power = getRasenganPower(stack, player, remainingUseDuration);
        if (power < rasenganProfile.minPower()) {
            player.displayClientMessage(Component.literal(String.format("%s power %.1f/%.1f",
                    rasenganProfile.displayName(), power, rasenganProfile.minPower())), true);
            return;
        }

        double chakraCost = rasenganProfile.chakraUsage() * power;
        if (!player.isCreative() && !Chakra.pathway(player).consume(chakraCost)) {
            return;
        }

        RasenganEntity entity = ModEntityTypes.RASENGAN.get().create(level);
        if (entity == null) {
            return;
        }
        stack.getOrCreateTag().putFloat(RASENGAN_SIZE_TAG, power);
        entity.configureAttached(player, power, rasenganProfile.senjutsuDamage());
        level.addFreshEntity(entity);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.SOUND_RASENGAN_START.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        player.getCooldowns().addCooldown(this, rasenganProfile.cooldownTicks());
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            PlayerTracker.logBattleExp(serverPlayer, Math.max(1.0D, power));
        }
    }

    @Override
    public boolean activateCloneHeldJutsus(ItemStack stack, LivingEntity clone) {
        if (clone.level().isClientSide) {
            return false;
        }
        boolean activated = false;
        if (isReplacementActive(stack)) {
            setReplacementActive(stack, false, clone.level().getGameTime());
        }
        if (isHidingInCamouflageActive(stack)) {
            setHidingInCamouflageActive(stack, false);
        }
        float rasenganPower = getStoredRasenganPower(stack);
        if (rasenganPower >= rasenganProfile.minPower()) {
            activated = spawnCloneRasengan(stack, clone, rasenganPower);
        }
        return activated;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return MAX_USE_DURATION;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    public static void clearRasenganSize(Player player) {
        clearRasenganSize(player.getMainHandItem());
        clearRasenganSize(player.getOffhandItem());
    }

    public static boolean isHidingInCamouflageActive(ItemStack stack) {
        return stack.getTag() != null && stack.getTag().getBoolean(HIDING_CAMOUFLAGE_ACTIVE_TAG);
    }

    public static void setHidingInCamouflageActive(ItemStack stack, boolean active) {
        stack.getOrCreateTag().putBoolean(HIDING_CAMOUFLAGE_ACTIVE_TAG, active);
    }

    public static boolean isReplacementActive(ItemStack stack) {
        return stack.getTag() != null && stack.getTag().contains(REPLACEMENT_LAST_USE_TAG);
    }

    public static void setReplacementActive(ItemStack stack, boolean active, long gameTime) {
        if (active) {
            setReplacementLastUse(stack, gameTime);
        } else if (stack.getTag() != null) {
            stack.getTag().remove(REPLACEMENT_LAST_USE_TAG);
        }
    }

    public static long getReplacementLastUse(ItemStack stack) {
        return stack.getTag() != null ? stack.getTag().getLong(REPLACEMENT_LAST_USE_TAG) : 0L;
    }

    public static void setReplacementLastUse(ItemStack stack, long gameTime) {
        stack.getOrCreateTag().putLong(REPLACEMENT_LAST_USE_TAG, gameTime);
    }

    private static void clearRasenganSize(ItemStack stack) {
        if (stack.getItem() instanceof NinjutsuItem && stack.getTag() != null) {
            stack.getTag().remove(RASENGAN_SIZE_TAG);
        }
    }

    private float getStoredRasenganPower(ItemStack stack) {
        return stack.getTag() != null ? stack.getTag().getFloat(RASENGAN_SIZE_TAG) : 0.0F;
    }

    private boolean spawnCloneRasengan(ItemStack stack, LivingEntity clone, float power) {
        RasenganEntity entity = ModEntityTypes.RASENGAN.get().create(clone.level());
        if (entity == null) {
            return false;
        }
        stack.getOrCreateTag().putFloat(RASENGAN_SIZE_TAG, power);
        entity.configureAttached(clone, power, rasenganProfile.senjutsuDamage());
        clone.level().addFreshEntity(entity);
        clone.level().playSound(null, clone.getX(), clone.getY(), clone.getZ(),
                ModSounds.SOUND_RASENGAN_START.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        return true;
    }

    private boolean canBeginCharge(Level level, Player player, ItemStack stack) {
        if (player.getCooldowns().isOnCooldown(this)) {
            return false;
        }
        if (!hasUsePrerequisites(level, player, stack)) {
            return false;
        }
        return player.isCreative() || getMaxRasenganPower(player) >= rasenganProfile.minPower();
    }

    private InteractionResultHolder<ItemStack> toggleReplacement(Level level, Player player, ItemStack stack) {
        if (isReplacementActive(stack)) {
            if (!level.isClientSide) {
                setReplacementActive(stack, false, level.getGameTime());
            }
            return InteractionResultHolder.success(stack);
        }
        if (!canActivateJutsu(level, player, stack, REPLACEMENT)) {
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide) {
            setReplacementActive(stack, true, level.getGameTime());
            addJutsuXp(stack, REPLACEMENT, 1);
        }
        return InteractionResultHolder.consume(stack);
    }

    private InteractionResultHolder<ItemStack> useKageBunshin(Level level, Player player, ItemStack stack) {
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                int removed = KageBunshinEntity.removeAllFor(player);
                player.displayClientMessage(Component.literal("Removed " + removed + " Shadow Clone(s)."), true);
            }
            return InteractionResultHolder.success(stack);
        }
        if (!canActivateJutsu(level, player, stack, KAGE_BUNSHIN)) {
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide) {
            if (!KageBunshinEntity.spawnFrom(player)) {
                return InteractionResultHolder.fail(stack);
            }
            addJutsuXp(stack, KAGE_BUNSHIN, 1);
        }
        return InteractionResultHolder.consume(stack);
    }

    private InteractionResultHolder<ItemStack> useLimboClone(Level level, Player player, ItemStack stack) {
        if (!LimboCloneEntity.getLimboClones(player).isEmpty()) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("Limbo Clone is already active."), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        if (!canActivateJutsu(level, player, stack, LIMBO_CLONE)) {
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide) {
            if (!player.isCreative() && !Chakra.pathway(player).consume(LimboCloneEntity.CHAKRA_USAGE)) {
                return InteractionResultHolder.fail(stack);
            }
            if (!LimboCloneEntity.spawnPairFrom(player)) {
                if (!player.isCreative()) {
                    Chakra.pathway(player).consume(-LimboCloneEntity.CHAKRA_USAGE, false);
                }
                return InteractionResultHolder.fail(stack);
            }
            setJutsuCooldown(stack, level, LIMBO_CLONE, LimboCloneEntity.COOLDOWN_TICKS);
            addJutsuXp(stack, LIMBO_CLONE, 1);
        }
        return InteractionResultHolder.consume(stack);
    }

    private InteractionResultHolder<ItemStack> useSealingChains(Level level, Player player, ItemStack stack) {
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                int retracted = SealingChainsEntity.retractOwnedNear(player, 4.0D);
                player.displayClientMessage(Component.literal("Retracted " + retracted + " Sealing Chain(s)."), true);
            }
            return InteractionResultHolder.success(stack);
        }
        if (!canActivateJutsu(level, player, stack, SEALING_CHAIN)) {
            return InteractionResultHolder.fail(stack);
        }
        LivingEntity target = SealingChainsEntity.findTarget(player);
        if (target == null) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("No valid Sealing Chains target."), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide) {
            if (!player.isCreative() && !Chakra.pathway(player).consume(SEALING_CHAIN.chakraUsage())) {
                return InteractionResultHolder.fail(stack);
            }
            if (!SealingChainsEntity.spawnFrom(player, target)) {
                if (!player.isCreative()) {
                    Chakra.pathway(player).consume(-SEALING_CHAIN.chakraUsage(), false);
                }
                return InteractionResultHolder.fail(stack);
            }
            addJutsuXp(stack, SEALING_CHAIN, 1);
        }
        return InteractionResultHolder.consume(stack);
    }

    private InteractionResultHolder<ItemStack> usePuppet(Level level, Player player, ItemStack stack) {
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                int released = AbstractPuppetEntity.releaseOwnedNear(player, 32.0D);
                player.displayClientMessage(Component.literal("Released " + released + " Puppet(s)."), true);
            }
            return InteractionResultHolder.success(stack);
        }
        if (!canActivateJutsu(level, player, stack, PUPPET)) {
            return InteractionResultHolder.fail(stack);
        }
        AbstractPuppetEntity puppet = findLookedAtPuppet(player);
        if (puppet == null) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("Look at a puppet within 4 blocks."), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide) {
            if (!player.isCreative() && !Chakra.pathway(player).consume(PUPPET.chakraUsage())) {
                return InteractionResultHolder.fail(stack);
            }
            puppet.bindTo(player);
            addJutsuXp(stack, PUPPET, 1);
        }
        return InteractionResultHolder.consume(stack);
    }

    private InteractionResultHolder<ItemStack> beginBugSwarmCharge(Level level, Player player, InteractionHand hand, ItemStack stack) {
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                int returning = BugSwarmEntity.returnOwnedNear(player, 32.0D);
                player.displayClientMessage(Component.literal("Returning " + returning + " Bug Swarm(s)."), true);
            }
            return InteractionResultHolder.success(stack);
        }
        if (!canActivateJutsu(level, player, stack, BUG_SWARM)) {
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    private void releaseBugSwarm(ItemStack stack, Level level, Player player, int remainingUseDuration) {
        BugSwarmEntity existingSwarm = BugSwarmEntity.findLookedAtSwarm(player);
        if (existingSwarm != null) {
            existingSwarm.triggerReturn();
            player.displayClientMessage(Component.literal("Bug Swarm returning."), true);
            return;
        }

        float power = getBugSwarmPower(stack, player, remainingUseDuration);
        if (power < BUG_SWARM_MIN_POWER) {
            player.displayClientMessage(Component.literal(String.format("Bug Swarm power %.1f/%.1f", power, BUG_SWARM_MIN_POWER)), true);
            return;
        }
        LivingEntity target = BugSwarmEntity.findTarget(player);
        if (target == null) {
            player.displayClientMessage(Component.literal("No valid Bug Swarm target."), true);
            return;
        }

        double chakraCost = BUG_SWARM.chakraUsage() * power;
        if (!player.isCreative() && !Chakra.pathway(player).consume(chakraCost)) {
            return;
        }
        if (!BugSwarmEntity.spawnFrom(player, target, power)) {
            if (!player.isCreative()) {
                Chakra.pathway(player).consume(-chakraCost, false);
            }
            return;
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.SOUND_BUGS.get(), SoundSource.PLAYERS, 0.8F, level.random.nextFloat() * 0.4F + 0.8F);
        addJutsuXp(stack, BUG_SWARM, 1);
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            PlayerTracker.logBattleExp(serverPlayer, Math.max(1.0D, power));
        }
    }

    private InteractionResultHolder<ItemStack> useTransformation(Level level, Player player, ItemStack stack) {
        if (player.isShiftKeyDown() || TransformationJutsuEntity.getActiveFor(player) != null) {
            if (!level.isClientSide) {
                int stopped = TransformationJutsuEntity.stopFor(player);
                player.displayClientMessage(Component.literal("Released " + stopped + " Transformation Jutsu."), true);
            }
            return InteractionResultHolder.success(stack);
        }
        if (!canActivateJutsu(level, player, stack, TRANSFORM)) {
            return InteractionResultHolder.fail(stack);
        }
        LivingEntity target = TransformationJutsuEntity.findTarget(player);
        if (target == null) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("No valid Transformation target."), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide) {
            if (!player.isCreative() && !Chakra.pathway(player).consume(TRANSFORM.chakraUsage())) {
                return InteractionResultHolder.fail(stack);
            }
            if (!TransformationJutsuEntity.spawnFrom(player, target)) {
                if (!player.isCreative()) {
                    Chakra.pathway(player).consume(-TRANSFORM.chakraUsage(), false);
                }
                return InteractionResultHolder.fail(stack);
            }
            addJutsuXp(stack, TRANSFORM, 1);
        }
        return InteractionResultHolder.consume(stack);
    }

    private InteractionResultHolder<ItemStack> toggleHidingInCamouflage(Level level, Player player, ItemStack stack) {
        boolean active = isHidingInCamouflageActive(stack);
        if (active) {
            if (!level.isClientSide) {
                setHidingInCamouflageActive(stack, false);
            }
            return InteractionResultHolder.success(stack);
        }
        if (!canActivateJutsu(level, player, stack, INVISIBILITY)) {
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide) {
            setHidingInCamouflageActive(stack, true);
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 21, 0, false, false));
            addJutsuXp(stack, INVISIBILITY, 1);
        }
        return InteractionResultHolder.consume(stack);
    }

    private boolean canActivateJutsu(Level level, Player player, ItemStack stack, JutsuDefinition definition) {
        if (player.isCreative()) {
            return true;
        }
        if (!PlayerTracker.isNinja(player)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("You need ninja experience to use this Ninjutsu jutsu."), true);
            }
            return false;
        }
        if (!isOwnedByOrUnbound(player, stack)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("This jutsu item belongs to another player."), true);
            }
            return false;
        }
        if (!level.isClientSide) {
            setOwnerIfMissing(stack, player);
        }
        if (!canUseJutsu(stack, definition, player)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("You need to learn this Ninjutsu jutsu from a scroll."), true);
            }
            return false;
        }
        if (!hasEnoughJutsuXp(stack, definition)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("Ninjutsu XP " + getJutsuXp(stack, definition)
                        + "/" + getRequiredXp(stack, definition)), true);
            }
            return false;
        }
        long cooldown = getRemainingCooldownTicks(stack, level, definition);
        if (cooldown > 0L) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("Cooldown " + cooldown / 20L + "s"), true);
            }
            return false;
        }
        if (Chakra.pathway(player).getAmount() < definition.chakraUsage()) {
            Chakra.pathway(player).warningDisplay();
            return false;
        }
        return true;
    }

    protected boolean hasUsePrerequisites(Level level, Player player, ItemStack stack) {
        if (!player.isCreative() && !PlayerTracker.isNinja(player)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("You need ninja experience to use " + rasenganProfile.displayName() + "."), true);
            }
            return false;
        }
        if (!player.isCreative() && !isOwnedByOrUnbound(player, stack)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("This jutsu item belongs to another player."), true);
            }
            return false;
        }
        if (!level.isClientSide) {
            setOwnerIfMissing(stack, player);
        }
        if (!player.isCreative() && !isRasenganLearnedForUse(player, stack)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("You need to learn " + rasenganProfile.displayName() + " from a scroll."), true);
            }
            return false;
        }
        return true;
    }

    protected boolean isRasenganLearnedForUse(Player player, ItemStack stack) {
        return hasLearnedRasengan(stack);
    }

    @Override
    protected void onMaxedOut(ItemStack stack) {
        super.onMaxedOut(stack);
        setRasenganLearned(stack, true);
    }

    public static boolean hasLearnedRasengan(ItemStack stack) {
        return stack.getTag() != null && stack.getTag().getBoolean(RASENGAN_LEARNED_TAG);
    }

    public static void setRasenganLearned(ItemStack stack, boolean learned) {
        if (learned) {
            stack.getOrCreateTag().putBoolean(RASENGAN_LEARNED_TAG, true);
        } else if (stack.getTag() != null) {
            stack.getTag().remove(RASENGAN_LEARNED_TAG);
        }
    }

    public static boolean hasAnyLearnedRasengan(Player player) {
        for (ItemStack stack : ProcedureUtils.getAllItemsOfSubType(player, NinjutsuItem.class)) {
            if (stack.getItem().getClass() == NinjutsuItem.class
                    && hasLearnedRasengan(stack)
                    && isOwnedByOrUnbound(player, stack)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public static ItemStack findOwnedOrUnboundNinjutsuStack(Player player) {
        for (ItemStack stack : ProcedureUtils.getAllItemsOfSubType(player, NinjutsuItem.class)) {
            if (stack.getItem().getClass() == NinjutsuItem.class && isOwnedByOrUnbound(player, stack)) {
                return stack;
            }
        }
        return null;
    }

    private float getRasenganPower(ItemStack stack, Player player, int remainingUseDuration) {
        int usedTicks = getUseTicks(stack, remainingUseDuration);
        double modifier = Math.max(Chakra.getChakraModifier(player), 0.05D);
        float chargePower = rasenganProfile.basePower() + (float)(usedTicks / (rasenganProfile.powerupDelay() * modifier));
        return Math.min(chargePower, getMaxRasenganPower(player));
    }

    private float getBugSwarmPower(ItemStack stack, Player player, int remainingUseDuration) {
        float chargePower = getChargingPower(stack, player, remainingUseDuration, 0.0F, BUG_SWARM_POWERUP_DELAY);
        return Math.min(chargePower, getMaxBugSwarmPower(player));
    }

    private static int getUseTicks(ItemStack stack, int remainingUseDuration) {
        return Math.max(stack.getUseDuration() - remainingUseDuration, 0);
    }

    private float getMaxRasenganPower(Player player) {
        if (player.isCreative()) {
            return rasenganProfile.maxPower();
        }
        return Math.min((float)(Chakra.pathway(player).getAmount() / rasenganProfile.chakraUsage() * 0.9999D), rasenganProfile.maxPower());
    }

    private float getMaxBugSwarmPower(Player player) {
        if (player.isCreative()) {
            return 10.0F;
        }
        return Math.max((float)(Chakra.pathway(player).getAmount() / BUG_SWARM.chakraUsage() * 0.9999D), 0.0F);
    }

    private static boolean isJutsu(JutsuDefinition first, JutsuDefinition second) {
        return first.index() == second.index() && first.translationKey().equals(second.translationKey());
    }

    @Nullable
    private static AbstractPuppetEntity findLookedAtPuppet(Player player) {
        net.minecraft.world.phys.HitResult hit = ProcedureUtils.objectEntityLookingAt(player, 4.0D, 0.25D, true, false,
                target -> target instanceof AbstractPuppetEntity);
        return hit instanceof net.minecraft.world.phys.EntityHitResult entityHit
                && entityHit.getEntity() instanceof AbstractPuppetEntity puppet ? puppet : null;
    }

    @Mod.EventBusSubscriber(modid = NarutomodMod.MODID)
    public static final class ForgeEvents {
        private ForgeEvents() {
        }

        @SubscribeEvent
        public static void onLivingTick(LivingEvent.LivingTickEvent event) {
            if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof Mob mob)) {
                return;
            }
            LivingEntity target = mob.getTarget();
            if (target != null && target.isInvisible() && !wearsInvisibleTargetCounter(mob)) {
                mob.setTarget(null);
            }
        }

        private static boolean wearsInvisibleTargetCounter(Mob mob) {
            ItemStack head = mob.getItemBySlot(EquipmentSlot.HEAD);
            return SusanooPowerIncreaseHandler.isSharinganHead(head) || ByakuganHandler.isByakuganHead(head);
        }

        @SubscribeEvent
        public static void onLivingAttack(LivingAttackEvent event) {
            if (event.getEntity().level().isClientSide) {
                return;
            }
            if (event.getSource().is(DamageTypes.FELL_OUT_OF_WORLD)) {
                return;
            }
            if (TransformationJutsuEntity.interceptAttack(event.getEntity(), event.getSource(), event.getAmount())) {
                event.setCanceled(true);
                return;
            }
            if (LimboCloneEntity.interceptAttack(event.getEntity(), event.getSource(), event.getAmount())) {
                event.setCanceled(true);
                return;
            }
            if (!(event.getEntity() instanceof Player player)) {
                return;
            }
            ItemStack stack = findActiveReplacementStack(player);
            if (stack == null) {
                return;
            }
            long gameTime = player.level().getGameTime();
            if (gameTime <= getReplacementLastUse(stack) + REPLACEMENT_COOLDOWN_TICKS) {
                return;
            }
            if (!player.isCreative() && !Chakra.pathway(player).consume(REPLACEMENT.chakraUsage())) {
                return;
            }
            if (!ReplacementCloneEntity.spawnFrom(player, event.getSource().getEntity())) {
                return;
            }
            event.setCanceled(true);
            setReplacementLastUse(stack, gameTime);
            if (stack.getItem() instanceof NinjutsuItem ninjutsuItem) {
                ninjutsuItem.addJutsuXp(stack, REPLACEMENT, 1);
            }
        }

        @Nullable
        private static ItemStack findActiveReplacementStack(Player player) {
            for (ItemStack stack : ProcedureUtils.getAllItemsOfSubType(player, NinjutsuItem.class)) {
                if (stack.getItem().getClass() == NinjutsuItem.class
                        && isReplacementActive(stack)
                        && isOwnedByOrUnbound(player, stack)) {
                    return stack;
                }
            }
            return null;
        }
    }

    protected record RasenganProfile(
            String displayName,
            double chakraUsage,
            float basePower,
            float minPower,
            float maxPower,
            float powerupDelay,
            int cooldownTicks,
            boolean senjutsuDamage) {
    }
}
