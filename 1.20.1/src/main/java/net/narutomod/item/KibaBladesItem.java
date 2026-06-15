package net.narutomod.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
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
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import net.narutomod.entity.FalseDarknessEntity;
import net.narutomod.entity.KibaBladeAuraEntity;
import net.narutomod.entity.LightningArcEntity;
import net.narutomod.particle.NarutoParticleKind;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModDamageTypes;
import net.narutomod.registry.ModEntityTypes;
import net.narutomod.registry.ModParticleTypes;
import net.narutomod.registry.ModSounds;

public final class KibaBladesItem extends Item implements ItemOnBody.Interface {
    private static final UUID KIBA_ATTACK_DAMAGE_UUID = UUID.fromString("d2b3bcbb-33c4-43aa-a4bb-730b982c367b");
    private static final UUID KIBA_ATTACK_SPEED_UUID = UUID.fromString("b216fa98-7738-4b65-8c23-4133f4b7f190");
    private static final int MIN_CHARGE_TICKS = 20;
    private static final float FALSE_DARKNESS_POWER = 2.0F;
    private static final float MELEE_LIGHTNING_DAMAGE = 2.0F;

    private final Multimap<Attribute, AttributeModifier> mainHandModifiers;

    public KibaBladesItem() {
        super(new Item.Properties().stacksTo(1));
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                KIBA_ATTACK_DAMAGE_UUID,
                "Kiba blades damage",
                12.0D,
                AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(
                KIBA_ATTACK_SPEED_UUID,
                "Kiba blades speed",
                -2.4D,
                AttributeModifier.Operation.ADDITION));
        this.mainHandModifiers = builder.build();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        int usedTicks = getUseDuration(stack) - remainingUseDuration;
        if (usedTicks < MIN_CHARGE_TICKS) {
            return;
        }
        if (level instanceof ServerLevel serverLevel && usedTicks % 5 == 0) {
            serverLevel.sendParticles(
                    ModParticleTypes.options(NarutoParticleKind.SMOKE_COLORED, 0x206AD1FF, 5, 40, 0xF0, livingEntity.getId(), 5),
                    livingEntity.getX(),
                    livingEntity.getY(),
                    livingEntity.getZ(),
                    4,
                    0.2D,
                    0.0D,
                    0.2D,
                    0.02D);
        }
        if (!level.isClientSide && usedTicks % 10 == 0) {
            level.playSound(null, livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(),
                    ModSounds.SOUND_CHARGING_CHAKRA.get(), SoundSource.PLAYERS, 0.05F,
                    level.random.nextFloat() + 0.5F);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int remainingUseDuration) {
        if (!(livingEntity instanceof Player player) || level.isClientSide) {
            return;
        }
        int usedTicks = getUseDuration(stack) - remainingUseDuration;
        if (usedTicks < MIN_CHARGE_TICKS) {
            return;
        }
        HitResult hit = ProcedureUtils.objectEntityLookingAt(player, 20.0D, 0.0D, false, false, target -> target != player);
        if (!(hit instanceof EntityHitResult entityHit) || !(entityHit.getEntity() instanceof LivingEntity target)) {
            player.displayClientMessage(Component.literal("Kiba Blades need a living target."), true);
            return;
        }
        FalseDarknessEntity entity = ModEntityTypes.FALSE_DARKNESS.get().create(level);
        if (entity == null) {
            return;
        }
        entity.configure(player, target, FALSE_DARKNESS_POWER);
        level.addFreshEntity(entity);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide && attacker.getRandom().nextInt(5) == 0) {
            DamageSource source = ModDamageTypes.ninjutsu(attacker.level(), attacker, attacker);
            LightningArcEntity.onStruck(target, source, MELEE_LIGHTNING_DAMAGE, false);
        }
        return true;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (!level.isClientSide && entity instanceof LivingEntity living && living.getMainHandItem() == stack) {
            KibaBladeAuraEntity.ensureFor(living, stack);
        }
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ToolAction toolAction) {
        return toolAction == ToolActions.SHIELD_BLOCK || super.canPerformAction(stack, toolAction);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        if (slot == EquipmentSlot.MAINHAND) {
            return this.mainHandModifiers;
        }
        return super.getDefaultAttributeModifiers(slot);
    }
}
