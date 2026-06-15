package net.narutomod.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.narutomod.Chakra;
import net.narutomod.entity.TenseiBakuGoldEntity;
import net.narutomod.entity.TenseiBakuSilverEntity;
import net.narutomod.entity.TenseiganOrbEntity;
import net.narutomod.particle.NarutoParticleKind;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModEffects;
import net.narutomod.registry.ModItems;
import net.narutomod.registry.ModParticleTypes;
import net.narutomod.registry.ModSounds;

public final class TenseiganChakraModeItem extends JutsuItem {
    public static final JutsuDefinition CHAKRA_ORBS = JutsuDefinition.ranked(0, "entity.narutomod.tenseigangun", 'S', 10.0D);
    public static final JutsuDefinition SILVER_BLAST = JutsuDefinition.ranked(1, "entity.narutomod.tensei_baku_silver", 'S', 50.0D)
            .withPower(10.0F, 20.0F);
    public static final JutsuDefinition GOLD_BLAST = JutsuDefinition.ranked(2, "entity.narutomod.tensei_baku_gold", 'S', 50.0D)
            .withPower(10.0F, 5.0F);
    private static final JutsuDefinition[] TENSEIGAN_JUTSUS = {
            CHAKRA_ORBS,
            SILVER_BLAST,
            GOLD_BLAST
    };
    public static final String BYAKUGAN_COUNT_TAG = "ByakuganCount";
    public static final String CHEST_ARMOR_DAMAGE_TAG = "ChestArmorDamage";
    public static final String LEG_ARMOR_DAMAGE_TAG = "LegArmorDamage";
    private static final float SILVER_BLAST_MAX_POWER = 20.0F;
    private static final float GOLD_BLAST_MAX_POWER = 50.0F;

