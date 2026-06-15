package net.narutomod.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.narutomod.Chakra;
import net.narutomod.PlayerTracker;
import net.narutomod.entity.GamabuntaEntity;
import net.narutomod.entity.MandaEntity;
import net.narutomod.entity.SlugSummonEntity;
import net.narutomod.entity.SnakeSummonEntity;
import net.narutomod.entity.ToadSummonEntity;
import net.narutomod.particle.NarutoParticleKind;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModParticleTypes;
import net.narutomod.registry.ModSounds;

public final class SummoningContractItem extends JutsuItem implements ItemOnBody.Interface {
    public static final JutsuDefinition SUMMON_TOAD = JutsuDefinition.ranked(0, "entity.narutomod.toad_summon", 'C', 100.0D)
            .withPower(0.0F, 80.0F);
    public static final JutsuDefinition SUMMON_SNAKE = JutsuDefinition.ranked(1, "entity.narutomod.snake_summon", 'C', 100.0D)
            .withPower(0.0F, 80.0F);
    public static final JutsuDefinition SUMMON_SLUG = JutsuDefinition.ranked(2, "entity.narutomod.slug", 'C', 100.0D)
            .withPower(0.0F, 80.0F);
    private static final JutsuDefinition[] SUMMONING_JUTSUS = {
            SUMMON_TOAD,
            SUMMON_SNAKE,
            SUMMON_SLUG
    };
    private static final int MAX_USE_DURATION = 72000;
    private static final float MIN_POWER = 0.1F;

    public SummoningContractItem() {
        super(JutsuType.KUCHIYOSE, SUMMONING_JUTSUS);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            return switchContract(level, player, stack);
        }
        if (!canBeginSummon(level, player, stack)) {
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
                    ModParticleTypes.options(NarutoParticleKind.SMOKE_COLORED, 0x106AD1FF, 40, 5, 0xF0, player.getId(), 4),
                    player.getX(),
                    player.getY() + 0.9D,
                    player.getZ(),
                    10,
                    0.25D,
                    0.0D,
                    0.25D,
                    0.04D
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
        JutsuDefinition current = getCurrentJutsu(stack);
        float power = getSummonPower(stack, player, current, remainingUseDuration);
        if (power < MIN_POWER) {
            player.displayClientMessage(Component.literal(String.format("Summoning power %.1f/%.1f", power, MIN_POWER)), true);
            return;
        }
        double chakraCost = current.chakraUsage() * power;
        if (!player.isCreative() && !Chakra.pathway(player).consume(chakraCost)) {
            return;
        }
        spawnSummoningFeedback((ServerLevel) level, player, power);
        if (spawnSummon(player, current, power)) {
            addCurrentJutsuXp(stack, 1);
            return;
        }
        player.displayClientMessage(Component.translatable(current.translationKey())
                .append(Component.literal(String.format(" could not be summoned. power=%.1f", power))), true);
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity target) {
        Entity resolvedTarget = target;
        if (target == player) {
            HitResult hit = ProcedureUtils.objectEntityLookingAt(player, 50.0D, 0.0D, true, false, candidate -> candidate != player);
            if (hit instanceof EntityHitResult entityHit) {
                resolvedTarget = entityHit.getEntity();
            }
        }
        if (resolvedTarget instanceof LivingEntity livingTarget && resolvedTarget != player) {
            player.setLastHurtMob(livingTarget);
            if (!player.level().isClientSide) {
                player.displayClientMessage(Component.literal("Summoning target: ")
                        .append(livingTarget.getDisplayName()), true);
            }
        }
        return super.onLeftClickEntity(stack, player, resolvedTarget);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return MAX_USE_DURATION;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        JutsuDefinition current = getCurrentJutsu(stack);
        tooltip.add(Component.literal(contractName(stack)).withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.translatable(current.translationKey())
                .append(Component.literal(" [" + current.rank() + "]"))
                .withStyle(ChatFormatting.GRAY));
    }

    public String describeCurrentJutsu(ItemStack stack) {
        JutsuDefinition current = getCurrentJutsu(stack);
        return current.translationKey()
                + "#" + current.index()
                + "/rank=" + current.rank()
                + "/enabled=" + isJutsuEnabled(stack, current)
                + "/xp=" + getJutsuXp(stack, current)
                + "/" + getRequiredXp(stack, current);
    }

