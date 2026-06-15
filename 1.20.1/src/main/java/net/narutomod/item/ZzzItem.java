package net.narutomod.item;

import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.narutomod.entity.AbstractSummonAnimalEntity;
import net.narutomod.entity.GamabuntaEntity;
import net.narutomod.entity.SlugSummonEntity;
import net.narutomod.entity.ToadSummonEntity;
import net.narutomod.procedure.ProcedureUtils;

public final class ZzzItem extends Item {
    public static final String ATTACKER_ID_TAG = "attackerID";

    public ZzzItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.clearFire();
        if (!level.isClientSide) {
            handleTargetSelection(level, player, stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!context.getLevel().isClientSide && context.getPlayer() != null) {
            handleSummonNavigation(context.getLevel(), context.getPlayer(), context.getClickedPos());
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return 1.0F;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.literal("attackerID=" + getAttackerId(stack)).withStyle(ChatFormatting.GRAY));
    }

    public static int getAttackerId(ItemStack stack) {
        return stack.getTag() != null ? (int) stack.getTag().getDouble(ATTACKER_ID_TAG) : -1;
    }

    public static void clearAttacker(ItemStack stack) {
        stack.getOrCreateTag().putDouble(ATTACKER_ID_TAG, -1.0D);
    }

    private static void handleSummonNavigation(Level level, Player player, BlockPos clickedPos) {
        BlockPos target = clickedPos.above();
        Vec3 center = Vec3.atCenterOf(clickedPos);
        List<AbstractSummonAnimalEntity> summons = level.getEntitiesOfClass(AbstractSummonAnimalEntity.class,
                new AABB(clickedPos).inflate(32.0D),
                entity -> entity instanceof ToadSummonEntity
                        || entity instanceof GamabuntaEntity
                        || entity instanceof SlugSummonEntity);
        summons.sort(Comparator.comparingDouble(entity -> entity.distanceToSqr(center)));
        for (AbstractSummonAnimalEntity summon : summons) {
            summon.commandNavigationTo(target);
        }
        if (summons.isEmpty()) {
            player.displayClientMessage(Component.literal("No Toad-family or Slug summon near target."), true);
        } else {
            player.displayClientMessage(Component.literal("sent " + summons.size() + " summon(s) to ")
                    .append(Component.literal(target.getX() + " " + target.getY() + " " + target.getZ())), false);
        }
    }

    private static void handleTargetSelection(Level level, Player player, ItemStack stack) {
        HitResult hit = ProcedureUtils.objectEntityLookingAt(player, 64.0D, 0.0D, true, false, target -> target != player);
        if (!(hit instanceof EntityHitResult entityHit) || !(entityHit.getEntity() instanceof LivingEntity target)) {
            player.displayClientMessage(Component.literal("Look at a living entity."), true);
            return;
        }

        int attackerId = getAttackerId(stack);
        if (attackerId < 0) {
            if (target instanceof Mob) {
                stack.getOrCreateTag().putDouble(ATTACKER_ID_TAG, target.getId());
                player.displayClientMessage(Component.literal("set attacker to ").append(target.getDisplayName()), false);
            } else {
                player.displayClientMessage(Component.literal("First target must be a mob attacker."), true);
            }
            return;
        }

        Entity attacker = level.getEntity(attackerId);
        if (!(attacker instanceof Mob mob)) {
            clearAttacker(stack);
            player.displayClientMessage(Component.literal("Stored attacker is gone."), true);
            return;
        }
        if (attacker.equals(target)) {
            player.displayClientMessage(Component.literal("Look at a different target."), true);
            return;
        }
        clearAttacker(stack);
        mob.setTarget(target);
        player.displayClientMessage(Component.literal("set target to ").append(target.getDisplayName()), false);
    }
}
