package net.narutomod.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import net.narutomod.Chakra;
import net.narutomod.entity.GroundShockEntity;
import net.narutomod.event.SpecialEvent;
import net.narutomod.registry.ModItems;

public final class BasicMeleeWeaponItem extends Item implements ItemOnBody.Interface {
    private static final double SAMEHADA_CHAKRA_TRANSFER = 50.0D;
    private static final String KABUTOWARI_HAMMER_SLAM_TIME = "KabutowariHammerSlamTime";
    private static final UUID ATTACK_DAMAGE_UUID = UUID.fromString("347d85f8-8e51-4b2d-b2fb-9907b28c8066");
    private static final UUID ATTACK_SPEED_UUID = UUID.fromString("05f72c6d-6e08-4e91-a2a0-d67208851a13");
    private static final UUID REACH_UUID = UUID.fromString("2ea719b4-d3ee-442b-97f6-3a6d704e5102");

    private final WeaponKind kind;
    private final Multimap<Attribute, AttributeModifier> defaultMainHandModifiers;

    public BasicMeleeWeaponItem(WeaponKind kind) {
        super(kind.createProperties());
        this.kind = kind;
        this.defaultMainHandModifiers = buildMainHandModifiers(kind.attackDamage, kind.attackSpeed, kind.reachBonus);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (this.kind == WeaponKind.KABUTOWARI) {
            if (!level.isClientSide) {
                splitKabutowari(player, hand);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (this.kind.blocking) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        return super.use(level, player, hand);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (level.isClientSide || !(entity instanceof Player player)) {
            return;
        }
        if (this.kind == WeaponKind.KABUTOWARI) {
            if (player.getMainHandItem() == stack) {
                splitKabutowari(player, InteractionHand.MAIN_HAND);
            } else if (player.getOffhandItem() == stack) {
                splitKabutowari(player, InteractionHand.OFF_HAND);
            }
        } else if (this.kind == WeaponKind.KABUTOWARI_HAMMER) {
            tickKabutowariHammerCooldown(player);
        } else if (this.kind == WeaponKind.SAMEHADA && player.getMainHandItem() == stack) {
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 2, 0, false, false));
        }
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide) {
            applyHitEffects(stack, target, attacker);
        }
        if (this.kind.hitDamage > 0 && stack.isDamageableItem()) {
            stack.hurtAndBreak(this.kind.hitDamage, attacker, owner -> owner.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        }
        return true;
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        if (!level.isClientSide && this.kind.blockDamage > 0 && state.getDestroySpeed(level, pos) != 0.0F && stack.isDamageableItem()) {
            stack.hurtAndBreak(this.kind.blockDamage, miner, owner -> owner.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        }
        return true;
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (this.kind == WeaponKind.ZABUZA_SWORD) {
            return (float)(10.0D * durabilityPercent(stack));
        }
        return this.kind.destroySpeed > 0.0F ? this.kind.destroySpeed : super.getDestroySpeed(stack, state);
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ToolAction toolAction) {
        return this.kind.blocking && toolAction == ToolActions.SHIELD_BLOCK || super.canPerformAction(stack, toolAction);
    }

    @Override
    public boolean canDisableShield(ItemStack stack, ItemStack shield, LivingEntity entity, LivingEntity attacker) {
        return this.kind == WeaponKind.KABUTOWARI_AXE || super.canDisableShield(stack, shield, entity, attacker);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return this.kind.blocking ? 72000 : super.getUseDuration(stack);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return this.kind.blocking ? UseAnim.BLOCK : super.getUseAnimation(stack);
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack stack, Player player) {
        return this.kind != WeaponKind.BONE_SWORD && this.kind != WeaponKind.BONE_DRILL && super.onDroppedByPlayer(stack, player);
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        if (this.kind == WeaponKind.KABUTOWARI_HAMMER && !entity.level().isClientSide
                && !entity.onGround() && getKabutowariHammerCooldown(entity) == 0) {
            entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 4.0F, entity.getRandom().nextFloat() * 0.4F + 0.8F);
            if (GroundShockEntity.spawnFrom(entity.level(), BlockPos.containing(entity.getX(), entity.getY(), entity.getZ()), 10)) {
                entity.getPersistentData().putInt(KABUTOWARI_HAMMER_SLAM_TIME, 15);
                return true;
            }
        }
        return super.onEntitySwing(stack, entity);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (this.kind.tooltipKey != null) {
            tooltip.add(Component.translatable(this.kind.tooltipKey).withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (slot == EquipmentSlot.MAINHAND && this.kind.hasMainHandModifiers()) {
            if (this.kind == WeaponKind.ZABUZA_SWORD) {
                return buildMainHandModifiers(1.0D + 22.0D * durabilityPercent(stack), this.kind.attackSpeed, this.kind.reachBonus);
            }
            return this.defaultMainHandModifiers;
        }
        return super.getAttributeModifiers(slot, stack);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        if (slot == EquipmentSlot.MAINHAND && this.kind.hasMainHandModifiers()) {
            return this.defaultMainHandModifiers;
        }
        return super.getDefaultAttributeModifiers(slot);
    }

    @Override
    public ItemOnBody.BodyPart showOnBody(ItemStack stack) {
        return switch (this.kind) {
            case ANBU_SWORD, CHOKUTO, KABUTOWARI, SAMEHADA, SHIBUKI_SWORD, ZABUZA_SWORD -> ItemOnBody.BodyPart.TORSO;
            default -> ItemOnBody.BodyPart.NONE;
        };
    }

    private void applyHitEffects(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (this.kind == WeaponKind.SAMEHADA) {
            applySamehadaEffects(target, attacker);
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1, false, false));
        } else if (this.kind == WeaponKind.SHIBUKI_SWORD && (!stack.isDamageableItem() || stack.getDamageValue() < stack.getMaxDamage())) {
            SpecialEvent.setVanillaExplosionEvent(attacker.level(), target, 0, 0, 0, 5.0F, attacker.level().getGameTime() + 20L, false);
        } else if (this.kind == WeaponKind.ZABUZA_SWORD && stack.isDamageableItem()) {
            stack.setDamageValue(Math.max(0, stack.getDamageValue() - 3));
        }
    }