    public TenseiganChakraModeItem() {
        super(JutsuType.TENSEIGAN, true, TENSEIGAN_JUTSUS);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.TENSEIGANHELMET.get())) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("Tenseigan Chakra Mode requires Tenseigan in the head slot."), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        JutsuDefinition current = getCurrentJutsu(stack);
        if (isChakraOrbs(current) && level.isClientSide) {
            return InteractionResultHolder.consume(stack);
        }
        if (isSilverBlast(current) || isGoldBlast(current)) {
            if (!canActivateTenseigan(level, player, stack, current)) {
                return InteractionResultHolder.fail(stack);
            }
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        if (!level.isClientSide) {
            setOwnerIfMissing(stack, player);
            if (isChakraOrbs(current)) {
                return activateChakraOrbs(level, player, stack)
                        ? InteractionResultHolder.consume(stack)
                        : InteractionResultHolder.fail(stack);
            }
            player.displayClientMessage(Component.translatable(current.translationKey())
                    .append(Component.literal(" runtime is not ported yet.")), true);
        }
        return InteractionResultHolder.fail(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        if (!(livingEntity instanceof Player player) || (!isSilverBlast(getCurrentJutsu(stack)) && !isGoldBlast(getCurrentJutsu(stack)))) {
            return;
        }
        int usedTicks = getUseDuration(stack) - remainingUseDuration;
        if (level instanceof ServerLevel serverLevel && usedTicks % 5 == 0) {
            serverLevel.sendParticles(
                    ModParticleTypes.options(NarutoParticleKind.SMOKE_COLORED, 0x106AD1FF, 5, 40, 0xF0, -1, 4),
                    player.getX(),
                    player.getY() + 1.0D,
                    player.getZ(),
                    4,
                    0.2D,
                    0.0D,
                    0.2D,
                    0.05D);
        }
        if (!level.isClientSide && usedTicks % 10 == 0) {
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
        if (isSilverBlast(current)) {
            activateSilverBlast(level, player, stack, remainingUseDuration);
        } else if (isGoldBlast(current)) {
            activateGoldBlast(level, player, stack, remainingUseDuration);
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
        if (level.isClientSide || !(entity instanceof Player player)) {
            return;
        }
        if (player.isCreative() || player.getCooldowns().isOnCooldown(this)) {
            return;
        }
        ItemStack tenseigan = ProcedureUtils.getMatchingItemStack(player, ModItems.TENSEIGANHELMET.get());
        if (tenseigan == null) {
            removeChakraArmorAndItem(stack, player);
            return;
        }
        if (selected) {
            tickSelected(stack, level, player);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        JutsuDefinition current = getCurrentJutsu(stack);
        tooltip.add(Component.literal("Requires Tenseigan").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable(current.translationKey())
                .append(Component.literal(" [" + current.rank() + "]"))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(CHEST_ARMOR_DAMAGE_TAG + "=" + stack.getOrCreateTag().getInt(CHEST_ARMOR_DAMAGE_TAG)
                + ", " + LEG_ARMOR_DAMAGE_TAG + "=" + stack.getOrCreateTag().getInt(LEG_ARMOR_DAMAGE_TAG))
                .withStyle(ChatFormatting.DARK_GRAY));
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

    public boolean isOnCooldown(Player player) {
        return player.getCooldowns().isOnCooldown(this);
    }

    public static boolean canUseChakraMode(ItemStack tenseiganHelmet) {
        return !tenseiganHelmet.isEmpty()
                && tenseiganHelmet.is(ModItems.TENSEIGANHELMET.get())
                && tenseiganHelmet.getTag() != null
                && tenseiganHelmet.getTag().getDouble(BYAKUGAN_COUNT_TAG) >= 5.0D;
    }

    private boolean activateChakraOrbs(Level level, Player player, ItemStack stack) {
        if (!canActivateTenseigan(level, player, stack, CHAKRA_ORBS)) {
            return false;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(CHAKRA_ORBS.chakraUsage())) {
            Chakra.pathway(player).warningDisplay();
            return false;
        }
        if (TenseiganOrbEntity.spawnFrom(player)) {
            addCurrentJutsuXp(stack, 1);
            return true;
        }
        player.displayClientMessage(Component.literal("Localised Rebirth Blast could not be created."), true);
        return false;
    }

    private void activateSilverBlast(Level level, Player player, ItemStack stack, int remainingUseDuration) {
        if (!canActivateTenseigan(level, player, stack, SILVER_BLAST)) {
            return;
        }
        float power = getTenseiganPower(stack, player, SILVER_BLAST, remainingUseDuration);
        if (power < SILVER_BLAST.basePower()) {
            player.displayClientMessage(Component.literal(String.format("Silver Wheel power %.2f/%.2f", power, SILVER_BLAST.basePower())), true);
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(SILVER_BLAST.chakraUsage() * power)) {
            Chakra.pathway(player).warningDisplay();
            return;
        }
        if (TenseiBakuSilverEntity.spawnFrom(player, power)) {
            addCurrentJutsuXp(stack, 1);
            return;
        }
        player.displayClientMessage(Component.literal("Silver Wheel Rebirth Blast could not be created."), true);
    }

    private void activateGoldBlast(Level level, Player player, ItemStack stack, int remainingUseDuration) {
        if (!canActivateTenseigan(level, player, stack, GOLD_BLAST)) {
            return;
        }
        float power = getTenseiganPower(stack, player, GOLD_BLAST, remainingUseDuration);
        if (power < GOLD_BLAST.basePower()) {
            player.displayClientMessage(Component.literal(String.format("Golden Wheel power %.2f/%.2f", power, GOLD_BLAST.basePower())), true);
            return;
        }
        if (!player.isCreative() && !Chakra.pathway(player).consume(GOLD_BLAST.chakraUsage() * power)) {
            Chakra.pathway(player).warningDisplay();
            return;
        }
        if (TenseiBakuGoldEntity.spawnFrom(player, power)) {
            addCurrentJutsuXp(stack, 1);
            return;
        }
        player.displayClientMessage(Component.literal("Golden Wheel Rebirth Blast could not be created."), true);
    }

    private boolean canActivateTenseigan(Level level, Player player, ItemStack stack, JutsuDefinition definition) {
        if (!player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.TENSEIGANHELMET.get())) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("Tenseigan Chakra Mode requires Tenseigan in the head slot."), true);
            }
            return false;
        }
        if (!player.isCreative() && !isOwnedByOrUnbound(player, stack)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("This Tenseigan Chakra Mode item belongs to another player."), true);
            }
            return false;
        }
        if (!level.isClientSide) {
            setOwnerIfMissing(stack, player);
        }
        if (!player.isCreative() && !isJutsuEnabled(stack, definition)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("This Tenseigan jutsu is not enabled."), true);
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

    private boolean isChakraOrbs(JutsuDefinition definition) {
        return definition.index() == CHAKRA_ORBS.index() && CHAKRA_ORBS.translationKey().equals(definition.translationKey());
    }

    private float getTenseiganPower(ItemStack stack, Player player, JutsuDefinition definition, int remainingUseDuration) {
        float power = getChargingPower(stack, player, remainingUseDuration, definition.basePower(), definition.powerUpDelay());
        float maxAllowed = isGoldBlast(definition) ? GOLD_BLAST_MAX_POWER : SILVER_BLAST_MAX_POWER;
        if (player.isCreative()) {
            return Math.min(Math.max(power, definition.basePower()), maxAllowed);
        }
        float maxPower = (float)(Chakra.pathway(player).getAmount() / Math.max(definition.chakraUsage(), 1.0D) * 0.9999D);
        return Math.min(Math.max(power, definition.basePower()), Math.min(maxPower, maxAllowed));
    }

    private boolean isSilverBlast(JutsuDefinition definition) {
        return definition.index() == SILVER_BLAST.index() && SILVER_BLAST.translationKey().equals(definition.translationKey());
    }

    private boolean isGoldBlast(JutsuDefinition definition) {
        return definition.index() == GOLD_BLAST.index() && GOLD_BLAST.translationKey().equals(definition.translationKey());
    }

    private void tickSelected(ItemStack stack, Level level, Player player) {
        player.addEffect(new MobEffectInstance(ModEffects.FLIGHT.get(), 2, 0, false, false));
        if (!player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.TENSEIGANHELMET.get())) {
            ProcedureUtils.swapItemToSlot(player, EquipmentSlot.HEAD, new ItemStack(ModItems.TENSEIGANHELMET.get()));
        }
        ensureArmorSlot(stack, level, player, EquipmentSlot.CHEST, ModItems.TENSEIGANBODY.get(), CHEST_ARMOR_DAMAGE_TAG);
        ensureArmorSlot(stack, level, player, EquipmentSlot.LEGS, ModItems.TENSEIGANLEGS.get(), LEG_ARMOR_DAMAGE_TAG);
        breakChakraArmorIfExhausted(stack, player);
    }

    private void ensureArmorSlot(ItemStack stack, Level level, Player player, EquipmentSlot slot, net.minecraft.world.item.Item item, String damageTag) {
        ItemStack equipped = player.getItemBySlot(slot);
        if (!equipped.is(item)) {
            ItemStack replacement = new ItemStack(item);
            replacement.setDamageValue(Math.min(stack.getOrCreateTag().getInt(damageTag), replacement.getMaxDamage()));
            boolean hadExisting = ProcedureUtils.getMatchingItemStack(player, item) != null;
            ProcedureUtils.swapItemToSlot(player, slot, replacement);
            if (!hadExisting) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        ModSounds.SOUND_CHARGING_CHAKRA.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        } else if (equipped.getDamageValue() != stack.getOrCreateTag().getInt(damageTag)) {
            stack.getOrCreateTag().putInt(damageTag, equipped.getDamageValue());
        }
    }

    private void breakChakraArmorIfExhausted(ItemStack stack, Player player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack legs = player.getItemBySlot(EquipmentSlot.LEGS);
        boolean chestBroken = chest.is(ModItems.TENSEIGANBODY.get()) && chest.getMaxDamage() > 0 && chest.getDamageValue() >= chest.getMaxDamage();
        boolean legsBroken = legs.is(ModItems.TENSEIGANLEGS.get()) && legs.getMaxDamage() > 0 && legs.getDamageValue() >= legs.getMaxDamage();
        if (!chestBroken && !legsBroken) {
            return;
        }
        player.getCooldowns().addCooldown(this, 2400);
        if (chest.is(ModItems.TENSEIGANBODY.get())) {
            chest.shrink(1);
        }
        if (legs.is(ModItems.TENSEIGANLEGS.get())) {
            legs.shrink(1);
        }
        stack.getOrCreateTag().putInt(CHEST_ARMOR_DAMAGE_TAG, 0);
        stack.getOrCreateTag().putInt(LEG_ARMOR_DAMAGE_TAG, 0);
    }

    private static void removeChakraArmorAndItem(ItemStack stack, Player player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack legs = player.getItemBySlot(EquipmentSlot.LEGS);
        if (chest.is(ModItems.TENSEIGANBODY.get())) {
            chest.shrink(1);
        }
        if (legs.is(ModItems.TENSEIGANLEGS.get())) {
            legs.shrink(1);
        }
        stack.shrink(1);
        player.displayClientMessage(Component.literal("Tenseigan Chakra Mode faded without Tenseigan."), true);
    }
}
