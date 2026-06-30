package net.narutomod.procedure;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.narutomod.NarutomodMod;
import net.narutomod.NarutomodModVariables;
import net.narutomod.entity.ChibakuSatelliteEntity;
import net.narutomod.registry.ModSounds;

public final class ProcedureMeteorStrike {
    private static final String METEOR_TEMPLATE_ID = "meteor";
    private static final int METEOR_OFFSET_XZ = 10;
    private static final int METEOR_HEIGHT_OFFSET = 90;
    private static final int CAPTURE_SIZE = 20;
    private static final int METEOR_FALL_DELAY_TICKS = 5;
    private static final int METEOR_MAX_FALL_TICKS = 160;
    private static final double REUSE_HORIZONTAL_RANGE = 64.0D;
    private static final double REUSE_VERTICAL_RANGE = 128.0D;
    private static final double HORIZONTAL_VELOCITY_SCALE = 1.2D;
    private static final double FALL_ACCELERATION = 0.04D;

    private ProcedureMeteorStrike() {
    }

    public static MeteorStrikeResult strike(ServerLevel level, LivingEntity owner, BlockPos target) {
        ChibakuSatelliteEntity satellite = findReusableSatellite(level, owner);
        level.playSound(null, target, ModSounds.SOUND_TENGAISHINSEI.get(), SoundSource.NEUTRAL, 5.0F, 1.0F);
        if (satellite != null) {
            redirectSatellite(satellite, target);
            return MeteorStrikeResult.reused(satellite);
        }

        Optional<StructureTemplate> templateOptional = level.getStructureManager().get(NarutomodMod.location(METEOR_TEMPLATE_ID));
        if (templateOptional.isEmpty()) {
            return MeteorStrikeResult.missingTemplate(METEOR_TEMPLATE_ID);
        }

        BlockPos spawnTo = target.offset(-METEOR_OFFSET_XZ, METEOR_HEIGHT_OFFSET, -METEOR_OFFSET_XZ);
        int highestY = spawnTo.getY() + CAPTURE_SIZE;
        if (spawnTo.getY() < level.getMinBuildHeight() || highestY >= level.getMaxBuildHeight()) {
            return MeteorStrikeResult.outOfBounds(spawnTo, highestY, level.getMaxBuildHeight());
        }

        StructureTemplate template = templateOptional.get();
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setRotation(Rotation.NONE)
                .setMirror(Mirror.NONE)
                .setIgnoreEntities(false);
        boolean placed = template.placeInWorld(level, spawnTo, spawnTo, settings, level.getRandom(), 2);
        if (!placed) {
            return MeteorStrikeResult.placementFailed(spawnTo);
        }

        List<BlockState> states = collectMeteorBlocks(level, spawnTo);
        if (states.isEmpty()) {
            return MeteorStrikeResult.noBlocks(spawnTo);
        }

        Vec3 center = Vec3.atLowerCornerOf(spawnTo).add(CAPTURE_SIZE * 0.5D, CAPTURE_SIZE * 0.5D, CAPTURE_SIZE * 0.5D);
        ChibakuSatelliteEntity created = ChibakuSatelliteEntity.spawnFromStates(
                owner,
                center,
                states,
                METEOR_FALL_DELAY_TICKS,
                METEOR_MAX_FALL_TICKS);
        if (created == null) {
            return MeteorStrikeResult.spawnFailed(spawnTo, states.size());
        }
        owner.getPersistentData().putDouble(NarutomodModVariables.INVULNERABLE_TIME, 300.0D);
        return MeteorStrikeResult.spawned(spawnTo, states.size(), created);
    }

    public static boolean hasReusableSatellite(ServerLevel level, LivingEntity owner) {
        return findReusableSatellite(level, owner) != null;
    }

