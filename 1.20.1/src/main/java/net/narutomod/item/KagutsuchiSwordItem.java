package net.narutomod.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.narutomod.Chakra;
import net.narutomod.entity.KagutsuchiFireballEntity;
import net.narutomod.entity.SusanooWingedEntity;
import net.narutomod.particle.NarutoParticleKind;
import net.narutomod.registry.ModEffects;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModItems;
import net.narutomod.registry.ModParticleTypes;

public final class KagutsuchiSwordItem extends Item {
    private static final int COOLDOWN_TICKS = 200;
    private static final double CHAKRA_TICK_COST = 20.0D;
    private static final int LEGACY_TEMPORARY_LIFETIME_TICKS = 5;
    private static final String SUMMONED_SUSANOO_ID_TAG = "summonedSusanooID";
    private static final String TICKS_USED_TAG = "KagutsuchiTicksUsed";
    private static final UUID ATTACK_DAMAGE_UUID = UUID.fromString("a96f74f3-2c4e-4c73-9d75-34e9b10675d3");
    private static final UUID ATTACK_SPEED_UUID = UUID.fromString("70767059-b2f6-42d3-98da-b11a2c337246");

    private final Multimap<Attribute, AttributeModifier> defaultMainHandModifiers;

    public KagutsuchiSwordItem() {
        super(new Item.Properties().stacksTo(1));
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                ATTACK_DAMAGE_UUID,
                "Kagutsuchi Sword damage",
                1.5D,
                AttributeModifier.Operation.MULTIPLY_BASE));
        builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(
                ATTACK_SPEED_UUID,
                "Kagutsuchi Sword speed",
                -2.4D,
                AttributeModifier.Operation.ADDITION));
        this.defaultMainHandModifiers = builder.build();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int remainingUseDuration) {
        if (!(livingEntity instanceof Player player) || level.isClientSide) {
            return;
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLAZE_SHOOT,
                SoundSource.NEUTRAL, 1.0F, 1.0F);
        boolean big = isHolderRidingWingedSusanoo(player);
        Entity owner = big && player.getVehicle() != null ? player.getVehicle() : player;
        Vec3 eye = new Vec3(player.getX(), player.getEyeY(), player.getZ());
        Vec3 look = player.getLookAngle();
        Vec3 origin = eye.add(look.scale(big ? 4.0D : 2.0D));
        spawnFireball(level, owner, player, origin, eye.add(look.scale(20.0D)), big);
        spawnFireball(level, owner, player, origin, eye.add(Vec3.directionFromRotation(player.getXRot(), player.getYRot() - 20.0F).scale(20.0D)), big);
        spawnFireball(level, owner, player, origin, eye.add(Vec3.directionFromRotation(player.getXRot(), player.getYRot() + 20.0F).scale(20.0D)), big);
        if (!player.isCreative()) {
            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        }
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide) {
            target.addEffect(new MobEffectInstance(ModEffects.AMATERASUFLAME.get(), 10000, amaterasuAmplifier(attacker), false, false));
        }
        return true;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (level.isClientSide || !(entity instanceof Player player)) {
            return;
        }
        tickLegacyLifetime(stack, player);
        if (player.getMainHandItem() == stack) {
            tickHeld(player);
        }
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (slot == EquipmentSlot.MAINHAND) {
            return this.defaultMainHandModifiers;
        }
        return super.getAttributeModifiers(slot, stack);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        if (slot == EquipmentSlot.MAINHAND) {
            return this.defaultMainHandModifiers;
        }
        return super.getDefaultAttributeModifiers(slot);
    }

    public static boolean isHolderRidingWingedSusanoo(Player player) {
        Entity vehicle = player.getVehicle();
        return vehicle != null && vehicle.getType() == ModEntityTypes.SUSANOOWINGED.get();
    }

    public static float susanooScale(Player player) {
        Entity vehicle = player.getVehicle();
        if (vehicle != null && vehicle.getType() == ModEntityTypes.SUSANOOWINGED.get()) {
            return (float)Math.max(vehicle.getPersistentData().getDouble("entityModelScale"), 1.0D);
        }
        return 1.0F;
    }

    private static void spawnFireball(Level level, Entity owner, Player player, Vec3 origin, Vec3 target, boolean big) {
        KagutsuchiFireballEntity fireball = (big
                ? ModEntityTypes.ENTITYKAGUTSUCHISWORDBIGFIREBALL.get()
                : ModEntityTypes.ENTITYKAGUTSUCHISWORDFIREBALL.get()).create(level);
        if (fireball == null) {
            return;
        }
        fireball.configure(owner, player, origin, target);
        level.addFreshEntity(fireball);
    }

    private static int amaterasuAmplifier(Entity entity) {
        return entity instanceof Player player ? player.experienceLevel / 30 + 1 : 0;
    }

    private static void tickLegacyLifetime(ItemStack stack, Player player) {
        CompoundTag tag = stack.getOrCreateTag();
        int ticksUsed = tag.getInt(TICKS_USED_TAG) + 1;
        tag.putInt(TICKS_USED_TAG, ticksUsed);
        if (ticksUsed > LEGACY_TEMPORARY_LIFETIME_TICKS
                && !player.isCreative()
                && !hasSummonedWingedSusanoo(player)) {
            stack.shrink(1);
        }
    }

    private static boolean hasSummonedWingedSusanoo(Player player) {
        int susanooId = player.getPersistentData().getInt(SUMMONED_SUSANOO_ID_TAG);
        if (susanooId > 0
                && player.level().getEntity(susanooId) instanceof SusanooWingedEntity susanoo
                && susanoo.isOwnedBy(player)) {
            return true;
        }
        return player.getVehicle() instanceof SusanooWingedEntity susanoo && susanoo.isOwnedBy(player);
    }

    private static void tickHeld(Player player) {
        if (!hasProtectiveEye(player)) {
            player.addEffect(new MobEffectInstance(ModEffects.AMATERASUFLAME.get(), 1, 4, false, false));
        }
        if (player.getRandom().nextFloat() < 0.05F) {
            player.level().playSound(null, player.getX(), player.getY() + susanooScale(player) * 0.9D, player.getZ(),
                    SoundEvents.FIRE_AMBIENT, SoundSource.NEUTRAL, 0.9F, player.getRandom().nextFloat() * 0.7F + 0.3F);
        }
        if (player.tickCount % 20 == 0) {
            Chakra.pathway(player).consume(CHAKRA_TICK_COST);
        }
        spawnHeldSmoke(player);
    }

    private static boolean hasProtectiveEye(Player player) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        return head.is(ModItems.MANGEKYOSHARINGANHELMET.get())
                || head.is(ModItems.MANGEKYOSHARINGANETERNALHELMET.get())
                || head.is(ModItems.MANGEKYOSHARINGANOBITOHELMET.get());
    }

    private static void spawnHeldSmoke(Player player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        float scale = susanooScale(player);
        int count = Math.max(1, Mth.ceil(scale));
        for (int i = 0; i < count; i++) {
            double random = player.getRandom().nextDouble();
            double yawRad = player.yBodyRot * Mth.DEG_TO_RAD;
            double sideRad = (player.yBodyRot + 90.0F) * Mth.DEG_TO_RAD;
            double x = player.getX() - Math.sin(sideRad) * scale * 0.38D - Math.sin(yawRad) * random * scale * 1.6D;
            double z = player.getZ() + Math.cos(sideRad) * scale * 0.38D + Math.cos(yawRad) * random * scale * 1.6D;
            double y = player.getY() + ((Math.sin(Mth.DEG_TO_RAD * 30.0F) * random) + 0.9D) * scale;
            serverLevel.sendParticles(
                    ModParticleTypes.options(NarutoParticleKind.SMOKE_COLORED, 0xD0080505, scale >= 4.0F ? 8 : 3, 35, 0, -1, 4),
                    x,
                    y,
                    z,
                    1,
                    scale >= 4.0F ? 0.5D : 0.0D,
                    scale >= 4.0F ? 0.5D : 0.0D,
                    scale >= 4.0F ? 0.5D : 0.0D,
                    0.01D);
        }
    }
}
