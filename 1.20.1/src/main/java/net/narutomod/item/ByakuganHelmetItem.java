package net.narutomod.item;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemHandlerHelper;
import net.narutomod.NarutomodModVariables;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModItems;

public final class ByakuganHelmetItem extends ArmorItem {
    private static final double INITIAL_BYAKUGAN_COUNT = 1.0D;
    private static final String TENSEIGAN_ACHIEVED_ADVANCEMENT = "narutomod:tenseigan_achieved";
    private static final String TENSEIGAN_LAST_EVOLVE_TICK_TAG = "TenseiganEvolutionLastTick";
    private static final UUID RINNESHARINGAN_HEALTH_UUID = UUID.fromString("a7628cc9-b0d5-4714-b822-0ff3dbdd25d2");
    private static final AttributeModifier RINNESHARINGAN_HEALTH_MODIFIER = new AttributeModifier(
            RINNESHARINGAN_HEALTH_UUID,
            "byakurinnesharingan.maxhealth",
            380.0D,
            AttributeModifier.Operation.ADDITION);

    private static final ArmorMaterial MATERIAL = new ArmorMaterial() {
        @Override
        public int getDurabilityForType(Type type) {
            return 0;
        }

        @Override
        public int getDefenseForType(Type type) {
            return type == Type.HELMET ? 2 : 0;
        }

        @Override
        public int getEnchantmentValue() {
            return 9;
        }

        @Override
        public SoundEvent getEquipSound() {
            return SoundEvents.ARMOR_EQUIP_LEATHER;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.EMPTY;
        }

        @Override
        public String getName() {
            return "narutomod:byakugan";
        }

        @Override
        public float getToughness() {
            return 0.0F;
        }

        @Override
        public float getKnockbackResistance() {
            return 0.0F;
        }
    };

    private final boolean rinnesharinganVariant;

    public ByakuganHelmetItem() {
        this(false);
    }

    public ByakuganHelmetItem(boolean rinnesharinganVariant) {
        super(MATERIAL, Type.HELMET, new Item.Properties().stacksTo(1));
        this.rinnesharinganVariant = rinnesharinganVariant;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (entity instanceof Player player) {
            tickServerByakuganStack(stack, level, player);
        }
    }

    @Override
    public void onArmorTick(ItemStack stack, Level level, Player player) {
        super.onArmorTick(stack, level, player);
        tickServerByakuganStack(stack, level, player);
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        ensureImplicitRinnesharinganTag(stack);
        return stack;
    }

