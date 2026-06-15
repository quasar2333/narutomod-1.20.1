package net.narutomod.item;

import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.narutomod.NarutomodMod;
import net.narutomod.entity.NuibariSwordEntity;
import net.narutomod.registry.ModItems;

public final class NuibariThrownItem extends Item {
    private static final String ENTITY_ID_TAG = "nuibariEntityId";

    public NuibariThrownItem() {
        super(new Item.Properties().stacksTo(1));
    }

    public static void bindEntity(ItemStack stack, NuibariSwordEntity entity) {
        stack.getOrCreateTag().putInt(ENTITY_ID_TAG, entity.getId());
    }

    @Nullable
    public static NuibariSwordEntity getBoundEntity(Level level, ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(ENTITY_ID_TAG)) {
            return null;
        }
        Entity entity = level.getEntity(tag.getInt(ENTITY_ID_TAG));
        return entity instanceof NuibariSwordEntity nuibari ? nuibari : null;
    }

    public static boolean matchesEntity(ItemStack stack, int entityId) {
        CompoundTag tag = stack.getTag();
        return stack.is(ModItems.NUIBARI_THROWN.get())
                && tag != null
                && tag.getInt(ENTITY_ID_TAG) == entityId;
    }

    public static boolean replaceBoundStack(Player player, int entityId, ItemStack replacement) {
        for (int index = 0; index < player.getInventory().items.size(); index++) {
            if (matchesEntity(player.getInventory().items.get(index), entityId)) {
                player.getInventory().items.set(index, replacement);
                return true;
            }
        }
        for (int index = 0; index < player.getInventory().offhand.size(); index++) {
            if (matchesEntity(player.getInventory().offhand.get(index), entityId)) {
                player.getInventory().offhand.set(index, replacement);
                return true;
            }
        }
        return false;
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
        NuibariSwordEntity entity = getBoundEntity(level, stack);
        if (entity != null && entity.isOwner(player)) {
            entity.retrieveToward(player);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (!level.isClientSide && entity instanceof Player player) {
            NuibariSwordEntity nuibari = getBoundEntity(level, stack);
            if (nuibari == null || !nuibari.isOwner(player)) {
                stack.shrink(1);
            }
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

    @Mod.EventBusSubscriber(modid = NarutomodMod.MODID)
    public static final class ForgeEvents {
        private ForgeEvents() {
        }

        @SubscribeEvent
        public static void onTossItem(ItemTossEvent event) {
            ItemEntity itemEntity = event.getEntity();
            ItemStack stack = itemEntity.getItem();
            if (!stack.is(ModItems.NUIBARI_THROWN.get())) {
                return;
            }
            event.setCanceled(true);
            NuibariSwordEntity entity = getBoundEntity(itemEntity.level(), stack);
            if (entity != null) {
                entity.clearLivingOwner();
            }
        }
    }
}
