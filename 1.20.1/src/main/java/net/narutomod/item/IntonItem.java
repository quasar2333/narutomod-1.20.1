package net.narutomod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.narutomod.Chakra;
import net.narutomod.PlayerTracker;
import net.narutomod.entity.MindTransferEntity;
import net.narutomod.entity.ShadowImitationEntity;
import net.narutomod.particle.NarutoParticleKind;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModEffects;
import net.narutomod.registry.ModParticleTypes;
import net.narutomod.registry.ModSounds;

public final class IntonItem extends JutsuItem {
    public static final JutsuDefinition GENJUTSU = JutsuDefinition.ranked(0, "genjutsu", 'B', 300.0D);
    public static final JutsuDefinition MIND_TRANSFER = JutsuDefinition.ranked(1, "mind_transfer", 'C', 300.0D);
    public static final JutsuDefinition SHADOW_IMITATION = JutsuDefinition.ranked(2, "shadow_imitation", 'B', 50.0D);
    private static final JutsuDefinition[] INTON_JUTSUS = {
            GENJUTSU,
            MIND_TRANSFER,
            SHADOW_IMITATION
    };
    private static final double TARGET_RANGE = 30.0D;
    private static final int GENJUTSU_DURATION = 200;
    private static final int GENJUTSU_COOLDOWN = 1200;

    public IntonItem() {
        super(JutsuType.INTON, INTON_JUTSUS);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        JutsuDefinition currentJutsu = getCurrentJutsu(stack);
        if (isJutsu(currentJutsu, GENJUTSU)) {
            activateGenjutsu(level, player, stack);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (isJutsu(currentJutsu, MIND_TRANSFER)) {
            activateMindTransfer(level, player, stack);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (isJutsu(currentJutsu, SHADOW_IMITATION)) {
            activateShadowImitation(level, player, stack);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (!level.isClientSide) {
            player.displayClientMessage(Component.literal("This Inton jutsu is not ported yet."), true);
        }
        return InteractionResultHolder.fail(stack);
    }

    private void activateGenjutsu(net.minecraft.world.level.Level level, Player player, ItemStack stack) {
        if (level.isClientSide || !canActivateInton(level, player, stack, GENJUTSU)) {
            return;
        }
        LivingEntity target = findLookedAtLiving(player);
        if (target == null) {
            player.displayClientMessage(Component.literal("No Genjutsu target."), true);
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(GENJUTSU.chakraUsage())) {
            Chakra.pathway(player).warningDisplay();
            return;
        }
        level.playSound(null, target.getX(), target.getY(), target.getZ(),
                ModSounds.SOUND_GENJUTSU.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        target.addEffect(new MobEffectInstance(ModEffects.PARALYSIS.get(), GENJUTSU_DURATION, 1, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, GENJUTSU_DURATION + 40, 0, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, GENJUTSU_DURATION, 0, false, true));
        if (target instanceof ServerPlayer serverTarget && level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    serverTarget,
                    ModParticleTypes.options(NarutoParticleKind.MOB_APPEARANCE, player.getId()),
                    true,
                    target.getX(),
                    target.getY() + target.getBbHeight() * 0.5D,
                    target.getZ(),
                    1,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.0D);
        }
        setJutsuCooldown(stack, level, GENJUTSU, GENJUTSU_COOLDOWN);
        addJutsuXp(stack, GENJUTSU, 1);
    }

    private void activateMindTransfer(net.minecraft.world.level.Level level, Player player, ItemStack stack) {
        if (level.isClientSide) {
            return;
        }
        if (MindTransferEntity.hasActiveFor(player)) {
            MindTransferEntity.stopFor(player);
            return;
        }
        if (!canActivateInton(level, player, stack, MIND_TRANSFER)) {
            return;
        }
        LivingEntity target = findLookedAtLiving(player);
        if (target == null) {
            player.displayClientMessage(Component.literal("No Mind Transfer target."), true);
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(MIND_TRANSFER.chakraUsage())) {
            Chakra.pathway(player).warningDisplay();
            return;
        }
        if (MindTransferEntity.spawnFrom(player, target, MIND_TRANSFER.chakraUsage() * 0.005D)) {
            addJutsuXp(stack, MIND_TRANSFER, 1);
        }
    }

    private void activateShadowImitation(net.minecraft.world.level.Level level, Player player, ItemStack stack) {
        if (level.isClientSide) {
            return;
        }
        if (player.isShiftKeyDown()) {
            int stopped = ShadowImitationEntity.stopOwnedNear(player, 128.0D);
            player.displayClientMessage(Component.literal("Released Shadow Imitation: " + stopped), true);
            return;
        }
        if (!canActivateInton(level, player, stack, SHADOW_IMITATION)) {
            return;
        }
        LivingEntity target = findLookedAtLiving(player);
        if (target == null) {
            player.displayClientMessage(Component.literal("No Shadow Imitation target."), true);
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(SHADOW_IMITATION.chakraUsage())) {
            Chakra.pathway(player).warningDisplay();
            return;
        }
        if (ShadowImitationEntity.spawnFrom(player, target, SHADOW_IMITATION.chakraUsage())) {
            addJutsuXp(stack, SHADOW_IMITATION, 1);
        }
    }

    private boolean canActivateInton(net.minecraft.world.level.Level level, Player player, ItemStack stack, JutsuDefinition definition) {
        if (!player.isCreative() && !PlayerTracker.isNinja(player)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("You need ninja experience to use Inton."), true);
            }
            return false;
        }
        if (!player.isCreative() && !isOwnedByOrUnbound(player, stack)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("This Inton item belongs to another player."), true);
            }
            return false;
        }
        if (!level.isClientSide) {
            setOwnerIfMissing(stack, player);
        }
        if (!player.isCreative() && !isJutsuEnabled(stack, definition)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("You need to learn this Inton jutsu from a scroll."), true);
            }
            return false;
        }
        if (!player.isCreative() && !hasEnoughJutsuXp(stack, definition)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("Inton XP " + getJutsuXp(stack, definition)
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
        if (!player.isCreative() && Chakra.pathway(player).getAmount() < definition.chakraUsage()) {
            Chakra.pathway(player).warningDisplay();
            return false;
        }
        return true;
    }

    private LivingEntity findLookedAtLiving(LivingEntity owner) {
        HitResult hit = ProcedureUtils.objectEntityLookingAt(owner, TARGET_RANGE, 0.0D, true, false,
                target -> target instanceof LivingEntity living && living != owner && living.isAlive());
        return hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living ? living : null;
    }

    private static boolean isJutsu(JutsuDefinition first, JutsuDefinition second) {
        return first.index() == second.index() && first.translationKey().equals(second.translationKey());
    }
}
