package net.narutomod.item;

import java.util.function.Consumer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.narutomod.NarutomodMod;
import net.narutomod.client.BoneArmorClientExtensions;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModItems;
import net.narutomod.registry.ModSounds;

public final class BoneArmorItem extends ArmorItem {
    private static final String LARCH_ACTIVE_TAG = "LarchActive";
    private static final String WILLOW_ACTIVE_TAG = "WillowActive";
    private static final ArmorMaterial MATERIAL = new ArmorMaterial() {
        @Override
        public int getDurabilityForType(Type type) {
            return switch (type) {
                case BOOTS -> 13;
                case LEGGINGS -> 15;
                case CHESTPLATE -> 16;
                case HELMET -> 11;
            } * 100;
        }

        @Override
        public int getDefenseForType(Type type) {
            return switch (type) {
                case BOOTS -> 2;
                case LEGGINGS -> 5;
                case CHESTPLATE -> 20;
                case HELMET -> 2;
            };
        }

        @Override
        public int getEnchantmentValue() {
            return 0;
        }

        @Override
        public SoundEvent getEquipSound() {
            return ModSounds.SOUND_BONECRACK.get();
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.EMPTY;
        }

        @Override
        public String getName() {
            return "narutomod:bone_armor";
        }

        @Override
        public float getToughness() {
            return 5.0F;
        }

        @Override
        public float getKnockbackResistance() {
            return 0.0F;
        }
    };

    public BoneArmorItem() {
        super(MATERIAL, Type.CHESTPLATE, new Item.Properties().stacksTo(1));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (!(entity instanceof LivingEntity livingEntity) || livingEntity.getItemBySlot(EquipmentSlot.CHEST) != stack) {
            if (!level.isClientSide) {
                stack.shrink(1);
            }
            return;
        }
        if (livingEntity instanceof Player player && !ProcedureUtils.hasItemInInventory(player, ModItems.SHIKOTSUMYAKU.get())) {
            if (!level.isClientSide) {
                stack.shrink(1);
            }
            return;
        }
        if (!level.isClientSide) {
            livingEntity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 3, isLarchActive(stack) ? 3 : 2, false, false));
            int strength = livingEntity.hasEffect(MobEffects.DAMAGE_BOOST)
                    ? livingEntity.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier()
                    : -1;
            strength += isWillowActive(stack) ? 5 : 0;
            if (strength >= 0) {
                livingEntity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 3, strength, false, false));
            }
        }
    }

    public static boolean toggleLarch(LivingEntity livingEntity) {
        ItemStack stack = getOrEquipBoneArmor(livingEntity);
        boolean active = !isLarchActive(stack);
        setLarchActive(stack, active);
        return active;
    }

    public static boolean toggleWillow(LivingEntity livingEntity) {
        ItemStack stack = getOrEquipBoneArmor(livingEntity);
        boolean active = !isWillowActive(stack);
        setWillowActive(stack, active);
        return active;
    }

    public static void setLarchActive(ItemStack stack, boolean active) {
        stack.getOrCreateTag().putBoolean(LARCH_ACTIVE_TAG, active);
    }

    public static boolean isLarchActive(ItemStack stack) {
        return isBoneArmor(stack) && stack.getOrCreateTag().getBoolean(LARCH_ACTIVE_TAG);
    }

    public static void setWillowActive(ItemStack stack, boolean active) {
        stack.getOrCreateTag().putBoolean(WILLOW_ACTIVE_TAG, active);
    }

    public static boolean isWillowActive(ItemStack stack) {
        return isBoneArmor(stack) && stack.getOrCreateTag().getBoolean(WILLOW_ACTIVE_TAG);
    }

    public static boolean isBoneArmor(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModItems.BONE_ARMORBODY.get());
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return "narutomod:textures/bonearmor.png";
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        BoneArmorClientExtensions.initialize(consumer);
    }

    private static ItemStack getOrEquipBoneArmor(LivingEntity livingEntity) {
        ItemStack chest = livingEntity.getItemBySlot(EquipmentSlot.CHEST);
        if (isBoneArmor(chest)) {
            return chest;
        }
        ItemStack replacement = new ItemStack(ModItems.BONE_ARMORBODY.get());
        if (livingEntity instanceof Player player) {
            ItemStack inventoryStack = ProcedureUtils.getMatchingItemStack(player, ModItems.BONE_ARMORBODY.get());
            if (inventoryStack != null && !inventoryStack.isEmpty()) {
                replacement = inventoryStack.copyWithCount(1);
                inventoryStack.shrink(1);
            }
            if (!chest.isEmpty()) {
                ItemStack displaced = chest.copy();
                player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
                if (!player.getInventory().add(displaced)) {
                    player.drop(displaced, false);
                }
            }
        }
        livingEntity.setItemSlot(EquipmentSlot.CHEST, replacement);
        return livingEntity.getItemBySlot(EquipmentSlot.CHEST);
    }

    @Mod.EventBusSubscriber(modid = NarutomodMod.MODID)
    public static final class ForgeEvents {
        private ForgeEvents() {
        }

        @SubscribeEvent
        public static void onLivingAttack(LivingAttackEvent event) {
            LivingEntity target = event.getEntity();
            if (target.level().isClientSide) {
                return;
            }
            DamageSource source = event.getSource();
            if (!isLarchActive(target.getItemBySlot(EquipmentSlot.CHEST))
                    || source.is(DamageTypes.THORNS)
                    || source.is(DamageTypeTags.IS_EXPLOSION)) {
                return;
            }
            Entity directEntity = source.getDirectEntity();
            if (directEntity instanceof LivingEntity attacker && attacker != target) {
                attacker.hurt(target.damageSources().thorns(target), event.getAmount() * 0.7F);
            }
        }
    }
}
