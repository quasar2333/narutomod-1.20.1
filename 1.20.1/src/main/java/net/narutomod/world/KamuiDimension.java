package net.narutomod.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;
import net.narutomod.NarutomodMod;
import net.narutomod.registry.ModBlocks;
import java.util.function.Function;

public final class KamuiDimension {
    public static final ResourceLocation ID = NarutomodMod.location("kamui_dimension");
    public static final ResourceKey<Level> KEY = ResourceKey.create(Registries.DIMENSION, ID);
    public static final int PLATFORM_Y = 64;

    private static final String RETURN_DIMENSION_TAG = "NarutomodKamuiReturnDimension";
    private static final String RETURN_X_TAG = "NarutomodKamuiReturnX";
    private static final String RETURN_Y_TAG = "NarutomodKamuiReturnY";
    private static final String RETURN_Z_TAG = "NarutomodKamuiReturnZ";
    private static final String RETURN_YAW_TAG = "NarutomodKamuiReturnYaw";
    private static final String RETURN_PITCH_TAG = "NarutomodKamuiReturnPitch";

    private KamuiDimension() {
    }

    public static boolean isKamui(Level level) {
        return level.dimension().equals(KEY);
    }

    public static ServerLevel level(MinecraftServer server) {
        return server.getLevel(KEY);
    }

    public static boolean toggle(ServerPlayer player) {
        return isKamui(player.level()) ? leave(player) : enter(player);
    }

    public static boolean toggleEntity(Entity entity) {
        if (entity instanceof ServerPlayer player) {
            return toggle(player);
        }
        return isKamui(entity.level()) ? leaveEntity(entity) : enterEntity(entity);
    }

    public static boolean enter(ServerPlayer player) {
        ServerLevel target = level(player.server);
        if (target == null) {
            player.displayClientMessage(Component.literal("Kamui dimension is not loaded."), true);
            return false;
        }
        rememberReturn(player);
        BlockPos platform = platformCenter(player.getX(), player.getZ());
        ensureEntryPlatform(target, platform);
        teleport(player, target, platform.getX() + 0.5D, PLATFORM_Y + 1.0D, platform.getZ() + 0.5D,
                player.getYRot(), player.getXRot());
        player.displayClientMessage(Component.literal("Entered Kamui dimension."), true);
        return true;
    }

    public static boolean enterEntity(Entity entity) {
        if (entity instanceof ServerPlayer player) {
            return enter(player);
        }
        if (!(entity.level() instanceof ServerLevel sourceLevel)) {
            return false;
        }
        ServerLevel target = level(sourceLevel.getServer());
        if (target == null || !canTeleportEntity(entity, sourceLevel, target)) {
            return false;
        }
        rememberReturn(entity);
        BlockPos platform = platformCenter(entity.getX(), entity.getZ());
        ensureEntryPlatform(target, platform);
        return teleportEntity(entity, target, platform.getX() + 0.5D, PLATFORM_Y + 1.0D, platform.getZ() + 0.5D,
                entity.getYRot(), entity.getXRot());
    }

    public static boolean leave(ServerPlayer player) {
        ReturnTarget target = resolveReturnTarget(player);
        teleport(player, target.level(), target.x(), target.y(), target.z(), target.yaw(), target.pitch());
        clearReturn(player);
        player.displayClientMessage(Component.literal("Returned from Kamui dimension."), true);
        return true;
    }

    public static boolean leaveEntity(Entity entity) {
        if (entity instanceof ServerPlayer player) {
            return leave(player);
        }
        if (!(entity.level() instanceof ServerLevel sourceLevel)) {
            return false;
        }
        ReturnTarget target = resolveReturnTarget(sourceLevel.getServer(), entity.getPersistentData(), entity.getYRot(), entity.getXRot());
        if (!canTeleportEntity(entity, sourceLevel, target.level())) {
            return false;
        }
        boolean changed = teleportEntity(entity, target.level(), target.x(), target.y(), target.z(), target.yaw(), target.pitch());
        if (changed) {
            clearReturn(entity.getPersistentData());
        }
        return changed;
    }

