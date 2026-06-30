package net.narutomod.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemHandlerHelper;
import net.narutomod.Chakra;
import net.narutomod.NarutomodModVariables;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModItems;

public final class NarutoConsumableItem extends Item {
    private final ConsumableKind kind;

    public NarutoConsumableItem(ConsumableKind kind) {
        super(kind.createProperties());
        this.kind = kind;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (this.kind.disableCreativeUse && player.isCreative()) {
            return new InteractionResultHolder<>(InteractionResult.FAIL, stack);
        }
        if (this.kind.forceEatInSurvival) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        return super.use(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        ItemStack result = super.finishUsingItem(stack, level, livingEntity);
        if (!level.isClientSide && livingEntity instanceof Player player) {
            this.kind.apply(player, level, this);
        }
        return result;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return this.kind.useDurationTicks;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return this.kind == ConsumableKind.CHAKRA_FRUIT || super.isFoil(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (this.kind.tooltipKey != null) {
            tooltip.add(Component.translatable(this.kind.tooltipKey).withStyle(ChatFormatting.GRAY));
        }
    }

    public enum ConsumableKind {
        MILITARY_RATIONS_PILL(10, 0.6F, false, 3, 120, true, true, "tooltip.mrp.browntip") {
            @Override
            void apply(Player player, Level level, Item item) {
                Chakra.pathway(player).consume(-200.0D, true);
                player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 800, 0, false, false));
                player.getCooldowns().addCooldown(item, 1200);
            }
        },
        MILITARY_RATIONS_PILL_GOLD(14, 0.6F, true, 3, 200, true, true, "tooltip.mrp.goldtip") {
            @Override
            void apply(Player player, Level level, Item item) {
                Chakra.pathway(player).consume(-500.0D, true);
                player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 2400, 0, false, false));
                player.getCooldowns().addCooldown(item, 2400);
            }
        },
        CHAKRA_FRUIT(20, 0.3F, true, 1, 32, false, false, null) {
            @Override
            void apply(Player player, Level level, Item item) {
                applyChakraFruit(player, level);
            }
        },
        WHITE_ZETSU_FLESH(0, 0.3F, true, 64, 32, false, false, null) {
            @Override
            void apply(Player player, Level level, Item item) {
                applyWhiteZetsuFlesh(player, level);
            }
        };

        private final int nutrition;
        private final float saturation;
        private final boolean alwaysEat;
        private final int maxStackSize;
        private final int useDurationTicks;
        private final boolean forceEatInSurvival;
        private final boolean disableCreativeUse;
        private final String tooltipKey;

        ConsumableKind(
                int nutrition,
                float saturation,
                boolean alwaysEat,
                int maxStackSize,
                int useDurationTicks,
                boolean forceEatInSurvival,
                boolean disableCreativeUse,
                String tooltipKey) {
            this.nutrition = nutrition;
            this.saturation = saturation;
            this.alwaysEat = alwaysEat;
            this.maxStackSize = maxStackSize;
            this.useDurationTicks = useDurationTicks;
            this.forceEatInSurvival = forceEatInSurvival;
            this.disableCreativeUse = disableCreativeUse;
            this.tooltipKey = tooltipKey;
        }

        abstract void apply(Player player, Level level, Item item);

        private Item.Properties createProperties() {
            FoodProperties.Builder food = new FoodProperties.Builder()
                    .nutrition(this.nutrition)
                    .saturationMod(this.saturation);
            if (this.alwaysEat) {
                food.alwaysEat();
            }
            return new Item.Properties().stacksTo(this.maxStackSize).food(food.build());
        }
    }

    private static void applyChakraFruit(Player player, Level level) {
        ItemStack rinnegan = ProcedureUtils.getMatchingItemStack(player, ModItems.RINNEGANHELMET.get());
        ItemStack tenseigan = ProcedureUtils.getMatchingItemStack(player, ModItems.TENSEIGANHELMET.get());
        if (rinnegan != null) {
            activateRinneSharingan(rinnegan);
        } else if (tenseigan != null) {
            markTenseiganAwakened(tenseigan);
            activateRinneSharingan(tenseigan);
        } else if (ProcedureUtils.hasItemInInventory(player, ModItems.BYAKUGANHELMET.get())) {
            ItemStack stack = new ItemStack(ModItems.TENSEIGANHELMET.get());
            markTenseiganAwakened(stack);
            give(player, stack);
        } else if (hasAnySharingan(player)) {
            give(player, new ItemStack(ModItems.RINNEGANHELMET.get()));
        } else if (player.getRandom().nextBoolean()) {
            give(player, new ItemStack(ModItems.RINNEGANHELMET.get()));
        } else {
            ItemStack stack = new ItemStack(ModItems.TENSEIGANHELMET.get());
            markTenseiganAwakened(stack);
            give(player, stack);
        }

        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            NarutomodModVariables.addBattleExperience(serverPlayer, 100000.0D);
            serverPlayer.giveExperiencePoints(100000);
            ProcedureUtils.grantAdvancement(serverPlayer, "narutomod:rinneganawakened", true);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.NEUTRAL, 1.0F, 1.0F);
    }

    private static void applyWhiteZetsuFlesh(Player player, Level level) {
        if (isWearingMangekyo(player)) {
            player.addEffect(new MobEffectInstance(MobEffects.HEAL, 10, 0, false, false));
        } else {
            player.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, player.getRandom().nextInt(3), false, false));
        }

        if ((ProcedureUtils.hasItemInInventory(player, ModItems.SUITON.get())
                || ProcedureUtils.hasItemInInventory(player, ModItems.DOTON.get()))
                && !ProcedureUtils.hasItemInInventory(player, ModItems.MOKUTON.get())
                && player.getRandom().nextFloat() < 0.1F) {
            give(player, new ItemStack(ModItems.MOKUTON.get()));
            if (!ProcedureUtils.hasItemInInventory(player, ModItems.SUITON.get())) {
                give(player, new ItemStack(ModItems.SUITON.get()));
            }
            if (!ProcedureUtils.hasItemInInventory(player, ModItems.DOTON.get())) {
                give(player, new ItemStack(ModItems.DOTON.get()));
            }
            ItemStack ninjutsu = ProcedureUtils.getMatchingItemStack(player, ModItems.NINJUTSU.get());
            if (ninjutsu != null && ninjutsu.getItem() instanceof NinjutsuItem ninjutsuItem) {
                ninjutsuItem.enableJutsu(ninjutsu, NinjutsuItem.KAGE_BUNSHIN, true);
            }
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                ProcedureUtils.grantAdvancement(serverPlayer, "narutomod:mokuton_acquired", true);
            }
        }
    }

    private static boolean isWearingMangekyo(Player player) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        return head.is(ModItems.MANGEKYOSHARINGANHELMET.get())
                || head.is(ModItems.MANGEKYOSHARINGANOBITOHELMET.get())
                || head.is(ModItems.MANGEKYOSHARINGANETERNALHELMET.get());
    }

    private static boolean hasAnySharingan(Player player) {
        return ProcedureUtils.hasItemInInventory(player, ModItems.SHARINGANHELMET.get())
                || ProcedureUtils.hasItemInInventory(player, ModItems.MANGEKYOSHARINGANHELMET.get())
                || ProcedureUtils.hasItemInInventory(player, ModItems.MANGEKYOSHARINGANOBITOHELMET.get())
                || ProcedureUtils.hasItemInInventory(player, ModItems.MANGEKYOSHARINGANETERNALHELMET.get());
    }

    private static void activateRinneSharingan(ItemStack stack) {
        stack.getOrCreateTag().putBoolean(NarutomodModVariables.RINNESHARINGAN_ACTIVATED, true);
    }

    private static void markTenseiganAwakened(ItemStack stack) {
        stack.getOrCreateTag().putDouble(TenseiganChakraModeItem.BYAKUGAN_COUNT_TAG, 5.0D);
    }

    private static void give(Player player, ItemStack stack) {
        ItemHandlerHelper.giveItemToPlayer(player, stack);
    }
}
