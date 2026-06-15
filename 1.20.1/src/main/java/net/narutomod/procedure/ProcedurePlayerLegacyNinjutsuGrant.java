package net.narutomod.procedure;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;
import net.narutomod.NarutomodModVariables;
import net.narutomod.item.JutsuItem;
import net.narutomod.registry.ModItems;

public final class ProcedurePlayerLegacyNinjutsuGrant {
    private static final String BAKUTON_ACQUIRED = "narutomod:bakuton_acquired";
    private static final String RANTON_ACQUIRED = "narutomod:ranton_acquired";
    private static final String FUTTON_ACQUIRED = "narutomod:futton_acquired";
    private static final String JITON_ACQUIRED = "narutomod:jiton_acquired";
    private static final String YOOTON_ACQUIRED = "narutomod:yooton_acquired";
    private static final String HYOTON_ACQUIRED = "narutomod:hyoton_acquired";
    private static final String SHAKUTON_ACQUIRED = "narutomod:shakuton_acquired";
    private static final String KEKKEI_TOTA_AWAKENED = "narutomod:kekkei_tota_awakened";
    private static final String MEDICAL_GENIN = "narutomod:achievementmedicalgenin";

    private ProcedurePlayerLegacyNinjutsuGrant() {
    }

    public static void apply(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)
                || player.experienceLevel < 10
                || !NarutomodModVariables.isNinja(player)) {
            return;
        }

        NarutomodModVariables.PlayerVariables variables = NarutomodModVariables.get(player);
        if (variables.getBoolean(NarutomodModVariables.FIRST_GOT_NINJUTSU)) {
            return;
        }

        variables.putBoolean(NarutomodModVariables.FIRST_GOT_NINJUTSU, true);
        NarutomodModVariables.sync(serverPlayer);

        giveAffinityIfMissing(player, ModItems.NINJUTSU.get());
        if (!hasAnyBaseNature(player)) {
            grantInitialNatureAffinity(serverPlayer);
        }

        grantLegacyMedicalJutsu(serverPlayer);
    }

    private static boolean hasAnyBaseNature(Player player) {
        return ProcedureUtils.hasItemInInventory(player, ModItems.KATON.get())
                || ProcedureUtils.hasItemInInventory(player, ModItems.SUITON.get())
                || ProcedureUtils.hasItemInInventory(player, ModItems.RAITON.get())
                || ProcedureUtils.hasItemInInventory(player, ModItems.FUTON.get())
                || ProcedureUtils.hasItemInInventory(player, ModItems.DOTON.get());
    }

    private static void grantInitialNatureAffinity(ServerPlayer player) {
        if (ProcedureUtils.advancementAchieved(player, BAKUTON_ACQUIRED)) {
            giveAffinities(player, ModItems.DOTON.get(), ModItems.RAITON.get());
        } else if (ProcedureUtils.advancementAchieved(player, RANTON_ACQUIRED)) {
            giveAffinities(player, ModItems.SUITON.get(), ModItems.RAITON.get());
        } else if (ProcedureUtils.advancementAchieved(player, FUTTON_ACQUIRED)) {
            giveAffinities(player, ModItems.SUITON.get(), ModItems.KATON.get());
        } else if (ProcedureUtils.advancementAchieved(player, JITON_ACQUIRED)) {
            giveAffinities(player, ModItems.FUTON.get(), ModItems.DOTON.get());
        } else if (ProcedureUtils.advancementAchieved(player, YOOTON_ACQUIRED)) {
            giveAffinities(player, ModItems.DOTON.get(), ModItems.KATON.get());
        } else if (ProcedureUtils.advancementAchieved(player, HYOTON_ACQUIRED)) {
            giveAffinities(player, ModItems.FUTON.get(), ModItems.SUITON.get());
        } else if (ProcedureUtils.advancementAchieved(player, SHAKUTON_ACQUIRED)) {
            giveAffinities(player, ModItems.KATON.get(), ModItems.FUTON.get());
        } else if (ProcedureUtils.advancementAchieved(player, KEKKEI_TOTA_AWAKENED)) {
            giveAffinities(player, ModItems.KATON.get(), ModItems.DOTON.get(), ModItems.FUTON.get());
        } else {
            double roll = player.getRandom().nextDouble();
            if (roll <= 0.2D) {
                giveAffinity(player, ModItems.KATON.get());
            } else if (roll <= 0.4D) {
                giveAffinity(player, ModItems.SUITON.get());
            } else if (roll <= 0.6D) {
                giveAffinity(player, ModItems.RAITON.get());
            } else if (roll <= 0.8D) {
                giveAffinity(player, ModItems.FUTON.get());
            } else {
                giveAffinity(player, ModItems.DOTON.get());
            }
        }
    }

    private static void giveAffinities(Player player, Item... items) {
        for (Item item : items) {
            giveAffinity(player, item);
        }
    }

    private static void grantLegacyMedicalJutsu(ServerPlayer player) {
        if (ProcedureUtils.hasItemInInventory(player, ModItems.IRYO_JUTSU.get())) {
            return;
        }

        boolean medicalGenin = ProcedureUtils.advancementAchieved(player, MEDICAL_GENIN);
        if (!medicalGenin && player.getRandom().nextDouble() > 0.25D) {
            return;
        }

        ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(ModItems.IRYO_JUTSU.get()));
        if (!medicalGenin) {
            ProcedureUtils.grantAdvancement(player, MEDICAL_GENIN, false);
        }
    }

    private static void giveAffinityIfMissing(Player player, Item item) {
        if (!ProcedureUtils.hasItemInInventory(player, item)) {
            giveAffinity(player, item);
        }
    }

    private static void giveAffinity(Player player, Item item) {
        ItemStack stack = new ItemStack(item);
        if (stack.getItem() instanceof JutsuItem) {
            stack.getOrCreateTag().putBoolean(JutsuItem.AFFINITY_TAG, true);
        }
        ItemHandlerHelper.giveItemToPlayer(player, stack);
    }
}
