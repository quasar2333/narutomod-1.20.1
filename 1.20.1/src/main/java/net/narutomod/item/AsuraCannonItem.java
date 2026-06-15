package net.narutomod.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.narutomod.entity.AsuraCannonballEntity;
import net.narutomod.registry.ModEntityTypes;

public final class AsuraCannonItem extends Item {
    private static final float PROJECTILE_SPEED = 2.0F;

    public AsuraCannonItem() {
        super(new Item.Properties().durability(100));
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
        AsuraCannonballEntity cannonball = ModEntityTypes.ENTITYBULLETASURACANON.get().create(level);
        if (cannonball == null) {
            return;
        }
        int usedTicks = getUseDuration(stack) - remainingUseDuration;
        float explosivePower = usedTicks / 15.0F + 1.0F;
        cannonball.configure(player, explosivePower);
        Vec3 look = player.getLookAngle();
        cannonball.moveTo(player.getX(), player.getEyeY() - 0.4D, player.getZ(), player.getYRot(), player.getXRot());
        cannonball.shoot(look.x(), look.y(), look.z(), PROJECTILE_SPEED, 0.0F);
        level.addFreshEntity(cannonball);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLAZE_SHOOT, SoundSource.NEUTRAL, 1.0F,
                1.0F / (level.random.nextFloat() * 0.5F + 1.0F) + 0.5F);
        if (!player.getAbilities().instabuild && stack.isDamageableItem()) {
            stack.hurtAndBreak(1, player, owner -> owner.broadcastBreakEvent(player.getUsedItemHand()));
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
}