    public static int transferBlockDropsToKamui(ServerLevel sourceLevel, BlockPos pos) {
        ServerLevel target = level(sourceLevel.getServer());
        if (target == null) {
            return 0;
        }
        BlockPos platform = platformCenter(pos.getX(), pos.getZ());
        ensureEntryPlatform(target, platform);
        return transferBlockDrops(sourceLevel, pos, target,
                new Vec3(platform.getX() + 0.5D, PLATFORM_Y + 1.2D, platform.getZ() + 0.5D));
    }

    public static int transferBlockDropsToReturn(ServerPlayer player, BlockPos pos) {
        if (!(player.level() instanceof ServerLevel sourceLevel) || !isKamui(sourceLevel)) {
            return 0;
        }
        ReturnTarget target = resolveReturnTarget(player);
        return transferBlockDrops(sourceLevel, pos, target.level(),
                new Vec3(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D));
    }

    private static int transferBlockDrops(ServerLevel sourceLevel, BlockPos pos, ServerLevel targetLevel, Vec3 targetPos) {
        BlockState state = sourceLevel.getBlockState(pos);
        if (state.isAir()) {
            return 0;
        }
        BlockEntity blockEntity = sourceLevel.getBlockEntity(pos);
        targetLevel.getChunk(BlockPos.containing(targetPos));
        int moved = 0;
        for (ItemStack drop : Block.getDrops(state, sourceLevel, pos, blockEntity)) {
            if (drop.isEmpty()) {
                continue;
            }
            ItemEntity itemEntity = new ItemEntity(targetLevel, targetPos.x(), targetPos.y(), targetPos.z(), drop.copy());
            itemEntity.setDefaultPickUpDelay();
            targetLevel.addFreshEntity(itemEntity);
            moved += drop.getCount();
        }
        sourceLevel.removeBlock(pos, false);
        return moved;
    }

    public static BlockPos platformCenter(double x, double z) {
        return new BlockPos(Mth.floor(x), PLATFORM_Y, Mth.floor(z));
    }

