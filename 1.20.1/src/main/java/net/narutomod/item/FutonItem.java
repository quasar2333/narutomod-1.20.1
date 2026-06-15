package net.narutomod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.narutomod.Chakra;
import net.narutomod.PlayerTracker;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModItems;
import net.narutomod.entity.FutonChakraFlowEntity;
import net.narutomod.entity.FutonGreatBreakthroughEntity;
import net.narutomod.entity.FutonVacuumEntity;
import net.narutomod.entity.RasenshurikenEntity;
import net.narutomod.particle.NarutoParticleKind;
import net.narutomod.registry.ModParticleTypes;
import net.narutomod.registry.ModSounds;

public final class FutonItem extends JutsuItem {
    public static final JutsuDefinition CHAKRA_FLOW = JutsuDefinition.ranked(0, "entity.narutomod.futonchakraflow", 'D', 20.0D);
    public static final JutsuDefinition RASENSHURIKEN = JutsuDefinition.ranked(1, "entity.narutomod.rasenshuriken", 'S', 1000.0D)
            .withPower(0.0F, 300.0F);
    public static final JutsuDefinition VACUUM = JutsuDefinition.ranked(2, "entity.narutomod.futon_vacuum", 'B', 20.0D)
            .withPower(0.0F, 20.0F);
    public static final JutsuDefinition BIG_BLOW = JutsuDefinition.ranked(3, "entity.narutomod.futon_great_breakthrough", 'C', 20.0D)
            .withPower(5.0F, 20.0F);
    private static final JutsuDefinition[] FUTON_JUTSUS = {
            CHAKRA_FLOW,
            RASENSHURIKEN,
            VACUUM,
            BIG_BLOW
    };

