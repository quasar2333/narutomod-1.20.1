package net.narutomod.item;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.narutomod.Chakra;
import net.narutomod.NarutomodMod;
import net.narutomod.NarutomodModVariables;
import net.narutomod.PlayerTracker;
import net.narutomod.entity.EarthBlocksEntity;
import net.narutomod.entity.ThrownNinjaToolEntity;
import net.narutomod.particle.NarutoParticleKind;
import net.narutomod.procedure.ProcedureSync;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModDamageTypes;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModItems;
import net.narutomod.registry.ModParticleTypes;
import net.narutomod.registry.ModSounds;
import net.narutomod.world.KamuiDimension;

@Mod.EventBusSubscriber(modid = NarutomodMod.MODID)
public final class ObitoKamuiHandler {
    public static final String KAMUI_INTANGIBLE_TAG = "kamui_intangible";
    public static final String KAMUI_TELEPORT_TAG = "kamui_teleport";
    public static final String KAMUI_TIMER_TAG = "kamui_timer";
    public static final String KAMUI_GRAB_TAG = "kamui_grab";
    public static final String KAMUI_JUTSU3_HELD_TAG = "kamui_jutsu3_held";

    private static final double TELEPORT_RANGE = 100.0D;
    private static final double GRAB_RANGE = 4.0D;
    private static final double CHEST_LINK_RANGE = 8.0D;
    private static final double PROJECTILE_CHAKRA_USAGE = 20.0D;
    private static final double COMPLETE_TRANSFER_THRESHOLD = 0.99999D;
    private static final int PORTAL_PARTICLE_COUNT = 64;
    private static final String CHEST_LINKED_TAG = "kamui_chest_linked";
    private static final String CHEST_X_TAG = "kamui_chest_x";
    private static final String CHEST_Y_TAG = "kamui_chest_y";
    private static final String CHEST_Z_TAG = "kamui_chest_z";
    private static final Map<UUID, GrabState> ACTIVE_GRABS = new HashMap<>();

    private ObitoKamuiHandler() {
    }

    public static boolean handleSpecialJutsuKey(ServerPlayer player, int key, boolean pressed) {
        if (!canHandleSpecialJutsuKey(player, key) || player.isSpectator() || !player.isAlive()) {
            return false;
        }
        if (!ObitoMangekyoHelmetItem.canUseKamui(player)) {
            clearAll(player);
            player.displayClientMessage(Component.literal("Kamui is unavailable while the Sharingan is blinded."), true);
            return true;
        }
        if (key == 3 && hasObitoHelmet(player)) {
            handleObitoExtendedKey(player, pressed);
        } else if (player.isShiftKeyDown()) {
            handleTeleportKey(player, pressed);
        } else if (shouldUseKamuiGrab(player, key)) {
            handleGrabKey(player, pressed);
        } else {
            handleIntangibleKey(player, pressed);
        }
        return true;
    }

    public static boolean pressForDebug(ServerPlayer player, boolean sneaking) {
        if (!hasKamuiHelmet(player)) {
            return false;
        }
        if (sneaking) {
            startTeleport(player);
        } else {
            startIntangible(player);
        }
        return true;
    }

    public static boolean grabForDebug(ServerPlayer player, boolean pressed) {
        if (!hasObitoHelmet(player) || !KamuiDimension.isKamui(player.level())) {
            return false;
        }
        handleGrabKey(player, pressed);
        return true;
    }

    public static boolean releaseForDebug(ServerPlayer player) {
        if (!hasKamuiHelmet(player)) {
            return false;
        }
        if (isTeleporting(player)) {
            releaseTeleport(player);
            return true;
        }
        if (isGrabbing(player)) {
            stopGrab(player);
            return true;
        }
        if (isIntangible(player)) {
            stopIntangible(player);
            return true;
        }
        return false;
    }

    public static boolean isIntangible(LivingEntity entity) {
        if (entity instanceof Player player) {
            return NarutomodModVariables.get(player).getBoolean(KAMUI_INTANGIBLE_TAG);
        }
        return entity.getPersistentData().getBoolean(KAMUI_INTANGIBLE_TAG);
    }

    public static boolean isTeleporting(Player player) {
        return NarutomodModVariables.get(player).getBoolean(KAMUI_TELEPORT_TAG);
    }

