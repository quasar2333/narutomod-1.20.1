package net.narutomod.item;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.narutomod.NarutomodModVariables;
import net.narutomod.client.DojutsuHelmetClientExtensions;
import net.narutomod.procedure.ProcedureUtils;

public final class EternalMangekyoHelmetItem extends ArmorItem {
    private static final String LAST_WORN_FOREIGN_DOJUTSU_TAG = "lastWornForeignDojutsu";
    private static final ArmorMaterial MATERIAL = new ArmorMaterial() {
        @Override
        public int getDurabilityForType(Type type) {
            return 0;
        }

        @Override
        public int getDefenseForType(Type type) {
            return type == Type.HELMET ? 2 : 0;
        }

        @Override
        public int getEnchantmentValue() {
            return 0;
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
            return "narutomod:sasuke";
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

    public EternalMangekyoHelmetItem() {
        super(MATERIAL, Type.HELMET, new Item.Properties().stacksTo(1));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (!level.isClientSide && entity instanceof Player player && player.isCreative() && ProcedureUtils.getOwnerId(stack) == null) {
            ProcedureUtils.setOriginalOwner(player, stack);
        }
    }

    @Override
    public void onArmorTick(ItemStack stack, Level level, Player player) {
        super.onArmorTick(stack, level, player);
        if (level.isClientSide) {
            return;
        }
        applyForeignOwnerPenalty(stack, player);
        NarutomodModVariables.get(player).putLong(NarutomodModVariables.MOST_RECENT_WORN_DOJUTSU_TIME, level.getGameTime());
        NarutomodModVariables.sync((net.minecraft.server.level.ServerPlayer) player);
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 2, 2, false, false));
        ObitoMangekyoHelmetItem.updateKamuiFlight(player);
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return "narutomod:textures/mangekyosharinganhelmet_eternal.png";
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        DojutsuHelmetClientExtensions.initialize(consumer);
    }

    @Override
    public Component getName(ItemStack stack) {
        return super.getName(stack).copy().withStyle(ChatFormatting.RED);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("key.mcreator.specialjutsu1")
                .append(": ")
                .append(Component.translatable("tooltip.mangekyo.amaterasu.jutsu1")));
        tooltip.add(Component.translatable("key.mcreator.specialjutsu2")
                .append(": ")
                .append(Component.translatable("entity.susanooclothed.name")));
        tooltip.add(Component.translatable("key.mcreator.specialjutsu3")
                .append(": ")
                .append(Component.translatable("tooltip.mangekyo.kamui.jutsu1")));
    }

    private static void applyForeignOwnerPenalty(ItemStack stack, Player player) {
        if (ProcedureUtils.isOriginalOwner(player, stack) || player.isCreative()) {
            return;
        }
        UUID ownerId = ProcedureUtils.getOwnerId(stack);
        CompoundTag tag = player.getPersistentData();
        if (ownerId != null && (!tag.hasUUID(LAST_WORN_FOREIGN_DOJUTSU_TAG) || !ownerId.equals(tag.getUUID(LAST_WORN_FOREIGN_DOJUTSU_TAG)))) {
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 1200, 0, false, false));
            tag.putUUID(LAST_WORN_FOREIGN_DOJUTSU_TAG, ownerId);
        }
    }
}
