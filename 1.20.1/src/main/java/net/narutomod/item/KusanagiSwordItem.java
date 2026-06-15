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
import net.narutomod.entity.KusanagiSwordEntity;
import net.narutomod.registry.ModEntityTypes;

public final class KusanagiSwordItem extends Item implements ItemOnBody.Interface {
    private static final UUID KUSANAGI_ATTACK_DAMAGE_UUID = UUID.fromString("2a4ff668-073e-4b31-bd49-10856db1d9bc");
    private static final UUID KUSANAGI_ATTACK_SPEED_UUID = UUID.fromString("0791a98a-6388-4d5a-8ac6-9c366be8fd53");

    private final Multimap<Attribute, AttributeModifier> mainHandModifiers;

    public KusanagiSwordItem() {
        super(new Item.Properties().stacksTo(1));
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                KUSANAGI_ATTACK_DAMAGE_UUID,
                "Kusanagi sword damage",
                19.0D,
                AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(
                KUSANAGI_ATTACK_SPEED_UUID,
                "Kusanagi sword speed",
                -2.4D,
                AttributeModifier.Operation.ADDITION));
        this.mainHandModifiers = builder.build();
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
        KusanagiSwordEntity sword = ModEntityTypes.ENTITYBULLETKUSANAGI_SWORD.get().create(level);
        if (sword == null) {
            return;
        }
        sword.configure(player);
        level.addFreshEntity(sword);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ARROW_SHOOT, SoundSource.NEUTRAL, 1.0F,
                1.0F / (level.random.nextFloat() * 0.5F + 1.0F) + 0.5F);
        stack.shrink(1);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.hasTag() && stack.getOrCreateTag().getBoolean("inAir");
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.kusanagi.description").withStyle(ChatFormatting.GRAY));
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
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        if (slot == EquipmentSlot.MAINHAND) {
            return this.mainHandModifiers;
        }
        return super.getDefaultAttributeModifiers(slot);
    }
}