    private InteractionResultHolder<ItemStack> switchContract(Level level, Player player, ItemStack stack) {
        if (!level.isClientSide && switchToNextUsableJutsu(stack, player)) {
            player.displayClientMessage(Component.literal("Summoning contract: ")
                    .append(Component.translatable(getCurrentJutsu(stack).translationKey())), true);
        }
        return InteractionResultHolder.success(stack);
    }

    private boolean canBeginSummon(Level level, Player player, ItemStack stack) {
        if (!level.isClientSide) {
            setOwnerIfMissing(stack, player);
        }
        JutsuDefinition current = getCurrentJutsu(stack);
        if (player.isCreative()) {
            return true;
        }
        if (!PlayerTracker.isNinja(player)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("You need ninja experience to use a Summoning Contract."), true);
            }
            return false;
        }
        if (!isOwnedByOrUnbound(player, stack)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("This Summoning Contract belongs to another player."), true);
            }
            return false;
        }
        if (!canUseCurrentJutsu(stack, player)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("This Summoning Contract has not unlocked ")
                        .append(Component.translatable(current.translationKey()))
                        .append(Component.literal(".")), true);
            }
            return false;
        }
        if (getCurrentJutsuXp(stack) < getCurrentJutsuRequiredXp(stack)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("Summoning XP " + getCurrentJutsuXp(stack)
                        + "/" + getCurrentJutsuRequiredXp(stack)), true);
            }
            return false;
        }
        long cooldown = getRemainingCooldownTicks(stack, level, current);
        if (cooldown > 0L) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("Cooldown " + cooldown / 20L + "s"), true);
            }
            return false;
        }
        if (Chakra.pathway(player).getAmount() < current.chakraUsage() * MIN_POWER) {
            Chakra.pathway(player).warningDisplay();
            return false;
        }
        return true;
    }

    private float getSummonPower(ItemStack stack, Player player, JutsuDefinition current, int remainingUseDuration) {
        float chargePower = getChargingPower(stack, player, remainingUseDuration, current.basePower(), current.powerUpDelay());
        if (player.isCreative()) {
            return chargePower;
        }
        float maxPower = (float) (Chakra.pathway(player).getAmount() / current.chakraUsage() * 0.9999D);
        return Math.min(chargePower, Math.max(maxPower, 0.0F));
    }

    private static void spawnSummoningFeedback(ServerLevel level, Player player, float power) {
        level.sendParticles(
                ModParticleTypes.options(NarutoParticleKind.SEAL_FORMULA, Math.max((int) (power * 40.0F), 1), 0, 60),
                player.getX(),
                player.getY() + 0.015D,
                player.getZ(),
                1,
                0.0D,
                0.0D,
                0.0D,
                0.0D
        );
        level.sendParticles(
                ModParticleTypes.options(NarutoParticleKind.SMOKE_COLORED, 0xD0FFFFFF, Math.max((int) (power * 30.0F), 5), 5, 0xF0, player.getId(), 4),
                player.getX(),
                player.getY() + 0.1D,
                player.getZ(),
                80,
                0.6D,
                0.25D,
                0.6D,
                0.04D
        );
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.SOUND_KUCHIYOSENOJUTSU.get(), SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    private boolean spawnSummon(Player player, JutsuDefinition definition, float power) {
        if (isJutsu(definition, SUMMON_TOAD)) {
            return power >= GamabuntaEntity.SUMMON_SCALE
                    ? GamabuntaEntity.spawnFrom(player)
                    : ToadSummonEntity.spawnFrom(player, power);
        }
        if (isJutsu(definition, SUMMON_SNAKE)) {
            return power >= MandaEntity.SUMMON_SCALE
                    ? MandaEntity.spawnFrom(player)
                    : SnakeSummonEntity.spawnFrom(player, power);
        }
        if (isJutsu(definition, SUMMON_SLUG)) {
            return SlugSummonEntity.spawnFrom(player, power);
        }
        return false;
    }

    private static boolean isJutsu(JutsuDefinition first, JutsuDefinition second) {
        return first.index() == second.index() && first.translationKey().equals(second.translationKey());
    }

    private String contractName(ItemStack stack) {
        if (isJutsuEnabled(stack, SUMMON_TOAD)) {
            return "Toad Summoning Contract";
        }
        if (isJutsuEnabled(stack, SUMMON_SNAKE)) {
            return "Snake Summoning Contract";
        }
        if (isJutsuEnabled(stack, SUMMON_SLUG)) {
            return "Slug Summoning Contract";
        }
        return "Unbound Summoning Contract";
    }
}
