package net.narutomod.item;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.narutomod.Chakra;
import net.narutomod.NarutomodMod;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModBlocks;
import net.narutomod.registry.ModEffects;
import net.narutomod.registry.ModItems;
import net.narutomod.registry.ModSounds;

/**
 * Amaterasu (black-flame) activation for the Mangekyo Sharingan and Eternal Mangekyo helmets,
 * wired onto Special Jutsu Key 1. Faithful to 1.12.2 {@code ProcedureAmaterasu} (+ PlaceBlock +
 * ExtinguishEntities): while the key is held the wielder continuously paints Amaterasu by looking
 * (igniting the looked-at living entity or placing a black-flame block); sneak-release extinguishes
 * fire/flame blocks and the flame DOT in a 31-block cube; a normal release applies a Weakness/Nausea
 * self-penalty proportional to how long it was used. The flame DOT effect and block already exist
 * ({@code AmaterasuFlameEffect}, {@code AMATERASUBLOCK}); this only restores the missing activation.
 *
 * Special Jutsu Key 1 is edge-triggered by the client (one press + one release), so the per-tick
 * ignite of the original (which polled the key each tick) is reproduced from a server tick while the
 * {@code amaterasu_active} flag is set.
 */
@Mod.EventBusSubscriber(modid = NarutomodMod.MODID)
public final class AmaterasuHandler {
    private static final String ACTIVE_TAG = "amaterasu_active";
    private static final String COOLDOWN_TAG = "amaterasu_cd";
    private static final String LAST_IGNITE_TAG = "amaterasu_last_ignite";
    private static final String BLINDED_TAG = "sharingan_blinded";
    private static final double AMATERASU_CHAKRA_USAGE = 100.0D; // 1.12.2 ItemMangekyoSharingan.AMATERASU_CHAKRA_USAGE
    private static final double LOOK_RANGE = 30.0D;
    private static final double EXTINGUISH_RANGE = 50.0D;
    private static final int EXTINGUISH_RADIUS = 15;
    private static final int FLAME_DURATION = 10000;

    private AmaterasuHandler() {
    }