    public static void ensureByakuganCount(ItemStack stack) {
        if (!stack.is(ModItems.BYAKUGANHELMET.get())) {
            return;
        }
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains(TenseiganChakraModeItem.BYAKUGAN_COUNT_TAG)) {
            tag.putDouble(TenseiganChakraModeItem.BYAKUGAN_COUNT_TAG, INITIAL_BYAKUGAN_COUNT);
        }
    }

    public static boolean isByakuganStack(ItemStack stack) {
        return stack.is(ModItems.BYAKUGANHELMET.get()) || stack.is(ModItems.BYAKURINNESHARINGANHELMET.get());
    }

    public static boolean isRinnesharinganStack(ItemStack stack) {
        return stack.is(ModItems.BYAKURINNESHARINGANHELMET.get())
                || stack.getTag() != null && stack.getTag().getBoolean(NarutomodModVariables.RINNESHARINGAN_ACTIVATED);
    }

    public static void ensureImplicitRinnesharinganTag(ItemStack stack) {
        if (stack.is(ModItems.BYAKURINNESHARINGANHELMET.get())) {
            stack.getOrCreateTag().putBoolean(NarutomodModVariables.RINNESHARINGAN_ACTIVATED, true);
        }
    }

    public static boolean finishTenseiganEvolution(ServerPlayer player, ItemStack stack) {
        if (!stack.is(ModItems.BYAKUGANHELMET.get()) || !ProcedureUtils.isOriginalOwner(player, stack)) {
            return false;
        }
        ensureByakuganCount(stack);
        stack.getOrCreateTag().putDouble(NarutomodModVariables.TENSEIGAN_EVOLVED_TIME, 0.0D);
        return evolveToTenseigan(player, stack);
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return isRinnesharinganStack(stack)
                ? "narutomod:textures/byakurinnesharingan_helmet.png"
                : "narutomod:textures/byakuganhelmet.png";
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        Multimap<Attribute, AttributeModifier> base = super.getAttributeModifiers(slot, stack);
        if (slot != EquipmentSlot.HEAD || !isRinnesharinganStack(stack)) {
            return base;
        }
        return ImmutableMultimap.<Attribute, AttributeModifier>builder()
                .putAll(base)
                .put(Attributes.MAX_HEALTH, RINNESHARINGAN_HEALTH_MODIFIER)
                .build();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        boolean rinnesharingan = isRinnesharinganStack(stack);
        if (rinnesharingan) {
            tooltip.add(Component.translatable("advancements.rinnesharinganactivated.title").withStyle(ChatFormatting.RED));
        }
        tooltip.add(Component.translatable("key.mcreator.specialjutsu1")
                .append(": ")
                .append(Component.translatable("tooltip.byakugan.jutsu1"))
                .append(" (L15)"));
        tooltip.add(Component.translatable("key.mcreator.specialjutsu2")
                .append(": ")
                .append(Component.translatable(rinnesharingan
                        ? "tooltip.byakurinnesharingan.jutsu2"
                        : "tooltip.byakugan.jutsu2"))
                .append(rinnesharingan ? "" : " (L20)"));
        tooltip.add(Component.translatable("key.mcreator.specialjutsu3")
                .append(": ")
                .append(Component.translatable("entity.hakkeshokeiten.name"))
                .append(" (L30)"));
        if (stack.getTag() != null && stack.getTag().contains(NarutomodModVariables.TENSEIGAN_EVOLVED_TIME)) {
            double ticks = stack.getTag().getDouble(NarutomodModVariables.TENSEIGAN_EVOLVED_TIME);
            if (ticks > 0.0D) {
                tooltip.add(Component.translatable("tooltip.byakugan.tenseigantime")
                        .append(String.valueOf((long)(ticks / 20.0D)))
                        .withStyle(ChatFormatting.AQUA));
            }
        }
    }

    private static void tickServerByakuganStack(ItemStack stack, Level level, Player player) {
        if (level.isClientSide) {
            return;
        }
        ensureImplicitRinnesharinganTag(stack);
        if (player.isCreative() && ProcedureUtils.getOwnerId(stack) == null) {
            ProcedureUtils.setOriginalOwner(player, stack);
        }
        if (stack.is(ModItems.BYAKUGANHELMET.get()) && ProcedureUtils.getOwnerId(stack) != null) {
            ensureByakuganCount(stack);
        }
        if (stack.is(ModItems.BYAKUGANHELMET.get())
                && player instanceof ServerPlayer serverPlayer
                && ProcedureUtils.isOriginalOwner(player, stack)) {
            tickTenseiganEvolution(serverPlayer, stack, level);
        }
    }

    private static void tickTenseiganEvolution(ServerPlayer player, ItemStack stack, Level level) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(NarutomodModVariables.TENSEIGAN_EVOLVED_TIME) || player.tickCount % 20 != 0) {
            return;
        }
        long gameTime = level.getGameTime();
        if (tag.contains(TENSEIGAN_LAST_EVOLVE_TICK_TAG) && tag.getLong(TENSEIGAN_LAST_EVOLVE_TICK_TAG) == gameTime) {
            return;
        }
        tag.putLong(TENSEIGAN_LAST_EVOLVE_TICK_TAG, gameTime);

        double remaining = tag.getDouble(NarutomodModVariables.TENSEIGAN_EVOLVED_TIME) - 20.0D;
        if (remaining > 0.0D) {
            tag.putDouble(NarutomodModVariables.TENSEIGAN_EVOLVED_TIME, remaining);
            return;
        }
        tag.putDouble(NarutomodModVariables.TENSEIGAN_EVOLVED_TIME, 0.0D);
        evolveToTenseigan(player, stack);
    }

    private static boolean evolveToTenseigan(ServerPlayer player, ItemStack stack) {
        ItemStack currentSnapshot = stack.copy();
        ItemStack restoredByakugan = stack.copyWithCount(1);
        stripEvolutionTags(restoredByakugan);

        ItemStack tenseigan = new ItemStack(ModItems.TENSEIGANHELMET.get());
        ProcedureUtils.setOriginalOwner(player, tenseigan);
        CompoundTag sourceTag = stack.getTag();
        if (sourceTag != null && sourceTag.contains(TenseiganChakraModeItem.BYAKUGAN_COUNT_TAG)) {
            tenseigan.getOrCreateTag().putDouble(
                    TenseiganChakraModeItem.BYAKUGAN_COUNT_TAG,
                    sourceTag.getDouble(TenseiganChakraModeItem.BYAKUGAN_COUNT_TAG));
        }

        if (!replacePlayerStack(player, stack, currentSnapshot, tenseigan)) {
            return false;
        }
        ItemHandlerHelper.giveItemToPlayer(player, restoredByakugan);
        ProcedureUtils.grantAdvancement(player, TENSEIGAN_ACHIEVED_ADVANCEMENT, true);
        return true;
    }

    private static void stripEvolutionTags(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return;
        }
        tag.remove(TenseiganChakraModeItem.BYAKUGAN_COUNT_TAG);
        tag.remove(NarutomodModVariables.TENSEIGAN_EVOLVED_TIME);
        tag.remove(TENSEIGAN_LAST_EVOLVE_TICK_TAG);
    }

    private static boolean replacePlayerStack(ServerPlayer player, ItemStack currentStack, ItemStack expected, ItemStack replacement) {
        if (player.getItemBySlot(EquipmentSlot.HEAD) == currentStack) {
            player.setItemSlot(EquipmentSlot.HEAD, replacement.copy());
            return true;
        }
        for (int index = 0; index < player.getInventory().items.size(); index++) {
            if (player.getInventory().items.get(index) == currentStack) {
                player.getInventory().items.set(index, replacement.copy());
                return true;
            }
        }
        for (int index = 0; index < player.getInventory().armor.size(); index++) {
            if (player.getInventory().armor.get(index) == currentStack) {
                player.getInventory().armor.set(index, replacement.copy());
                return true;
            }
        }
        for (int index = 0; index < player.getInventory().offhand.size(); index++) {
            if (player.getInventory().offhand.get(index) == currentStack) {
                player.getInventory().offhand.set(index, replacement.copy());
                return true;
            }
        }

        if (ItemStack.isSameItemSameTags(player.getItemBySlot(EquipmentSlot.HEAD), expected)) {
            player.setItemSlot(EquipmentSlot.HEAD, replacement.copy());
            return true;
        }
        for (int index = 0; index < player.getInventory().items.size(); index++) {
            if (ItemStack.isSameItemSameTags(player.getInventory().items.get(index), expected)) {
                player.getInventory().items.set(index, replacement.copy());
                return true;
            }
        }
        for (int index = 0; index < player.getInventory().armor.size(); index++) {
            if (ItemStack.isSameItemSameTags(player.getInventory().armor.get(index), expected)) {
                player.getInventory().armor.set(index, replacement.copy());
                return true;
            }
        }
        for (int index = 0; index < player.getInventory().offhand.size(); index++) {
            if (ItemStack.isSameItemSameTags(player.getInventory().offhand.get(index), expected)) {
                player.getInventory().offhand.set(index, replacement.copy());
                return true;
            }
        }
        return false;
    }
}
