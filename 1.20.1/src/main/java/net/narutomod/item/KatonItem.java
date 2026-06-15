package net.narutomod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.narutomod.Chakra;
import net.narutomod.PlayerTracker;
import net.narutomod.entity.HidingInAshEntity;
import net.narutomod.entity.KatonFireballEntity;
import net.narutomod.entity.KatonFireStreamEntity;
import net.narutomod.particle.NarutoParticleKind;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModParticleTypes;
import net.narutomod.registry.ModSounds;

public final class KatonItem extends JutsuItem {
    public static final JutsuDefinition GREAT_FIREBALL = JutsuDefinition.ranked(0, "entity.narutomod.katonfireball", 'C', 30.0D)
            .withPower(0.1F, 30.0F);
    public static final JutsuDefinition FIRE_ANNIHILATION = JutsuDefinition.ranked(1, "tooltip.katon.annihilation", 'B', 50.0D)
            .withPower(1.0F, 30.0F);
    public static final JutsuDefinition HIDING_IN_ASH = JutsuDefinition.ranked(2, "entity.narutomod.hiding_in_ash", 'B', 50.0D)
            .withPower(1.0F, 15.0F);
    public static final JutsuDefinition GREAT_FLAME = JutsuDefinition.ranked(3, "entity.narutomod.katonfirestream", 'C', 20.0D)
            .withPower(1.0F, 30.0F);
    private static final JutsuDefinition[] KATON_JUTSUS = {
            GREAT_FIREBALL,
            FIRE_ANNIHILATION,
            HIDING_IN_ASH,
            GREAT_FLAME
    };

    public KatonItem() {
        super(JutsuType.KATON, KATON_JUTSUS);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!canBeginKaton(level, player, stack)) {
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        if (!(livingEntity instanceof Player player) || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        int usedTicks = getUseDuration(stack) - remainingUseDuration;
        if (usedTicks % 5 == 0) {
            serverLevel.sendParticles(
                    ModParticleTypes.options(NarutoParticleKind.FLAME_COLORED, 0xCCFF6600, 10),
                    player.getX(),
                    player.getY() + 1.0D,
                    player.getZ(),
                    4,
                    0.15D,
                    0.15D,
                    0.15D,
                    0.01D);
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
        if (isJutsu(currentJutsu, GREAT_FIREBALL)) {
            activateGreatFireball(level, player, stack, remainingUseDuration);
        } else if (isJutsu(currentJutsu, FIRE_ANNIHILATION)) {
            activateFireAnnihilation(level, player, stack, remainingUseDuration);
        } else if (isJutsu(currentJutsu, HIDING_IN_ASH)) {
            activateHidingInAsh(level, player, stack, remainingUseDuration);
        } else if (isJutsu(currentJutsu, GREAT_FLAME)) {
            activateGreatFlame(level, player, stack, remainingUseDuration);
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

    private void activateGreatFireball(Level level, Player player, ItemStack stack, int remainingUseDuration) {
        if (!canActivateKaton(level, player, stack, GREAT_FIREBALL)) {
            return;
        }
        float power = getKatonPower(stack, player, GREAT_FIREBALL, remainingUseDuration);
        if (power <= 0.0F) {
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(GREAT_FIREBALL.chakraUsage() * power)) {
            return;
        }
        KatonFireballEntity fireball = ModEntityTypes.KATONFIREBALL.get().create(level);
        if (fireball == null) {
            return;
        }
        fireball.configure(player, power);
        level.addFreshEntity(fireball);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.SOUND_FLAMETHROW.get(), SoundSource.PLAYERS, 1.0F, 0.8F + level.random.nextFloat() * 0.4F);
        addCurrentJutsuXp(stack, 1);
    }

    private void activateHidingInAsh(Level level, Player player, ItemStack stack, int remainingUseDuration) {
        if (!canActivateKaton(level, player, stack, HIDING_IN_ASH)) {
            return;
        }
        float power = getKatonPower(stack, player, HIDING_IN_ASH, remainingUseDuration);
        if (power <= 0.0F) {
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(HIDING_IN_ASH.chakraUsage() * power)) {
            return;
        }
        HidingInAshEntity entity = ModEntityTypes.HIDING_IN_ASH.get().create(level);
        if (entity == null) {
            return;
        }
        entity.configure(player, power);
        level.addFreshEntity(entity);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.SOUND_HIDING_IN_ASH.get(), SoundSource.PLAYERS, 5.0F, 1.0F);
        addCurrentJutsuXp(stack, 1);
    }

    private void activateFireAnnihilation(Level level, Player player, ItemStack stack, int remainingUseDuration) {
        activateFireStream(level, player, stack, FIRE_ANNIHILATION, remainingUseDuration,
                power -> power * 0.8F,
                power -> power * 1.5F,
                power -> KatonFireStreamEntity.ANNIHILATION_WAIT_TICKS,
                power -> KatonFireStreamEntity.DEFAULT_MAX_LIFE,
                true);
    }

    private void activateGreatFlame(Level level, Player player, ItemStack stack, int remainingUseDuration) {
        activateFireStream(level, player, stack, GREAT_FLAME, remainingUseDuration,
                power -> power * 0.1F,
                power -> power,
                power -> 0,
                power -> Math.max((int)(power * 10.0F), 1),
                false);
    }

    private void activateFireStream(
            Level level,
            Player player,
            ItemStack stack,
            JutsuDefinition definition,
            int remainingUseDuration,
            PowerFloat widthFactory,
            PowerFloat rangeFactory,
            PowerInt waitFactory,
            PowerInt lifeFactory,
            boolean playAnnihilationStartSound) {
        if (!canActivateKaton(level, player, stack, definition)) {
            return;
        }
        float power = getKatonPower(stack, player, definition, remainingUseDuration);
        if (power <= 0.0F) {
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(definition.chakraUsage() * power)) {
            return;
        }
        KatonFireStreamEntity entity = ModEntityTypes.KATONFIRESTREAM.get().create(level);
        if (entity == null) {
            return;
        }
        entity.configure(player, widthFactory.apply(power), rangeFactory.apply(power), waitFactory.apply(power), lifeFactory.apply(power));
        level.addFreshEntity(entity);
        if (playAnnihilationStartSound) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.SOUND_KATON_GOKAMEKEKU.get(), SoundSource.PLAYERS, 5.0F, 1.0F);
        }
        addCurrentJutsuXp(stack, 1);
    }