    private static ChibakuSatelliteEntity findReusableSatellite(ServerLevel level, LivingEntity owner) {
        AABB searchBox = owner.getBoundingBox()
                .inflate(REUSE_HORIZONTAL_RANGE, 0.0D, REUSE_HORIZONTAL_RANGE)
                .expandTowards(0.0D, REUSE_VERTICAL_RANGE, 0.0D);
        return level.getEntitiesOfClass(ChibakuSatelliteEntity.class, searchBox, entity -> entity.isAlive())
                .stream()
                .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(owner)))
                .orElse(null);
    }

    private static void redirectSatellite(ChibakuSatelliteEntity satellite, BlockPos target) {
        Vec3 targetCenter = Vec3.atCenterOf(target);
        double dx = targetCenter.x() - satellite.getX();
        double dy = targetCenter.y() - satellite.getY();
        double dz = targetCenter.z() - satellite.getZ();
        double fallTime = Math.max(Mth.sqrt((float)(Math.abs(dy) * 2.0D / FALL_ACCELERATION)), 1.0D);
        satellite.setNoGravity(false);
        satellite.setDeltaMovement(dx / fallTime * HORIZONTAL_VELOCITY_SCALE, satellite.getDeltaMovement().y(), dz / fallTime * HORIZONTAL_VELOCITY_SCALE);
        satellite.hurtMarked = true;
    }

    private static List<BlockState> collectMeteorBlocks(ServerLevel level, BlockPos spawnTo) {
        List<BlockState> states = new ArrayList<>();
        BlockPos end = spawnTo.offset(CAPTURE_SIZE - 1, CAPTURE_SIZE - 1, CAPTURE_SIZE - 1);
        for (BlockPos pos : BlockPos.betweenClosed(spawnTo, end)) {
            BlockState state = level.getBlockState(pos);
            float hardness = state.getDestroySpeed(level, pos);
            if (!state.isAir() && hardness >= 0.0F && hardness <= 1000.0F) {
                states.add(state);
                level.removeBlock(pos, false);
            }
        }
        return states;
    }

    public record MeteorStrikeResult(boolean success, boolean reusedSatellite, boolean spawnedSatellite,
            BlockPos spawnTo, int capturedBlocks, int satelliteId, String failureReason,
            String missingTemplate, int highestY, int maxY) {
        private static MeteorStrikeResult reused(ChibakuSatelliteEntity satellite) {
            return new MeteorStrikeResult(true, true, false, satellite.blockPosition(), 0, satellite.getId(), "", "", 0, 0);
        }

        private static MeteorStrikeResult spawned(BlockPos spawnTo, int capturedBlocks, ChibakuSatelliteEntity satellite) {
            return new MeteorStrikeResult(true, false, true, spawnTo, capturedBlocks, satellite.getId(), "", "", 0, 0);
        }

        private static MeteorStrikeResult missingTemplate(String templateId) {
            return failure("missing_template", null, 0, -1, templateId, 0, 0);
        }

        private static MeteorStrikeResult outOfBounds(BlockPos spawnTo, int highestY, int maxY) {
            return failure("out_of_bounds", spawnTo, 0, -1, "", highestY, maxY);
        }

        private static MeteorStrikeResult placementFailed(BlockPos spawnTo) {
            return failure("placement_failed", spawnTo, 0, -1, "", 0, 0);
        }

        private static MeteorStrikeResult noBlocks(BlockPos spawnTo) {
            return failure("no_captured_blocks", spawnTo, 0, -1, "", 0, 0);
        }

        private static MeteorStrikeResult spawnFailed(BlockPos spawnTo, int capturedBlocks) {
            return failure("satellite_spawn_failed", spawnTo, capturedBlocks, -1, "", 0, 0);
        }

        private static MeteorStrikeResult failure(String reason, BlockPos spawnTo, int capturedBlocks,
                int satelliteId, String missingTemplate, int highestY, int maxY) {
            return new MeteorStrikeResult(false, false, false, spawnTo, capturedBlocks, satelliteId, reason, missingTemplate, highestY, maxY);
        }
    }
}