    public FutonItem() {
        super(JutsuType.FUTON, FUTON_JUTSUS);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (level.isClientSide || !(entity instanceof Player player)) {
            return;
        }
        // Faithful to 1.12.2 ItemFuton.onUpdate (RangedItem): Rasenshuriken is usable only while the
        // player can currently use Rasengan from their Ninjutsu tome; auto-enable/disable to match.
        boolean rasenshurikenEnabled = isJutsuEnabled(stack, RASENSHURIKEN);
        ItemStack ninjutsuStack = ProcedureUtils.getMatchingItemStack(player, ModItems.NINJUTSU.get());
        boolean rasenganUsable = ninjutsuStack != null
                && ninjutsuStack.getItem() instanceof NinjutsuItem ninjutsuItem
                && ninjutsuItem.canPlayerUseJutsu(ninjutsuStack, NinjutsuItem.RASENGAN, player);
        if (rasenshurikenEnabled && !rasenganUsable) {
            enableJutsu(stack, RASENSHURIKEN, false);
        } else if (!rasenshurikenEnabled && rasenganUsable) {
            enableJutsu(stack, RASENSHURIKEN, true);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!canBeginFuton(level, player, stack)) {
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
                    ModParticleTypes.options(NarutoParticleKind.SMOKE_COLORED, 0x20FFFFFF, 12, 5, 0xF0, player.getId(), 4),
                    player.getX(),
                    player.getY() + 1.0D,
                    player.getZ(),
                    4,
                    0.25D,
                    0.1D,
                    0.25D,
                    0.01D);
        }
        if (usedTicks % 10 == 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.SOUND_WIND.get(), SoundSource.PLAYERS, 0.08F, level.random.nextFloat() + 0.5F);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int remainingUseDuration) {
        if (!(livingEntity instanceof Player player) || level.isClientSide) {
            return;
        }
        JutsuDefinition currentJutsu = getCurrentJutsu(stack);
        if (isJutsu(currentJutsu, CHAKRA_FLOW)) {
            activateChakraFlow(level, player, stack);
        } else if (isJutsu(currentJutsu, RASENSHURIKEN)) {
            activateRasenshuriken(level, player, stack, remainingUseDuration);
        } else if (isJutsu(currentJutsu, VACUUM)) {
            activateVacuum(level, player, stack, remainingUseDuration);
        } else if (isJutsu(currentJutsu, BIG_BLOW)) {
            activateBigBlow(level, player, stack, remainingUseDuration);
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

    private void activateVacuum(Level level, Player player, ItemStack stack, int remainingUseDuration) {
        if (!canActivateFuton(level, player, stack, VACUUM)) {
            return;
        }
        float power = getFutonPower(stack, player, VACUUM, remainingUseDuration);
        if (power < 1.0F) {
            player.displayClientMessage(Component.literal(String.format("Vacuum Sphere power %.2f/1.00", power)), true);
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(VACUUM.chakraUsage() * power)) {
            return;
        }
        if (!FutonVacuumEntity.spawnFrom(player, power)) {
            player.displayClientMessage(Component.literal("Vacuum Sphere could not be created."), true);
            return;
        }
        addCurrentJutsuXp(stack, 1);
    }

    private void activateRasenshuriken(Level level, Player player, ItemStack stack, int remainingUseDuration) {
        if (!canActivateFuton(level, player, stack, RASENSHURIKEN)) {
            return;
        }
        float power = getFutonPower(stack, player, RASENSHURIKEN, remainingUseDuration);
        if (power < 0.1F) {
            player.displayClientMessage(Component.literal(String.format("Rasenshuriken power %.2f/0.10", power)), true);
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(RASENSHURIKEN.chakraUsage() * power)) {
            return;
        }
        if (!RasenshurikenEntity.spawnFrom(player, power)) {
            player.displayClientMessage(Component.literal("Rasenshuriken could not be created."), true);
            return;
        }
        addCurrentJutsuXp(stack, 1);
    }

    private void activateChakraFlow(Level level, Player player, ItemStack stack) {
        if (FutonChakraFlowEntity.stopActive(player)) {
            return;
        }
        if (!canActivateFuton(level, player, stack, CHAKRA_FLOW)) {
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(CHAKRA_FLOW.chakraUsage())) {
            return;
        }
        int strengthModifier = FutonChakraFlowEntity.strengthModifierFor(player, stack);
        if (!FutonChakraFlowEntity.spawnOrToggle(player, stack, strengthModifier)) {
            player.displayClientMessage(Component.literal("Chakra Flow could not be created."), true);
            return;
        }
        addCurrentJutsuXp(stack, 1);
    }

    private void activateBigBlow(Level level, Player player, ItemStack stack, int remainingUseDuration) {
        if (!canActivateFuton(level, player, stack, BIG_BLOW)) {
            return;
        }
        float power = getFutonPower(stack, player, BIG_BLOW, remainingUseDuration);
        if (power <= 0.0F) {
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(BIG_BLOW.chakraUsage() * power)) {
            return;
        }
        if (!FutonGreatBreakthroughEntity.spawnFrom(player, power)) {
            player.displayClientMessage(Component.literal("Great Breakthrough could not be created."), true);
            return;
        }
        addCurrentJutsuXp(stack, 1);
    }

    private boolean canBeginFuton(Level level, Player player, ItemStack stack) {
        JutsuDefinition currentJutsu = getCurrentJutsu(stack);
        if (!isJutsu(currentJutsu, CHAKRA_FLOW)
                && !isJutsu(currentJutsu, RASENSHURIKEN)
                && !isJutsu(currentJutsu, VACUUM)
                && !isJutsu(currentJutsu, BIG_BLOW)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("This Futon jutsu is not ported yet."), true);
            }
            return false;
        }
        if (!level.isClientSide) {
            setOwnerIfMissing(stack, player);
        }
        return canActivateFuton(level, player, stack, currentJutsu);
    }

    private boolean canActivateFuton(Level level, Player player, ItemStack stack, JutsuDefinition definition) {
        if (player.isCreative()) {
            return true;
        }
        if (!PlayerTracker.isNinja(player)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("You need ninja experience to use this Futon jutsu."), true);
            }
            return false;
        }
        if (!canUseJutsu(stack, definition, player)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("You need to learn this Futon jutsu from a scroll."), true);
            }
            return false;
        }
        if (!hasEnoughJutsuXp(stack, definition)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("Futon XP " + getJutsuXp(stack, definition)
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

    private float getFutonPower(ItemStack stack, Player player, JutsuDefinition definition, int remainingUseDuration) {
        float power = getChargingPower(stack, player, remainingUseDuration, definition.basePower(), definition.powerUpDelay());
        return Math.min(power, getMaxFutonPower(player, definition));
    }

    private float getMaxFutonPower(Player player, JutsuDefinition definition) {
        if (player.isCreative()) {
            if (definition == VACUUM) {
                return 50.0F;
            }
            if (definition == RASENSHURIKEN) {
                return 2.0F;
            }
            if (definition == BIG_BLOW) {
                return 50.0F;
            }
            return definition.basePower();
        }
        if (definition.chakraUsage() <= 0.0D) {
            return definition.basePower();
        }
        float maxPower = (float)(Chakra.pathway(player).getAmount() / definition.chakraUsage() * 0.9999D);
        if (definition == VACUUM) {
            return Math.min(maxPower, 50.0F);
        }
        if (definition == RASENSHURIKEN) {
            return Math.min(maxPower, 2.0F);
        }
        return maxPower;
    }

    private static boolean isJutsu(JutsuDefinition first, JutsuDefinition second) {
        return first.index() == second.index() && first.translationKey().equals(second.translationKey());
    }
}
