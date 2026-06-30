package net.narutomod.item;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.narutomod.NarutomodMod;
import net.narutomod.NarutomodModVariables;
import net.narutomod.client.BijuCloakClientExtensions;
import net.narutomod.entity.BijuManager;
import net.narutomod.entity.JinchurikiCloneEntity;
import net.narutomod.particle.NarutoParticleKind;
import net.narutomod.registry.ModEffects;
import net.narutomod.registry.ModItems;
import net.narutomod.registry.ModParticleTypes;

public final class BijuCloakItem extends ArmorItem {
    public static final String TAILS_TAG = "Tails";
    public static final String CLOAK_LEVEL_TAG = "BijuCloakLevel";
    public static final String CLOAK_XP_TAG = "BijuCloakXp";
    public static final String WEARING_TICKS_TAG = "WearingBijuCloakTicks";
    public static final String WEARING_FULL_SET_TAG = "WearingFullSetBijuCloak";
    public static final String CLONE_ID_TAG = "CloneID";
    private static final UUID CLOAK_HEALTH_UUID = UUID.fromString("e884e4a0-7f08-422d-9aac-119972cd764d");
    private static final AttributeModifier CLOAK_HEALTH_MODIFIER = new AttributeModifier(
            CLOAK_HEALTH_UUID,
            "bijucloak.maxhealth",
            180.0D,
            AttributeModifier.Operation.ADDITION);

    private final EquipmentSlot equipmentSlot;

    public BijuCloakItem(Type type) {
        super(ArmorMaterials.DIAMOND, type, new Item.Properties().stacksTo(1));
        this.equipmentSlot = type.getSlot();
    }

    public static ItemStack createStack(Item item, int tails, int level, int xp) {
        ItemStack stack = new ItemStack(item);
        setTails(stack, tails);
        setCloakLevel(stack, level);
        setCloakXp(stack, xp);
        stack.getOrCreateTag().putInt(CLONE_ID_TAG, -1);
        return stack;
    }

