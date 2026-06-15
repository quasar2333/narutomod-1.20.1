package net.narutomod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;
import net.narutomod.NarutomodMod;
import net.narutomod.NarutomodModVariables;
import net.narutomod.entity.BijuManager;
import net.narutomod.entity.TailedBeastEntity;
import net.narutomod.entity.TenTailsEntity;
import net.narutomod.item.EightGatesItem;
import net.narutomod.item.JutsuItem;
import net.narutomod.registry.ModItems;

/**
 * Restores the original narutomod gameplay commands as Brigadier commands and adds a new "max
 * everything" command:
 * <ul>
 *   <li>{@code /addninjaxp <targets> <amount>} — add ninja (battle) XP to players (op, perm 4).</li>
 *   <li>{@code /addxp2jutsu <amount>} — add XP to the held jutsu tome / Eight Gates / worn biju cloak.</li>
 *   <li>{@code /locateentity biju <num> | jinchuriki <list|revoke ...|assign ...>} — biju/jinchuriki admin.</li>
 *   <li>{@code /narutomax [targets]} — max ninja level, chakra, and every learnable jutsu (op, perm 2).</li>
 * </ul>
 * The {@code /narutoport} debug/validation tree lives separately in {@link PortingDebugCommands}.
 */
@Mod.EventBusSubscriber(modid = NarutomodMod.MODID)
public final class NarutoCommands {
    /** Battle-XP cap from {@link NarutomodModVariables#setBattleExperience}; ninja level = sqrt(xp) ~= 316. */
    private static final double MAX_BATTLE_EXPERIENCE = 100000.0D;
    /** Well past the 8th gate's XP requirement, so {@code /narutomax} unlocks every gate. */
    private static final int MAX_EIGHT_GATES_XP = 1_000_000;

