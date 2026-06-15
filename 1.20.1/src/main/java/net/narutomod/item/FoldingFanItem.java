package net.narutomod.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.util.UUID;
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
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.narutomod.entity.FoldingFanProjectileEntity;
import net.narutomod.entity.FutonGreatBreakthroughEntity;
import net.narutomod.registry.ModEntityTypes;

public final class FoldingFanItem extends Item {
    private static final UUID FOLDING_FAN_ATTACK_DAMAGE_UUID = UUID.fromString("15d37d37-d097-4516-b3b8-398b3b5ce801");
    private static final UUID FOLDING_FAN_ATTACK_SPEED_UUID = UUID.fromString("ef16f461-6f4f-40f5-b38c-c5ea18dc3f22");
    private static final float PROJECTILE_SPEED = 2.0F;
    private static final float WIND_POWER_PER_TICK = 0.5F;
    private static final float MAX_WIND_POWER = 50.0F;

    private final Multimap<Attribute, AttributeModifier> mainHandModifiers;

    public FoldingFanItem() {
        super(new Item.Properties().durability(500));
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                FOLDING_FAN_ATTACK_DAMAGE_UUID,
                "Folding fan damage",
                4.0D,
                AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(
                FOLDING_FAN_ATTACK_SPEED_UUID,
                "Folding fan speed",
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
        FoldingFanProjectileEntity projectile = ModEntityTypes.ENTITYBULLETFOLDING_FAN.get().create(level);
        if (projectile != null) {
            projectile.configure(player);
            Vec3 look = player.getLookAngle();
            projectile.moveTo(
                    player.getX() + look.x() * 0.25D,
                    player.getEyeY() - 0.1D,
                    player.getZ() + look.z() * 0.25D,
                    player.getYRot(),
                    player.getXRot());
            projectile.shoot(look.x(), look.y(), look.z(), PROJECTILE_SPEED, 0.0F);
            level.addFreshEntity(projectile);
        }
        player.clearFire();
        int usedTicks = getUseDuration(stack) - remainingUseDuration;
        float windPower = Math.min(Math.max(usedTicks * WIND_POWER_PER_TICK, 1.0F), MAX_WIND_POWER);
        FutonGreatBreakthroughEntity.spawnFrom(player, windPower);
        if (!player.getAbilities().instabuild && stack.isDamageableItem()) {
            stack.hurtAndBreak(2, player, owner -> owner.broadcastBreakEvent(player.getUsedItemHand()));
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
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        if (slot == EquipmentSlot.MAINHAND) {
            return this.mainHandModifiers;
        }
        return super.getDefaultAttributeModifiers(slot);
    }
}