    public static boolean ensureEntryPlatform(ServerLevel level, BlockPos center) {
        boolean changed = false;
        for (int y = PLATFORM_Y; y >= level.getMinBuildHeight(); y--) {
            changed |= setKamuiBlock(level, new BlockPos(center.getX(), y, center.getZ()));
        }
        int radius = 5;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                changed |= setKamuiBlock(level, center.offset(x, 0, z));
                changed |= clearForSpawn(level, center.offset(x, 1, z));
                changed |= clearForSpawn(level, center.offset(x, 2, z));
            }
        }
        return changed;
    }

    public static String describeReturn(Entity entity) {
        CompoundTag tag = entity.getPersistentData();
        if (!tag.contains(RETURN_DIMENSION_TAG)) {
            return "none";
        }
        return tag.getString(RETURN_DIMENSION_TAG)
                + "@"
                + Mth.floor(tag.getDouble(RETURN_X_TAG))
                + ","
                + Mth.floor(tag.getDouble(RETURN_Y_TAG))
                + ","
                + Mth.floor(tag.getDouble(RETURN_Z_TAG));
    }

    private static void rememberReturn(Entity entity) {
        CompoundTag tag = entity.getPersistentData();
        tag.putString(RETURN_DIMENSION_TAG, entity.level().dimension().location().toString());
        tag.putDouble(RETURN_X_TAG, entity.getX());
        tag.putDouble(RETURN_Y_TAG, entity.getY());
        tag.putDouble(RETURN_Z_TAG, entity.getZ());
        tag.putFloat(RETURN_YAW_TAG, entity.getYRot());
        tag.putFloat(RETURN_PITCH_TAG, entity.getXRot());
    }

    private static void rememberReturn(ServerPlayer player) {
        rememberReturn((Entity) player);
    }

    private static ReturnTarget resolveReturnTarget(ServerPlayer player) {
        return resolveReturnTarget(player.server, player.getPersistentData(), player.getYRot(), player.getXRot());
    }

    private static ReturnTarget resolveReturnTarget(MinecraftServer server, CompoundTag tag, float fallbackYaw, float fallbackPitch) {
        ServerLevel fallback = server.overworld();
        if (!tag.contains(RETURN_DIMENSION_TAG)) {
            BlockPos spawn = fallback.getSharedSpawnPos();
            return new ReturnTarget(fallback, spawn.getX() + 0.5D, spawn.getY() + 1.0D, spawn.getZ() + 0.5D,
                    fallbackYaw, fallbackPitch);
        }
        ResourceLocation id = ResourceLocation.tryParse(tag.getString(RETURN_DIMENSION_TAG));
        ServerLevel target = id == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
        if (target == null || target.dimension().equals(KEY)) {
            target = fallback;
        }
        return new ReturnTarget(target,
                tag.getDouble(RETURN_X_TAG),
                tag.getDouble(RETURN_Y_TAG),
                tag.getDouble(RETURN_Z_TAG),
                tag.getFloat(RETURN_YAW_TAG),
                tag.getFloat(RETURN_PITCH_TAG));
    }

    private static void clearReturn(ServerPlayer player) {
        clearReturn(player.getPersistentData());
    }

    private static void clearReturn(CompoundTag tag) {
        tag.remove(RETURN_DIMENSION_TAG);
        tag.remove(RETURN_X_TAG);
        tag.remove(RETURN_Y_TAG);
        tag.remove(RETURN_Z_TAG);
        tag.remove(RETURN_YAW_TAG);
        tag.remove(RETURN_PITCH_TAG);
    }

    private static void teleport(ServerPlayer player, ServerLevel level, double x, double y, double z, float yaw, float pitch) {
        player.fallDistance = 0.0F;
        player.teleportTo(level, x, y, z, yaw, pitch);
        player.setDeltaMovement(0.0D, 0.0D, 0.0D);
        player.hasImpulse = true;
    }

    private static boolean teleportEntity(Entity entity, ServerLevel level, double x, double y, double z, float yaw, float pitch) {
        Entity changed = entity.changeDimension(level, new DirectTeleporter(new Vec3(x, y, z), Vec3.ZERO, yaw, pitch));
        if (changed == null) {
            return false;
        }
        changed.fallDistance = 0.0F;
        changed.setDeltaMovement(Vec3.ZERO);
        changed.hasImpulse = true;
        return true;
    }

    private static boolean canTeleportEntity(Entity entity, ServerLevel source, ServerLevel target) {
        return !entity.isPassenger()
                && !entity.isVehicle()
                && (source.dimension().equals(target.dimension()) || entity.canChangeDimensions());
    }

    private record DirectTeleporter(Vec3 pos, Vec3 speed, float yaw, float pitch) implements ITeleporter {
        @Override
        public Entity placeEntity(
                Entity entity,
                ServerLevel currentWorld,
                ServerLevel destinationWorld,
                float yaw,
                Function<Boolean, Entity> repositionEntity) {
            Entity changed = repositionEntity.apply(false);
            changed.moveTo(this.pos.x(), this.pos.y(), this.pos.z(), this.yaw, this.pitch);
            changed.setDeltaMovement(this.speed);
            return changed;
        }

        @Override
        public PortalInfo getPortalInfo(Entity entity, ServerLevel destinationWorld, Function<ServerLevel, PortalInfo> defaultPortalInfo) {
            return new PortalInfo(this.pos, this.speed, this.yaw, this.pitch);
        }

        @Override
        public boolean playTeleportSound(ServerPlayer player, ServerLevel sourceWorld, ServerLevel destinationWorld) {
            return false;
        }
    }

    private static boolean setKamuiBlock(ServerLevel level, BlockPos pos) {
        if (level.getBlockState(pos).is(ModBlocks.KAMUIBLOCK.get())) {
            return false;
        }
        return level.setBlock(pos, ModBlocks.KAMUIBLOCK.get().defaultBlockState(), 3);
    }

    private static boolean clearForSpawn(ServerLevel level, BlockPos pos) {
        if (level.isEmptyBlock(pos)) {
            return false;
        }
        return level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }

    private record ReturnTarget(ServerLevel level, double x, double y, double z, float yaw, float pitch) {
    }
}
