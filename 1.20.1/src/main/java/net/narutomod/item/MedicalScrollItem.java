package net.narutomod.item;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import net.narutomod.NarutomodModVariables;
import net.narutomod.menu.MedicalScrollMenu;
import net.narutomod.procedure.ProcedureUtils;
import net.narutomod.registry.ModItems;

public final class MedicalScrollItem extends Item {
    public static final String SHARINGAN_COLOR_TAG = "SharinganColor";
    private static final String LEGACY_COLOR_TAG = "color";
    private static final String LEGACY_COLOR_CAPS_TAG = "Color";
    private static final String MEDICAL_GENIN_ADVANCEMENT = "narutomod:achievementmedicalgenin";
    private static final String ETERNAL_MANGEKYO_ADVANCEMENT = "narutomod:eternalmangekyoachieved";
    private static final double TENSEIGAN_INITIAL_EVOLVE_TIME = 1728000.0D;
    private static final double TENSEIGAN_EVOLVE_STEP = 345600.0D;

    public MedicalScrollItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider(
                    (containerId, inventory, menuPlayer) -> new MedicalScrollMenu(containerId, inventory),
                    Component.translatable("item.narutomod.medical_scroll")));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    public static boolean activate(MedicalScrollMenu menu, ServerPlayer player) {
        if (!ProcedureUtils.advancementAchieved(player, MEDICAL_GENIN_ADVANCEMENT)) {
            player.displayClientMessage(Component.literal("Medical Genin advancement is required."), true);
            return false;
        }
        if (!menu.outputEmpty()) {
            player.displayClientMessage(Component.literal("Take the medical scroll output first."), true);
            return false;
        }

        ItemStack primary = menu.input(MedicalScrollMenu.PRIMARY_SLOT);
        ItemStack secondary = menu.input(MedicalScrollMenu.SECONDARY_SLOT);
        ItemStack result = craftEternalMangekyo(primary, secondary, player);
        if (result.isEmpty()) {
            result = craftByakuganProgress(primary, secondary);
        }
        if (result.isEmpty()) {
            player.displayClientMessage(Component.literal("No valid medical scroll recipe."), true);
            return false;
        }

        menu.setOutput(result);
        menu.consumeInputs();
        player.displayClientMessage(Component.literal("Medical scroll activated."), true);
        return true;
    }

    private static ItemStack craftEternalMangekyo(ItemStack primary, ItemStack secondary, ServerPlayer player) {
        if (!isMangekyo(primary) || !isMangekyo(secondary)) {
            return ItemStack.EMPTY;
        }
        UUID primaryOwner = ProcedureUtils.getOwnerId(primary);
        UUID secondaryOwner = ProcedureUtils.getOwnerId(secondary);
        if (primaryOwner == null || secondaryOwner == null || primaryOwner.equals(secondaryOwner)) {
            return ItemStack.EMPTY;
        }

        ServerPlayer owner = player.server.getPlayerList().getPlayer(primaryOwner);
        if (owner == null) {
            return ItemStack.EMPTY;
        }

        ItemStack result = new ItemStack(ModItems.MANGEKYOSHARINGANETERNALHELMET.get());
        ProcedureUtils.setOriginalOwner(owner, result);
        copySharinganColor(primary, result);
        ProcedureUtils.grantAdvancement(owner, ETERNAL_MANGEKYO_ADVANCEMENT, true);
        return result;
    }

    private static ItemStack craftByakuganProgress(ItemStack primary, ItemStack secondary) {
        if ((!primary.is(ModItems.BYAKUGANHELMET.get()) && !primary.is(ModItems.TENSEIGANHELMET.get()))
                || !secondary.is(ModItems.BYAKUGANHELMET.get())) {
            return ItemStack.EMPTY;
        }
        UUID primaryOwner = ProcedureUtils.getOwnerId(primary);
        UUID secondaryOwner = ProcedureUtils.getOwnerId(secondary);
        if (primaryOwner == null || secondaryOwner == null || primaryOwner.equals(secondaryOwner)) {
            return ItemStack.EMPTY;
        }

        ItemStack result = primary.copy();
        result.setCount(1);
        if (result.is(ModItems.BYAKUGANHELMET.get())) {
            ByakuganHelmetItem.ensureByakuganCount(result);
        }
        CompoundTag tag = result.getOrCreateTag();
        double byakuganCount = tag.contains(TenseiganChakraModeItem.BYAKUGAN_COUNT_TAG)
                ? tag.getDouble(TenseiganChakraModeItem.BYAKUGAN_COUNT_TAG)
                : primary.is(ModItems.BYAKUGANHELMET.get()) ? 1.0D : 0.0D;
        tag.putDouble(TenseiganChakraModeItem.BYAKUGAN_COUNT_TAG, byakuganCount + 1.0D);

        if (primary.is(ModItems.BYAKUGANHELMET.get())) {
            if (!tag.contains(NarutomodModVariables.TENSEIGAN_EVOLVED_TIME)) {
                tag.putDouble(NarutomodModVariables.TENSEIGAN_EVOLVED_TIME, TENSEIGAN_INITIAL_EVOLVE_TIME);
            } else {
                tag.putDouble(
                        NarutomodModVariables.TENSEIGAN_EVOLVED_TIME,
                        tag.getDouble(NarutomodModVariables.TENSEIGAN_EVOLVED_TIME) - TENSEIGAN_EVOLVE_STEP);
            }
        }
        return result;
    }

    private static boolean isMangekyo(ItemStack stack) {
        return stack.is(ModItems.MANGEKYOSHARINGANHELMET.get())
                || stack.is(ModItems.MANGEKYOSHARINGANOBITOHELMET.get());
    }

    private static void copySharinganColor(ItemStack source, ItemStack target) {
        CompoundTag sourceTag = source.getTag();
        if (sourceTag == null) {
            return;
        }
        copyTagValue(sourceTag, target.getOrCreateTag(), SHARINGAN_COLOR_TAG);
        copyTagValue(sourceTag, target.getOrCreateTag(), LEGACY_COLOR_TAG);
        copyTagValue(sourceTag, target.getOrCreateTag(), LEGACY_COLOR_CAPS_TAG);
    }

    private static void copyTagValue(CompoundTag source, CompoundTag target, String key) {
        Tag tag = source.get(key);
        if (tag != null) {
            target.put(key, tag.copy());
        }
    }
}
