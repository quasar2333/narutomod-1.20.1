package net.narutomod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.narutomod.Chakra;
import net.narutomod.PlayerTracker;
import net.narutomod.entity.EarthSandwichEntity;
import net.narutomod.entity.EarthSpearsEntity;
import net.narutomod.entity.EarthWallEntity;
import net.narutomod.entity.HidingInRockEntity;
import net.narutomod.entity.SwampPitEntity;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModSounds;

public final class DotonItem extends JutsuItem {
    public static final JutsuDefinition HIDING_IN_ROCK = JutsuDefinition.ranked(0, "entity.narutomod.entityhidinginrock", 'C', 10.0D)
            .withPower(2.0F, 50.0F);
    public static final JutsuDefinition EARTH_WALL = JutsuDefinition.ranked(1, "entity.narutomod.entityearthwall", 'B', 20.0D)
            .withPower(2.0F, 15.0F);
    public static final JutsuDefinition EARTH_SANDWICH = JutsuDefinition.ranked(2, "entity.narutomod.earth_sandwich", 'B', 100.0D)
            .withPower(2.0F, 75.0F);
    public static final JutsuDefinition SWAMP_PIT = JutsuDefinition.ranked(3, "entity.narutomod.swamp_pit", 'A', 100.0D)
            .withPower(1.0F, 30.0F);
    public static final JutsuDefinition EARTH_SPEARS = JutsuDefinition.ranked(4, "entity.narutomod.earth_spears", 'C', 50.0D)
            .withPower(0.5F, 150.0F);
    private static final JutsuDefinition[] DOTON_JUTSUS = {
            HIDING_IN_ROCK,
            EARTH_WALL,
            EARTH_SANDWICH,
            SWAMP_PIT,
            EARTH_SPEARS
    };

