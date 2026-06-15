package net.narutomod.item;

import java.util.List;
import java.util.UUID;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.narutomod.NarutomodModVariables;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModItems;
import net.narutomod.world.KamuiDimension;

public final class ObitoMangekyoHelmetItem extends ArmorItem {
    public static final String SHARINGAN_BLINDED_TAG = "sharingan_blinded";
    private static final String LAST_WORN_FOREIGN_DOJUTSU_TAG = "lastWornForeignDojutsu";
    private static final String KAMUI_FLIGHT_GRANTED_TAG = "NarutomodObitoKamuiFlightGranted";
    private static final String SUSANOO_ACTIVATED_TAG = "susanoo_activated";
    private static final double INTANGIBLE_CHAKRA_USAGE = 1.0D;
    private static final double TELEPORT_CHAKRA_USAGE = 8.0D;
    private static final ArmorMaterial MATERIAL = new ArmorMaterial() {
        @Override
        public int getDurabilityForType(Type type) {
            return type == Type.HELMET ? 13 * 1024 : 0;
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
            return "narutomod:mangekyosharingan_obito";
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

    public ObitoMangekyoHelmetItem() {
        super(MATERIAL, Type.HELMET, new Item.Properties().stacksTo(1));
    }

    public static double getIntangibleChakraUsage(LivingEntity entity) {
        return chakraUsage(entity, INTANGIBLE_CHAKRA_USAGE);
    }

    public static double getTeleportChakraUsage(LivingEntity entity) {
        return chakraUsage(entity, TELEPORT_CHAKRA_USAGE);
    }

    public static boolean isBlinded(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(SHARINGAN_BLINDED_TAG);
    }

    public static boolean canUseKamui(Player player) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        return isKamuiCapableHead(head) && !isBlinded(head);
    }

    public static void revokeKamuiFlightIfNeeded(Player player) {
        if (!player.level().isClientSide
                && player.getPersistentData().getBoolean(KAMUI_FLIGHT_GRANTED_TAG)
                && !player.isCreative()
                && !player.isSpectator()) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
            player.getPersistentData().remove(KAMUI_FLIGHT_GRANTED_TAG);
        }
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
        updateKamuiFlight(player);
        damageForActiveKamuiOrSusanoo(stack, player);
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return "narutomod:textures/mangekyosharinganhelmet_obito.png";
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
                .append(Component.translatable("tooltip.mangekyo.kamui.jutsu1")));
        tooltip.add(Component.translatable("key.mcreator.specialjutsu2")
                .append(": ")
                .append(Component.translatable("entity.susanooclothed.name")));
    }

    private static double chakraUsage(LivingEntity entity, double baseUsage) {
        ItemStack head = entity.getItemBySlot(EquipmentSlot.HEAD);
        if (!isKamuiCapableHead(head)) {
            return Double.MAX_VALUE * 0.001D;
        }
        return ProcedureUtils.isOriginalOwner(entity, head) ? baseUsage : baseUsage * 2.0D;
    }

    private static boolean isKamuiCapableHead(ItemStack stack) {
        return stack.is(ModItems.MANGEKYOSHARINGANOBITOHELMET.get())
                || stack.is(ModItems.MANGEKYOSHARINGANETERNALHELMET.get());
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

    public static void updateKamuiFlight(Player player) {
        boolean shouldAllowFlight = player.isCreative() || player.isSpectator() || KamuiDimension.isKamui(player.level());
        if (shouldAllowFlight) {
            if (!player.getAbilities().mayfly) {
                player.getPersistentData().putBoolean(KAMUI_FLIGHT_GRANTED_TAG, true);
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
            }
        } else {
            revokeKamuiFlightIfNeeded(player);
        }
    }

    private static void damageForActiveKamuiOrSusanoo(ItemStack stack, Player player) {
        boolean active = ObitoKamuiHandler.isTeleporting(player)
                || NarutomodModVariables.get(player).getBoolean(SUSANOO_ACTIVATED_TAG);
        if (!active) {
            return;
        }
        int interval = ProcedureUtils.isOriginalOwner(player, stack) ? 2 : 1;
        if (player.tickCount % interval == 0) {
            stack.hurtAndBreak(1, player, owner -> owner.broadcastBreakEvent(EquipmentSlot.HEAD));
        }
    }
}
