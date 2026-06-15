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
import net.narutomod.entity.SuitonStreamEntity;
import net.narutomod.entity.SuitonMistEntity;
import net.narutomod.entity.WaterDragonEntity;
import net.narutomod.entity.WaterPrisonEntity;
import net.narutomod.entity.WaterSharkEntity;
import net.narutomod.entity.WaterShockwaveEntity;
import net.narutomod.particle.NarutoParticleKind;
import net.narutomod.registry.ModParticleTypes;
import net.narutomod.registry.ModSounds;

public final class SuitonItem extends JutsuItem {
    public static final JutsuDefinition HIDING_IN_MIST = JutsuDefinition.ranked(0, "entity.narutomod.suitonmist", 'D', 100.0D);
    public static final JutsuDefinition WATER_BULLET = JutsuDefinition.ranked(1, "entity.narutomod.suitonstream", 'C', 10.0D)
            .withPower(5.0F, 20.0F);
    public static final JutsuDefinition WATER_DRAGON = JutsuDefinition.ranked(2, "entity.narutomod.water_dragon", 'B', 50.0D)
            .withPower(0.9F, 150.0F);
    public static final JutsuDefinition WATER_PRISON = JutsuDefinition.ranked(3, "entity.narutomod.water_prison", 'C', 200.0D);
    public static final JutsuDefinition WATER_SHARK = JutsuDefinition.ranked(4, "entity.narutomod.suiton_shark", 'B', 75.0D)
            .withPower(0.9F, 150.0F);
    public static final JutsuDefinition WATER_SHOCKWAVE = JutsuDefinition.ranked(5, "entity.narutomod.water_shockwave", 'B', 30.0D)
            .withPower(5.0F, 50.0F);
    private static final JutsuDefinition[] SUITON_JUTSUS = {
            HIDING_IN_MIST,
            WATER_BULLET,
            WATER_DRAGON,
            WATER_PRISON,
            WATER_SHARK,
            WATER_SHOCKWAVE
    };

    public SuitonItem() {
        super(JutsuType.SUITON, SUITON_JUTSUS);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!canBeginSuiton(level, player, stack)) {
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
                    ModParticleTypes.options(NarutoParticleKind.SUSPENDED_COLORED, 0xA050B0FF, 10, 20),
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
        if (isJutsu(currentJutsu, HIDING_IN_MIST)) {
            activateHidingInMist(level, player, stack);
        } else if (isJutsu(currentJutsu, WATER_BULLET)) {
            activateWaterBullet(level, player, stack, remainingUseDuration);
        } else if (isJutsu(currentJutsu, WATER_DRAGON)) {
            activateWaterDragon(level, player, stack, remainingUseDuration);
        } else if (isJutsu(currentJutsu, WATER_PRISON)) {
            activateWaterPrison(level, player, stack);
        } else if (isJutsu(currentJutsu, WATER_SHARK)) {
            activateWaterShark(level, player, stack, remainingUseDuration);
        } else if (isJutsu(currentJutsu, WATER_SHOCKWAVE)) {
            activateWaterShockwave(level, player, stack, remainingUseDuration);
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

    private void activateWaterBullet(Level level, Player player, ItemStack stack, int remainingUseDuration) {
        if (!canActivateSuiton(level, player, stack, WATER_BULLET)) {
            return;
        }
        float power = getSuitonPower(stack, player, WATER_BULLET, remainingUseDuration);
        if (power <= 0.0F) {
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(WATER_BULLET.chakraUsage() * power)) {
            return;
        }
        if (!SuitonStreamEntity.spawnFrom(player, power)) {
            player.displayClientMessage(Component.literal("Water Bullet could not be created."), true);
            return;
        }
        addCurrentJutsuXp(stack, 1);
    }

    private void activateHidingInMist(Level level, Player player, ItemStack stack) {
        if (!canActivateSuiton(level, player, stack, HIDING_IN_MIST)) {
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(HIDING_IN_MIST.chakraUsage())) {
            return;
        }
        if (!SuitonMistEntity.spawnFrom(player)) {
            player.displayClientMessage(Component.literal("Hiding in Mist could not be created."), true);
            return;
        }
        addCurrentJutsuXp(stack, 1);
    }

    private void activateWaterDragon(Level level, Player player, ItemStack stack, int remainingUseDuration) {
        if (!canActivateSuiton(level, player, stack, WATER_DRAGON)) {
            return;
        }
        float power = getSuitonPower(stack, player, WATER_DRAGON, remainingUseDuration);
        if (power < 1.0F) {
            player.displayClientMessage(Component.literal(String.format("Water Dragon power %.2f/1.00", power)), true);
            return;
        }
        if (!player.onGround()) {
            player.displayClientMessage(Component.literal("Water Dragon needs solid footing."), true);
            return;
        }
        double chakraCost = WATER_DRAGON.chakraUsage() * power;
        if (!isOverWater(player)) {
            chakraCost += WATER_DRAGON.chakraUsage() * 2.0D;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(chakraCost)) {
            return;
        }
        if (!WaterDragonEntity.spawnFrom(player, power)) {
            player.displayClientMessage(Component.literal("Water Dragon could not be created."), true);
            return;
        }
        addCurrentJutsuXp(stack, 1);
    }

    private void activateWaterPrison(Level level, Player player, ItemStack stack) {
        if (!canActivateSuiton(level, player, stack, WATER_PRISON)) {
            return;
        }
        LivingEntity target = WaterPrisonEntity.findTarget(player);
        if (target == null) {
            player.displayClientMessage(Component.literal("Water Prison needs a living target within 4 blocks."), true);
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(WATER_PRISON.chakraUsage())) {
            return;
        }
        if (!WaterPrisonEntity.spawnFrom(player, target)) {
            player.displayClientMessage(Component.literal("Water Prison could not be created."), true);
            return;
        }
        addCurrentJutsuXp(stack, 1);
    }

    private void activateWaterShark(Level level, Player player, ItemStack stack, int remainingUseDuration) {
        if (!canActivateSuiton(level, player, stack, WATER_SHARK)) {
            return;
        }
        float power = getSuitonPower(stack, player, WATER_SHARK, remainingUseDuration);
        if (power < 1.0F) {
            player.displayClientMessage(Component.literal(String.format("Water Shark power %.2f/1.00", power)), true);
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(WATER_SHARK.chakraUsage() * power)) {
            return;
        }
        if (!WaterSharkEntity.spawnFrom(player, power)) {
            player.displayClientMessage(Component.literal("Water Shark could not be created."), true);
            return;
        }
        addCurrentJutsuXp(stack, 1);
    }

    private void activateWaterShockwave(Level level, Player player, ItemStack stack, int remainingUseDuration) {
        WaterShockwaveEntity existing = WaterShockwaveEntity.getActive(player);
        if (existing != null) {
            existing.setShouldDie();
            return;
        }
        if (!canActivateSuiton(level, player, stack, WATER_SHOCKWAVE)) {
            return;
        }
        float power = getSuitonPower(stack, player, WATER_SHOCKWAVE, remainingUseDuration);
        if (power <= 0.0F) {
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(WATER_SHOCKWAVE.chakraUsage() * power)) {
            return;
        }
        WaterShockwaveEntity.ToggleResult result = WaterShockwaveEntity.toggleFrom(player, power);
        if (result == WaterShockwaveEntity.ToggleResult.FAILED) {
            player.displayClientMessage(Component.literal("Water Shockwave could not be created."), true);
            return;
        }
        if (result == WaterShockwaveEntity.ToggleResult.CREATED) {
            addCurrentJutsuXp(stack, 1);
        }
    }

    private boolean canBeginSuiton(Level level, Player player, ItemStack stack) {
        JutsuDefinition currentJutsu = getCurrentJutsu(stack);
        if (!isJutsu(currentJutsu, HIDING_IN_MIST)
                && !isJutsu(currentJutsu, WATER_BULLET)
                && !isJutsu(currentJutsu, WATER_DRAGON)
                && !isJutsu(currentJutsu, WATER_PRISON)
                && !isJutsu(currentJutsu, WATER_SHARK)
                && !isJutsu(currentJutsu, WATER_SHOCKWAVE)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("This Suiton jutsu is not ported yet."), true);
            }
            return false;
        }
        if (!level.isClientSide) {
            setOwnerIfMissing(stack, player);
        }
        return canActivateSuiton(level, player, stack, currentJutsu);
    }

