package net.narutomod.item;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.narutomod.Chakra;
import net.narutomod.NarutomodModVariables;
import net.narutomod.entity.BijuManager;
import net.narutomod.entity.GedoStatueEntity;
import net.narutomod.entity.IntonRaihaEntity;
import net.narutomod.entity.RantonKogaEntity;
import net.narutomod.entity.RasenshurikenEntity;
import net.narutomod.entity.TruthSeekerBallEntity;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModItems;
import net.narutomod.registry.ModSounds;

public final class SixPathSenjutsuItem extends JutsuItem {
    public static final JutsuDefinition SHOOT = JutsuDefinition.ranked(0, "tooltip.6psenjutsu.shoot", 'S', 50.0D);
    public static final JutsuDefinition SHIELD = JutsuDefinition.ranked(1, "tooltip.6psenjutsu.shield", 'S', 50.0D);
    public static final JutsuDefinition THUNDER = JutsuDefinition.ranked(2, "inton_raiha", 'S', 100.0D)
            .withPower(1.0F, 80.0F);
    public static final JutsuDefinition LASER = JutsuDefinition.ranked(3, "ranton_koga", 'S', 100.0D)
            .withPower(1.0F, 50.0F);
    public static final JutsuDefinition RASENSHURIKEN = JutsuDefinition.ranked(4, "tooltip.6psenjutsu.rasenshuriken", 'S', 1000.0D);
    public static final JutsuDefinition OUTER_PATH = JutsuDefinition.ranked(5, "chattext.rinnegan.path5", 'S', 2000.0D);
    private static final double OUTER_PATH_REQUIRED_NINJA_LEVEL = 90.0D;
    private static final JutsuDefinition[] SIX_PATH_JUTSUS = {
            SHOOT,
            SHIELD,
            THUNDER,
            LASER,
            RASENSHURIKEN,
            OUTER_PATH
    };
    public static final String SPAWNED_BALLS_TAG = "SpawnedTruthSeekingBallsId";
    public static final String CURRENT_BALL_TAG = "CurrentTruthSeekingBallIdx";