    private boolean canBeginKaton(Level level, Player player, ItemStack stack) {
        JutsuDefinition currentJutsu = getCurrentJutsu(stack);
        if (!isJutsu(currentJutsu, GREAT_FIREBALL)
                && !isJutsu(currentJutsu, FIRE_ANNIHILATION)
                && !isJutsu(currentJutsu, HIDING_IN_ASH)
                && !isJutsu(currentJutsu, GREAT_FLAME)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("This Katon jutsu is not ported yet."), true);
            }
            return false;
        }
        if (!level.isClientSide) {
            setOwnerIfMissing(stack, player);
        }
        return canActivateKaton(level, player, stack, currentJutsu);
    }

    private boolean canActivateKaton(Level level, Player player, ItemStack stack, JutsuDefinition definition) {
        if (player.isCreative()) {
            return true;
        }
        if (!PlayerTracker.isNinja(player)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("You need ninja experience to use this Katon jutsu."), true);
            }
            return false;
        }
        if (!canUseJutsu(stack, definition, player)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("You need to learn this Katon jutsu from a scroll."), true);
            }
            return false;
        }
        if (!hasEnoughJutsuXp(stack, definition)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("Katon XP " + getJutsuXp(stack, definition)
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

    private float getKatonPower(ItemStack stack, Player player, JutsuDefinition definition, int remainingUseDuration) {
        float power = getChargingPower(stack, player, remainingUseDuration, definition.basePower(), definition.powerUpDelay());
        return Math.min(power, getMaxKatonPower(player, definition));
    }

    private float getMaxKatonPower(Player player, JutsuDefinition definition) {
        if (player.isCreative()) {
            return definition == GREAT_FIREBALL ? 10.0F : 30.0F;
        }
        if (definition.chakraUsage() <= 0.0D) {
            return definition.basePower();
        }
        float maxPower = (float)(Chakra.pathway(player).getAmount() / definition.chakraUsage() * 0.9999D);
        if (definition == GREAT_FIREBALL) {
            return Math.min(maxPower, 10.0F);
        }
        if (definition == FIRE_ANNIHILATION || definition == GREAT_FLAME) {
            return Math.min(maxPower, 30.0F);
        }
        if (definition == HIDING_IN_ASH) {
            return Math.min(maxPower, 15.0F);
        }
        return maxPower;
    }

    private static boolean isJutsu(JutsuDefinition first, JutsuDefinition second) {
        return first.index() == second.index() && first.translationKey().equals(second.translationKey());
    }

    @FunctionalInterface
    private interface PowerFloat {
        float apply(float power);
    }

    @FunctionalInterface
    private interface PowerInt {
        int apply(float power);
    }
}