    public DotonItem() {
        super(JutsuType.DOTON, DOTON_JUTSUS);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!canBeginDoton(level, player, stack)) {
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int remainingUseDuration) {
        if (!(livingEntity instanceof Player player) || level.isClientSide) {
            return;
        }
        if (isJutsu(getCurrentJutsu(stack), HIDING_IN_ROCK)) {
            activateHidingInRock(level, player, stack);
        } else if (isJutsu(getCurrentJutsu(stack), EARTH_WALL)) {
            activateEarthWall(level, player, stack, remainingUseDuration);
        } else if (isJutsu(getCurrentJutsu(stack), EARTH_SANDWICH)) {
            activateEarthSandwich(level, player, stack, remainingUseDuration);
        } else if (isJutsu(getCurrentJutsu(stack), SWAMP_PIT)) {
            activateSwampPit(level, player, stack, remainingUseDuration);
        } else if (isJutsu(getCurrentJutsu(stack), EARTH_SPEARS)) {
            activateEarthSpears(level, player, stack, remainingUseDuration);
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

    private void activateHidingInRock(Level level, Player player, ItemStack stack) {
        if (!canActivateDoton(level, player, stack, HIDING_IN_ROCK)) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel) level;
        HidingInRockEntity active = findActiveHidingInRock(serverLevel, player);
        if (active != null || HidingInRockEntity.isIntangible(player)) {
            player.displayClientMessage(Component.literal("Hiding in Rock is already active."), true);
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(HIDING_IN_ROCK.chakraUsage())) {
            return;
        }
        HidingInRockEntity entity = ModEntityTypes.ENTITYHIDINGINROCK.get().create(level);
        if (entity == null) {
            return;
        }
        entity.configure(player);
        level.addFreshEntity(entity);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.SOUND_JUTSU.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        addCurrentJutsuXp(stack, 1);
    }

    private void activateEarthSpears(Level level, Player player, ItemStack stack, int remainingUseDuration) {
        if (!canActivateDoton(level, player, stack, EARTH_SPEARS)) {
            return;
        }
        float power = getDotonPower(stack, player, EARTH_SPEARS, remainingUseDuration);
        if (power <= 0.0F) {
            return;
        }
        if (!EarthSpearsEntity.hasUpwardGroundTarget(player)) {
            player.displayClientMessage(Component.literal("Earth Spears needs an upward ground target."), true);
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(EARTH_SPEARS.chakraUsage() * power)) {
            return;
        }
        int spawned = EarthSpearsEntity.spawnFrom(player, power);
        if (spawned <= 0) {
            player.displayClientMessage(Component.literal("Earth Spears needs an upward ground target."), true);
            return;
        }
        addCurrentJutsuXp(stack, 1);
    }

    private void activateEarthWall(Level level, Player player, ItemStack stack, int remainingUseDuration) {
        if (!canActivateDoton(level, player, stack, EARTH_WALL)) {
            return;
        }
        float power = getDotonPower(stack, player, EARTH_WALL, remainingUseDuration);
        if (power < 5.0F) {
            player.displayClientMessage(Component.literal(String.format("Earth Wall power %.2f/5.00", power)), true);
            return;
        }
        if (!EarthWallEntity.hasBlockTarget(player)) {
            player.displayClientMessage(Component.literal("Earth Wall needs a block target."), true);
            return;
        }
        if (!EarthWallEntity.hasBuildableTarget(player, power)) {
            player.displayClientMessage(Component.literal("Earth Wall needs nearby earth, stone, sand, clay, or gravel."), true);
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(EARTH_WALL.chakraUsage() * power)) {
            return;
        }
        if (!EarthWallEntity.spawnFrom(player, power)) {
            player.displayClientMessage(Component.literal("Earth Wall could not be created."), true);
            return;
        }
        addCurrentJutsuXp(stack, 1);
    }

    private void activateSwampPit(Level level, Player player, ItemStack stack, int remainingUseDuration) {
        if (!canActivateDoton(level, player, stack, SWAMP_PIT)) {
            return;
        }
        float power = getDotonPower(stack, player, SWAMP_PIT, remainingUseDuration);
        if (power <= 0.0F) {
            return;
        }
        if (!SwampPitEntity.hasBlockTarget(player)) {
            player.displayClientMessage(Component.literal("Swamp Pit needs a block target."), true);
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(SWAMP_PIT.chakraUsage() * power)) {
            return;
        }
        if (!SwampPitEntity.spawnFrom(player, power)) {
            player.displayClientMessage(Component.literal("Swamp Pit could not be created."), true);
            return;
        }
        addCurrentJutsuXp(stack, 1);
    }

    private void activateEarthSandwich(Level level, Player player, ItemStack stack, int remainingUseDuration) {
        if (!canActivateDoton(level, player, stack, EARTH_SANDWICH)) {
            return;
        }
        float power = getDotonPower(stack, player, EARTH_SANDWICH, remainingUseDuration);
        if (power < 2.0F) {
            player.displayClientMessage(Component.literal(String.format("Earth Sandwich power %.2f/2.00", power)), true);
            return;
        }
        if (!EarthSandwichEntity.hasTarget(player)) {
            player.displayClientMessage(Component.literal("Earth Sandwich needs a target entity."), true);
            return;
        }
        if (!EarthSandwichEntity.canSpawnFrom(player, power)) {
            player.displayClientMessage(Component.literal("Earth Sandwich needs earth or stone near the target."), true);
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(EARTH_SANDWICH.chakraUsage() * power)) {
            return;
        }
        if (!EarthSandwichEntity.spawnFrom(player, power)) {
            player.displayClientMessage(Component.literal("Earth Sandwich could not be created."), true);
            return;
        }
        addCurrentJutsuXp(stack, 1);
    }

    private HidingInRockEntity findActiveHidingInRock(ServerLevel level, Player player) {
        return level.getEntitiesOfClass(HidingInRockEntity.class, player.getBoundingBox().inflate(8.0D), entity -> entity.isOwnedBy(player))
                .stream()
                .findFirst()
                .orElse(null);
    }

    private boolean canBeginDoton(Level level, Player player, ItemStack stack) {
        JutsuDefinition currentJutsu = getCurrentJutsu(stack);
        if (!isJutsu(currentJutsu, HIDING_IN_ROCK)
                && !isJutsu(currentJutsu, EARTH_WALL)
                && !isJutsu(currentJutsu, EARTH_SANDWICH)
                && !isJutsu(currentJutsu, SWAMP_PIT)
                && !isJutsu(currentJutsu, EARTH_SPEARS)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("This Doton jutsu is not ported yet."), true);
            }
            return false;
        }
        if (!level.isClientSide) {
            setOwnerIfMissing(stack, player);
        }
        return canActivateDoton(level, player, stack, currentJutsu);
    }

    private boolean canActivateDoton(Level level, Player player, ItemStack stack, JutsuDefinition definition) {
        if (player.isCreative()) {
            return true;
        }
        if (!PlayerTracker.isNinja(player)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("You need ninja experience to use this Doton jutsu."), true);
            }
            return false;
        }
        if (!canUseJutsu(stack, definition, player)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("You need to learn this Doton jutsu from a scroll."), true);
            }
            return false;
        }
        if (!hasEnoughJutsuXp(stack, definition)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("Doton XP " + getJutsuXp(stack, definition)
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

    private float getDotonPower(ItemStack stack, Player player, JutsuDefinition definition, int remainingUseDuration) {
        float power = getChargingPower(stack, player, remainingUseDuration, definition.basePower(), definition.powerUpDelay());
        power = Math.min(power, getMaxDotonPower(player, definition));
        if (isJutsu(definition, EARTH_WALL)) {
            return Math.min(power, 50.0F);
        }
        if (isJutsu(definition, EARTH_SANDWICH)) {
            return Math.min(power, 8.0F);
        }
        return power;
    }

    private float getMaxDotonPower(Player player, JutsuDefinition definition) {
        if (player.isCreative()) {
            if (isJutsu(definition, EARTH_WALL)) {
                return 50.0F;
            }
            if (isJutsu(definition, EARTH_SANDWICH)) {
                return 8.0F;
            }
            return 100.0F;
        }
        if (definition.chakraUsage() <= 0.0D) {
            return definition.basePower();
        }
        return (float)(Chakra.pathway(player).getAmount() / definition.chakraUsage() * 0.9999D);
    }

    private boolean isJutsu(JutsuDefinition actual, JutsuDefinition expected) {
        return actual.index() == expected.index() && actual.translationKey().equals(expected.translationKey());
    }
}