    public SixPathSenjutsuItem() {
        super(JutsuType.SIXPATHSENJUTSU, true, SIX_PATH_JUTSUS);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!hasRinneSharingan(player) && !player.isCreative()) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("Six Path Senjutsu requires an active Rinne Sharingan."), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        JutsuDefinition current = getCurrentJutsu(stack);
        if ((isShoot(current) || isShield(current) || isRasenshuriken(current) || isOuterPath(current)) && level.isClientSide) {
            return InteractionResultHolder.consume(stack);
        }
        if (isThunder(current) || isLaser(current)) {
            if (!canActivateSixPath(level, player, stack, current)) {
                return InteractionResultHolder.fail(stack);
            }
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        if (!level.isClientSide) {
            setOwnerIfMissing(stack, player);
            if (isShoot(current)) {
                return activateShoot(level, player, stack)
                        ? InteractionResultHolder.consume(stack)
                        : InteractionResultHolder.fail(stack);
            }
            if (isShield(current)) {
                return activateShield(level, player, stack)
                        ? InteractionResultHolder.consume(stack)
                        : InteractionResultHolder.fail(stack);
            }
            if (isRasenshuriken(current)) {
                return activateRasenshuriken(level, player, stack)
                        ? InteractionResultHolder.consume(stack)
                        : InteractionResultHolder.fail(stack);
            }
            if (isOuterPath(current)) {
                return activateOuterPath(level, player, stack)
                        ? InteractionResultHolder.consume(stack)
                        : InteractionResultHolder.fail(stack);
            }
            player.displayClientMessage(Component.translatable(current.translationKey())
                    .append(Component.literal(" runtime is not ported yet.")), true);
        }
        return InteractionResultHolder.fail(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int remainingUseDuration) {
        if (!(livingEntity instanceof Player player) || level.isClientSide) {
            return;
        }
        JutsuDefinition current = getCurrentJutsu(stack);
        if (isThunder(current)) {
            activateThunder(level, player, stack, remainingUseDuration);
        } else if (isLaser(current)) {
            activateLaser(level, player, stack, remainingUseDuration);
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

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (!level.isClientSide && entity instanceof Player player) {
            if (!player.isCreative() && !hasRinneSharingan(player)) {
                stack.shrink(1);
                player.displayClientMessage(Component.literal("Six Path Senjutsu faded without an active Rinne Sharingan."), true);
                return;
            }
            setOwnerIfMissing(stack, player);
            TruthSeekerBallEntity.maintainSixPathBalls(player, stack);
            TruthSeekerBallEntity.runSixPathSentry(player, stack);
            if (player.tickCount % 40 == 5) {
                updateRasenshurikenGate(player, stack);
            }
        }
    }

    private boolean activateShoot(Level level, Player player, ItemStack stack) {
        JutsuDefinition current = getCurrentJutsu(stack);
        if (!canActivateSixPath(level, player, stack, current)) {
            return false;
        }
        TruthSeekerBallEntity ball = TruthSeekerBallEntity.nextAvailableSixPathBall(player, stack);
        if (ball == null) {
            player.displayClientMessage(Component.literal("No available Truth-Seeking Ball."), true);
            return false;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(current.chakraUsage())) {
            return false;
        }
        if (ball.shootFromOwner(player)) {
            addCurrentJutsuXp(stack, 1);
            return true;
        }
        return false;
    }

    private boolean activateShield(Level level, Player player, ItemStack stack) {
        JutsuDefinition current = getCurrentJutsu(stack);
        if (!canActivateSixPath(level, player, stack, current)) {
            return false;
        }
        TruthSeekerBallEntity ball = TruthSeekerBallEntity.nextAvailableSixPathBall(player, stack);
        if (ball == null) {
            player.displayClientMessage(Component.literal("No available Truth-Seeking Ball."), true);
            return false;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(current.chakraUsage())) {
            return false;
        }
        if (ball.toggleShield(player)) {
            addCurrentJutsuXp(stack, 1);
            return true;
        }
        return false;
    }

    private void activateThunder(Level level, Player player, ItemStack stack, int remainingUseDuration) {
        JutsuDefinition current = getCurrentJutsu(stack);
        if (!canActivateSixPath(level, player, stack, current)) {
            return;
        }
        float power = getSixPathPower(stack, player, current, remainingUseDuration);
        if (!player.isCreative() && !Chakra.pathway(player).consume(current.chakraUsage() * power)) {
            Chakra.pathway(player).warningDisplay();
            return;
        }
        if (IntonRaihaEntity.spawnFrom(player, power)) {
            addCurrentJutsuXp(stack, 1);
        }
    }

    private void activateLaser(Level level, Player player, ItemStack stack, int remainingUseDuration) {
        JutsuDefinition current = getCurrentJutsu(stack);
        if (!canActivateSixPath(level, player, stack, current)) {
            return;
        }
        float power = getSixPathPower(stack, player, current, remainingUseDuration);
        if (!player.isCreative() && !Chakra.pathway(player).consume(current.chakraUsage() * power)) {
            Chakra.pathway(player).warningDisplay();
            return;
        }
        if (RantonKogaEntity.spawnFrom(player, power)) {
            addCurrentJutsuXp(stack, 1);
        }
    }

    private boolean activateRasenshuriken(Level level, Player player, ItemStack stack) {
        JutsuDefinition current = getCurrentJutsu(stack);
        if (!canActivateSixPath(level, player, stack, current)) {
            return false;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(current.chakraUsage())) {
            Chakra.pathway(player).warningDisplay();
            return false;
        }
        if (RasenshurikenEntity.spawnTruthSeekingVariantFrom(player)) {
            addCurrentJutsuXp(stack, 1);
            return true;
        }
        player.displayClientMessage(Component.literal("Truth Seeking Rasenshuriken could not be created."), true);
        return false;
    }

    private boolean canActivateSixPath(Level level, Player player, ItemStack stack, JutsuDefinition definition) {
        return canActivateSixPath(level, player, stack, definition, true);
    }

    private boolean canActivateSixPath(Level level, Player player, ItemStack stack, JutsuDefinition definition, boolean checkChakra) {
        if (!hasRinneSharingan(player) && !player.isCreative()) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("Six Path Senjutsu requires an active Rinne Sharingan."), true);
            }
            return false;
        }
        if (!player.isCreative() && !isOwnedByOrUnbound(player, stack)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("This Six Path Senjutsu item belongs to another player."), true);
            }
            return false;
        }
        if (!level.isClientSide) {
            setOwnerIfMissing(stack, player);
        }
        if (!player.isCreative() && !isJutsuEnabled(stack, definition)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("This Six Path jutsu is not enabled."), true);
            }
            return false;
        }
        if (isRasenshuriken(definition) && !player.isCreative() && !hasUsableFutonRasenshuriken(player)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("Truth Seeking Rasenshuriken requires a usable Futon Rasenshuriken."), true);
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
        if (checkChakra && !player.isCreative() && Chakra.pathway(player).getAmount() < definition.chakraUsage()) {
            Chakra.pathway(player).warningDisplay();
            return false;
        }
        return true;
    }

    private boolean activateOuterPath(Level level, Player player, ItemStack stack) {
        JutsuDefinition current = getCurrentJutsu(stack);
        if (!canActivateSixPath(level, player, stack, current, false) || !(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        if (!triggerOuterPath(serverPlayer, current.chakraUsage(), true)) {
            return false;
        }
        addCurrentJutsuXp(stack, 1);
        return true;
    }

    public static boolean triggerOuterPath(ServerPlayer player, boolean showMessages) {
        return triggerOuterPath(player, OUTER_PATH.chakraUsage(), showMessages);
    }

    public static boolean triggerOuterPath(ServerPlayer player, double chakraUsage, boolean showMessages) {
        ServerLevel serverLevel = player.serverLevel();
        if (!player.isCreative() && NarutomodModVariables.getNinjaLevel(player) < OUTER_PATH_REQUIRED_NINJA_LEVEL) {
            if (showMessages) {
                player.displayClientMessage(Component.translatable("chattext.outerpath.notenoughxp"), false);
            }
            return false;
        }
        UUID tenTailsJinchuriki = BijuManager.getJinchurikiUuid(player.getServer(), BijuManager.MAX_TAILS);
        if (tenTailsJinchuriki != null && !tenTailsJinchuriki.equals(player.getUUID())) {
            if (showMessages) {
                player.displayClientMessage(Component.translatable("chattext.outerpath.hasjinchuriki"), false);
            }
            return false;
        }
        player.swing(InteractionHand.MAIN_HAND, true);
        GedoStatueEntity existing = GedoStatueEntity.findLoaded(player.getServer());
        if (existing != null) {
            Vec3 pos = existing.position();
            double height = existing.getBbHeight();
            existing.discard();
            playOuterPathEffects(serverLevel, pos, height);
            return true;
        }

        if (!player.isCreative() && !Chakra.pathway(player).consume(chakraUsage)) {
            Chakra.pathway(player).warningDisplay();
            return false;
        }
        Vec3 pos = outerPathTarget(player);
        GedoStatueEntity gedo = GedoStatueEntity.spawnAt(serverLevel, pos, player.getYRot(), player);
        if (gedo == null) {
            if (!player.isCreative()) {
                Chakra.pathway(player).consume(-chakraUsage, false);
            }
            if (showMessages) {
                player.displayClientMessage(Component.literal("Outer Path could not summon the Gedo Statue."), true);
            }
            return false;
        }
        player.getPersistentData().putDouble(NarutomodModVariables.INVULNERABLE_TIME, 100.0D);
        playOuterPathEffects(serverLevel, gedo.position(), gedo.getBbHeight());
        return true;
    }

    private static Vec3 outerPathTarget(Player player) {
        BlockHitResult hit = ProcedureUtils.raytraceBlocks(player, 10.0D);
        Vec3 target = hit.getType() == HitResult.Type.MISS
                ? player.getEyePosition().add(player.getLookAngle().scale(10.0D))
                : hit.getLocation();
        return new Vec3(target.x(), player.getY(), target.z());
    }

    private static void playOuterPathEffects(ServerLevel level, Vec3 pos, double height) {
        level.playSound(null, pos.x(), pos.y(), pos.z(), ModSounds.SOUND_KUCHIYOSENOJUTSU.get(),
                SoundSource.PLAYERS, 2.0F, 0.9F);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                pos.x(), pos.y() + height * 0.5D, pos.z(),
                3, 5.0D, Math.max(height, 1.0D), 5.0D, 1.0D);
    }

    private float getSixPathPower(ItemStack stack, Player player, JutsuDefinition definition, int remainingUseDuration) {
        float power = getChargingPower(stack, player, remainingUseDuration, definition.basePower(), definition.powerUpDelay());
        float maxJutsuPower = isLaser(definition) ? 10.0F : 6.0F;
        if (player.isCreative()) {
            return Math.min(Math.max(power, definition.basePower()), isLaser(definition) ? maxJutsuPower : 100.0F);
        }
        float maxPower = (float)(Chakra.pathway(player).getAmount() / Math.max(definition.chakraUsage(), 1.0D) * 0.9999D);
        return Math.min(Math.max(power, definition.basePower()), Math.min(maxPower, maxJutsuPower));
    }

    private boolean isShoot(JutsuDefinition definition) {
        return definition.index() == SHOOT.index() && SHOOT.translationKey().equals(definition.translationKey());
    }

    private boolean isShield(JutsuDefinition definition) {
        return definition.index() == SHIELD.index() && SHIELD.translationKey().equals(definition.translationKey());
    }

    private boolean isThunder(JutsuDefinition definition) {
        return definition.index() == THUNDER.index() && THUNDER.translationKey().equals(definition.translationKey());
    }

    private boolean isLaser(JutsuDefinition definition) {
        return definition.index() == LASER.index() && LASER.translationKey().equals(definition.translationKey());
    }

    private boolean isRasenshuriken(JutsuDefinition definition) {
        return definition.index() == RASENSHURIKEN.index() && RASENSHURIKEN.translationKey().equals(definition.translationKey());
    }

    private boolean isOuterPath(JutsuDefinition definition) {
        return definition.index() == OUTER_PATH.index() && OUTER_PATH.translationKey().equals(definition.translationKey());
    }

    private void updateRasenshurikenGate(Player player, ItemStack stack) {
        enableJutsu(stack, RASENSHURIKEN, player.isCreative() || hasUsableFutonRasenshuriken(player));
    }

    private boolean hasUsableFutonRasenshuriken(Player player) {
        ItemStack futonStack = ProcedureUtils.getMatchingItemStack(player, ModItems.FUTON.get());
        if (futonStack == null || !(futonStack.getItem() instanceof FutonItem futonItem)) {
            return false;
        }
        return JutsuItem.isOwnedByOrUnbound(player, futonStack)
                && futonItem.isJutsuEnabled(futonStack, FutonItem.RASENSHURIKEN);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        JutsuDefinition current = getCurrentJutsu(stack);
        tooltip.add(Component.literal("Requires active Rinne Sharingan").withStyle(ChatFormatting.LIGHT_PURPLE));
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

    public static boolean hasRinneSharingan(Player player) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        return (head.is(ModItems.RINNEGANHELMET.get()) || head.is(ModItems.TENSEIGANHELMET.get()))
                && head.getTag() != null
                && head.getTag().getBoolean(NarutomodModVariables.RINNESHARINGAN_ACTIVATED);
    }
}
