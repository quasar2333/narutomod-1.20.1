package net.narutomod.item;

import java.util.function.Consumer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.narutomod.client.AkatsukiRobeClientExtensions;
import net.narutomod.registry.ModSounds;

public final class AkatsukiRobeItem extends ArmorItem {
    private static final ArmorMaterial MATERIAL = new ArmorMaterial() {
        @Override
        public int getDurabilityForType(Type type) {
            return switch (type) {
                case BOOTS -> 13;
                case LEGGINGS -> 15;
                case CHESTPLATE -> 16;
                case HELMET -> 11;
            } * 5;
        }

        @Override
        public int getDefenseForType(Type type) {
            return switch (type) {
                case BOOTS -> 1;
                case LEGGINGS -> 2;
                case CHESTPLATE -> 3;
                case HELMET -> 1;
            };
        }

        @Override
        public int getEnchantmentValue() {
            return 9;
        }

        @Override
        public SoundEvent getEquipSound() {
            return SoundEvents.ARMOR_EQUIP_GENERIC;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.EMPTY;
        }

        @Override
        public String getName() {
            return "narutomod:akatsuki_robe";
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

    public AkatsukiRobeItem(Type type) {
        super(MATERIAL, type, new Item.Properties().stacksTo(1));
        this.type = type;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (level.isClientSide || this.type != Type.HELMET || !(entity instanceof Player player)
                || player.getItemBySlot(EquipmentSlot.HEAD) != stack || player.getRandom().nextInt(200) != 0) {
            return;
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.SOUND_DINGDING.get(),
                SoundSource.PLAYERS, 0.8F, player.getRandom().nextFloat() * 0.1F + 0.95F);
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return "narutomod:textures/robe_akatsuki.png";
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        AkatsukiRobeClientExtensions.initialize(consumer);
    }
}