    public static boolean isBijuCloak(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.is(ModItems.BIJU_CLOAKHELMET.get())
                || stack.is(ModItems.BIJU_CLOAKBODY.get())
                || stack.is(ModItems.BIJU_CLOAKLEGS.get()));
    }

    public static int getTails(ItemStack stack) {
        return stack.getTag() != null ? stack.getTag().getInt(TAILS_TAG) : 0;
    }

    public static void setTails(ItemStack stack, int tails) {
        stack.getOrCreateTag().putInt(TAILS_TAG, Math.max(tails, 0));
    }

    public static int getCloakLevel(ItemStack stack) {
        return stack.getTag() != null ? stack.getTag().getInt(CLOAK_LEVEL_TAG) : 0;
    }

    public static void setCloakLevel(ItemStack stack, int level) {
        stack.getOrCreateTag().putInt(CLOAK_LEVEL_TAG, Math.max(Math.min(level, 3), 0));
    }

    public static int getCloakXp(ItemStack stack) {
        return stack.getTag() != null ? stack.getTag().getInt(CLOAK_XP_TAG) : 0;
    }

    public static void setCloakXp(ItemStack stack, int xp) {
        stack.getOrCreateTag().putInt(CLOAK_XP_TAG, Math.max(xp, 0));
    }

    public static boolean hasKuramaShine(ItemStack stack) {
        return getTails(stack) == 9 && getCloakLevel(stack) == 2 && getCloakXp(stack) >= 800;
    }

    public static ResourceLocation getCloakTexture(ItemStack stack) {
        int tails = getTails(stack);
        if (tails == 1) {
            return NarutomodMod.location("textures/bijucloak_sand.png");
        }
        if (getCloakLevel(stack) == 2) {
            return hasKuramaShine(stack)
                    ? NarutomodMod.location("textures/bijucloak_kurama.png")
                    : NarutomodMod.location("textures/bijucloakl2.png");
        }
        return NarutomodMod.location("textures/bijucloakl1.png");
    }

    public static int getWearingTicks(Entity entity) {
        if (entity instanceof Player player) {
            return NarutomodModVariables.get(player).getInt(WEARING_TICKS_TAG);
        }
        return entity.getPersistentData().getInt(WEARING_TICKS_TAG);
    }

    public static void setWearingTicks(Entity entity, int ticks) {
        int value = Math.max(ticks, 0);
        if (entity instanceof ServerPlayer player) {
            NarutomodModVariables.get(player).putInt(WEARING_TICKS_TAG, value);
            NarutomodModVariables.sync(player);
        } else {
            entity.getPersistentData().putInt(WEARING_TICKS_TAG, value);
        }
    }

    public static boolean isWearingFullSet(ItemStack stack) {
        return stack.getTag() != null && stack.getTag().getBoolean(WEARING_FULL_SET_TAG);
    }

    private static void setWearingFullSet(ItemStack stack, boolean fullSet) {
        stack.getOrCreateTag().putBoolean(WEARING_FULL_SET_TAG, fullSet);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (level.isClientSide || !(entity instanceof LivingEntity living)) {
            return;
        }
        if (living.getItemBySlot(this.equipmentSlot) != stack) {
            if (entity instanceof Player) {
                stack.shrink(1);
            }
            return;
        }
        if (entity instanceof JinchurikiCloneEntity) {
            if (this.equipmentSlot == EquipmentSlot.CHEST) {
                tickClone(stack, living);
            }
            return;
        }
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }

        int tails = BijuManager.getAssignedTail(player);
        int levelInManager = BijuManager.getCloakLevel(player);
        if (tails <= 0 || levelInManager <= 0) {
            stack.shrink(1);
            return;
        }

        syncStackFromManager(player, stack, tails, levelInManager);
        if (this.equipmentSlot == EquipmentSlot.CHEST) {
            tickPlayerChest(stack, player, tails, levelInManager);
        }
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        Multimap<Attribute, AttributeModifier> base = super.getAttributeModifiers(slot, stack);
        if (slot != EquipmentSlot.CHEST || !isWearingFullSet(stack)) {
            return base;
        }
        return ImmutableMultimap.<Attribute, AttributeModifier>builder()
                .putAll(base)
                .put(Attributes.MAX_HEALTH, CLOAK_HEALTH_MODIFIER)
                .build();
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return getCloakTexture(stack).toString();
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        BijuCloakClientExtensions.initialize(consumer);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        int tails = getTails(stack);
        int cloakLevel = getCloakLevel(stack);
        tooltip.add(Component.literal("Tails: " + tails + " / Cloak " + cloakLevel).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("XP: " + getCloakXp(stack)).withStyle(ChatFormatting.GREEN));
        if (stack.is(ModItems.BIJU_CLOAKBODY.get())) {
            tooltip.add(Component.literal("Special 2: Jinchuriki Clone").withStyle(ChatFormatting.DARK_GRAY));
            if (cloakLevel == 2) {
                tooltip.add(Component.literal("Special 3: Tail Beast Ball").withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }

    private static void syncStackFromManager(ServerPlayer player, ItemStack stack, int tails, int cloakLevel) {
        int[] xp = BijuManager.getCloakXp(player.getServer(), tails);
        int activeXp = cloakLevel >= 1 && cloakLevel <= 3 ? xp[cloakLevel - 1] : 0;
        setTails(stack, tails);
        setCloakLevel(stack, cloakLevel);
        setCloakXp(stack, activeXp + getWearingTicks(player) / 20);
    }

    private static void tickPlayerChest(ItemStack stack, ServerPlayer player, int tails, int cloakLevel) {
        boolean fullSet = hasFullSet(player, tails);
        setWearingFullSet(stack, fullSet);
        if (!fullSet) {
            return;
        }
        if (SusanooPowerIncreaseHandler.hasActiveOwnedSusanoo(player)) {
            BijuManager.toggleBijuCloak(player);
            return;
        }

        int wearingTicks = getWearingTicks(player) + 1;
        int storedXp = BijuManager.getCurrentCloakXp(player);
        int displayedXp = storedXp + wearingTicks / 20;
        setWearingTicks(player, wearingTicks);
        setCloakXp(stack, displayedXp);

        int maxTicks = storedXp * 5 + 200;
        if (wearingTicks > maxTicks && !player.isCreative()) {
            if (storedXp < 400 || cloakLevel == 2 && storedXp < 800) {
                discardTrainingClone(player, stack);
            }
            BijuManager.toggleBijuCloak(player);
            return;
        }

        boolean trained = displayedXp >= 800 || cloakLevel == 1 && displayedXp >= 400;
        if (trained) {
            discardTrainingClone(player, stack);
            applyEffects(player, cloakLevel, tails != 1 && cloakLevel == 1);
        } else {
            spawnTrainingClone(player, stack);
        }
    }

    private static void tickClone(ItemStack stack, LivingEntity clone) {
        int wearingTicks = getWearingTicks(clone) + 1;
        setWearingTicks(clone, wearingTicks);
        applyEffects(clone, getCloakLevel(stack), getTails(stack) != 1 && getCloakLevel(stack) == 1);
    }

    public static void applyEffects(LivingEntity entity, int cloakLevel) {
        applyEffects(entity, cloakLevel, true);
    }

    public static void applyEffects(LivingEntity entity, int cloakLevel, boolean smoke) {
        int level = Math.max(Math.min(cloakLevel, 3), 1);
        if (smoke && entity.level() instanceof ServerLevel serverLevel && entity.tickCount % 10 == 0) {
            serverLevel.sendParticles(
                    ModParticleTypes.options(NarutoParticleKind.SMOKE_COLORED, 0x2088001B, 20, 5, 0, entity.getId(), 4),
                    entity.getX(),
                    entity.getY() + 0.8D,
                    entity.getZ(),
                    12,
                    0.2D,
                    0.4D,
                    0.2D,
                    0.02D);
        }
        if (entity.tickCount % 5 != 4) {
            return;
        }
        entity.addEffect(new MobEffectInstance(ModEffects.CHAKRA_ENHANCED_STRENGTH.get(), 7, level * 24, false, false));
        entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 7, level * 32, false, false));
        entity.addEffect(new MobEffectInstance(MobEffects.JUMP, 7, 7, false, false));
        entity.addEffect(new MobEffectInstance(ModEffects.REACH.get(), 7, level - 1, false, false));
        if (level >= 2) {
            entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 7, 2, false, false));
        }
        if (entity.getHealth() < entity.getMaxHealth() && entity.getHealth() > 0.0F) {
            entity.heal((float)level);
        }
    }

    public static boolean hasFullSet(LivingEntity entity, int tails) {
        return isMatchingCloak(entity.getItemBySlot(EquipmentSlot.HEAD), ModItems.BIJU_CLOAKHELMET.get(), tails)
                && isMatchingCloak(entity.getItemBySlot(EquipmentSlot.CHEST), ModItems.BIJU_CLOAKBODY.get(), tails)
                && isMatchingCloak(entity.getItemBySlot(EquipmentSlot.LEGS), ModItems.BIJU_CLOAKLEGS.get(), tails);
    }

    private static boolean isMatchingCloak(ItemStack stack, Item item, int tails) {
        return !stack.isEmpty() && stack.is(item) && getTails(stack) == tails;
    }

    private static void spawnTrainingClone(ServerPlayer player, ItemStack stack) {
        if (getClone(player.level(), stack) != null) {
            return;
        }
        JinchurikiCloneEntity clone = JinchurikiCloneEntity.spawnFrom(player);
        if (clone != null) {
            stack.getOrCreateTag().putInt(CLONE_ID_TAG, clone.getId());
        }
    }

    private static void discardTrainingClone(ServerPlayer player, ItemStack stack) {
        JinchurikiCloneEntity clone = getClone(player.level(), stack);
        if (clone != null) {
            clone.discard();
        }
        stack.getOrCreateTag().putInt(CLONE_ID_TAG, -1);
    }

    @Nullable
    private static JinchurikiCloneEntity getClone(Level level, ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || tag.getInt(CLONE_ID_TAG) <= 0) {
            return null;
        }
        Entity entity = level.getEntity(tag.getInt(CLONE_ID_TAG));
        return entity instanceof JinchurikiCloneEntity clone ? clone : null;
    }
}
