package net.narutomod.item;

import java.util.function.Consumer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.narutomod.client.RinneganRobeClientExtensions;

public final class RinneganRobeItem extends ArmorItem {
    private static final ArmorMaterial MATERIAL = new ArmorMaterial() {
        @Override
        public int getDurabilityForType(Type type) {
            return 0;
        }

        @Override
        public int getDefenseForType(Type type) {
            return switch (type) {
                case BOOTS -> 2;
                case LEGGINGS -> 5;
                case CHESTPLATE -> 6;
                case HELMET -> 2;
            };
        }

        @Override
        public int getEnchantmentValue() {
            return 9;
        }

        @Override
        public SoundEvent getEquipSound() {
            return SoundEvents.ARMOR_EQUIP_LEATHER;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.EMPTY;
        }

        @Override
        public String getName() {
            return "narutomod:rinnegan_robe";
        }

        @Override
        public float getToughness() {
            return 0.0F;
        }

        @Override
        public float getKnockbackResistance() {
            return 0.0F;
        }
    };

    public RinneganRobeItem(Type type) {
        super(MATERIAL, type, new Item.Properties().stacksTo(1));
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return "narutomod:textures/madara_jinchuriki.png";
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        RinneganRobeClientExtensions.initialize(consumer);
    }
}
