package net.narutomod;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public final class EntityTracker {
    private static final Map<UUID, DataHolder> ENTITY_MAP = new ConcurrentHashMap<>();

    private EntityTracker() {
    }

    public static DataHolder getOrCreate(Entity entity) {
        DataHolder holder = ENTITY_MAP.get(entity.getUUID());
        if (holder == null || holder.entity.isRemoved()) {
            if (holder != null) {
                holder.remove();
            }
            holder = new DataHolder(entity);
        }
        return holder;
    }

    public static void clearRemovedData() {
        Iterator<DataHolder> iterator = ENTITY_MAP.values().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().entity.isRemoved()) {
                iterator.remove();
            }
        }
    }

    public static int trackingTotal() {
        return ENTITY_MAP.size();
    }

    public static final class DataHolder {
        public final Entity entity;
        public BlockPos previousBlockPos;
        public AABB lastBoundingBox;

        private DataHolder(Entity entity) {
            this.entity = entity;
            ENTITY_MAP.put(entity.getUUID(), this);
        }

        public static DataHolder get(Entity entity) {
            return ENTITY_MAP.get(entity.getUUID());
        }

        public void remove() {
            ENTITY_MAP.remove(entity.getUUID());
        }

        public void saveBoundingBox() {
            lastBoundingBox = entity.getBoundingBox();
            previousBlockPos = entity.blockPosition();
        }

        public double lastX() {
            return lastBoundingBox == null ? entity.getX() : lastBoundingBox.getCenter().x;
        }

        public double lastY() {
            return lastBoundingBox == null ? entity.getY() : lastBoundingBox.minY;
        }

        public double lastZ() {
            return lastBoundingBox == null ? entity.getZ() : lastBoundingBox.getCenter().z;
        }

        @Override
        public String toString() {
            return "lastBB:" + lastBoundingBox;
        }
    }

    @Mod.EventBusSubscriber(modid = NarutomodMod.MODID)
    public static final class ForgeEvents {
        private ForgeEvents() {
        }

        @SubscribeEvent
        public static void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                clearRemovedData();
            }
        }
    }
}
