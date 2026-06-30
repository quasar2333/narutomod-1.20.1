package net.narutomod.item;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.narutomod.Chakra;
import net.narutomod.NarutomodMod;
import net.narutomod.NarutomodModVariables;
import net.narutomod.entity.ChibakuTenseiBallEntity;
import net.narutomod.entity.EarthBlocksEntity;
import net.narutomod.entity.GiantDog2hEntity;
import net.narutomod.entity.HakkeshoKeitenEntity;
import net.narutomod.entity.KingOfHellEntity;
import net.narutomod.entity.PretaShieldEntity;
import net.narutomod.entity.SandShieldEntity;
import net.narutomod.event.SpecialEvent;
import net.narutomod.particle.NarutoParticleKind;
import net.narutomod.procedure.ProcedureGravityPower;
import net.narutomod.procedure.ProcedureMeteorStrike;
import net.narutomod.procedure.ProcedurePullAndHold;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModDamageTypes;
import net.narutomod.registry.ModEffects;
import net.narutomod.registry.ModItems;
import net.narutomod.registry.ModParticleTypes;
import net.narutomod.registry.ModSounds;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = NarutomodMod.MODID)
public final class RinneganSpecialJutsuHandler {
    public static final String WHICH_PATH_TAG = "which_path";
    public static final String CHIBAKU_COOLDOWN_TAG = "chibakutenseicd";
    public static final String SUMMONED_ANIMAL_ID_TAG = "SummonedAnimal_id";
    public static final String KING_OF_HELL_ID_TAG = "KoH_id";
    public static final String ASURA_TICKS_USED_TAG = "ticks_used";
    public static final double CHIBAKU_TENSEI_CHAKRA_USAGE = 5000.0D;
    public static final double TENGAISHINSEI_CHAKRA_USAGE = 5000.0D;
    public static final double ANIMAL_PATH_CHAKRA_USAGE = 200.0D;
    public static final double PRETA_PATH_CHAKRA_USAGE = 10.0D;
    public static final double NARAKA_PATH_CHAKRA_USAGE = 100.0D;
    public static final String SHINRATENSEI_COOLDOWN_TAG = "shinratenseicd";
    public static final String SHINRATENSEI_POWER_TAG = "shinratensei_power";
    public static final String SHINRATENSEI_WAS_PRESSED_TAG = "was_pressed";
    public static final String BANSHOTENIN_COOLDOWN_TAG = "BanshoTenin_cooldown";
    public static final double SHINRA_TENSEI_CHAKRA_USAGE = 10.0D;
    public static final double BANSHO_TENIN_CHAKRA_USAGE = 0.5D;
    public static final int CHIBAKU_TENSEI_COOLDOWN_TICKS = 6000;
    public static final int DEVA_PATH = 0;
    public static final int ASURA_PATH = 1;
    public static final int ANIMAL_PATH = 2;
    public static final int PRETA_PATH = 3;
    public static final int NARAKA_PATH = 4;
    public static final int OUTER_PATH = 5;
    private static final double SHINRA_TENSEI_START_POWER = 10.0D;
    private static final double SHINRA_TENSEI_MIN_CAST_POWER = 5.0D;
    private static final double SHINRA_TENSEI_MAX_POWER = 100.0D;
    private static final double SHINRA_TENSEI_POWER_STEP = 0.1D;
    private static final double SHINRA_TENSEI_DAMAGE_MULTIPLIER = 1.8D;
    private static final int SHINRA_TENSEI_INVULNERABLE_TICKS = 60;
    private static final int BANSHO_TENIN_BLOCK_SIZE = 5;
    private static final int MAX_PATH = OUTER_PATH;
    private static final Map<Player, ProcedurePullAndHold> BANSHO_TENIN_PULLS = new WeakHashMap<>();

    private RinneganSpecialJutsuHandler() {
    }

    public static boolean handleSpecialJutsuKey(ServerPlayer player, int key, boolean pressed) {
        if ((key < 1 || key > 3) || player.isSpectator() || !player.isAlive()) {
            return false;
        }
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!isRinneganLikeHead(head)) {
            return false;
        }

