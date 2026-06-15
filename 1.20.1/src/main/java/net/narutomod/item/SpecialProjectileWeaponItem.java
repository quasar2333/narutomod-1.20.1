package net.narutomod.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.narutomod.entity.ThrownSpecialWeaponEntity;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModEffects;
import net.narutomod.registry.ModItems;
import net.narutomod.registry.ModSounds;

public final class SpecialProjectileWeaponItem extends Item {
    private static final UUID WEAPON_ATTACK_DAMAGE_UUID = UUID.fromString("d43a0ded-dfc2-40bc-9046-8903f7c1319e");
    private static final UUID WEAPON_ATTACK_SPEED_UUID = UUID.fromString("f42cc5cd-58a4-498a-bd6d-b438648332a0");

    private final WeaponKind kind;
    private final Supplier<? extends EntityType<ThrownSpecialWeaponEntity>> projectileType;
    private final int cooldownTicks;
    private final Multimap<Attribute, AttributeModifier> mainHandModifiers;

    public SpecialProjectileWeaponItem(
            WeaponKind kind,
            Supplier<? extends EntityType<ThrownSpecialWeaponEntity>> projectileType,
            Properties properties) {
        super(properties);
        this.kind = kind;
        this.projectileType = projectileType;
        this.cooldownTicks = kind == WeaponKind.ASH_BONES ? 80 : 40;
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        if (kind == WeaponKind.BLACK_RECEIVER) {
            builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                    WEAPON_ATTACK_DAMAGE_UUID,
                    "Black receiver damage",
                    10.0D,
                    AttributeModifier.Operation.ADDITION));
            builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(
                    WEAPON_ATTACK_SPEED_UUID,
                    "Black receiver speed",
                    -2.4D,
                    AttributeModifier.Operation.ADDITION));
        }
        this.mainHandModifiers = builder.build();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int remainingUseDuration) {
        if (!(livingEntity instanceof Player player) || level.isClientSide) {
            return;
        }
        ThrownSpecialWeaponEntity projectile = this.projectileType.get().create(level);
        if (projectile == null) {
            return;
        }
        projectile.configure(player);
        Vec3 look = player.getLookAngle();
        projectile.moveTo(
                player.getX() + look.x() * 0.25D,
                player.getEyeY() - 0.1D,
                player.getZ() + look.z() * 0.25D,
                player.getYRot(),
                player.getXRot());
        projectile.shoot(look.x(), look.y(), look.z(), 2.0F, 0.0F);
        level.addFreshEntity(projectile);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.SOUND_HAND_SHOOT.get(),
                SoundSource.NEUTRAL, 1.0F, 1.0F / (level.random.nextFloat() * 0.5F + 1.0F) + 0.5F);
        if (this.kind == WeaponKind.BLACK_RECEIVER && stack.isDamageableItem()) {
            stack.hurtAndBreak(1, player, owner -> owner.broadcastBreakEvent(player.getUsedItemHand()));
        }
        if (this.kind == WeaponKind.BLACK_RECEIVER || !player.getAbilities().instabuild) {
            player.getCooldowns().addCooldown(this, this.cooldownTicks);
        }
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (this.kind == WeaponKind.ASH_BONES) {
            ThrownSpecialWeaponEntity.applyAshBonesEffect(target);
        } else {
            int amplifier = 1;
            MobEffectInstance current = target.getEffect(ModEffects.HEAVINESS.get());
            if (current != null) {
                amplifier += current.getAmplifier();
            }
            target.addEffect(new MobEffectInstance(ModEffects.HEAVINESS.get(), 600, amplifier, false, false));
        }
        return true;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (level.isClientSide) {
            return;
        }
        if (this.kind == WeaponKind.BLACK_RECEIVER) {
            tickBlackReceiver(entity);
        } else if (!canKeepAshBones(entity)) {
            stack.shrink(1);
        }
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack item, Player player) {
        return false;
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
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        if (slot == EquipmentSlot.MAINHAND && !this.mainHandModifiers.isEmpty()) {
            return this.mainHandModifiers;
        }
        return super.getDefaultAttributeModifiers(slot);
    }

    private void tickBlackReceiver(Entity entity) {
        if (entity instanceof Player player) {
            if (!ProcedureUtils.hasItemInInventory(player, ModItems.RINNEGANHELMET.get())
                    && !ProcedureUtils.hasItemInInventory(player, ModItems.TENSEIGANHELMET.get())) {
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 1, false, false));
            }
        } else if (entity instanceof Mob mob) {
            mob.setNoAi(true);
        }
    }

    private static boolean canKeepAshBones(Entity entity) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        if (player.getAbilities().instabuild) {
            return true;
        }
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        return head.is(ModItems.BYAKURINNESHARINGANHELMET.get())
                || head.is(ModItems.RINNEGANHELMET.get())
                || head.is(ModItems.TENSEIGANHELMET.get());
    }

    public enum WeaponKind {
        ASH_BONES,
        BLACK_RECEIVER
    }
}
