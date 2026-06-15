package net.narutomod.item;

import java.util.function.Consumer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import net.narutomod.client.NinjaArmorClientExtensions;

public final class NinjaArmorItem extends ArmorItem {
    private static final ArmorMaterial NINJA_MATERIAL = new LegacyArmorMaterial(
            "narutomod:ninja_armor",
            5,
            new int[]{2, 5, 6, 2},
            0,
            SoundEvents.ARMOR_EQUIP_GENERIC,
            1.0F,
            Ingredient.EMPTY
    );
    private static final ArmorMaterial SAMURAI_MATERIAL = new LegacyArmorMaterial(
            "narutomod:samurai_armor",
            30,
            new int[]{2, 5, 8, 2},
            9,
            SoundEvents.ARMOR_EQUIP_IRON,
            0.0F,
            Ingredient.EMPTY
    );

    private final Style style;
    private final Type type;

    public NinjaArmorItem(Style style, Type type) {
        super(style == Style.SAMURAI ? SAMURAI_MATERIAL : NINJA_MATERIAL, type, new Item.Properties().stacksTo(1));
        this.style = style;
        this.type = type;
    }

    public Style getStyle() {
        return this.style;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (level.isClientSide || !hasHelmetTickEffects() || this.type != Type.HELMET
                || !(entity instanceof Player player) || player.getItemBySlot(EquipmentSlot.HEAD) != stack) {
            return;
        }
        player.removeEffect(MobEffects.POISON);
        player.removeEffect(MobEffects.WITHER);
        if (player.tickCount % 20 == 3) {
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 21, 0, false, false));
        }
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return this.style.texture;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        NinjaArmorClientExtensions.initialize(consumer);
    }

    private boolean hasHelmetTickEffects() {
        return this.style == Style.AME || this.style == Style.SAMURAI;
    }

    public enum Style {
        KONOHA("narutomod:textures/konohaarmor.png"),
        IWA("narutomod:textures/iwaarmor.png"),
        SUNA("narutomod:textures/sunaarmor.png"),
        KIRI("narutomod:textures/kiriarmor.png"),
        KUMO("narutomod:textures/kumoarmor.png"),
        ANBU("narutomod:textures/anbuarmor.png"),
        JUMPSUIT("narutomod:textures/jumpsuitarmor.png"),
        FISHNET("narutomod:textures/fishnetarmor.png"),
        AME("narutomod:textures/amearmor.png"),
        WAR1("narutomod:textures/wa1armor.png"),
        SAMURAI("narutomod:textures/samuraiarmor.png");

        private final String texture;

        Style(String texture) {
            this.texture = texture;
        }
    }

    private static final class LegacyArmorMaterial implements ArmorMaterial {
        private final String name;
        private final int durabilityFactor;
        private final int[] defense;
        private final int enchantmentValue;
        private final SoundEvent equipSound;
        private final float toughness;
        private final Ingredient repairIngredient;

        private LegacyArmorMaterial(String name, int durabilityFactor, int[] defense, int enchantmentValue,
                                    SoundEvent equipSound, float toughness, Ingredient repairIngredient) {
            this.name = name;
            this.durabilityFactor = durabilityFactor;
            this.defense = defense;
            this.enchantmentValue = enchantmentValue;
            this.equipSound = equipSound;
            this.toughness = toughness;
            this.repairIngredient = repairIngredient;
        }

        @Override
        public int getDurabilityForType(Type type) {
            return switch (type) {
                case BOOTS -> 13;
                case LEGGINGS -> 15;
                case CHESTPLATE -> 16;
                case HELMET -> 11;
            } * this.durabilityFactor;
        }

        @Override
        public int getDefenseForType(Type type) {
            return switch (type) {
                case BOOTS -> this.defense[0];
                case LEGGINGS -> this.defense[1];
                case CHESTPLATE -> this.defense[2];
                case HELMET -> this.defense[3];
            };
        }

        @Override
        public int getEnchantmentValue() {
            return this.enchantmentValue;
        }

        @Override
        public SoundEvent getEquipSound() {
            return this.equipSound;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return this.repairIngredient;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public float getToughness() {
            return this.toughness;
        }

        @Override
        public float getKnockbackResistance() {
            return 0.0F;
        }
    }
}
