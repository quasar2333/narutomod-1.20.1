package net.narutomod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.narutomod.Chakra;
import net.narutomod.PlayerTracker;
import net.narutomod.entity.ChidoriEntity;
import net.narutomod.entity.ChidoriSpearEntity;
import net.narutomod.entity.FalseDarknessEntity;
import net.narutomod.entity.KirinEntity;
import net.narutomod.entity.LightningBeastEntity;
import net.narutomod.entity.RaitonChakraModeEntity;
import net.narutomod.particle.NarutoParticleKind;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModParticleTypes;
import net.narutomod.registry.ModSounds;

public final class RaitonItem extends JutsuItem {
    public static final JutsuDefinition CHIDORI = JutsuDefinition.ranked(0, "entity.narutomod.chidori", 'A', 150.0D);
    public static final JutsuDefinition CHAKRA_MODE = JutsuDefinition.ranked(1, "entity.narutomod.raitonchakramode", 'B', 10.0D);
    public static final JutsuDefinition CHASING_DOG = JutsuDefinition.ranked(2, "entity.narutomod.lightning_beast", 'C', 20.0D).withPower(5.0F, 30.0F);
    public static final JutsuDefinition FALSE_DARKNESS = JutsuDefinition.ranked(3, "entity.narutomod.false_darkness", 'B', 100.0D).withPower(1.0F, 150.0F);
    public static final JutsuDefinition KIRIN = JutsuDefinition.ranked(4, "entity.narutomod.kirin", 'S', 1500.0D).withPower(0.0F, 400.0F);
    private static final JutsuDefinition[] RAITON_JUTSUS = {
            CHIDORI,
            CHAKRA_MODE,
            CHASING_DOG,
            FALSE_DARKNESS,
            KIRIN
    };

    public RaitonItem() {
        super(JutsuType.RAITON, RAITON_JUTSUS);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!canBeginRaiton(level, player, stack)) {
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide && isJutsu(getCurrentJutsu(stack), KIRIN)) {
            KirinEntity.startWeatherThunder(player);
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
        if (isJutsu(getCurrentJutsu(stack), KIRIN)) {
            KirinEntity.chargingEffects(player, getRaitonPower(stack, player, KIRIN, remainingUseDuration));
        }
        if (usedTicks % 5 == 0) {
            serverLevel.sendParticles(
                    ModParticleTypes.options(NarutoParticleKind.SMOKE_COLORED, 0x2080D0FF, 5, 50, 0xF0, player.getId(), 4),
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

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int remainingUseDuration) {
        if (!(livingEntity instanceof Player player) || level.isClientSide) {
            return;
        }
        JutsuDefinition currentJutsu = getCurrentJutsu(stack);
        if (isJutsu(currentJutsu, CHIDORI)) {
            activateChidori(level, player, stack);
        } else if (isJutsu(currentJutsu, CHAKRA_MODE)) {
            toggleChakraMode(level, player, stack);
        } else if (isJutsu(currentJutsu, CHASING_DOG)) {
            activateLightningBeast(level, player, stack, remainingUseDuration);
        } else if (isJutsu(currentJutsu, FALSE_DARKNESS)) {
            activateFalseDarkness(level, player, stack, remainingUseDuration);
        } else if (isJutsu(currentJutsu, KIRIN)) {
            activateKirin(level, player, stack, remainingUseDuration);
        }
    }

    public void setChakraModeCooldown(ItemStack stack, Level level, LivingEntity owner, int activeTicks) {
        long cooldown = (long)(Math.max(activeTicks, 0) * jutsuModifier(stack, owner, CHAKRA_MODE));
        setJutsuCooldown(stack, level, CHAKRA_MODE, cooldown);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    private void activateChidori(Level level, Player player, ItemStack stack) {
        if (!canActivateRaiton(level, player, stack, CHIDORI)) {
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(CHIDORI.chakraUsage())) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel) level;
        ChidoriEntity active = findActiveChidori(serverLevel, player);
        if (active != null) {
            active.stopChidori();
            ChidoriSpearEntity spear = ModEntityTypes.CHIDORI_SPEAR.get().create(level);
            if (spear != null) {
                spear.configure(player, player.isShiftKeyDown());
                level.addFreshEntity(spear);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        ModSounds.SOUND_ELECTRICITY.get(), SoundSource.PLAYERS, 1.0F, 1.5F);
                addCurrentJutsuXp(stack, 1);
            }
            return;
        }
        ChidoriEntity entity = ModEntityTypes.CHIDORI.get().create(level);
        if (entity == null) {
            return;
        }
        int duration = chidoriDuration(stack, player);
        entity.configure(player, ChidoriEntity.CHAKRA_BURN_PER_SECOND, duration);
        level.addFreshEntity(entity);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.SOUND_CHIDORI.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        addCurrentJutsuXp(stack, 1);
    }

    private void toggleChakraMode(Level level, Player player, ItemStack stack) {
        ServerLevel serverLevel = (ServerLevel) level;
        RaitonChakraModeEntity active = findActiveChakraMode(serverLevel, player);
        if (active != null) {
            active.stopChakraMode();
            return;
        }
        if (!canActivateRaiton(level, player, stack, CHAKRA_MODE)) {
            return;
        }
        RaitonChakraModeEntity entity = ModEntityTypes.RAITONCHAKRAMODE.get().create(level);
        if (entity == null) {
            return;
        }
        entity.configure(player, stack);
        level.addFreshEntity(entity);
        player.getPersistentData().putInt(RaitonChakraModeEntity.ENTITY_ID_KEY, entity.getId());
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.SOUND_ELECTRICITY.get(), SoundSource.PLAYERS, 0.5F, 0.8F);
        addCurrentJutsuXp(stack, 1);
    }