    public static double timer(Player player) {
        return NarutomodModVariables.get(player).getDouble(KAMUI_TIMER_TAG);
    }

    public static boolean isGrabbing(Player player) {
        return NarutomodModVariables.get(player).getBoolean(KAMUI_GRAB_TAG);
    }

    public static String grabbedDescription(ServerPlayer player) {
        GrabState state = ACTIVE_GRABS.get(player.getUUID());
        if (state == null) {
            return "none";
        }
        Entity entity = findEntity(player, state.entityId());
        return entity == null ? "missing" : entity.getDisplayName().getString();
    }

    public static boolean hasObitoHelmet(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.MANGEKYOSHARINGANOBITOHELMET.get());
    }

    public static boolean hasEternalHelmet(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.MANGEKYOSHARINGANETERNALHELMET.get());
    }

    public static boolean hasKamuiHelmet(Player player) {
        return hasObitoHelmet(player) || hasEternalHelmet(player);
    }

    private static boolean canHandleSpecialJutsuKey(Player player, int key) {
        return hasObitoHelmet(player) && (key == 1 || key == 3)
                || key == 3 && hasEternalHelmet(player);
    }

    private static boolean shouldUseKamuiGrab(Player player, int key) {
        return key == 1 && hasObitoHelmet(player) && KamuiDimension.isKamui(player.level());
    }

    private static void handleIntangibleKey(ServerPlayer player, boolean pressed) {
        if (pressed) {
            stopGrab(player);
            clearTeleport(player);
            if (!isIntangible(player)) {
                startIntangible(player);
            }
        } else if (isIntangible(player)) {
            stopIntangible(player);
        }
    }

    private static void handleTeleportKey(ServerPlayer player, boolean pressed) {
        stopGrab(player);
        if (isIntangible(player)) {
            if (pressed) {
                keepIntangible(player);
            } else {
                stopIntangible(player);
            }
            return;
        }
        if (pressed) {
            if (!isTeleporting(player)) {
                startTeleport(player);
            }
        } else if (isTeleporting(player)) {
            releaseTeleport(player);
        }
    }

    private static void handleGrabKey(ServerPlayer player, boolean pressed) {
        clearTeleport(player);
        if (isIntangible(player)) {
            stopIntangible(player);
        }
        if (pressed) {
            maintainGrab(player, true);
        } else {
            stopGrab(player);
        }
    }

    private static void handleObitoExtendedKey(ServerPlayer player, boolean pressed) {
        clearTeleport(player);
        stopGrab(player);
        CompoundTag data = player.getPersistentData();
        boolean wasPressed = data.getBoolean(KAMUI_JUTSU3_HELD_TAG);
        data.putBoolean(KAMUI_JUTSU3_HELD_TAG, pressed);
        if (pressed && !wasPressed) {
            ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
            executeObitoExtendedKamui(player, ObitoMangekyoHelmetItem.getKamuiMode(head));
        }
    }

    private static void executeObitoExtendedKamui(ServerPlayer player, int mode) {
        if (mode == 0) {
            teleportSelf(player);
        } else if (mode == 1) {
            if (player.isShiftKeyDown()) {
                linkKamuiChest(player);
            } else {
                openKamuiChest(player);
            }
        } else if (mode == 2) {
            fireKamuiProjectile(player);
        }
    }

    private static void teleportSelf(ServerPlayer player) {
        double chakraUsage = ObitoMangekyoHelmetItem.getTeleportChakraUsage(player);
        if (!canSpend(player, chakraUsage)) {
            return;
        }
        if (KamuiDimension.toggle(player)) {
            if (!player.getAbilities().instabuild) {
                Chakra.pathway(player).consume(chakraUsage);
            }
        } else {
            player.displayClientMessage(Component.translatable("chattext.kamui.teleport.failed"), true);
        }
    }

    private static void linkKamuiChest(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level) || !KamuiDimension.isKamui(level)) {
            player.displayClientMessage(Component.translatable("chattext.kamui.chest.look"), true);
            return;
        }
        CompoundTag data = player.getPersistentData();
        if (data.getBoolean(CHEST_LINKED_TAG)) {
            player.displayClientMessage(Component.translatable("chattext.kamui.chest.already"), true);
            return;
        }
        BlockHitResult hit = ProcedureUtils.raytraceBlocks(player, CHEST_LINK_RANGE);
        if (hit.getType() != HitResult.Type.BLOCK) {
            player.displayClientMessage(Component.translatable("chattext.kamui.chest.look"), true);
            return;
        }
        BlockPos pos = hit.getBlockPos();
        if (resolveContainerAt(level, pos) == null) {
            player.displayClientMessage(Component.translatable("chattext.kamui.chest.look"), true);
            return;
        }
        data.putBoolean(CHEST_LINKED_TAG, true);
        data.putInt(CHEST_X_TAG, pos.getX());
        data.putInt(CHEST_Y_TAG, pos.getY());
        data.putInt(CHEST_Z_TAG, pos.getZ());
        player.displayClientMessage(Component.translatable("chattext.kamui.chest.linked"), true);
    }

    private static void openKamuiChest(ServerPlayer player) {
        ResolvedKamuiContainer chest = resolvePhysicalChest(player, true);
        if (chest == null) {
            return;
        }
        RemoteContainerProxy remote = new RemoteContainerProxy(chest, player.getUUID());
        player.openMenu(new SimpleMenuProvider((id, inventory, p) ->
                createChestMenu(id, inventory, remote), Component.translatable("container.chest")));
    }

    private static void fireKamuiProjectile(ServerPlayer player) {
        ResolvedKamuiContainer resolved = resolvePhysicalChest(player, true);
        if (resolved == null || !canSpend(player, PROJECTILE_CHAKRA_USAGE)) {
            return;
        }
        Container chest = resolved.container();
        resolved.keepLoaded();
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            ItemStack stack = chest.getItem(slot);
            if (stack.isEmpty() || !isAllowedProjectile(stack)) {
                continue;
            }
            if (spawnKamuiProjectile(player, stack)) {
                stack.shrink(1);
                chest.setItem(slot, stack);
                chest.setChanged();
                resolved.markChanged();
                if (!player.getAbilities().instabuild) {
                    Chakra.pathway(player).consume(PROJECTILE_CHAKRA_USAGE);
                }
            }
            return;
        }
        player.displayClientMessage(Component.translatable("chattext.kamui.chest.empty"), true);
    }

    private static boolean spawnKamuiProjectile(ServerPlayer player, ItemStack stack) {
        Vec3 look = player.getLookAngle();
        if (look.lengthSqr() < 1.0E-6D) {
            return false;
        }
        look = look.normalize();
        Level level = player.level();
        Entity root = player.getRootVehicle();
        double offset = root == player ? 1.4D : Math.max(root.getBbWidth(), root.getBbHeight()) * 0.5D + 1.8D;
        Vec3 spawn = player.getEyePosition().add(look.scale(offset));
        Projectile projectile = buildKamuiProjectile(player, stack, level);
        if (projectile == null) {
            return false;
        }
        projectile.setPos(spawn.x(), spawn.y(), spawn.z());
        projectile.shoot(look.x(), look.y(), look.z(), 3.0F, 0.0F);
        boolean spawned = level.addFreshEntity(projectile);
        if (spawned && level instanceof ServerLevel serverLevel) {
            ParticleOptions options = ModParticleTypes.options(NarutoParticleKind.PORTAL_SPIRAL, 2, 0x20000000, 22);
            serverLevel.sendParticles(options, spawn.x(), spawn.y(), spawn.z(), 60, 0.0D, 0.0D, 0.0D, 0.0D);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.SOUND_KAMUISFX.get(), SoundSource.NEUTRAL, 1.0F, 1.4F);
        }
        return spawned;
    }

    private static Projectile buildKamuiProjectile(ServerPlayer player, ItemStack stack, Level level) {
        if (stack.is(ModItems.KUNAI.get()) && ModItems.KUNAI.get() instanceof NinjaToolItem tool) {
            ThrownNinjaToolEntity projectile = ModEntityTypes.ENTITYBULLETKUNAI.get().create(level);
            if (projectile != null) {
                projectile.configure(player, tool, true);
                projectile.setBaseDamage(5.0F);
            }
            return projectile;
        }
        if (stack.is(ModItems.SHURIKEN.get()) && ModItems.SHURIKEN.get() instanceof NinjaToolItem tool) {
            ThrownNinjaToolEntity projectile = ModEntityTypes.ENTITYBULLETSHURIKEN.get().create(level);
            if (projectile != null) {
                projectile.configure(player, tool, true);
                projectile.setBaseDamage(4.0F);
            }
            return projectile;
        }
        if (stack.is(Items.SNOWBALL)) {
            return new Snowball(level, player);
        }
        if (stack.is(Items.ENDER_PEARL)) {
            return new ThrownEnderpearl(level, player);
        }
        return stack.is(Items.EGG) ? new ThrownEgg(level, player) : null;
    }

    private static boolean isAllowedProjectile(ItemStack stack) {
        return stack.is(ModItems.KUNAI.get())
                || stack.is(ModItems.SHURIKEN.get())
                || stack.is(Items.SNOWBALL)
                || stack.is(Items.ENDER_PEARL)
                || stack.is(Items.EGG);
    }

    private static ResolvedKamuiContainer resolvePhysicalChest(ServerPlayer player, boolean showError) {
        CompoundTag data = player.getPersistentData();
        if (!data.getBoolean(CHEST_LINKED_TAG)) {
            if (showError) {
                player.displayClientMessage(Component.translatable("chattext.kamui.chest.none"), true);
            }
            return null;
        }
        ServerLevel kamui = KamuiDimension.isKamui(player.level())
                ? (ServerLevel) player.level()
                : KamuiDimension.level(player.server);
        if (kamui == null) {
            if (showError) {
                player.displayClientMessage(Component.translatable("chattext.kamui.chest.invalid"), true);
            }
            return null;
        }
        BlockPos pos = new BlockPos(data.getInt(CHEST_X_TAG), data.getInt(CHEST_Y_TAG), data.getInt(CHEST_Z_TAG));
        Container container = resolveContainerAt(kamui, pos);
        if (container == null) {
            clearLinkedChest(player);
            if (showError) {
                player.displayClientMessage(Component.translatable("chattext.kamui.chest.invalid"), true);
            }
            return null;
        }
        return new ResolvedKamuiContainer(kamui, pos, container);
    }

    private static void clearLinkedChest(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        data.remove(CHEST_LINKED_TAG);
        data.remove(CHEST_X_TAG);
        data.remove(CHEST_Y_TAG);
        data.remove(CHEST_Z_TAG);
    }

    private static Container resolveContainerAt(ServerLevel level, BlockPos pos) {
        level.getChunk(pos);
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return null;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (state.getBlock() instanceof ChestBlock chestBlock) {
            Container combined = ChestBlock.getContainer(chestBlock, state, level, pos, true);
            if (combined != null) {
                return combined;
            }
        }
        if (blockEntity instanceof Container container) {
            return container;
        }
        if (blockEntity != null) {
            Optional<IItemHandler> capability = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve();
            if (capability.isPresent() && capability.get() instanceof IItemHandlerModifiable modifiable) {
                return new ItemHandlerContainer(modifiable);
            }
        }
        return null;
    }

    private static ChestMenu createChestMenu(int id, Inventory inventory, Container remote) {
        int rows = Math.max(1, Math.min(6, (remote.getContainerSize() + 8) / 9));
        MenuType<ChestMenu> type = switch (rows) {
            case 1 -> MenuType.GENERIC_9x1;
            case 2 -> MenuType.GENERIC_9x2;
            case 4 -> MenuType.GENERIC_9x4;
            case 5 -> MenuType.GENERIC_9x5;
            case 6 -> MenuType.GENERIC_9x6;
            default -> MenuType.GENERIC_9x3;
        };
        return new ChestMenu(type, id, inventory, remote, rows);
    }

    private static boolean canSpend(ServerPlayer player, double amount) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        if (Chakra.pathway(player).getAmount() <= amount) {
            Chakra.pathway(player).warningDisplay();
            return false;
        }
        return true;
    }

    private static void startIntangible(ServerPlayer player) {
        double chakraUsage = ObitoMangekyoHelmetItem.getIntangibleChakraUsage(player);
        if (!player.isCreative() && Chakra.pathway(player).getAmount() < chakraUsage) {
            Chakra.pathway(player).warningDisplay();
            return;
        }
        ProcedureUtils.purgeHarmfulEffects(player);
        setIntangible(player, true);
        setTimer(player, Math.max(1.0D, timer(player)));
        player.displayClientMessage(Component.translatable("chattext.intangible").append("true"), true);
    }

    private static void keepIntangible(ServerPlayer player) {
        setIntangible(player, true);
        setTimer(player, timer(player) + 1.0D);
    }

    private static void stopIntangible(ServerPlayer player) {
        setIntangible(player, false);
        setTimer(player, 0.0D);
        player.displayClientMessage(Component.translatable("chattext.intangible").append("false"), true);
    }

    private static void startTeleport(ServerPlayer player) {
        double chakraUsage = ObitoMangekyoHelmetItem.getTeleportChakraUsage(player);
        if (!player.isCreative() && Chakra.pathway(player).getAmount() < chakraUsage) {
            Chakra.pathway(player).warningDisplay();
            setTimer(player, 0.0D);
            return;
        }
        setTeleport(player, true);
        setTimer(player, Math.max(1.0D, timer(player)));
        spawnTeleportFeedback(player);
    }

    private static void releaseTeleport(ServerPlayer player) {
        HitResult hit = ProcedureUtils.objectEntityLookingAt(player, TELEPORT_RANGE, 0.0D, true, false, target -> target != player);
        if (isSelfHit(player, hit)) {
            transferEntityTarget(player, player);
        } else if (hit instanceof EntityHitResult entityHit) {
            releaseEntityTarget(player, entityHit.getEntity(), entityHit.getLocation());
        } else if (hit instanceof BlockHitResult blockHit) {
            releaseBlockTarget(player, blockHit);
        } else {
            player.displayClientMessage(Component.literal("Kamui found no target."), true);
        }
        clearTeleport(player);
        setTimer(player, 0.0D);
    }

    private static boolean isSelfHit(ServerPlayer player, HitResult hit) {
        return hit.getType() != HitResult.Type.MISS
                && BlockPos.containing(hit.getLocation()).equals(player.blockPosition());
    }

    private static void releaseEntityTarget(ServerPlayer player, Entity target, Vec3 hitLocation) {
        double transferProgress = transferProgress(player, target, hitLocation);
        if (target != player && transferProgress > 0.0D && transferProgress <= COMPLETE_TRANSFER_THRESHOLD) {
            if (target instanceof LivingEntity livingTarget) {
                float damage = (float)(transferProgress * livingTarget.getMaxHealth());
                livingTarget.hurt(ModDamageTypes.ninjutsu(player.level(), player, player), damage);
                player.displayClientMessage(Component.literal("Kamui partially damaged "
                        + target.getDisplayName().getString()
                        + " (" + oneDecimal(damage) + ")."), true);
            } else {
                player.displayClientMessage(Component.literal("Kamui charge was too low to transfer this target."), true);
            }
            return;
        }
        transferEntityTarget(player, target);
    }

    private static boolean transferEntityTarget(ServerPlayer player, Entity target) {
        if (!KamuiDimension.toggleEntity(target)) {
            player.displayClientMessage(Component.literal("Kamui teleport failed."), true);
            return false;
        }
        if (target != player) {
            player.displayClientMessage(Component.literal("Kamui transferred " + target.getDisplayName().getString()), true);
        }
        return true;
    }

    private static void releaseBlockTarget(ServerPlayer player, BlockHitResult hit) {
        if (hit.getType() != HitResult.Type.BLOCK || !(player.level() instanceof ServerLevel level)) {
            player.displayClientMessage(Component.literal("Kamui found no block target."), true);
            return;
        }
        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.getDestroySpeed(level, pos) < 0.0F) {
            player.displayClientMessage(Component.literal("Kamui cannot transfer that block."), true);
            return;
        }
        if (KamuiDimension.isKamui(level)) {
            int moved = KamuiDimension.transferBlockDropsToReturn(player, pos);
            player.displayClientMessage(Component.literal("Kamui returned block drops: " + moved), true);
        } else {
            int moved = KamuiDimension.transferBlockDropsToKamui(level, pos);
            player.displayClientMessage(Component.literal("Kamui transferred block drops: " + moved), true);
        }
    }

    private static double transferProgress(ServerPlayer player, Entity target, Vec3 hitLocation) {
        double edge = averageEdgeLength(target);
        double distance = Math.max(player.position().distanceTo(hitLocation), 0.01D);
        double levelFactor = Math.max(2.01D - PlayerTracker.getNinjaLevel(player) / 300.1D, 0.01D);
        return timer(player) / (distance * edge * levelFactor);
    }

    private static double averageEdgeLength(Entity target) {
        return Math.max((target.getBoundingBox().getXsize()
                + target.getBoundingBox().getYsize()
                + target.getBoundingBox().getZsize()) / 3.0D, 0.01D);
    }

    private static double oneDecimal(float value) {
        return Math.round(value * 10.0F) / 10.0D;
    }

    private static boolean maintainGrab(ServerPlayer player, boolean allowNewTarget) {
        GrabState state = ACTIVE_GRABS.get(player.getUUID());
        if (state == null) {
            if (!allowNewTarget) {
                setGrab(player, false);
                return false;
            }
            Entity target = findGrabTarget(player);
            if (target == null) {
                setGrab(player, false);
                return false;
            }
            state = new GrabState(target.getUUID(), target.isNoGravity());
            ACTIVE_GRABS.put(player.getUUID(), state);
            player.displayClientMessage(Component.literal("Kamui grabbed " + target.getDisplayName().getString()), true);
        }

        Entity grabbed = findEntity(player, state.entityId());
        if (grabbed == null || !grabbed.isAlive() || grabbed.level() != player.level()) {
            stopGrab(player);
            return false;
        }
        moveGrabbedEntity(player, grabbed);
        setGrab(player, true);
        return true;
    }

    private static Entity findGrabTarget(ServerPlayer player) {
        HitResult hit = ProcedureUtils.objectEntityLookingAt(player, GRAB_RANGE, 0.0D, true, false,
                target -> target != player && target.isAlive() && !target.isSpectator());
        return hit instanceof EntityHitResult entityHit ? entityHit.getEntity() : null;
    }

    private static Entity findEntity(ServerPlayer player, UUID entityId) {
        for (ServerLevel level : player.server.getAllLevels()) {
            Entity entity = level.getEntity(entityId);
            if (entity != null) {
                return entity;
            }
        }
        return player.server.getPlayerList().getPlayer(entityId);
    }

    private static void moveGrabbedEntity(ServerPlayer player, Entity grabbed) {
        if (grabbed instanceof ItemEntity || grabbed instanceof ExperienceOrb) {
            grabbed.moveTo(player.getX(), player.getY(), player.getZ(), grabbed.getYRot(), grabbed.getXRot());
            grabbed.setDeltaMovement(Vec3.ZERO);
            return;
        }

        if (grabbed instanceof EarthBlocksEntity) {
            Vec3 point = grabPoint(player, 5.0D);
            if (grabbed.position().distanceTo(point) > 2.0D) {
                grabbed.setNoGravity(true);
                ProcedureUtils.pullEntity(point, grabbed, 2.5F / (float)averageEdgeLength(grabbed));
            } else {
                grabbed.moveTo(point.x(), point.y() - 0.5D, point.z(), grabbed.getYRot(), grabbed.getXRot());
                grabbed.setDeltaMovement(Vec3.ZERO);
            }
            grabbed.hasImpulse = true;
            return;
        }

        Vec3 point = grabPoint(player, 3.0D);
        grabbed.setNoGravity(true);
        grabbed.moveTo(point.x(), point.y() - grabbed.getBbHeight() * 0.5D, point.z(), grabbed.getYRot(), grabbed.getXRot());
        grabbed.setDeltaMovement(Vec3.ZERO);
        grabbed.hasImpulse = true;
    }

    private static Vec3 grabPoint(ServerPlayer player, double range) {
        BlockHitResult hit = ProcedureUtils.raytraceBlocks(player, range);
        return hit.getType() == HitResult.Type.MISS
                ? player.getEyePosition().add(player.getLookAngle().scale(range))
                : hit.getLocation();
    }

    private static void stopGrab(ServerPlayer player) {
        GrabState state = ACTIVE_GRABS.remove(player.getUUID());
        if (state != null) {
            Entity grabbed = findEntity(player, state.entityId());
            if (grabbed != null && grabbed.isAlive()) {
                grabbed.setNoGravity(state.originalNoGravity());
            }
        }
        if (isGrabbing(player)) {
            setGrab(player, false);
        }
    }

    private static void serverTick(ServerPlayer player) {
        if (!hasKamuiHelmet(player)) {
            clearAll(player);
            ObitoMangekyoHelmetItem.revokeKamuiFlightIfNeeded(player);
            return;
        }
        if (!ObitoMangekyoHelmetItem.canUseKamui(player)) {
            clearAll(player);
            return;
        }
        if (isGrabbing(player)) {
            if (!hasObitoHelmet(player) || !KamuiDimension.isKamui(player.level())) {
                stopGrab(player);
            } else {
                maintainGrab(player, false);
            }
        }
        if (isIntangible(player)) {
            double chakraUsage = ObitoMangekyoHelmetItem.getIntangibleChakraUsage(player);
            if (!player.isCreative() && !Chakra.pathway(player).consume(chakraUsage)) {
                stopIntangible(player);
                return;
            }
            keepIntangible(player);
        }
        if (isTeleporting(player)) {
            double chakraUsage = ObitoMangekyoHelmetItem.getTeleportChakraUsage(player);
            if (!player.isCreative() && !Chakra.pathway(player).consume(chakraUsage)) {
                clearTeleport(player);
                setTimer(player, 0.0D);
                return;
            }
            setTimer(player, timer(player) + 1.0D);
            if ((int) timer(player) % 10 == 1) {
                spawnTeleportFeedback(player);
            }
        }
    }

    private static void spawnTeleportFeedback(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        HitResult hit = ProcedureUtils.objectEntityLookingAt(player, TELEPORT_RANGE, 0.0D, true, false, target -> target != player);
        Vec3 pos = hit.getType() == HitResult.Type.MISS
                ? player.getEyePosition().add(player.getLookAngle().scale(6.0D))
                : hit.getLocation();
        ParticleOptions options = ModParticleTypes.options(NarutoParticleKind.PORTAL_SPIRAL, 5, 0x20000000, 30);
        level.sendParticles(options, pos.x(), pos.y(), pos.z(), PORTAL_PARTICLE_COUNT, 0.0D, 0.0D, 0.0D, 0.0D);
        if ((int) timer(player) % 60 == 1) {
            level.playSound(null, pos.x(), pos.y(), pos.z(), ModSounds.SOUND_KAMUISFX.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        }
    }

    private static void setIntangible(ServerPlayer player, boolean intangible) {
        player.noPhysics = intangible;
        player.invulnerableTime = intangible ? Math.max(player.invulnerableTime, 2) : player.invulnerableTime;
        ProcedureSync.EntityNBTTag.setAndSync(player, KAMUI_INTANGIBLE_TAG, intangible);
        ProcedureSync.EntityNBTTag.setAndSync(player, NarutomodModVariables.NO_CLIP_FLAG, intangible);
    }

    private static void setTeleport(ServerPlayer player, boolean teleport) {
        ProcedureSync.EntityNBTTag.setAndSync(player, KAMUI_TELEPORT_TAG, teleport);
    }

    private static void setTimer(ServerPlayer player, double value) {
        ProcedureSync.EntityNBTTag.setAndSync(player, KAMUI_TIMER_TAG, value);
    }

    private static void setGrab(ServerPlayer player, boolean grabbing) {
        ProcedureSync.EntityNBTTag.setAndSync(player, KAMUI_GRAB_TAG, grabbing);
    }

    private static void clearTeleport(ServerPlayer player) {
        setTeleport(player, false);
    }

    private static void clearAll(ServerPlayer player) {
        if (isIntangible(player)) {
            setIntangible(player, false);
        }
        if (isTeleporting(player)) {
            clearTeleport(player);
        }
        if (timer(player) != 0.0D) {
            setTimer(player, 0.0D);
        }
        player.getPersistentData().remove(KAMUI_JUTSU3_HELD_TAG);
        stopGrab(player);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
            serverTick(player);
        }
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (isIntangible(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    private static final class ItemHandlerContainer implements Container {
        private final IItemHandlerModifiable handler;

        private ItemHandlerContainer(IItemHandlerModifiable handler) {
            this.handler = handler;
        }

        private boolean valid(int slot) {
            return slot >= 0 && slot < this.handler.getSlots();
        }

        @Override
        public int getContainerSize() {
            return this.handler.getSlots();
        }

        @Override
        public boolean isEmpty() {
            for (int slot = 0; slot < this.handler.getSlots(); slot++) {
                if (!this.handler.getStackInSlot(slot).isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            return valid(slot) ? this.handler.getStackInSlot(slot) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            return valid(slot) ? this.handler.extractItem(slot, amount, false) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return valid(slot) ? this.handler.extractItem(slot, this.handler.getSlotLimit(slot), false) : ItemStack.EMPTY;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            if (valid(slot)) {
                this.handler.setStackInSlot(slot, stack);
            }
        }

        @Override
        public int getMaxStackSize() {
            return 64;
        }

        @Override
        public void setChanged() {
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return valid(slot) && this.handler.isItemValid(slot, stack);
        }

        @Override
        public void clearContent() {
            for (int slot = 0; slot < this.handler.getSlots(); slot++) {
                this.handler.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    }

    private static final class RemoteContainerProxy implements Container {
        private final ResolvedKamuiContainer target;
        private final UUID owner;
        private final int exposedSlots;

        private RemoteContainerProxy(ResolvedKamuiContainer target, UUID owner) {
            this.target = target;
            this.owner = owner;
            int rows = Math.max(1, Math.min(6, (target.container().getContainerSize() + 8) / 9));
            this.exposedSlots = rows * 9;
            this.target.keepLoaded();
        }

        private Container delegate() {
            Container fresh = this.target.resolveFresh();
            return fresh != null ? fresh : this.target.container();
        }

        @Override
        public int getContainerSize() {
            return this.exposedSlots;
        }

        @Override
        public boolean isEmpty() {
            Container delegate = delegate();
            if (delegate == null) {
                return true;
            }
            for (int slot = 0; slot < Math.min(this.exposedSlots, delegate.getContainerSize()); slot++) {
                if (!delegate.getItem(slot).isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            Container delegate = delegate();
            return delegate != null && slot >= 0 && slot < delegate.getContainerSize()
                    ? delegate.getItem(slot)
                    : ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            Container delegate = delegate();
            if (delegate == null || slot < 0 || slot >= delegate.getContainerSize()) {
                return ItemStack.EMPTY;
            }
            ItemStack out = delegate.removeItem(slot, amount);
            if (!out.isEmpty()) {
                this.target.markChanged();
            }
            return out;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            Container delegate = delegate();
            if (delegate == null || slot < 0 || slot >= delegate.getContainerSize()) {
                return ItemStack.EMPTY;
            }
            ItemStack out = delegate.removeItemNoUpdate(slot);
            if (!out.isEmpty()) {
                this.target.markChanged();
            }
            return out;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            Container delegate = delegate();
            if (delegate != null && slot >= 0 && slot < delegate.getContainerSize()) {
                delegate.setItem(slot, stack);
                this.target.markChanged();
            }
        }

        @Override
        public int getMaxStackSize() {
            Container delegate = delegate();
            return delegate != null ? delegate.getMaxStackSize() : 64;
        }

        @Override
        public void setChanged() {
            this.target.markChanged();
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            Container delegate = delegate();
            return delegate != null && slot >= 0 && slot < delegate.getContainerSize() && delegate.canPlaceItem(slot, stack);
        }

        @Override
        public void clearContent() {
            Container delegate = delegate();
            if (delegate != null) {
                delegate.clearContent();
                this.target.markChanged();
            }
        }

        @Override
        public void startOpen(Player player) {
            this.target.keepLoaded();
        }

        @Override
        public void stopOpen(Player player) {
            this.target.keepLoaded();
        }

        @Override
        public boolean stillValid(Player player) {
            this.target.keepLoaded();
            return player != null
                    && player.isAlive()
                    && player.getPersistentData().getBoolean(CHEST_LINKED_TAG)
                    && this.owner.equals(player.getUUID());
        }
    }

    private record ResolvedKamuiContainer(ServerLevel level, BlockPos pos, Container container) {
        void keepLoaded() {
            this.level.getChunk(this.pos);
        }

        Container resolveFresh() {
            this.keepLoaded();
            return ObitoKamuiHandler.resolveContainerAt(this.level, this.pos);
        }

        void markChanged() {
            this.keepLoaded();
            Container fresh = this.resolveFresh();
            if (fresh != null) {
                fresh.setChanged();
            } else {
                this.container.setChanged();
            }
            BlockState state = this.level.getBlockState(this.pos);
            BlockEntity blockEntity = this.level.getBlockEntity(this.pos);
            if (blockEntity != null) {
                blockEntity.setChanged();
            }
            this.level.sendBlockUpdated(this.pos, state, state, 2);
        }
    }

    private record GrabState(UUID entityId, boolean originalNoGravity) {
    }
}
