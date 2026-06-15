package net.narutomod.item;

import java.util.function.Consumer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.narutomod.client.UchihaArmorClientExtensions;
import net.narutomod.registry.ModItems;

public final class UchihaArmorItem extends ArmorItem {
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
            return "narutomod:uchiha";
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

    private final Type type;

    public UchihaArmorItem(Type type) {
        super(MATERIAL, type, new Item.Properties().stacksTo(1));
        this.type = type;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (level.isClientSide || this.type != Type.CHESTPLATE || !(entity instanceof Player player)
                || player.getItemBySlot(EquipmentSlot.CHEST) != stack || !hasLegacyUchihaSet(player)) {
            return;
        }
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 3, 1, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 3, 2, false, false));
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return slot == EquipmentSlot.LEGS
                ? "narutomod:textures/models/armor/sasuke__layer_2.png"
                : "narutomod:textures/models/armor/sasuke__layer_1.png";
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        UchihaArmorClientExtensions.initialize(consumer);
    }

    private static boolean hasLegacyUchihaSet(Player player) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        return player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.UCHIHALEGS.get())
                && player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.UCHIHABOOTS.get())
                && (head.is(ModItems.SHARINGANHELMET.get())
                || head.is(ModItems.MANGEKYOSHARINGANHELMET.get())
                || head.is(ModItems.MANGEKYOSHARINGANOBITOHELMET.get()));
    }
}