        if (key == 1) {
            return handleShinraTensei(player, pressed);
        }
        if (key == 3) {
            return handleBanShoTenin(player, pressed);
        }
        return handlePathKey(player, head, pressed);
    }

    private static boolean handlePathKey(ServerPlayer player, ItemStack head, boolean pressed) {
        NarutomodModVariables.get(player).putBoolean(NarutomodModVariables.JUTSU_KEY_2_PRESSED, pressed);
        NarutomodModVariables.sync(player);
        if (pressed) {
            return true;
        }

        int path = getRinneganPath(head);
        triggerPath(player, path, player.isShiftKeyDown(), true);
        return true;
    }

    public static boolean handlePowerIncreaseKey(ServerPlayer player, boolean pressed) {
        if (player.isSpectator() || !player.isAlive() || !player.level().isLoaded(player.blockPosition())) {
            return false;
        }
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!isRinneganLikeHead(head)) {
            return false;
        }
        if (!pressed) {
            int path = cycleRinneganPath(head);
            player.displayClientMessage(pathDisplayName(path), true);
        }
        return true;
    }

    public static boolean triggerDevaPath(ServerPlayer player, boolean meteor, boolean showMessages) {
        return triggerPath(player, DEVA_PATH, meteor, showMessages);
    }

    public static boolean triggerPath(ServerPlayer player, int path, boolean meteor, boolean showMessages) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        return switch (path) {
            case DEVA_PATH -> meteor
                    ? triggerTengaishinsei(serverLevel, player, showMessages)
                    : triggerChibakuTensei(serverLevel, player, showMessages);
            case ANIMAL_PATH -> triggerAnimalPath(serverLevel, player, showMessages);
            case PRETA_PATH -> triggerPretaPath(serverLevel, player, showMessages);
            case NARAKA_PATH -> triggerNarakaPath(serverLevel, player, showMessages);
            case ASURA_PATH -> triggerAsuraPath(player, showMessages);
            case OUTER_PATH -> triggerOuterPath(player, showMessages);
            default -> unselectedPath(player, showMessages);
        };
    }

    private static boolean handleShinraTensei(ServerPlayer player, boolean pressed) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        if (pressed && !canUseShinraTensei(player, level.getGameTime())) {
            return true;
        }
        if (pressed) {
            chargeShinraTensei(player);
            return true;
        }

        CompoundTag tag = player.getPersistentData();
        boolean wasPressed = tag.getBoolean(SHINRATENSEI_WAS_PRESSED_TAG);
        double power = tag.getDouble(SHINRATENSEI_POWER_TAG);
        tag.putBoolean(SHINRATENSEI_WAS_PRESSED_TAG, false);
        tag.putDouble(SHINRATENSEI_POWER_TAG, 0.0D);
        if (!wasPressed || power < SHINRA_TENSEI_MIN_CAST_POWER) {
            return true;
        }

        double chakraUsage = rinneganTechniqueCost(player, SHINRA_TENSEI_CHAKRA_USAGE);
        if (player.isCreative() || Chakra.pathway(player).consume(power * chakraUsage)) {
            castShinraTensei(level, player, power);
            if (!player.isCreative()) {
                long cooldownUntil = level.getGameTime() + (long)(power * 10.0D * Chakra.getChakraModifier(player));
                tag.putLong(SHINRATENSEI_COOLDOWN_TAG, cooldownUntil);
            }
        }
        return true;
    }

    private static boolean canUseShinraTensei(ServerPlayer player, long now) {
        if (player.isCreative()) {
            return true;
        }
        long cooldownUntil = player.getPersistentData().getLong(SHINRATENSEI_COOLDOWN_TAG);
        if (now < cooldownUntil - 400L || now > cooldownUntil + 100L) {
            return true;
        }
        player.displayClientMessage(Component.literal("Cooldown: "
                + String.format("%.1f", (cooldownUntil - now + 100L) / 20.0D) + "s"), true);
        return false;
    }

    private static void chargeShinraTensei(ServerPlayer player) {
        CompoundTag tag = player.getPersistentData();
        double chakraUsage = rinneganTechniqueCost(player, SHINRA_TENSEI_CHAKRA_USAGE);
        double power = tag.getBoolean(SHINRATENSEI_WAS_PRESSED_TAG)
                ? tag.getDouble(SHINRATENSEI_POWER_TAG)
                : SHINRA_TENSEI_START_POWER;
        double maxPower = maxShinraTenseiPower(player, chakraUsage);
        if (power + SHINRA_TENSEI_POWER_STEP < maxPower && power < SHINRA_TENSEI_MAX_POWER) {
            power += SHINRA_TENSEI_POWER_STEP;
        }
        power = Mth.clamp(power, SHINRA_TENSEI_MIN_CAST_POWER, SHINRA_TENSEI_MAX_POWER);
        tag.putBoolean(SHINRATENSEI_WAS_PRESSED_TAG, true);
        tag.putDouble(SHINRATENSEI_POWER_TAG, power);
        player.displayClientMessage(Component.literal("Power " + (int)power), true);
        player.addEffect(new MobEffectInstance(ModEffects.FLIGHT.get(), 200, 1, false, false));
    }

    private static double maxShinraTenseiPower(ServerPlayer player, double chakraUsage) {
        if (player.isCreative()) {
            return SHINRA_TENSEI_MAX_POWER;
        }
        return Math.min(SHINRA_TENSEI_MAX_POWER, Chakra.pathway(player).getAmount() / Math.max(chakraUsage, 0.001D));
    }

    private static void castShinraTensei(ServerLevel level, ServerPlayer player, double power) {
        if (launchGrabbedEarthBlocks(player, power)) {
            return;
        }
        player.invulnerableTime = Math.max(player.invulnerableTime, SHINRA_TENSEI_INVULNERABLE_TICKS);
        level.sendParticles(ModParticleTypes.options(NarutoParticleKind.SMOKE_COLORED, 0x10FFFFFF, 30, 0, 0, -1, 4),
                player.getX(), player.getY() + 1.4D, player.getZ(),
                1000, 1.0D, 0.0D, 1.0D, 1.0D);
        if (power >= 20.0D) {
            SpecialEvent.setSphericalExplosionEvent(level,
                    Mth.floor(player.getX()), Mth.floor(player.getY()) + 2, Mth.floor(player.getZ()),
                    (int)(power * power / 200.0D), player);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.SOUND_SHINRATENSEI.get(), SoundSource.NEUTRAL, 5.0F, 1.0F);
        } else {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.SOUND_BANSHOTENIN.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        }
        player.displayClientMessage(Component.literal("Power " + (int)power), true);
        damageAndKnockbackShinraTargets(player, power);
        ProcedureUtils.purgeHarmfulEffects(player);
        player.clearFire();
    }

    private static void damageAndKnockbackShinraTargets(ServerPlayer player, double power) {
        AABB box = player.getBoundingBox().inflate(power);
        for (Entity target : player.level().getEntities(player, box, target -> isShinraTarget(player, target, power))) {
            target.setNoGravity(false);
            ProcedureUtils.pushEntity(player, target, power, 2.0F);
            if (target instanceof LivingEntity living) {
                living.invulnerableTime = 0;
                living.hurt(ModDamageTypes.shinratensei(player.level(), player, player),
                        (float)(power * SHINRA_TENSEI_DAMAGE_MULTIPLIER));
            }
        }
    }

    private static boolean isShinraTarget(ServerPlayer player, Entity target, double power) {
        return target.isAlive()
                && !target.isSpectator()
                && target != player
                && target.getRootVehicle() != player.getRootVehicle()
                && target.distanceToSqr(player) <= power * power;
    }

    private static boolean launchGrabbedEarthBlocks(ServerPlayer player, double power) {
        List<EarthBlocksEntity> earthBlocks = getBanshoGrabbedEarthBlocks(player);
        if (earthBlocks.isEmpty()) {
            return false;
        }
        HitResult hit = ProcedureUtils.objectEntityLookingAt(player, 40.0D, 3.0D, true, false,
                target -> !earthBlocks.contains(target));
        if (!(hit instanceof EntityHitResult entityHit) || entityHit.getEntity() == null) {
            return false;
        }
        Entity target = entityHit.getEntity();
        Vec3 targetCenter = target instanceof LivingEntity living ? living.getEyePosition() : target.getBoundingBox().getCenter();
        for (EarthBlocksEntity earthBlock : earthBlocks) {
            if (!earthBlock.isAlive()) {
                continue;
            }
            earthBlock.setMovementEnabled(true);
            Vec3 source = earthBlock.getBoundingBox().getCenter();
            Vec3 motion = targetCenter.subtract(source);
            if (motion.lengthSqr() > 1.0E-8D) {
                Vec3 launched = motion.normalize().scale(power * 0.1D);
                ProcedureUtils.setVelocity(earthBlock, launched.x(), launched.y(), launched.z());
            }
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.SOUND_BANSHOTENIN.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        resetBanshoTenin(player);
        return true;
    }

    private static boolean handleBanShoTenin(ServerPlayer player, boolean pressed) {
        ProcedurePullAndHold procedure = BANSHO_TENIN_PULLS.computeIfAbsent(player, key -> new ProcedurePullAndHold());
        if (!pressed) {
            boolean hadGrabbed = procedure.execute(false, player, null);
            if (hadGrabbed && !player.isCreative()) {
                player.getPersistentData().putLong(BANSHOTENIN_COOLDOWN_TAG, player.level().getGameTime() + 100L);
            }
            return true;
        }

        long cooldownUntil = player.getPersistentData().getLong(BANSHOTENIN_COOLDOWN_TAG);
        if (!player.isCreative() && cooldownUntil > player.level().getGameTime()) {
            player.displayClientMessage(Component.literal("Ban Sho Tenin cooldown "
                    + String.format("%.1f", (cooldownUntil - player.level().getGameTime()) / 20.0D) + "s"), true);
            return true;
        }

        double chakraUsage = rinneganTechniqueCost(player, BANSHO_TENIN_CHAKRA_USAGE);
        HitResult hit = ProcedureUtils.objectEntityLookingAt(player, 50.0D, 3.0D, true, false,
                RinneganSpecialJutsuHandler::canGrabBanShoTarget);
        if (!procedure.hasGrabbedEntity()) {
            if (hit instanceof EntityHitResult entityHit && canGrabBanShoTarget(entityHit.getEntity())) {
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        ModSounds.SOUND_BANSHOTENIN.get(), SoundSource.NEUTRAL, 5.0F, 1.0F);
                return consumeAndHoldBanSho(player, procedure, entityHit.getEntity(), chakraUsage);
            }
            if (player.isShiftKeyDown() && hit instanceof BlockHitResult blockHit
                    && blockHit.getType() == HitResult.Type.BLOCK) {
                double blockCost = chakraUsage * 50.0D;
                if (!consumeChakra(player, blockCost)) {
                    return true;
                }
                EarthBlocksEntity earthBlocks = ProcedureGravityPower.dislodgeBlocks(
                        player.level(), blockHit.getBlockPos(), BANSHO_TENIN_BLOCK_SIZE, player);
                if (earthBlocks != null) {
                    player.level().playSound(null, blockHit.getBlockPos(),
                            ModSounds.SOUND_ROCKS.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
                    procedure.addEarthBlock(earthBlocks);
                    procedure.execute(true, player, earthBlocks);
                }
                return true;
            }
        }
        if (!procedure.hasGrabbedEntity()) {
            return true;
        }
        return consumeAndHoldBanSho(player, procedure, null, chakraUsage);
    }

    private static boolean consumeAndHoldBanSho(ServerPlayer player, ProcedurePullAndHold procedure,
            Entity target, double chakraUsage) {
        if (!player.isCreative() && !Chakra.pathway(player).consume(chakraUsage)) {
            procedure.reset();
            return true;
        }
        procedure.execute(true, player, target);
        return true;
    }

    private static boolean canGrabBanShoTarget(Entity target) {
        return target != null
                && target.isAlive()
                && !(target instanceof ChibakuTenseiBallEntity)
                && !(target instanceof HakkeshoKeitenEntity)
                && !(target instanceof PretaShieldEntity)
                && !(target instanceof SandShieldEntity)
                && target.getBbHeight() < 24.0F
                && (target instanceof ItemEntity
                    || target instanceof ExperienceOrb
                    || target instanceof EarthBlocksEntity
                    || target instanceof LivingEntity
                    || target.isPickable());
    }

    public static List<EarthBlocksEntity> getBanshoGrabbedEarthBlocks(Entity entity) {
        if (!(entity instanceof Player player)) {
            return List.of();
        }
        ProcedurePullAndHold procedure = BANSHO_TENIN_PULLS.get(player);
        return procedure == null ? List.of() : procedure.getGrabbedEarthBlocks();
    }

    public static void resetBanshoTenin(Player player) {
        ProcedurePullAndHold procedure = BANSHO_TENIN_PULLS.remove(player);
        if (procedure != null) {
            procedure.reset();
        }
    }

    public static boolean maintainAsuraPath(ServerPlayer player) {
        if (player.isSpectator() || !player.isAlive()) {
            return false;
        }
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        boolean hasRinneganHead = isRinneganLikeHead(head);
        boolean active = hasRinneganHead
                && getRinneganPath(head) == ASURA_PATH
                && !player.getMainHandItem().is(ModItems.TENSEIGAN_CHAKRA_MODE.get());
        if (!active) {
            clearInactiveAsuraGear(player, hasRinneganHead);
            return false;
        }

        if (!player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.ASURAPATHARMORBODY.get())) {
            ProcedureUtils.swapItemToSlot(player, EquipmentSlot.CHEST, new ItemStack(ModItems.ASURAPATHARMORBODY.get()));
        }
        if (!player.getOffhandItem().is(ModItems.ASURACANON.get())) {
            ProcedureUtils.swapItemToSlot(player, EquipmentSlot.OFFHAND, new ItemStack(ModItems.ASURACANON.get()));
        }
        tickAsuraBody(player, player.getItemBySlot(EquipmentSlot.CHEST));
        return true;
    }

    public static boolean hasAsuraBodyEquipped(Player player) {
        return player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.ASURAPATHARMORBODY.get());
    }

    public static boolean hasAsuraCannonOffhand(Player player) {
        return player.getOffhandItem().is(ModItems.ASURACANON.get());
    }

    public static boolean isRinneganLikeHead(ItemStack stack) {
        return stack.is(ModItems.RINNEGANHELMET.get()) || stack.is(ModItems.TENSEIGANHELMET.get());
    }

    public static void setRinneganPath(ItemStack stack, int path) {
        stack.getOrCreateTag().putDouble(WHICH_PATH_TAG, path);
    }

    public static int cycleRinneganPath(ItemStack stack) {
        int next = getRinneganPath(stack) + 1;
        if (next > MAX_PATH) {
            next = DEVA_PATH;
        }
        setRinneganPath(stack, next);
        return next;
    }

    public static int getRinneganPath(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(WHICH_PATH_TAG) ? (int)tag.getDouble(WHICH_PATH_TAG) : -1;
    }

    public static Component pathDisplayName(int path) {
        return Component.translatable("chattext.rinnegan.path" + path);
    }

    private static boolean triggerChibakuTensei(ServerLevel level, ServerPlayer player, boolean showMessages) {
        long now = level.getGameTime();
        long cooldownUntil = player.getPersistentData().getLong(CHIBAKU_COOLDOWN_TAG);
        if (!player.isCreative() && cooldownUntil > now) {
            if (showMessages) {
                player.displayClientMessage(Component.literal("Chibaku Tensei cooldown "
                        + String.format("%.1f", (cooldownUntil - now) / 20.0D) + "s"), true);
            }
            return false;
        }
        if (!consumeChakra(player, CHIBAKU_TENSEI_CHAKRA_USAGE)) {
            return false;
        }

        ChibakuTenseiBallEntity ball = ChibakuTenseiBallEntity.spawnFrom(player);
        if (ball == null) {
            refundChakra(player, CHIBAKU_TENSEI_CHAKRA_USAGE);
            if (showMessages) {
                player.displayClientMessage(Component.literal("Could not spawn Chibaku Tensei."), true);
            }
            return false;
        }
        if (!player.isCreative()) {
            player.getPersistentData().putLong(CHIBAKU_COOLDOWN_TAG, now + CHIBAKU_TENSEI_COOLDOWN_TICKS);
        }
        return true;
    }

    private static boolean triggerTengaishinsei(ServerLevel level, ServerPlayer player, boolean showMessages) {
        double chakraCost = ProcedureMeteorStrike.hasReusableSatellite(level, player)
                ? TENGAISHINSEI_CHAKRA_USAGE * 0.2D
                : TENGAISHINSEI_CHAKRA_USAGE;
        if (!consumeChakra(player, chakraCost)) {
            return false;
        }

        ProcedureMeteorStrike.MeteorStrikeResult result = ProcedureMeteorStrike.strike(level, player, meteorTarget(player));
        if (!result.success()) {
            refundChakra(player, chakraCost);
            if (showMessages) {
                player.displayClientMessage(Component.literal("Tengaishinsei failed: " + result.failureReason()), true);
            }
            return false;
        }
        return true;
    }

    private static boolean triggerAnimalPath(ServerLevel level, ServerPlayer player, boolean showMessages) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        int summonedId = head.getOrCreateTag().getInt(SUMMONED_ANIMAL_ID_TAG);
        if (summonedId > 0) {
            head.getOrCreateTag().putInt(SUMMONED_ANIMAL_ID_TAG, 0);
            Entity summoned = level.getEntity(summonedId);
            if (summoned instanceof GiantDog2hEntity dog && dog.isAlive()) {
                dog.dismissWithPoof();
            }
            return true;
        }

        if (!consumeChakra(player, ANIMAL_PATH_CHAKRA_USAGE)) {
            return false;
        }
        player.swing(InteractionHand.MAIN_HAND, true);
        GiantDog2hEntity dog = GiantDog2hEntity.spawnFor(player, GiantDog2hEntity.DEFAULT_MAX_HEALTH);
        if (dog == null) {
            refundChakra(player, ANIMAL_PATH_CHAKRA_USAGE);
            if (showMessages) {
                player.displayClientMessage(Component.literal("Animal Path summon failed."), true);
            }
            return false;
        }
        head.getOrCreateTag().putInt(SUMMONED_ANIMAL_ID_TAG, dog.getId());
        spawnSummonEffects(level, dog.position(), dog.getBbHeight());
        return true;
    }

    private static boolean triggerPretaPath(ServerLevel level, ServerPlayer player, boolean showMessages) {
        if (!consumeChakra(player, PRETA_PATH_CHAKRA_USAGE)) {
            return false;
        }
        player.swing(InteractionHand.MAIN_HAND, true);
        PretaShieldEntity before = PretaShieldEntity.findActive(level, player);
        boolean spawned = PretaShieldEntity.spawnFrom(player);
        PretaShieldEntity after = PretaShieldEntity.findActive(level, player);
        if (!spawned && before == null && after == null) {
            refundChakra(player, PRETA_PATH_CHAKRA_USAGE);
            if (showMessages) {
                player.displayClientMessage(Component.literal("Preta Path shield failed."), true);
            }
            return false;
        }
        return true;
    }

    private static boolean triggerNarakaPath(ServerLevel level, ServerPlayer player, boolean showMessages) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (head.getOrCreateTag().hasUUID(KING_OF_HELL_ID_TAG)) {
            Entity entity = level.getEntity(head.getOrCreateTag().getUUID(KING_OF_HELL_ID_TAG));
            if (entity instanceof KingOfHellEntity king && king.isAlive()) {
                king.dismiss();
            }
            ProcedureUtils.removeUniqueIdTag(head, KING_OF_HELL_ID_TAG);
            return true;
        }

        if (!consumeChakra(player, NARAKA_PATH_CHAKRA_USAGE)) {
            return false;
        }
        player.swing(InteractionHand.MAIN_HAND, true);
        KingOfHellEntity king = KingOfHellEntity.spawnFrom(player);
        if (king == null) {
            refundChakra(player, NARAKA_PATH_CHAKRA_USAGE);
            if (showMessages) {
                player.displayClientMessage(Component.literal("Naraka Path summon failed."), true);
            }
            return false;
        }
        head.getOrCreateTag().putUUID(KING_OF_HELL_ID_TAG, king.getUUID());
        return true;
    }

    private static boolean triggerAsuraPath(ServerPlayer player, boolean showMessages) {
        boolean active = maintainAsuraPath(player);
        if (showMessages) {
            player.displayClientMessage(Component.literal(active
                    ? "Asura Path armed: use the offhand Asura Cannon."
                    : "Asura Path is passive but could not arm right now."), true);
        }
        return active;
    }

    private static boolean triggerOuterPath(ServerPlayer player, boolean showMessages) {
        return SixPathSenjutsuItem.triggerOuterPath(player, showMessages);
    }

    private static boolean unsupportedPath(ServerPlayer player, int path, boolean showMessages) {
        if (showMessages) {
            player.displayClientMessage(Component.literal("This Rinnegan path is not reconnected yet: " + path), true);
        }
        return false;
    }

    private static boolean unselectedPath(ServerPlayer player, boolean showMessages) {
        if (showMessages) {
            player.displayClientMessage(Component.literal("Select a Rinnegan path first."), true);
        }
        return false;
    }

    private static void spawnSummonEffects(ServerLevel level, Vec3 pos, double height) {
        level.playSound(null, pos.x(), pos.y(), pos.z(), ModSounds.SOUND_KUCHIYOSENOJUTSU.get(),
                SoundSource.NEUTRAL, 2.0F, 0.8F);
        level.sendParticles(ModParticleTypes.options(NarutoParticleKind.SEAL_FORMULA, 20, 0, 60),
                pos.x(), pos.y() + 0.015D, pos.z(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                pos.x(), pos.y() + height * 0.5D, pos.z(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    private static BlockPos meteorTarget(ServerPlayer player) {
        BlockHitResult hit = ProcedureUtils.raytraceBlocks(player, 100.0D);
        if (hit.getType() == HitResult.Type.BLOCK) {
            return hit.getBlockPos();
        }
        Vec3 target = player.getEyePosition().add(player.getLookAngle().scale(100.0D));
        return BlockPos.containing(target);
    }

    private static boolean consumeChakra(ServerPlayer player, double amount) {
        return player.isCreative() || Chakra.pathway(player).consume(amount);
    }

    private static double rinneganTechniqueCost(ServerPlayer player, double baseCost) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!isRinneganLikeHead(head)) {
            return Double.MAX_VALUE * 0.001D;
        }
        return player.isCreative() || ProcedureUtils.getOwnerId(head) == null || ProcedureUtils.isOriginalOwner(player, head)
                ? baseCost
                : baseCost * 2.0D;
    }

    private static void refundChakra(ServerPlayer player, double amount) {
        if (!player.isCreative()) {
            Chakra.pathway(player).consume(-amount, false);
        }
    }

    private static void clearInactiveAsuraGear(ServerPlayer player, boolean hasRinneganHead) {
        if (hasRinneganHead) {
            removeAllMatching(player, ModItems.ASURAPATHARMORBODY.get());
            removeAllMatching(player, ModItems.ASURACANON.get());
            return;
        }
        if (player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.ASURAPATHARMORBODY.get())) {
            player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        }
        if (player.getOffhandItem().is(ModItems.ASURACANON.get())) {
            player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        }
    }

    private static void removeAllMatching(Player player, Item item) {
        for (int index = 0; index < player.getInventory().getContainerSize(); index++) {
            ItemStack stack = player.getInventory().getItem(index);
            if (stack.is(item)) {
                player.getInventory().setItem(index, ItemStack.EMPTY);
            }
        }
    }

    private static void tickAsuraBody(ServerPlayer player, ItemStack chestStack) {
        if (!chestStack.is(ModItems.ASURAPATHARMORBODY.get())) {
            return;
        }
        double ticksUsed = chestStack.getTag() != null && chestStack.getTag().contains(ASURA_TICKS_USED_TAG)
                ? chestStack.getTag().getDouble(ASURA_TICKS_USED_TAG)
                : -1.0D;
        ticksUsed += 1.0D;
        chestStack.getOrCreateTag().putDouble(ASURA_TICKS_USED_TAG, ticksUsed);
        if (ticksUsed % 40.0D == 1.0D) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 41, 24, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 41, 16, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 41, 5, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.JUMP, 41, 5, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 41, 0, false, false));
        }
    }

    private static void tickShinraTenseiCharge(ServerPlayer player) {
        if (!player.getPersistentData().getBoolean(SHINRATENSEI_WAS_PRESSED_TAG)) {
            return;
        }
        if (!player.isAlive() || player.isSpectator() || !isRinneganLikeHead(player.getItemBySlot(EquipmentSlot.HEAD))) {
            player.getPersistentData().putBoolean(SHINRATENSEI_WAS_PRESSED_TAG, false);
            player.getPersistentData().putDouble(SHINRATENSEI_POWER_TAG, 0.0D);
            return;
        }
        chargeShinraTensei(player);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
            maintainAsuraPath(player);
            tickShinraTenseiCharge(player);
            if (!isRinneganLikeHead(player.getItemBySlot(EquipmentSlot.HEAD))) {
                resetBanshoTenin(player);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player player) {
            resetBanshoTenin(player);
        }
    }
}
