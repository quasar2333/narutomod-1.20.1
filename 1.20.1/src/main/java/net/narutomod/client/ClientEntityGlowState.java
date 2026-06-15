package net.narutomod.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.narutomod.NarutomodMod;
import net.narutomod.network.EntityGlowMessage;

@Mod.EventBusSubscriber(modid = NarutomodMod.MODID, value = Dist.CLIENT)
public final class ClientEntityGlowState {
    private static final Map<Integer, GlowEntry> GLOWING = new HashMap<>();

    private ClientEntityGlowState() {
    }

    public static void apply(EntityGlowMessage message) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !message.glow() || message.durationTicks() <= 0) {
            clear(message.entityId());
            return;
        }

        Entity entity = level.getEntity(message.entityId());
        if (entity == null) {
            return;
        }

        long now = level.getGameTime();
        GlowEntry entry = GLOWING.computeIfAbsent(message.entityId(), id -> new GlowEntry(entity.isCurrentlyGlowing()));
        entry.expiresAtTick = now + message.durationTicks();
        entity.setGlowingTag(true);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            GLOWING.clear();
            return;
        }
        long now = level.getGameTime();
        Iterator<Map.Entry<Integer, GlowEntry>> iterator = GLOWING.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, GlowEntry> entry = iterator.next();
            if (now < entry.getValue().expiresAtTick) {
                continue;
            }
            Entity entity = level.getEntity(entry.getKey());
            if (entity != null && entity.isCurrentlyGlowing() && !entry.getValue().wasGlowing) {
                entity.setGlowingTag(false);
            }
            iterator.remove();
        }
    }

    private static void clear(int entityId) {
        ClientLevel level = Minecraft.getInstance().level;
        GlowEntry entry = GLOWING.remove(entityId);
        if (level == null || entry == null) {
            return;
        }
        Entity entity = level.getEntity(entityId);
        if (entity != null && entity.isCurrentlyGlowing() && !entry.wasGlowing) {
            entity.setGlowingTag(false);
        }
    }

    private static final class GlowEntry {
        private final boolean wasGlowing;
        private long expiresAtTick;

        private GlowEntry(boolean wasGlowing) {
            this.wasGlowing = wasGlowing;
        }
    }
}
