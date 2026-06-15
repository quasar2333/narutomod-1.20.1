package net.narutomod.item;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.narutomod.NarutomodMod;
import net.narutomod.network.InventoryTrackerSyncMessage;

public final class ItemOnBody {
    private static final Vec3 RIGHT_LEG_OFFSET = new Vec3(0.125D, -0.6875D, 0.0D);
    private static final Vec3 LEFT_LEG_OFFSET = new Vec3(-0.125D, -0.6875D, 0.0D);

    private ItemOnBody() {
    }

    public interface Interface {
        default Vec3 getOffset(ItemStack stack) {
            return switch (showOnBody(stack)) {
                case RIGHT_LEG -> RIGHT_LEG_OFFSET;
                case LEFT_LEG -> LEFT_LEG_OFFSET;
                default -> Vec3.ZERO;
            };
        }

        default BodyPart showOnBody(ItemStack stack) {
            return BodyPart.TORSO;
        }
    }

    public enum BodyPart {
        NONE,
        HEAD,
        TORSO,
        RIGHT_ARM,
        LEFT_ARM,
        RIGHT_LEG,
        LEFT_LEG
    }

    @Mod.EventBusSubscriber(modid = NarutomodMod.MODID)
    public static final class InventoryTracker {
        private static final Map<Integer, InventoryTracker> TRACKERS = new HashMap<>();

        private final ServerPlayer player;
        private final Map<Integer, ItemStack> slotMap = new HashMap<>();
        private int selectedSlot;
        private boolean forceUpdate;

        private InventoryTracker(ServerPlayer player) {
            this.player = player;
            this.selectedSlot = player.getInventory().selected;
        }

        private static InventoryTracker getOrCreate(ServerPlayer player) {
            return TRACKERS.computeIfAbsent(player.getId(), ignored -> new InventoryTracker(player));
        }

        private static boolean isTrackedStack(ItemStack stack) {
            return stack != null
                    && !stack.isEmpty()
                    && stack.getItem() instanceof Interface itemOnBody
                    && itemOnBody.showOnBody(stack) != BodyPart.NONE;
        }

        private void syncToTrackingIfNeeded() {
            if (needsUpdate()) {
                InventoryTrackerSyncMessage.sendToTracking(this.player, this.selectedSlot, this.slotMap);
                this.forceUpdate = false;
            }
        }

        private void syncTo(ServerPlayer watcher) {
            this.forceUpdate = true;
            if (needsUpdate()) {
                InventoryTrackerSyncMessage.sendTo(watcher, this.player.getId(), this.selectedSlot, this.slotMap);
                this.forceUpdate = false;
            }
        }

        private boolean needsUpdate() {
            boolean update = false;
            Inventory inventory = this.player.getInventory();
            for (int index = 0; index < inventory.items.size(); index++) {
                ItemStack current = inventory.items.get(index);
                ItemStack tracked = this.slotMap.get(index);
                if ((tracked == null || !ItemStack.matches(current, tracked))
                        && (isTrackedStack(current) || isTrackedStack(tracked))) {
                    this.slotMap.put(index, current.copy());
                    update = true;
                } else if (tracked != null && ItemStack.matches(current, tracked) && !isTrackedStack(tracked)) {
                    this.slotMap.remove(index);
                }
            }

            if (!this.slotMap.isEmpty() && (this.forceUpdate || this.selectedSlot != inventory.selected)) {
                putSlotSnapshot(inventory, this.selectedSlot);
                putSlotSnapshot(inventory, inventory.selected);
                update = true;
            }
            this.selectedSlot = inventory.selected;
            return update;
        }

        private void putSlotSnapshot(Inventory inventory, int index) {
            if (index >= 0 && index < inventory.items.size()) {
                this.slotMap.put(index, inventory.items.get(index).copy());
            }
        }

        @SubscribeEvent
        public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase == TickEvent.Phase.END
                    && event.player instanceof ServerPlayer player
                    && player.tickCount % 30 == 9) {
                getOrCreate(player).syncToTrackingIfNeeded();
            }
        }

        @SubscribeEvent
        public static void onStartTracking(PlayerEvent.StartTracking event) {
            if (event.getTarget() instanceof ServerPlayer tracked && event.getEntity() instanceof ServerPlayer watcher) {
                getOrCreate(tracked).syncTo(watcher);
            }
        }

        @SubscribeEvent
        public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                TRACKERS.remove(player.getId());
            }
        }
    }
}