    private void activateLightningBeast(Level level, Player player, ItemStack stack, int remainingUseDuration) {
        if (!canActivateRaiton(level, player, stack, CHASING_DOG)) {
            return;
        }
        float power = getRaitonPower(stack, player, CHASING_DOG, remainingUseDuration);
        if (power <= 0.0F) {
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(CHASING_DOG.chakraUsage() * power)) {
            return;
        }
        LightningBeastEntity beast = ModEntityTypes.LIGHTNING_BEAST.get().create(level);
        if (beast == null) {
            return;
        }
        beast.configure(player, power);
        level.addFreshEntity(beast);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.SOUND_ELECTRICITY.get(), SoundSource.PLAYERS, 1.0F, 1.1F);
        addCurrentJutsuXp(stack, 1);
    }

    private void activateFalseDarkness(Level level, Player player, ItemStack stack, int remainingUseDuration) {
        if (!canActivateRaiton(level, player, stack, FALSE_DARKNESS)) {
            return;
        }
        HitResult hit = ProcedureUtils.objectEntityLookingAt(player, 20.0D, 0.0D, false, false, target -> target != player);
        if (!(hit instanceof EntityHitResult entityHit) || !(entityHit.getEntity() instanceof LivingEntity target)) {
            player.displayClientMessage(Component.literal("False Darkness needs a living target."), true);
            return;
        }
        float power = getRaitonPower(stack, player, FALSE_DARKNESS, remainingUseDuration);
        if (power <= 0.0F) {
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(FALSE_DARKNESS.chakraUsage() * power)) {
            return;
        }
        FalseDarknessEntity entity = ModEntityTypes.FALSE_DARKNESS.get().create(level);
        if (entity == null) {
            return;
        }
        entity.configure(player, target, power);
        level.addFreshEntity(entity);
        addCurrentJutsuXp(stack, 1);
    }

    private void activateKirin(Level level, Player player, ItemStack stack, int remainingUseDuration) {
        if (!canActivateRaiton(level, player, stack, KIRIN)) {
            return;
        }
        float power = getRaitonPower(stack, player, KIRIN, remainingUseDuration);
        if (power < 1.0F) {
            player.displayClientMessage(Component.literal(String.format("Kirin power %.2f/1.00", power)), true);
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(KIRIN.chakraUsage() * power)) {
            return;
        }
        KirinEntity entity = ModEntityTypes.KIRIN.get().create(level);
        if (entity == null) {
            return;
        }
        entity.configure(player);
        level.addFreshEntity(entity);
        if (!player.isCreative()) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 300, 0, false, false));
            setJutsuCooldown(stack, level, KIRIN, (long)(3600.0F * jutsuModifier(stack, player, KIRIN)));
        }
        addCurrentJutsuXp(stack, 1);
    }

    private boolean canBeginRaiton(Level level, Player player, ItemStack stack) {
        JutsuDefinition currentJutsu = getCurrentJutsu(stack);
        if (!isJutsu(currentJutsu, CHIDORI)
                && !isJutsu(currentJutsu, CHAKRA_MODE)
                && !isJutsu(currentJutsu, CHASING_DOG)
                && !isJutsu(currentJutsu, FALSE_DARKNESS)
                && !isJutsu(currentJutsu, KIRIN)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("This Raiton jutsu is not ported yet."), true);
            }
            return false;
        }
        if (!level.isClientSide) {
            setOwnerIfMissing(stack, player);
            if (isJutsu(currentJutsu, CHAKRA_MODE) && findActiveChakraMode((ServerLevel) level, player) != null) {
                return true;
            }
        }
        return canActivateRaiton(level, player, stack, currentJutsu);
    }

    private boolean canActivateRaiton(Level level, Player player, ItemStack stack, JutsuDefinition definition) {
        if (player.isCreative()) {
            return true;
        }
        if (!PlayerTracker.isNinja(player)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("You need ninja experience to use this Raiton jutsu."), true);
            }
            return false;
        }
        if (!canUseJutsu(stack, definition, player)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("You need to learn this Raiton jutsu from a scroll."), true);
            }
            return false;
        }
        if (!hasEnoughJutsuXp(stack, definition)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("Raiton XP " + getJutsuXp(stack, definition)
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

    private int chidoriDuration(ItemStack stack, Player player) {
        float xpModifier = Math.max(getCurrentJutsuXpModifier(stack, player), 0.05F);
        return Math.max((int)(PlayerTracker.getNinjaLevel(player) * 5.0D / xpModifier), ChidoriEntity.GROW_TIME + 20);
    }

    private float getRaitonPower(ItemStack stack, Player player, JutsuDefinition definition, int remainingUseDuration) {
        float power = getChargingPower(stack, player, remainingUseDuration, definition.basePower(), definition.powerUpDelay());
        return Math.min(power, getMaxRaitonPower(player, definition));
    }

    private float getMaxRaitonPower(Player player, JutsuDefinition definition) {
        if (player.isCreative()) {
            return definition == KIRIN ? 1.0F : Math.max(definition.basePower(), 100.0F);
        }
        if (definition.chakraUsage() <= 0.0D) {
            return definition.basePower();
        }
        float maxPower = (float)(Chakra.pathway(player).getAmount() / definition.chakraUsage() * 0.9999D);
        return definition == KIRIN ? Math.min(maxPower, 1.0F) : maxPower;
    }

    private float jutsuModifier(ItemStack stack, LivingEntity entity, JutsuDefinition definition) {
        int required = getRequiredXp(stack, definition);
        int current = entity instanceof Player player && !player.isCreative() ? getJutsuXp(stack, definition) : required;
        float xpModifier = current != 0 ? (float) required / (float) current : 0.0F;
        return (float) Chakra.getChakraModifier(entity) * xpModifier;
    }

    private static boolean isJutsu(JutsuDefinition first, JutsuDefinition second) {
        return first.index() == second.index() && first.translationKey().equals(second.translationKey());
    }

    private static ChidoriEntity findActiveChidori(ServerLevel level, Player player) {
        for (ChidoriEntity entity : level.getEntitiesOfClass(ChidoriEntity.class, player.getBoundingBox().inflate(32.0D))) {
            if (entity.isOwnedBy(player)) {
                return entity;
            }
        }
        return null;
    }

    private static RaitonChakraModeEntity findActiveChakraMode(ServerLevel level, Player player) {
        int entityId = player.getPersistentData().getInt(RaitonChakraModeEntity.ENTITY_ID_KEY);
        if (entityId > 0 && level.getEntity(entityId) instanceof RaitonChakraModeEntity entity && entity.isOwnedBy(player)) {
            return entity;
        }
        for (RaitonChakraModeEntity entity : level.getEntitiesOfClass(RaitonChakraModeEntity.class, player.getBoundingBox().inflate(64.0D))) {
            if (entity.isOwnedBy(player)) {
                player.getPersistentData().putInt(RaitonChakraModeEntity.ENTITY_ID_KEY, entity.getId());
                return entity;
            }
        }
        return null;
    }
}
