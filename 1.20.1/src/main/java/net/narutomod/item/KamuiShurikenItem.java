package net.narutomod.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.narutomod.Chakra;
import net.narutomod.entity.KamuiShurikenEntity;
import net.narutomod.entity.SusanooWingedEntity;
import net.narutomod.registry.ModEntityTypes;

public final class KamuiShurikenItem extends Item {
    public static final double CHAKRA_USAGE = 500.0D;
    private static final float PROJECTILE_SPEED = 1.0F;
    private static final String SUMMONED_SUSANOO_ID_TAG = "summonedSusanooID";

    public KamuiShurikenItem() {
        super(new Item.Properties().stacksTo(1));
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
        KamuiShurikenEntity projectile = ModEntityTypes.ENTITYBULLETKAMUISHURIKEN.get().create(level);
        if (projectile == null) {
            return;
        }
        projectile.configure(player, susanooScale(player));
        Vec3 look = player.getLookAngle();
        projectile.moveTo(player.getX(), player.getEyeY() - 0.1D, player.getZ(), player.getYRot(), player.getXRot());
        projectile.shoot(look.x(), look.y(), look.z(), PROJECTILE_SPEED, 0.0F);
        level.addFreshEntity(projectile);
        Chakra.pathway(player).consume(CHAKRA_USAGE);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (level.isClientSide
                || !(entity instanceof Player player)
                || player.isCreative()
                || hasSummonedWingedSusanoo(player)) {
            return;
        }
        stack.shrink(1);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    private static float susanooScale(Player player) {
        Entity vehicle = player.getVehicle();
        if (vehicle != null && vehicle.getType() == ModEntityTypes.SUSANOOWINGED.get()) {
            return (float)Math.max(vehicle.getPersistentData().getDouble("entityModelScale"), 1.0D);
        }
        return 1.0F;
    }

    private static boolean hasSummonedWingedSusanoo(Player player) {
        int susanooId = player.getPersistentData().getInt(SUMMONED_SUSANOO_ID_TAG);
        if (susanooId > 0
                && player.level().getEntity(susanooId) instanceof SusanooWingedEntity susanoo
                && susanoo.isOwnedBy(player)) {
            return true;
        }
        return player.getVehicle() instanceof SusanooWingedEntity susanoo && susanoo.isOwnedBy(player);
    }
}