    private NarutoCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("addninjaxp")
                .requires(source -> source.hasPermission(4))
                .then(Commands.argument("targets", EntityArgument.players())
                        .then(Commands.argument("amount", IntegerArgumentType.integer())
                                .executes(NarutoCommands::addNinjaXp))));

        dispatcher.register(Commands.literal("addxp2jutsu")
                .then(Commands.argument("amount", IntegerArgumentType.integer())
                        .executes(NarutoCommands::addXpToJutsu)));

        dispatcher.register(Commands.literal("locateentity")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("biju")
                        .then(Commands.argument("tails",
                                        IntegerArgumentType.integer(BijuManager.MIN_TAILS, BijuManager.MAX_TAILS))
                                .executes(NarutoCommands::locateBiju)))
                .then(Commands.literal("jinchuriki")
                        .then(Commands.literal("list").executes(NarutoCommands::jinchurikiList))
                        .then(Commands.literal("revoke")
                                .then(Commands.literal("all").executes(NarutoCommands::jinchurikiRevokeAll))
                                .then(Commands.argument("tails",
                                                IntegerArgumentType.integer(BijuManager.MIN_TAILS, BijuManager.MAX_TAILS))
                                        .executes(NarutoCommands::jinchurikiRevokeByTail)))
                        .then(Commands.literal("assign")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("tails",
                                                        IntegerArgumentType.integer(BijuManager.MIN_TAILS, BijuManager.MAX_TAILS))
                                                .executes(NarutoCommands::jinchurikiAssign))))));

        dispatcher.register(Commands.literal("narutomax")
                .requires(source -> source.hasPermission(2))
                .executes(context -> maxPlayers(context, List.of(context.getSource().getPlayerOrException())))
                .then(Commands.argument("targets", EntityArgument.players())
                        .executes(context -> maxPlayers(context, EntityArgument.getPlayers(context, "targets")))));
    }

    // ===== /addninjaxp <targets> <amount> =====
    private static int addNinjaXp(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        int amount = IntegerArgumentType.getInteger(context, "amount");
        int affected = 0;
        for (ServerPlayer target : targets) {
            if (!NarutomodModVariables.isNinja(target) && !target.isCreative()) {
                context.getSource().sendFailure(Component.literal(target.getScoreboardName() + " is not a ninja."));
                continue;
            }
            NarutomodModVariables.addBattleExperience(target, amount);
            affected++;
            context.getSource().sendSuccess(() -> Component.literal(
                    "Added " + amount + " ninja XP to " + target.getScoreboardName()
                            + " (level " + (int) NarutomodModVariables.getNinjaLevel(target) + ")"), true);
        }
        return affected;
    }

    // ===== /addxp2jutsu <amount> =====
    private static int addXpToJutsu(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        int amount = IntegerArgumentType.getInteger(context, "amount");
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        if (BijuManager.getCloakLevel(player) > 0) {
            BijuManager.addCloakXp(player, amount);
        } else if (main.getItem() instanceof EightGatesItem || off.getItem() instanceof EightGatesItem) {
            EightGatesItem.addBattleXP(player, amount);
        } else if (main.getItem() instanceof JutsuItem mainJutsu) {
            mainJutsu.addXpToCurrentJutsu(main, amount);
        } else if (off.getItem() instanceof JutsuItem offJutsu) {
            offJutsu.addXpToCurrentJutsu(off, amount);
        } else {
            context.getSource().sendFailure(Component.literal(
                    "Hold a jutsu tome (or the Eight Gates, or wear a biju cloak) to add jutsu XP."));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Added " + amount + " jutsu XP."), false);
        return 1;
    }

    // ===== /locateentity biju <tails> =====
    private static int locateBiju(CommandContext<CommandSourceStack> context) {
        int tails = IntegerArgumentType.getInteger(context, "tails");
        MinecraftServer server = context.getSource().getServer();
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                boolean match = tails == BijuManager.MAX_TAILS
                        ? entity instanceof TenTailsEntity && entity.isAlive()
                        : entity instanceof TailedBeastEntity beast && beast.isAlive() && beast.getTailCount() == tails;
                if (match) {
                    context.getSource().sendSuccess(() -> Component.literal(
                            BijuManager.displayName(tails) + " at "
                                    + (int) Math.floor(entity.getX()) + ", "
                                    + (int) Math.floor(entity.getY()) + ", "
                                    + (int) Math.floor(entity.getZ())
                                    + " in " + level.dimension().location()), false);
                    return 1;
                }
            }
        }
        UUID holder = BijuManager.getJinchurikiUuid(server, tails);
        if (holder != null) {
            context.getSource().sendSuccess(() -> Component.literal(
                    BijuManager.displayName(tails) + " is sealed in "
                            + BijuManager.getJinchurikiName(server, tails)), false);
            return 1;
        }
        context.getSource().sendFailure(Component.literal(
                BijuManager.displayName(tails) + " is not currently loaded or sealed."));
        return 0;
    }

    // ===== /locateentity jinchuriki list =====
    private static int jinchurikiList(CommandContext<CommandSourceStack> context) {
        MinecraftServer server = context.getSource().getServer();
        for (String row : BijuManager.listAssignments(server)) {
            context.getSource().sendSuccess(() -> Component.literal(row), false);
        }
        return 1;
    }

    // ===== /locateentity jinchuriki revoke all =====
    private static int jinchurikiRevokeAll(CommandContext<CommandSourceStack> context) {
        int revoked = BijuManager.revokeAll(context.getSource().getServer());
        context.getSource().sendSuccess(() -> Component.literal("Revoked " + revoked + " jinchuriki."), true);
        return revoked;
    }

    // ===== /locateentity jinchuriki revoke <tails> =====
    private static int jinchurikiRevokeByTail(CommandContext<CommandSourceStack> context) {
        int tails = IntegerArgumentType.getInteger(context, "tails");
        if (BijuManager.revokeByTail(context.getSource().getServer(), tails)) {
            context.getSource().sendSuccess(() ->
                    Component.literal("Revoked the " + BijuManager.displayName(tails) + " jinchuriki."), true);
            return 1;
        }
        context.getSource().sendFailure(Component.literal("No jinchuriki assigned for " + BijuManager.displayName(tails) + "."));
        return 0;
    }

    // ===== /locateentity jinchuriki assign <player> <tails> =====
    private static int jinchurikiAssign(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        int tails = IntegerArgumentType.getInteger(context, "tails");
        if (BijuManager.setPlayerAsJinchuriki(target, tails)) {
            context.getSource().sendSuccess(() -> Component.literal(
                    "Assigned " + target.getScoreboardName() + " as the "
                            + BijuManager.displayName(tails) + " jinchuriki."), true);
            return 1;
        }
        context.getSource().sendFailure(Component.literal("Could not assign jinchuriki (invalid tail count or already sealed)."));
        return 0;
    }

    // ===== /narutomax [targets] =====
    private static int maxPlayers(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets) {
        for (ServerPlayer player : targets) {
            maxPlayer(player);
            context.getSource().sendSuccess(() -> Component.literal(
                    "Maxed ninja level, chakra, and all jutsu for " + player.getScoreboardName() + "."), true);
        }
        return targets.size();
    }

    private static void maxPlayer(ServerPlayer player) {
        NarutomodModVariables.setBattleExperience(player, MAX_BATTLE_EXPERIENCE);
        double chakraMax = NarutomodModVariables.getBattleExperience(player) * 0.5D;
        NarutomodModVariables.get(player).putDouble(NarutomodModVariables.CHAKRA_PATHWAY_SYSTEM, chakraMax);

        for (RegistryObject<Item> registered : ModItems.all()) {
            Item item = registered.get();
            if (item instanceof JutsuItem jutsuItem) {
                ItemStack stack = findInInventory(player, item);
                if (stack != null) {
                    jutsuItem.maxOut(stack, player);
                } else {
                    ItemStack created = new ItemStack(item);
                    jutsuItem.maxOut(created, player);
                    give(player, created);
                }
            }
        }

        Item gates = ModItems.EIGHTGATES.get();
        ItemStack gatesStack = findInInventory(player, gates);
        boolean missingGates = gatesStack == null;
        if (missingGates) {
            gatesStack = new ItemStack(gates);
        }
        if (gatesStack.getItem() instanceof EightGatesItem eightGatesItem) {
            eightGatesItem.bindOwner(gatesStack, player);
            eightGatesItem.setBattleXP(gatesStack, MAX_EIGHT_GATES_XP);
        }
        if (missingGates) {
            give(player, gatesStack);
        }

        NarutomodModVariables.sync(player);
    }

    private static ItemStack findInInventory(ServerPlayer player, Item item) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(item)) {
                return stack;
            }
        }
        return null;
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}