    private boolean canActivateSuiton(Level level, Player player, ItemStack stack, JutsuDefinition definition) {
        if (player.isCreative()) {
            return true;
        }
        if (!PlayerTracker.isNinja(player)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("You need ninja experience to use this Suiton jutsu."), true);
            }
            return false;
        }
        if (!canUseJutsu(stack, definition, player)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("You need to learn this Suiton jutsu from a scroll."), true);
            }
            return false;
        }
        if (!hasEnoughJutsuXp(stack, definition)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("Suiton XP " + getJutsuXp(stack, definition)
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

    private float getSuitonPower(ItemStack stack, Player player, JutsuDefinition definition, int remainingUseDuration) {
        float power = getChargingPower(stack, player, remainingUseDuration, definition.basePower(), definition.powerUpDelay());
        return Math.min(power, getMaxSuitonPower(player, definition));
    }

    private float getMaxSuitonPower(Player player, JutsuDefinition definition) {
        if (player.isCreative()) {
            if (definition == WATER_BULLET) {
                return 30.0F;
            }
            if (definition == WATER_DRAGON || definition == WATER_SHARK) {
                return 5.0F;
            }
            if (definition == WATER_SHOCKWAVE) {
                return 25.0F;
            }
            return definition.basePower();
        }
        if (definition.chakraUsage() <= 0.0D) {
            return definition.basePower();
        }
        float maxPower = (float)(Chakra.pathway(player).getAmount() / definition.chakraUsage() * 0.9999D);
        if (definition == WATER_BULLET) {
            return Math.min(maxPower, 30.0F);
        }
        if (definition == WATER_DRAGON || definition == WATER_SHARK) {
            return Math.min(maxPower, 5.0F);
        }
        if (definition == WATER_SHOCKWAVE) {
            return Math.min(maxPower, 25.0F);
        }
        return maxPower;
    }

    private boolean isOverWater(Player player) {
        return player.isInWaterOrBubble()
                || player.level().getBlockState(player.blockPosition().below()).is(net.minecraft.world.level.block.Blocks.WATER);
    }

    private static boolean isJutsu(JutsuDefinition first, JutsuDefinition second) {
        return first.index() == second.index() && first.translationKey().equals(second.translationKey());
    }
}
