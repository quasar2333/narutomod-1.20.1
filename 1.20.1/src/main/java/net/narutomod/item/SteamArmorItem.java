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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.narutomod.client.SteamArmorClientExtensions;

public final class SteamArmorItem extends ArmorItem {
    private static final ArmorMaterial MATERIAL = new ArmorMaterial() {
        @Override
        public int getDurabilityForType(Type type) {
            return switch (type) {
                case BOOTS -> 13;
                case LEGGINGS -> 15;
                case CHESTPLATE -> 16;
                case HELMET -> 11;
            } * 50;
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
            return Ingredient.of(Items.LEATHER);
        }

        @Override
        public String getName() {
            return "narutomod:steam_armor";
        }

        @Override
        public float getToughness() {
            return 1.5F;
        }

        @Override
        public float getKnockbackResistance() {
            return 0.0F;
        }
    };

    public SteamArmorItem(Type type) {
        super(MATERIAL, type, new Item.Properties().stacksTo(1));
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return "narutomod:textures/steamarmor.png";
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        SteamArmorClientExtensions.initialize(consumer);
    }
}
