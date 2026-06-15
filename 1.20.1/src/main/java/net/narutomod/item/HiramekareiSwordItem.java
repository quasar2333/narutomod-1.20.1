package net.narutomod.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.narutomod.Chakra;
import net.narutomod.entity.HiramekareiEffectEntity;

public final class HiramekareiSwordItem extends Item implements ItemOnBody.Interface {
    public static final int COOLDOWN_TICKS = 500;
    private static final double DEFAULT_ATTACK_DAMAGE = 6.0D;
    private static final UUID ATTACK_DAMAGE_UUID = UUID.fromString("1d708a54-aa18-48d1-911f-76d177f12519");
    private static final UUID ATTACK_SPEED_UUID = UUID.fromString("05d590fc-8891-488b-8d18-dfcf7a4f57f0");

    private final Multimap<Attribute, AttributeModifier> inactiveMainHandModifiers;

    public HiramekareiSwordItem() {
        super(new Item.Properties().stacksTo(1));
        this.inactiveMainHandModifiers = buildMainHandModifiers(DEFAULT_ATTACK_DAMAGE);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int remainingUseDuration) {
        if (!(livingEntity instanceof Player player) || level.isClientSide) {
            return;
        }
        double strength = Chakra.getLevel(player) * 0.5D;
        if (HiramekareiEffectEntity.spawnFor(player, stack, strength)) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.7F,
                    0.8F + level.random.nextFloat() * 0.4F);
            if (!player.isCreative()) {
                player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
            }
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (!level.isClientSide) {
            HiramekareiEffectEntity.clearIfInactive(level, stack);
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return HiramekareiEffectEntity.hasActiveTag(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.hiramekarei.general").withStyle(ChatFormatting.GRAY));
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
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (slot == EquipmentSlot.MAINHAND) {
            if (HiramekareiEffectEntity.hasActiveTag(stack)) {
                return buildMainHandModifiers(HiramekareiEffectEntity.getActiveStrength(stack));
            }
            return this.inactiveMainHandModifiers;
        }
        return super.getAttributeModifiers(slot, stack);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        if (slot == EquipmentSlot.MAINHAND) {
            return this.inactiveMainHandModifiers;
        }
        return super.getDefaultAttributeModifiers(slot);
    }

    private static Multimap<Attribute, AttributeModifier> buildMainHandModifiers(double attackDamage) {
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                ATTACK_DAMAGE_UUID,
                "Hiramekarei sword damage",
                attackDamage,
                AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(
                ATTACK_SPEED_UUID,
                "Hiramekarei sword speed",
                -2.4D,
                AttributeModifier.Operation.ADDITION));
        return builder.build();
    }
}