    private static void applySamehadaEffects(LivingEntity target, LivingEntity attacker) {
        if (Chakra.pathway(target).consume(SAMEHADA_CHAKRA_TRANSFER)) {
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 3, false, false));
            target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 1, false, false));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1, false, false));
            if (Chakra.pathway(attacker).consume(-SAMEHADA_CHAKRA_TRANSFER, true)) {
                attacker.heal(4.0F);
            }
        }
    }

    private static int getKabutowariHammerCooldown(LivingEntity entity) {
        return entity.getPersistentData().getInt(KABUTOWARI_HAMMER_SLAM_TIME);
    }

    private static void tickKabutowariHammerCooldown(LivingEntity entity) {
        CompoundTag tag = entity.getPersistentData();
        int ticks = tag.getInt(KABUTOWARI_HAMMER_SLAM_TIME);
        if (ticks > 0) {
            tag.putInt(KABUTOWARI_HAMMER_SLAM_TIME, ticks - 1);
        } else {
            tag.remove(KABUTOWARI_HAMMER_SLAM_TIME);
        }
    }

    private static void splitKabutowari(Player player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND) {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.KABUTOWARI_HAMMER.get()));
            giveOrPlaceOffhand(player, new ItemStack(ModItems.KABUTOWARI_AXE.get()));
        } else {
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.KABUTOWARI_HAMMER.get()));
            giveOrPlaceMainhand(player, new ItemStack(ModItems.KABUTOWARI_AXE.get()));
        }
    }

    private static void giveOrPlaceMainhand(Player player, ItemStack stack) {
        if (player.getMainHandItem().isEmpty()) {
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        } else if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private static void giveOrPlaceOffhand(Player player, ItemStack stack) {
        if (player.getOffhandItem().isEmpty()) {
            player.setItemInHand(InteractionHand.OFF_HAND, stack);
        } else if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private static double durabilityPercent(ItemStack stack) {
        if (!stack.isDamageableItem() || stack.getMaxDamage() <= 0) {
            return 1.0D;
        }
        return 1.0D - (double)stack.getDamageValue() / (double)stack.getMaxDamage();
    }

    private static Multimap<Attribute, AttributeModifier> buildMainHandModifiers(double attackDamage, double attackSpeed, double reachBonus) {
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                ATTACK_DAMAGE_UUID,
                "Narutomod melee weapon damage",
                attackDamage,
                AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(
                ATTACK_SPEED_UUID,
                "Narutomod melee weapon speed",
                attackSpeed,
                AttributeModifier.Operation.ADDITION));
        if (reachBonus > 0.0D) {
            builder.put(ForgeMod.ENTITY_REACH.get(), new AttributeModifier(
                    REACH_UUID,
                    "Narutomod melee weapon reach",
                    reachBonus,
                    AttributeModifier.Operation.ADDITION));
        }
        return builder.build();
    }

    public enum WeaponKind {
        ANBU_SWORD(6.0D, -1.8D, 500, true, 0, 0, 0.0F, null, 0.0D),
        BONE_SWORD(11.0D, -3.0D, 50, true, 1, 0, 0.0F, null, 0.0D),
        BONE_DRILL(79.0D, -3.5D, 50, true, 2, 1, 5.0F, null, 0.0D),
        CHOKUTO(11.0D, -3.0D, 2000, true, 1, 0, 0.0F, null, 0.0D),
        KABUTOWARI(0.0D, 0.0D, 0, false, 0, 0, 0.0F, "tooltip.kabutowari.general", 0.0D),
        KABUTOWARI_AXE(9.0D, -3.0D, 0, true, 0, 0, 20.0F, null, 0.0D),
        KABUTOWARI_HAMMER(9.0D, -3.0D, 0, true, 0, 0, 20.0F, null, 0.0D),
        SAMEHADA(11.0D, -3.4D, 0, true, 0, 1, 1.0F, "tooltip.samehada.general", 0.0D),
        SHIBUKI_SWORD(7.0D, -3.5D, 100, true, 1, 0, 0.0F, "tooltip.shibuki.general", 0.0D),
        TOTSUKA_SWORD(19.0D, -3.0D, 0, false, 0, 0, 0.0F, null, 0.0D),
        ZABUZA_SWORD(23.0D, -3.4D, 300, true, 0, 2, 0.0F, "tooltip.zabuzasword.general", 1.0D);

        private final double attackDamage;
        private final double attackSpeed;
        private final int durability;
        private final boolean blocking;
        private final int hitDamage;
        private final int blockDamage;
        private final float destroySpeed;
        private final String tooltipKey;
        private final double reachBonus;

        WeaponKind(
                double attackDamage,
                double attackSpeed,
                int durability,
                boolean blocking,
                int hitDamage,
                int blockDamage,
                float destroySpeed,
                String tooltipKey,
                double reachBonus) {
            this.attackDamage = attackDamage;
            this.attackSpeed = attackSpeed;
            this.durability = durability;
            this.blocking = blocking;
            this.hitDamage = hitDamage;
            this.blockDamage = blockDamage;
            this.destroySpeed = destroySpeed;
            this.tooltipKey = tooltipKey;
            this.reachBonus = reachBonus;
        }

        private Item.Properties createProperties() {
            Item.Properties properties = new Item.Properties().stacksTo(1);
            return this.durability > 0 ? properties.durability(this.durability) : properties;
        }

        private boolean hasMainHandModifiers() {
            return this.attackDamage != 0.0D || this.attackSpeed != 0.0D || this.reachBonus != 0.0D;
        }
    }
}
