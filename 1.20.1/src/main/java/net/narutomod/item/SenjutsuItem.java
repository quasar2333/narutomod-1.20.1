package net.narutomod.item;

import java.util.List;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.narutomod.Chakra;
import net.narutomod.NarutomodMod;
import net.narutomod.NarutomodModVariables;
import net.narutomod.PlayerTracker;
import net.narutomod.entity.Buddha1000Entity;
import net.narutomod.entity.RasenshurikenEntity;
import net.narutomod.entity.SenjutsuSitPlatformEntity;
import net.narutomod.entity.Snake8HeadsEntity;
import net.narutomod.particle.NarutoParticleKind;
import net.narutomod.procedure.ProcedureSync;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModItems;
import net.narutomod.registry.ModParticleTypes;
import net.narutomod.registry.ModSounds;

public final class SenjutsuItem extends NinjutsuItem {
    public static final JutsuDefinition SAGE_MODE = JutsuDefinition.ranked(0, "item.sage_mode_armorhelmet.name", 'S', 10.0D);
    public static final JutsuDefinition SAGE_RASENGAN = JutsuDefinition.ranked(1, "tooltip.senjutsu.rasengan", 'S', 150.0D);
    public static final JutsuDefinition SAGE_RASENSHURIKEN = JutsuDefinition.ranked(2, "tooltip.senjutsu.rasenshuriken", 'S', 1000.0D);
    public static final JutsuDefinition WOOD_BUDDHA = JutsuDefinition.ranked(3, "buddha_1000", 'S', 5000.0D);
    public static final JutsuDefinition SNAKE_8_HEADS = JutsuDefinition.ranked(4, "snake_8_heads", 'S', 3000.0D);
    private static final JutsuDefinition[] SENJUTSU_JUTSUS = {
            SAGE_MODE,
            SAGE_RASENGAN,
            SAGE_RASENSHURIKEN,
            WOOD_BUDDHA,
            SNAKE_8_HEADS
    };
    public static final String SAGE_MODE_ACTIVATED_TAG = "SageModeActivated";
    public static final String SAGE_CHAKRA_DEPLETION_AMOUNT_TAG = "SageChakraDepletionAmount";
    public static final String SAGE_TYPE_TAG = "SageType";
    private static final String SAGE_MODE_CHARGING_TAG = "SageModeCharging";
    private static final String SAGE_PREVIOUS_FOOD_TAG = "SagePreviousFood";
    private static final double SAGE_MODE_BASE_CHAKRA_COST = 1000.0D;
    private static final float SAGE_MODE_MIN_POWER = 100.0F;
    private static final float SAGE_MODE_POWERUP_DELAY = 20.0F;
    private static final int SAGE_MODE_CHAKRA_DRAIN = 50;
    private static final RasenganProfile SENJUTSU_RASENGAN = new RasenganProfile("Sage Rasengan", 150.0D, 2.9F, 3.0F, 7.0F, 200.0F, 40, true);
    private static final List<SageModeModifier> SAGE_MODE_MODIFIERS = List.of(
            new SageModeModifier(ForgeMod.ENTITY_REACH.get(), new AttributeModifier(
                    UUID.fromString("c3ee1250-8b80-4668-b58a-33af5ea73ee6"),
                    "sagemode.reach",
                    1.5D,
                    AttributeModifier.Operation.ADDITION)),
            new SageModeModifier(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                    UUID.fromString("6d6202e1-9aac-4c3d-ba0c-6684bdd58868"),
                    "sagemode.damage",
                    60.0D,
                    AttributeModifier.Operation.ADDITION)),
            new SageModeModifier(Attributes.ATTACK_SPEED, new AttributeModifier(
                    UUID.fromString("33b7fa14-828a-4964-b014-b61863526589"),
                    "sagemode.damagespeed",
                    2.0D,
                    AttributeModifier.Operation.MULTIPLY_BASE)),
            new SageModeModifier(Attributes.MOVEMENT_SPEED, new AttributeModifier(
                    UUID.fromString("74f3ab51-a73f-45e3-a4c4-aae6974b6414"),
                    "sagemode.movement",
                    1.5D,
                    AttributeModifier.Operation.MULTIPLY_BASE)),
            new SageModeModifier(Attributes.MAX_HEALTH, new AttributeModifier(
                    UUID.fromString("70e0acc2-cf75-4bbd-a21a-753088324a59"),
                    "sagemode.health",
                    80.0D,
                    AttributeModifier.Operation.ADDITION)));

    public SenjutsuItem() {
        super(SENJUTSU_RASENGAN, JutsuType.SENJUTSU, SENJUTSU_JUTSUS);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!canBeginSageModeCharge(level, player, stack)) {
                return InteractionResultHolder.fail(stack);
            }
            if (!level.isClientSide && !SenjutsuSitPlatformEntity.spawnFor(player)) {
                return InteractionResultHolder.fail(stack);
            }
            stack.getOrCreateTag().putBoolean(SAGE_MODE_CHARGING_TAG, true);
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        stack.getOrCreateTag().putBoolean(SAGE_MODE_CHARGING_TAG, false);
        if (isJutsu(getCurrentJutsu(stack), SAGE_RASENSHURIKEN)) {
            return beginSageRasenshurikenCharge(level, player, hand, stack);
        }
        if (isJutsu(getCurrentJutsu(stack), WOOD_BUDDHA)) {
            return useWoodBuddha(level, player, stack);
        }
        if (isJutsu(getCurrentJutsu(stack), SNAKE_8_HEADS)) {
            return useSnake8Heads(level, player, stack);
        }
        return super.use(level, player, hand);
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        if (isChargingSageMode(stack)) {
            if (livingEntity instanceof Player player && level instanceof ServerLevel serverLevel) {
                if (!player.isShiftKeyDown()) {
                    player.stopUsingItem();
                    return;
                }
                if (!SenjutsuSitPlatformEntity.isRidingPlatform(player)) {
                    player.stopUsingItem();
                    return;
                }
                int usedTicks = getUseDuration(stack) - remainingUseDuration;
                float power = getSageModePower(stack, player, remainingUseDuration);
                if (usedTicks % 20 == 0) {
                    player.displayClientMessage(Component.literal(String.format("Sage Mode power %.1f/%.1f", power, SAGE_MODE_MIN_POWER)), true);
                }
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
                            0.05D);
                }
                if (usedTicks % 10 == 0) {
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            ModSounds.SOUND_CHARGING_CHAKRA.get(), SoundSource.PLAYERS, 0.05F, level.random.nextFloat() + 0.5F);
                }
            }
            return;
        }
        super.onUseTick(level, livingEntity, stack, remainingUseDuration);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int remainingUseDuration) {
        if (isChargingSageMode(stack)) {
            stack.getOrCreateTag().putBoolean(SAGE_MODE_CHARGING_TAG, false);
            if (livingEntity instanceof Player player && !level.isClientSide) {
                activateSageModeFromCharge(stack, player, remainingUseDuration);
                SenjutsuSitPlatformEntity.stopRidingIfOnPlatform(player);
            }
            return;
        }
        if (isJutsu(getCurrentJutsu(stack), SAGE_RASENSHURIKEN)) {
            if (livingEntity instanceof Player player && !level.isClientSide) {
                releaseSageRasenshuriken(stack, level, player, remainingUseDuration);
            }
            return;
        }
        super.releaseUsing(stack, level, livingEntity, remainingUseDuration);
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity target) {
        if (target == player
                && getSageType(stack) == SageType.TOAD
                && isSageModeActivated(stack)
                && !player.level().isClientSide) {
            HitResult hit = ProcedureUtils.objectEntityLookingAt(player, ProcedureUtils.getReachDistance(player), 2.0D,
                    true, false, candidate -> candidate != player);
            if (hit instanceof EntityHitResult entityHit) {
                player.attack(entityHit.getEntity());
                return true;
            }
        }
        return super.onLeftClickEntity(stack, player, target);
    }

    @Override
    protected boolean isRasenganLearnedForUse(Player player, ItemStack stack) {
        return hasAnyLearnedRasengan(player);
    }

    @Override
    protected boolean hasUsePrerequisites(Level level, Player player, ItemStack stack) {
        if (!super.hasUsePrerequisites(level, player, stack)) {
            return false;
        }
        if (!isSageModeActivated(stack)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("Sage Mode must be active to use Sage Rasengan."), true);
            }
            return false;
        }
        return true;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (level.isClientSide || !(entity instanceof Player player)) {
            return;
        }
        if (player.tickCount % 40 == 5) {
            updateInheritedJutsuGates(stack, player);
        }
        if (isSageModeActivated(stack)) {
            tickActiveSageMode(stack, player);
        }
    }

    public static boolean isSageModeActivated(ItemStack stack) {
        return stack.getTag() != null && stack.getTag().getBoolean(SAGE_MODE_ACTIVATED_TAG);
    }

    public static void setSageModeActivated(ItemStack stack, boolean active) {
        stack.getOrCreateTag().putBoolean(SAGE_MODE_ACTIVATED_TAG, active);
        if (!active) {
            stack.getOrCreateTag().putDouble(SAGE_CHAKRA_DEPLETION_AMOUNT_TAG, 0.0D);
        }
    }

    private static boolean isChargingSageMode(ItemStack stack) {
        return stack.getTag() != null && stack.getTag().getBoolean(SAGE_MODE_CHARGING_TAG);
    }

    private boolean canBeginSageModeCharge(Level level, Player player, ItemStack stack) {
        if (isSageModeActivated(stack)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("Sage Mode is already active."), true);
            }
            return false;
        }
        if (!player.isCreative() && !PlayerTracker.isNinja(player)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("You need ninja experience to use Sage Mode."), true);
            }
            return false;
        }
        if (!player.isCreative() && !isOwnedByOrUnbound(player, stack)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("This senjutsu item belongs to another player."), true);
            }
            return false;
        }
        if (!level.isClientSide) {
            setOwnerIfMissing(stack, player);
            ensureSageType(stack, level);
        }
        return player.isCreative() || Chakra.pathway(player).getAmount() >= SAGE_MODE_BASE_CHAKRA_COST;
    }

    private static void activateSageModeFromCharge(ItemStack stack, Player player, int remainingUseDuration) {
        float power = getSageModePower(stack, player, remainingUseDuration);
        if (power < SAGE_MODE_MIN_POWER) {
            player.displayClientMessage(Component.literal(String.format("Sage Mode power %.1f/%.1f", power, SAGE_MODE_MIN_POWER)), true);
            return;
        }

        Chakra.Pathway pathway = Chakra.pathway(player);
        double activationThreshold = pathway.getAmount();
        if (!player.isCreative() && activationThreshold < SAGE_MODE_BASE_CHAKRA_COST) {
            pathway.warningDisplay();
            return;
        }
        if (!player.isCreative()) {
            pathway.consume(-0.6F, true);
            if (!pathway.consume(SAGE_MODE_BASE_CHAKRA_COST)) {
                return;
            }
        }

        SageType sageType = ensureSageType(stack, player.level());
        stack.getOrCreateTag().putDouble(SAGE_CHAKRA_DEPLETION_AMOUNT_TAG, activationThreshold);
        stack.getOrCreateTag().putInt(SAGE_PREVIOUS_FOOD_TAG, player.getFoodData().getFoodLevel());
        setSageModeActivated(stack, true);
        ProcedureSync.EntityNBTTag.setAndSync(player, SAGE_MODE_ACTIVATED_TAG, true);
        ProcedureSync.EntityNBTTag.setAndSync(player, SAGE_TYPE_TAG, sageType.id());
        applySageModeModifiers(player);
        player.displayClientMessage(Component.literal("Sage Mode activated."), true);
    }

    private static float getSageModePower(ItemStack stack, Player player, int remainingUseDuration) {
        int usedTicks = Math.max(stack.getUseDuration() - remainingUseDuration, 0);
        double modifier = Math.max(Chakra.getChakraModifier(player), 0.05D);
        return (float) (usedTicks / (SAGE_MODE_POWERUP_DELAY * modifier));
    }

    private static void tickActiveSageMode(ItemStack stack, Player player) {
        applySageModeModifiers(player);
        player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 3, 0, false, false));

        Chakra.Pathway pathway = Chakra.pathway(player);
        double deactivationThreshold = stack.getOrCreateTag().getDouble(SAGE_CHAKRA_DEPLETION_AMOUNT_TAG);
        if (!player.isCreative() && pathway.getAmount() < deactivationThreshold) {
            deactivateSageMode(stack, player);
            return;
        }
        if (!player.isCreative() && player.tickCount % 20 == 10) {
            pathway.consume(SAGE_MODE_CHAKRA_DRAIN);
        }
    }

    private static void deactivateSageMode(ItemStack stack, Player player) {
        setSageModeActivated(stack, false);
        ProcedureSync.EntityNBTTag.removeAndSync(player, SAGE_MODE_ACTIVATED_TAG);
        ProcedureSync.EntityNBTTag.removeAndSync(player, SAGE_TYPE_TAG);
        removeSageModeModifiers(player);
        if (stack.getTag() != null && stack.getTag().contains(SAGE_PREVIOUS_FOOD_TAG)) {
            player.getFoodData().setFoodLevel(Math.max(stack.getTag().getInt(SAGE_PREVIOUS_FOOD_TAG) - 5, 0));
        }
        player.displayClientMessage(Component.literal("Sage Mode ended."), true);
    }

    private InteractionResultHolder<ItemStack> beginSageRasenshurikenCharge(Level level, Player player, InteractionHand hand, ItemStack stack) {
        if (!canActivateSageRasenshuriken(level, player, stack)) {
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    private void releaseSageRasenshuriken(ItemStack stack, Level level, Player player, int remainingUseDuration) {
        if (!canActivateSageRasenshuriken(level, player, stack)) {
            return;
        }
        float power = getSageRasenshurikenPower(stack, player, remainingUseDuration);
        if (power < 0.1F) {
            player.displayClientMessage(Component.literal(String.format("Sage Rasenshuriken power %.2f/0.10", power)), true);
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(SAGE_RASENSHURIKEN.chakraUsage() * power)) {
            return;
        }
        if (!RasenshurikenEntity.spawnFrom(player, power)) {
            player.displayClientMessage(Component.literal("Sage Rasenshuriken could not be created."), true);
            return;
        }
        addJutsuXp(stack, SAGE_RASENSHURIKEN, 1);
    }

    private boolean canActivateSageRasenshuriken(Level level, Player player, ItemStack stack) {
        if (player.isCreative()) {
            return true;
        }
        if (!PlayerTracker.isNinja(player)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("You need ninja experience to use this Senjutsu jutsu."), true);
            }
            return false;
        }
        if (!isOwnedByOrUnbound(player, stack)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("This senjutsu item belongs to another player."), true);
            }
            return false;
        }
        if (!level.isClientSide) {
            setOwnerIfMissing(stack, player);
            updateInheritedJutsuGates(stack, player);
        }
        if (!isSageModeActivated(stack)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("Sage Mode must be active to use Sage Rasenshuriken."), true);
            }
            return false;
        }
        if (!hasUsableFutonRasenshuriken(player)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("Sage Rasenshuriken requires a usable Futon Rasenshuriken."), true);
            }
            return false;
        }
        if (!canUseJutsu(stack, SAGE_RASENSHURIKEN, player)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("You need to learn this Senjutsu jutsu from a scroll."), true);
            }
            return false;
        }
        if (!hasEnoughJutsuXp(stack, SAGE_RASENSHURIKEN)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("Senjutsu XP " + getJutsuXp(stack, SAGE_RASENSHURIKEN)
                        + "/" + getRequiredXp(stack, SAGE_RASENSHURIKEN)), true);
            }
            return false;
        }
        long cooldown = getRemainingCooldownTicks(stack, level, SAGE_RASENSHURIKEN);
        if (cooldown > 0L) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("Cooldown " + cooldown / 20L + "s"), true);
            }
            return false;
        }
        double chakraBurn = SAGE_RASENSHURIKEN.chakraUsage() * Math.max(getCurrentJutsuXpModifier(stack, player), 0.05F);
        if (Chakra.pathway(player).getAmount() < chakraBurn) {
            Chakra.pathway(player).warningDisplay();
            return false;
        }
        return true;
    }

    private float getSageRasenshurikenPower(ItemStack stack, Player player, int remainingUseDuration) {
        float power = getChargingPower(stack, player, remainingUseDuration, SAGE_RASENSHURIKEN.basePower(), SAGE_RASENSHURIKEN.powerUpDelay());
        if (player.isCreative()) {
            return Math.min(Math.max(power, 0.1F), 2.0F);
        }
        float maxPower = (float)(Chakra.pathway(player).getAmount() / SAGE_RASENSHURIKEN.chakraUsage() * 0.9999D);
        return Math.min(Math.max(power, 0.0F), Math.min(maxPower, 2.0F));
    }

    private void updateInheritedJutsuGates(ItemStack stack, Player player) {
        enableJutsu(stack, SAGE_RASENGAN, player.isCreative() || NinjutsuItem.hasAnyLearnedRasengan(player));
        enableJutsu(stack, SAGE_RASENSHURIKEN, player.isCreative() || hasUsableFutonRasenshuriken(player));
        enableJutsu(stack, WOOD_BUDDHA, player.isCreative() || hasUsableMokutonWoodGolem(player));
    }

    private boolean hasUsableFutonRasenshuriken(Player player) {
        ItemStack futonStack = ProcedureUtils.getMatchingItemStack(player, ModItems.FUTON.get());
        if (futonStack == null || !(futonStack.getItem() instanceof FutonItem futonItem)) {
            return false;
        }
        return JutsuItem.isOwnedByOrUnbound(player, futonStack)
                && futonItem.isJutsuEnabled(futonStack, FutonItem.RASENSHURIKEN);
    }

    private boolean hasUsableMokutonWoodGolem(Player player) {
        ItemStack mokutonStack = ProcedureUtils.getMatchingItemStack(player, ModItems.MOKUTON.get());
        if (mokutonStack == null || !(mokutonStack.getItem() instanceof AdvancedNatureJutsuItem mokutonItem)) {
            return false;
        }
        return JutsuItem.isOwnedByOrUnbound(player, mokutonStack)
                && mokutonItem.kind() == AdvancedNatureJutsuItem.AdvancedNatureKind.MOKUTON
                && mokutonItem.isJutsuEnabled(mokutonStack,
                        AdvancedNatureJutsuItem.AdvancedNatureKind.MOKUTON.definitionByIndex(3));
    }

    private InteractionResultHolder<ItemStack> useWoodBuddha(Level level, Player player, ItemStack stack) {
        if (!canActivateWoodBuddha(level, player, stack)) {
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide) {
            Entity vehicle = player.getVehicle();
            if (vehicle instanceof Buddha1000Entity buddha && buddha.isOwnedBy(player)) {
                if (buddha.isSitting()) {
                    buddha.setSitting(false);
                    level.playSound(null, buddha.getX(), buddha.getY(), buddha.getZ(),
                            ModSounds.SOUND_WOODSPAWN.get(), SoundSource.NEUTRAL, 5.0F,
                            player.getRandom().nextFloat() * 0.6F + 0.6F);
                    addJutsuXp(stack, WOOD_BUDDHA, 1);
                }
            } else {
                double chakraBurn = WOOD_BUDDHA.chakraUsage() * 0.02D * Math.max(getCurrentJutsuXpModifier(stack, player), 0.05F);
                if (!Buddha1000Entity.spawnFrom(player, chakraBurn)) {
                    player.displayClientMessage(Component.literal("Wood Buddha could not be created."), true);
                    return InteractionResultHolder.fail(stack);
                }
                addJutsuXp(stack, WOOD_BUDDHA, 1);
            }
            setJutsuCooldown(stack, level, WOOD_BUDDHA, 20L);
        }
        return InteractionResultHolder.consume(stack);
    }

    private boolean canActivateWoodBuddha(Level level, Player player, ItemStack stack) {
        if (player.isCreative()) {
            return true;
        }
        if (!PlayerTracker.isNinja(player)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("You need ninja experience to use this Senjutsu jutsu."), true);
            }
            return false;
        }
        if (!isOwnedByOrUnbound(player, stack)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("This senjutsu item belongs to another player."), true);
            }
            return false;
        }
        if (!level.isClientSide) {
            setOwnerIfMissing(stack, player);
            updateInheritedJutsuGates(stack, player);
        }
        if (!isSageModeActivated(stack)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("Sage Mode must be active to use Wood Buddha."), true);
            }
            return false;
        }
        if (!hasUsableMokutonWoodGolem(player)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("Wood Buddha requires a usable Mokuton Wood Golem."), true);
            }
            return false;
        }
        if (!canUseJutsu(stack, WOOD_BUDDHA, player)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("You need to learn this Senjutsu jutsu from a scroll."), true);
            }
            return false;
        }
        if (!hasEnoughJutsuXp(stack, WOOD_BUDDHA)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("Senjutsu XP " + getJutsuXp(stack, WOOD_BUDDHA)
                        + "/" + getRequiredXp(stack, WOOD_BUDDHA)), true);
            }
            return false;
        }
        long cooldown = getRemainingCooldownTicks(stack, level, WOOD_BUDDHA);
        if (cooldown > 0L) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("Cooldown " + cooldown / 20L + "s"), true);
            }
            return false;
        }
        double chakraBurn = WOOD_BUDDHA.chakraUsage() * 0.02D * Math.max(getCurrentJutsuXpModifier(stack, player), 0.05F);
        if (Chakra.pathway(player).getAmount() < chakraBurn) {
            Chakra.pathway(player).warningDisplay();
            return false;
        }
        return true;
    }

    private InteractionResultHolder<ItemStack> useSnake8Heads(Level level, Player player, ItemStack stack) {
        if (!canActivateSnake8Heads(level, player, stack)) {
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide) {
            double chakraBurn = SNAKE_8_HEADS.chakraUsage() * 0.02D * Math.max(getCurrentJutsuXpModifier(stack, player), 0.05F);
            Snake8HeadsEntity entity = Snake8HeadsEntity.spawnFrom(player, chakraBurn);
            if (entity == null) {
                return InteractionResultHolder.fail(stack);
            }
            addCurrentJutsuXp(stack, 1);
            setCurrentJutsuCooldown(stack, level, 20L);
        }
        return InteractionResultHolder.consume(stack);
    }

    private boolean canActivateSnake8Heads(Level level, Player player, ItemStack stack) {
        if (player.isCreative()) {
            return true;
        }
        if (!PlayerTracker.isNinja(player)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("You need ninja experience to use this Senjutsu jutsu."), true);
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
        if (!isSageModeActivated(stack)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("Sage Mode must be active to use 8-branch Jutsu."), true);
            }
            return false;
        }
        if (!canUseJutsu(stack, SNAKE_8_HEADS, player)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("You need to learn this Senjutsu jutsu from a scroll."), true);
            }
            return false;
        }
        if (!hasEnoughJutsuXp(stack, SNAKE_8_HEADS)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("Senjutsu XP " + getJutsuXp(stack, SNAKE_8_HEADS)
                        + "/" + getRequiredXp(stack, SNAKE_8_HEADS)), true);
            }
            return false;
        }
        long cooldown = getRemainingCooldownTicks(stack, level, SNAKE_8_HEADS);
        if (cooldown > 0L) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("Cooldown " + cooldown / 20L + "s"), true);
            }
            return false;
        }
        double chakraBurn = SNAKE_8_HEADS.chakraUsage() * 0.02D * Math.max(getCurrentJutsuXpModifier(stack, player), 0.05F);
        if (Chakra.pathway(player).getAmount() < chakraBurn) {
            Chakra.pathway(player).warningDisplay();
            return false;
        }
        return true;
    }

    private static boolean isJutsu(JutsuDefinition first, JutsuDefinition second) {
        return first.index() == second.index() && first.translationKey().equals(second.translationKey());
    }

    private static void applySageModeModifiers(Player player) {
        for (SageModeModifier sageModifier : SAGE_MODE_MODIFIERS) {
            AttributeInstance attribute = player.getAttribute(sageModifier.attribute());
            if (attribute != null && attribute.getModifier(sageModifier.modifier().getId()) == null) {
                attribute.addTransientModifier(sageModifier.modifier());
            }
        }
        player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + 0.1F));
    }

    private static void removeSageModeModifiers(Player player) {
        for (SageModeModifier sageModifier : SAGE_MODE_MODIFIERS) {
            AttributeInstance attribute = player.getAttribute(sageModifier.attribute());
            if (attribute != null && attribute.getModifier(sageModifier.modifier().getId()) != null) {
                attribute.removeModifier(sageModifier.modifier().getId());
            }
        }
    }

    private static ItemStack findActiveSageModeStack(Player player) {
        for (ItemStack stack : ProcedureUtils.getAllItemsOfSubType(player, SenjutsuItem.class)) {
            if (isSageModeActivated(stack)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private record SageModeModifier(Attribute attribute, AttributeModifier modifier) {
    }

    public static SageType getSageType(ItemStack stack) {
        return stack.getTag() == null ? SageType.NONE : SageType.byId(stack.getTag().getInt(SAGE_TYPE_TAG));
    }

    public static void setSageType(ItemStack stack, SageType sageType) {
        stack.getOrCreateTag().putInt(SAGE_TYPE_TAG, sageType.id());
    }

    private static SageType ensureSageType(ItemStack stack, Level level) {
        SageType sageType = getSageType(stack);
        if (sageType == SageType.NONE) {
            sageType = SageType.random(level);
            setSageType(stack, sageType);
        }
        return sageType;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.senjutsu.type")
                .append(Component.translatable(getSageType(stack).translationKey())));
    }

    public enum SageType {
        NONE(0, "none", "textures/sagetoadhelmet.png"),
        TOAD(1, "entity.narutomod.toad", "textures/sagetoadhelmet.png"),
        SNAKE(2, "entity.narutomod.snake", "textures/sagesnakehelmet.png"),
        SLUG(3, "entity.narutomod.slug", "textures/sageslughelmet.png");

        private final int id;
        private final String translationKey;
        private final String texturePath;

        SageType(int id, String translationKey, String texturePath) {
            this.id = id;
            this.translationKey = translationKey;
            this.texturePath = texturePath;
        }

        public int id() {
            return id;
        }

        public String translationKey() {
            return translationKey;
        }

        public String texturePath() {
            return texturePath;
        }

        public static SageType byId(int id) {
            for (SageType sageType : values()) {
                if (sageType.id == id) {
                    return sageType;
                }
            }
            return NONE;
        }

        public static SageType random(Level level) {
            return switch (level.random.nextInt(3)) {
                case 0 -> TOAD;
                case 1 -> SNAKE;
                default -> SLUG;
            };
        }
    }

    @Mod.EventBusSubscriber(modid = NarutomodMod.MODID)
    public static final class ForgeEvents {
        private ForgeEvents() {
        }

        @SubscribeEvent
        public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
                return;
            }
            ItemStack activeStack = findActiveSageModeStack(event.player);
            if (activeStack.isEmpty()) {
                removeSageModeModifiers(event.player);
                if (NarutomodModVariables.get(event.player).getBoolean(SAGE_MODE_ACTIVATED_TAG)) {
                    ProcedureSync.EntityNBTTag.removeAndSync(event.player, SAGE_MODE_ACTIVATED_TAG);
                    ProcedureSync.EntityNBTTag.removeAndSync(event.player, SAGE_TYPE_TAG);
                }
            }
        }
    }
}
