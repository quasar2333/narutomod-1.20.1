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
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.narutomod.entity.NuibariSwordEntity;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModItems;

public final class NuibariSwordItem extends Item implements ItemOnBody.Interface {
    private static final UUID NUIBARI_ATTACK_DAMAGE_UUID = UUID.fromString("4fb85741-b012-4cff-bc4b-4f8749d5e5b0");
    private static final UUID NUIBARI_ATTACK_SPEED_UUID = UUID.fromString("d66106a1-a30a-45bf-8d45-fdd4614a1cc0");
    private static final double PROJECTILE_DAMAGE = 16.0D;

    private final Multimap<Attribute, AttributeModifier> mainHandModifiers;

    public NuibariSwordItem() {
        super(new Item.Properties().stacksTo(1));
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                NUIBARI_ATTACK_DAMAGE_UUID,
                "Nuibari sword damage",
                8.0D,
                AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(
                NUIBARI_ATTACK_SPEED_UUID,
                "Nuibari sword speed",
                -2.0D,
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
        NuibariSwordEntity projectile = ModEntityTypes.ENTITYBULLETNUIBARI_SWORD.get().create(level);
        if (projectile == null) {
            return;
        }
        float power = BowItem.getPowerForTime(getUseDuration(stack) - remainingUseDuration);
        projectile.configure(player, PROJECTILE_DAMAGE);
        Vec3 look = player.getLookAngle();
        projectile.moveTo(player.getX(), player.getEyeY() - 0.1D, player.getZ(), player.getYRot(), player.getXRot());
        projectile.shoot(look.x(), look.y(), look.z(), power * 2.0F, 0.0F);
        level.addFreshEntity(projectile);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ARROW_SHOOT, SoundSource.NEUTRAL, 1.0F,
                1.0F / (level.random.nextFloat() * 0.5F + 1.0F) + power);

        ItemStack thrown = new ItemStack(ModItems.NUIBARI_THROWN.get());
        NuibariThrownItem.bindEntity(thrown, projectile);
        player.setItemInHand(player.getUsedItemHand(), thrown);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.nuibari.general").withStyle(ChatFormatting.GRAY));
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