    public static boolean handleSpecialJutsuKey(ServerPlayer player, int key, boolean pressed) {
        if (key != 1 || player.isSpectator() || !player.isAlive()) {
            return false;
        }
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!isAmaterasuHead(head)) {
            return false;
        }
        CompoundTag headTag = head.getTag();
        if (headTag != null && headTag.getBoolean(BLINDED_TAG)) {
            setActive(player, false);
            return true;
        }
        if (pressed) {
            handlePress(player);
        } else {
            handleRelease(player);
        }
        return true;
    }

    private static void handlePress(ServerPlayer player) {
        double usage = getAmaterasuChakraUsage(player);
        if (!player.isCreative() && Chakra.pathway(player).getAmount() < usage * 1.25D) {
            return;
        }
        if (!isActive(player)) {
            if (!player.isShiftKeyDown()) {
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        ModSounds.SOUND_AMATERASU2.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
            }
            double cdModifier = ProcedureUtils.getCooldownModifier(player);
            player.getPersistentData().putDouble(COOLDOWN_TAG, player.level().getGameTime() + cdModifier * 300.0D);
            if (!player.isCreative()) {
                Chakra.pathway(player).consume(usage);
            }
            setActive(player, true);
        }
        igniteTick(player, usage);
    }

    public static void tick(ServerPlayer player) {
        if (!isActive(player)) {
            return;
        }
        if (!isAmaterasuHead(player.getItemBySlot(EquipmentSlot.HEAD)) || player.isSpectator() || !player.isAlive()) {
            setActive(player, false);
            return;
        }
        if (player.getPersistentData().getLong(LAST_IGNITE_TAG) == player.level().getGameTime()) {
            return; // already ignited this tick (from the key-press event)
        }
        double usage = getAmaterasuChakraUsage(player);
        if (!player.isCreative() && Chakra.pathway(player).getAmount() < usage * 1.25D) {
            return; // out of chakra: pause this tick but stay active (matches 1.12.2)
        }
        igniteTick(player, usage);
    }

    private static void igniteTick(ServerPlayer player, double usage) {
        long now = player.level().getGameTime();
        double cdModifier = ProcedureUtils.getCooldownModifier(player);
        double cooldown = player.getPersistentData().getDouble(COOLDOWN_TAG);
        if (cooldown - now < 2000.0D) {
            cooldown += cdModifier * 10.0D;
            player.getPersistentData().putDouble(COOLDOWN_TAG, cooldown);
        }
        if (!player.isCreative()) {
            Chakra.pathway(player).consume(usage * 0.25D);
        }
        player.getPersistentData().putLong(LAST_IGNITE_TAG, now);

        HitResult hit = ProcedureUtils.objectEntityLookingAt(player, LOOK_RANGE);
        if (hit.getType() == HitResult.Type.ENTITY) {
            Entity target = ((EntityHitResult) hit).getEntity();
            if (target instanceof LivingEntity living) {
                int amplifier = player.experienceLevel / 30;
                living.addEffect(new MobEffectInstance(ModEffects.AMATERASUFLAME.get(), FLAME_DURATION, amplifier, false, false));
            }
        } else if (hit.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hit;
            BlockPos place = blockHit.getBlockPos().relative(blockHit.getDirection());
            if (player.level().getBlockState(place).isAir()) {
                player.level().setBlock(place, ModBlocks.AMATERASUBLOCK.get().defaultBlockState(), 3);
            }
        }
    }

    private static void handleRelease(ServerPlayer player) {
        if (player.isShiftKeyDown()) {
            extinguish(player);
        } else if (isActive(player) && !player.isCreative()) {
            double cooldown = player.getPersistentData().getDouble(COOLDOWN_TAG);
            int duration = (int) ((cooldown - player.level().getGameTime()) * 0.5D);
            if (duration > 0) {
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 2));
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, duration, 0));
            }
        }
        setActive(player, false);
    }

    private static void extinguish(ServerPlayer player) {
        Level level = player.level();
        HitResult hit = ProcedureUtils.objectEntityLookingAt(player, EXTINGUISH_RANGE);
        BlockPos center = hit.getType() == HitResult.Type.BLOCK
                ? ((BlockHitResult) hit).getBlockPos()
                : BlockPos.containing(hit.getLocation());
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -EXTINGUISH_RADIUS; dx <= EXTINGUISH_RADIUS; dx++) {
            for (int dy = -EXTINGUISH_RADIUS; dy <= EXTINGUISH_RADIUS; dy++) {
                for (int dz = -EXTINGUISH_RADIUS; dz <= EXTINGUISH_RADIUS; dz++) {
                    pos.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    BlockState state = level.getBlockState(pos);
                    if (state.getBlock() instanceof BaseFireBlock || state.is(ModBlocks.AMATERASUBLOCK.get())) {
                        level.removeBlock(pos, false);
                    }
                }
            }
        }
        AABB box = new AABB(center.getX(), center.getY(), center.getZ(),
                center.getX(), center.getY(), center.getZ()).inflate(EXTINGUISH_RADIUS);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, box)) {
            living.removeEffect(ModEffects.AMATERASUFLAME.get());
            living.clearFire();
        }
    }

    public static boolean isAmaterasuHead(ItemStack stack) {
        return stack.is(ModItems.MANGEKYOSHARINGANHELMET.get())
                || stack.is(ModItems.MANGEKYOSHARINGANETERNALHELMET.get());
    }

    private static double getAmaterasuChakraUsage(ServerPlayer player) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!isAmaterasuHead(head)) {
            return Double.MAX_VALUE * 0.001D;
        }
        return player.isCreative() || ProcedureUtils.isOriginalOwner(player, head)
                ? AMATERASU_CHAKRA_USAGE
                : AMATERASU_CHAKRA_USAGE * 2.0D;
    }

    private static boolean isActive(ServerPlayer player) {
        return player.getPersistentData().getBoolean(ACTIVE_TAG);
    }

    private static void setActive(ServerPlayer player, boolean active) {
        player.getPersistentData().putBoolean(ACTIVE_TAG, active);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
            tick(player);
        }
    }
}
