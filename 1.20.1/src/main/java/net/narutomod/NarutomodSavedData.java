package net.narutomod;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public final class NarutomodSavedData {
    public static final String[] TAILED_BEAST_DATA_NAMES = {
            NarutomodMod.MODID + "_onetail",
            NarutomodMod.MODID + "_twotails",
            NarutomodMod.MODID + "_threetails",
            NarutomodMod.MODID + "_fourtails",
            NarutomodMod.MODID + "_fivetails",
            NarutomodMod.MODID + "_sixtails",
            NarutomodMod.MODID + "_seventails",
            NarutomodMod.MODID + "_eighttails",
            NarutomodMod.MODID + "_ninetails",
            NarutomodMod.MODID + "_tentails",
    };

    private NarutomodSavedData() {
    }

    public static MapVariables mapVariables(MinecraftServer server) {
        return overworld(server, MapVariables::load, MapVariables::new, MapVariables.DATA_NAME);
    }

    public static WorldVariables worldVariables(ServerLevel level) {
        return levelData(level, WorldVariables::load, WorldVariables::new, WorldVariables.DATA_NAME);
    }

    public static SpecialEvents specialEvents(MinecraftServer server) {
        return overworld(server, SpecialEvents::load, SpecialEvents::new, SpecialEvents.DATA_NAME);
    }

    public static TailedBeast tailedBeast(MinecraftServer server, String dataName) {
        return overworld(server, tag -> TailedBeast.load(dataName, tag), () -> new TailedBeast(dataName), dataName);
    }

    public static Map<String, TailedBeast> tailedBeasts(MinecraftServer server) {
        Map<String, TailedBeast> result = new LinkedHashMap<>();
        for (String dataName : TAILED_BEAST_DATA_NAMES) {
            result.put(dataName, tailedBeast(server, dataName));
        }
        return result;
    }

    public static void loadAll(MinecraftServer server) {
        mapVariables(server);
        specialEvents(server);
        tailedBeasts(server);
    }

    private static <T extends SavedData> T overworld(MinecraftServer server, Function<CompoundTag, T> load, Supplier<T> create, String dataName) {
        return levelData(server.overworld(), load, create, dataName);
    }

    private static <T extends SavedData> T levelData(ServerLevel level, Function<CompoundTag, T> load, Supplier<T> create, String dataName) {
        return level.getDataStorage().computeIfAbsent(load, create, dataName);
    }

    public abstract static class CompoundBackedSavedData extends SavedData {
        private final String dataName;
        private CompoundTag data = new CompoundTag();

        protected CompoundBackedSavedData(String dataName) {
            this.dataName = dataName;
        }

        public String dataName() {
            return dataName;
        }

        public CompoundTag data() {
            return data;
        }

        public void loadRaw(CompoundTag tag) {
            data = tag.copy();
        }

        public void replace(CompoundTag tag) {
            data = tag.copy();
            setDirty();
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            tag.merge(data.copy());
            return tag;
        }
    }

    public static final class MapVariables extends CompoundBackedSavedData {
        public static final String DATA_NAME = "narutomod_mapvars";

        public MapVariables() {
            super(DATA_NAME);
        }

        public static MapVariables load(CompoundTag tag) {
            MapVariables data = new MapVariables();
            data.loadRaw(tag);
            return data;
        }
    }

    public static final class WorldVariables extends CompoundBackedSavedData {
        public static final String DATA_NAME = "narutomod_worldvars";

        public WorldVariables() {
            super(DATA_NAME);
        }

        public static WorldVariables load(CompoundTag tag) {
            WorldVariables data = new WorldVariables();
            data.loadRaw(tag);
            return data;
        }
    }

    public static final class SpecialEvents extends CompoundBackedSavedData {
        public static final String DATA_NAME = NarutomodMod.MODID + "_specialevents";

        public SpecialEvents() {
            super(DATA_NAME);
        }

        public static SpecialEvents load(CompoundTag tag) {
            SpecialEvents data = new SpecialEvents();
            data.loadRaw(tag);
            return data;
        }
    }

    public static final class TailedBeast extends CompoundBackedSavedData {
        private TailedBeast(String dataName) {
            super(dataName);
        }

        private static TailedBeast load(String dataName, CompoundTag tag) {
            TailedBeast data = new TailedBeast(dataName);
            data.loadRaw(tag);
            return data;
        }
    }

    @Mod.EventBusSubscriber(modid = NarutomodMod.MODID)
    public static final class ForgeEvents {
        private ForgeEvents() {
        }

        @SubscribeEvent
        public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
            if (!event.getEntity().level().isClientSide()) {
                loadAll(event.getEntity().getServer());
                if (event.getEntity().level() instanceof ServerLevel level) {
                    worldVariables(level);
                }
            }
        }
    }
}
