package net.narutomod.procedure;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.narutomod.NarutomodMod;

public final class ProcedureSpawnGodTree {
    private static final List<GodTreeTemplatePlacement> PLACEMENTS = List.of(
            new GodTreeTemplatePlacement("world_tree_1", 0, 0, 0),
            new GodTreeTemplatePlacement("world_tree_2", 0, 31, 0),
            new GodTreeTemplatePlacement("world_tree_3", 0, 60, 0),
            new GodTreeTemplatePlacement("world_tree_6", 0, 90, 0),
            new GodTreeTemplatePlacement("world_tree_4", -23, 60, 0),
            new GodTreeTemplatePlacement("world_tree_15", 27, 60, 0),
            new GodTreeTemplatePlacement("world_tree_9", 0, 60, -23),
            new GodTreeTemplatePlacement("world_tree_10", 0, 60, 27),
            new GodTreeTemplatePlacement("world_tree_12", 0, 90, -23),
            new GodTreeTemplatePlacement("world_tree_14", 0, 90, 27),
            new GodTreeTemplatePlacement("world_tree_7", -23, 60, -23),
            new GodTreeTemplatePlacement("world_tree_8", -23, 60, 27),
            new GodTreeTemplatePlacement("world_tree_5", -23, 90, 0),
            new GodTreeTemplatePlacement("world_tree_11", -23, 90, -23),
            new GodTreeTemplatePlacement("world_tree_13", -23, 90, 27),
            new GodTreeTemplatePlacement("world_tree_15", 27, 60, 0),
            new GodTreeTemplatePlacement("world_tree_16", 27, 60, -23),
            new GodTreeTemplatePlacement("world_tree_17", 27, 60, 27),
            new GodTreeTemplatePlacement("world_tree_19", 27, 90, 0),
            new GodTreeTemplatePlacement("world_tree_18", 27, 90, -23),
            new GodTreeTemplatePlacement("world_tree_20", 27, 90, 27));

    private ProcedureSpawnGodTree() {
    }

    public static GodTreePlacementResult executeProcedure(Map<String, Object> dependencies) {
        Integer x = intDependency(dependencies, "x");
        if (x == null) {
            return missingDependency("x");
        }
        Integer y = intDependency(dependencies, "y");
        if (y == null) {
            return missingDependency("y");
        }
        Integer z = intDependency(dependencies, "z");
        if (z == null) {
            return missingDependency("z");
        }
        Object world = dependencies.get("world");
        if (!(world instanceof Level level)) {
            return missingDependency("world");
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return GodTreePlacementResult.invalid("client_world");
        }
        return place(serverLevel, new BlockPos(x, y, z));
    }

    public static GodTreePlacementResult place(ServerLevel level, BlockPos origin) {
        List<LoadedPlacement> loaded = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();
        int highestY = origin.getY();

        for (GodTreeTemplatePlacement placement : PLACEMENTS) {
            Optional<StructureTemplate> template = level.getStructureManager().get(NarutomodMod.location(placement.templateId()));
            if (template.isEmpty()) {
                missing.add(placement.templateId());
                continue;
            }
            StructureTemplate loadedTemplate = template.get();
            highestY = Math.max(highestY, origin.getY() + placement.offset().getY() + loadedTemplate.getSize().getY());
            loaded.add(new LoadedPlacement(placement, loadedTemplate));
        }

        if (!missing.isEmpty()) {
            return GodTreePlacementResult.missing(missing);
        }
        if (origin.getY() < minY || highestY >= maxY) {
            return GodTreePlacementResult.outOfBounds(highestY, maxY);
        }

        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setRotation(Rotation.NONE)
                .setMirror(Mirror.NONE)
                .setIgnoreEntities(false);
        int placed = 0;
        for (LoadedPlacement loadedPlacement : loaded) {
            BlockPos spawnTo = origin.offset(loadedPlacement.placement().offset());
            if (loadedPlacement.template().placeInWorld(level, spawnTo, spawnTo, settings, level.getRandom(), 2)) {
                placed++;
            }
        }
        return GodTreePlacementResult.placed(placed, PLACEMENTS.size());
    }

    private static Integer intDependency(Map<String, Object> dependencies, String key) {
        if (dependencies == null) {
            return null;
        }
        Object value = dependencies.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    private static GodTreePlacementResult missingDependency(String key) {
        System.err.println("Failed to load dependency " + key + " for procedure SpawnGodTree!");
        return GodTreePlacementResult.invalid("missing_dependency_" + key);
    }

    public record GodTreePlacementResult(boolean success, int placed, int expected, List<String> missing,
            boolean outOfBounds, int highestY, int maxY, String failureReason) {
        private static GodTreePlacementResult placed(int placed, int expected) {
            return new GodTreePlacementResult(placed == expected, placed, expected, List.of(), false, 0, 0,
                    placed == expected ? "" : "placement_failed");
        }

        private static GodTreePlacementResult missing(List<String> missing) {
            return new GodTreePlacementResult(false, 0, PLACEMENTS.size(), List.copyOf(missing), false, 0, 0,
                    "missing_templates");
        }

        private static GodTreePlacementResult outOfBounds(int highestY, int maxY) {
            return new GodTreePlacementResult(false, 0, PLACEMENTS.size(), List.of(), true, highestY, maxY,
                    "out_of_bounds");
        }

        private static GodTreePlacementResult invalid(String failureReason) {
            return new GodTreePlacementResult(false, 0, PLACEMENTS.size(), List.of(), false, 0, 0, failureReason);
        }
    }

    private record GodTreeTemplatePlacement(String templateId, BlockPos offset) {
        private GodTreeTemplatePlacement(String templateId, int x, int y, int z) {
            this(templateId, new BlockPos(x, y, z));
        }
    }

    private record LoadedPlacement(GodTreeTemplatePlacement placement, StructureTemplate template) {
    }
}
