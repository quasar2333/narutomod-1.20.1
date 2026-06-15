package net.narutomod.item;

import java.util.UUID;
import java.util.function.Consumer;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.narutomod.client.AsuraPathArmorClientExtensions;

public final class AsuraPathArmorItem extends ArmorItem {
    private static final UUID HEALTH_UUID = UUID.fromString("92f6ba90-c2b1-460d-a88e-5dd725be3c17");
    private static final AttributeModifier HEALTH_MODIFIER = new AttributeModifier(
            HEALTH_UUID,
            "Asura bonus",
            40.0D,
            AttributeModifier.Operation.ADDITION);

    private static final ArmorMaterial MATERIAL = new ArmorMaterial() {
        @Override
        public int getDurabilityForType(Type type) {
            return switch (type) {
                case BOOTS -> 13;
                case LEGGINGS -> 15;
                case CHESTPLATE -> 16;
                case HELMET -> 11;
            } * 1024;
        }

        @Override
        public int getDefenseForType(Type type) {
            return switch (type) {
                case BOOTS -> 2;
                case LEGGINGS -> 5;
                case CHESTPLATE -> 1024;
                case HELMET -> 2;
            };
        }

        @Override
        public int getEnchantmentValue() {
            return 0;
        }

        @Override
        public SoundEvent getEquipSound() {
            return SoundEvents.ARMOR_EQUIP_IRON;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.EMPTY;
        }

        @Override
        public String getName() {
            return "narutomod:asurapatharmor";
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

    public AsuraPathArmorItem() {
        super(MATERIAL, Type.CHESTPLATE, new Item.Properties().stacksTo(1));
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return "narutomod:textures/asura_path.png";
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        AsuraPathArmorClientExtensions.initialize(consumer);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        Multimap<Attribute, AttributeModifier> base = super.getAttributeModifiers(slot, stack);
        if (slot != EquipmentSlot.CHEST) {
            return base;
        }
        return ImmutableMultimap.<Attribute, AttributeModifier>builder()
                .putAll(base)
                .put(Attributes.MAX_HEALTH, HEALTH_MODIFIER)
                .build();
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack item, Player player) {
        return false;
    }
}
