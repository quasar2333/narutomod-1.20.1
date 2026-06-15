package net.narutomod.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.util.UUID;
import java.util.function.Supplier;
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
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.narutomod.entity.ThrownNinjaToolEntity;

public final class NinjaToolItem extends Item implements ItemOnBody.Interface {
    private static final UUID NINJA_TOOL_ATTACK_DAMAGE_UUID = UUID.fromString("8a6f978f-d87b-4fb8-b91f-b055535453f5");
    private static final UUID NINJA_TOOL_ATTACK_SPEED_UUID = UUID.fromString("d5ac5627-d343-4d1f-8ea6-22ae1260e7b6");

    private final Supplier<? extends net.minecraft.world.entity.EntityType<ThrownNinjaToolEntity>> projectileType;
    private final boolean chargeVelocity;
    private final float velocity;
    private final boolean canDropOnBlock;
    private final boolean consumesOnThrow;
    private final SoundEventsRef throwSound;
    private final Multimap<Attribute, AttributeModifier> mainHandModifiers;
    private final ItemOnBody.BodyPart bodyPart;

    public NinjaToolItem(
            int stackSize,
            Supplier<? extends net.minecraft.world.entity.EntityType<ThrownNinjaToolEntity>> projectileType,
            boolean chargeVelocity,
            float velocity,
            boolean canDropOnBlock,
            boolean consumesOnThrow,
            SoundEventsRef throwSound,
            double meleeDamage) {
        this(stackSize, projectileType, chargeVelocity, velocity, canDropOnBlock, consumesOnThrow, throwSound, meleeDamage, ItemOnBody.BodyPart.NONE);
    }

    public NinjaToolItem(
            int stackSize,
            Supplier<? extends net.minecraft.world.entity.EntityType<ThrownNinjaToolEntity>> projectileType,
            boolean chargeVelocity,
            float velocity,
            boolean canDropOnBlock,
            boolean consumesOnThrow,
            SoundEventsRef throwSound,
            double meleeDamage,
            ItemOnBody.BodyPart bodyPart) {
        super(new Item.Properties().stacksTo(stackSize));
        this.projectileType = projectileType;
        this.chargeVelocity = chargeVelocity;
        this.velocity = velocity;
        this.canDropOnBlock = canDropOnBlock;
        this.consumesOnThrow = consumesOnThrow;
        this.throwSound = throwSound;
        this.bodyPart = bodyPart;
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        if (meleeDamage > 0.0D) {
            builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                    NINJA_TOOL_ATTACK_DAMAGE_UUID,
                    "Ninja tool damage",
                    meleeDamage,
                    AttributeModifier.Operation.ADDITION));
            builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(
                    NINJA_TOOL_ATTACK_SPEED_UUID,
                    "Ninja tool speed",
                    -2.4D,
                    AttributeModifier.Operation.ADDITION));
        }
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
        ThrownNinjaToolEntity projectile = this.projectileType.get().create(level);
        if (projectile == null) {
            return;
        }
        boolean infinite = player.getAbilities().instabuild
                || EnchantmentHelper.getItemEnchantmentLevel(Enchantments.INFINITY_ARROWS, stack) > 0;
        projectile.configure(player, this, this.canDropOnBlock && !infinite);
        Vec3 look = player.getLookAngle();
        projectile.moveTo(
                player.getX() + look.x() * 0.25D,
                player.getEyeY() - 0.1D,
                player.getZ() + look.z() * 0.25D,
                player.getYRot(),
                player.getXRot());
        float power = this.chargeVelocity ? BowItem.getPowerForTime(getUseDuration(stack) - remainingUseDuration) : 1.0F;
        projectile.shoot(look.x(), look.y(), look.z(), Math.max(this.velocity * power, 0.1F), 0.0F);
        level.addFreshEntity(projectile);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), this.throwSound.sound(),
                SoundSource.NEUTRAL, 1.0F, 1.0F / (level.random.nextFloat() * 0.5F + 1.0F) + power / 2.0F);
        if (this.consumesOnThrow && !infinite) {
            stack.shrink(1);
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
        if (slot == EquipmentSlot.MAINHAND && !this.mainHandModifiers.isEmpty()) {
            return this.mainHandModifiers;
        }
        return super.getDefaultAttributeModifiers(slot);
    }

    @Override
    public ItemOnBody.BodyPart showOnBody(ItemStack stack) {
        return this.bodyPart;
    }

    public enum SoundEventsRef {
        ARROW_SHOOT,
        SNOWBALL_THROW;

        private net.minecraft.sounds.SoundEvent sound() {
            return this == SNOWBALL_THROW ? SoundEvents.SNOWBALL_THROW : SoundEvents.ARROW_SHOOT;
        }
    }
}
