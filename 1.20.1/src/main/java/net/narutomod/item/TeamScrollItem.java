package net.narutomod.item;

import java.util.Collection;
import java.util.Collections;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.narutomod.PlayerTracker;

public final class TeamScrollItem extends Item {
    public static final String TEAM_NAME_TAG = "teamName";
    public static final String TEAM_DISPLAY_NAME_TAG = "teamDisplayName";
    public static final String TEAM_MEMBERS_TAG = "teamMembers";
    public static final String MEMBER_UUID_TAG = "memberUUID";

    public TeamScrollItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isCreative() && (!PlayerTracker.isNinja(player) || player.experienceLevel < 15)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("Team Scroll requires ninja experience and level 15."), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (player.isShiftKeyDown()) {
                boolean removed = removeTeamMember(stack, serverPlayer);
                serverPlayer.displayClientMessage(Component.literal(removed
                        ? "Left team " + getTeamDisplayName(level, stack) + "."
                        : "You are not in this Team Scroll team."), true);
            } else {
                boolean added = addTeamMember(stack, serverPlayer);
                serverPlayer.displayClientMessage(Component.literal(added
                        ? "Joined team " + getTeamDisplayName(level, stack) + "."
                        : "You are already in this Team Scroll team."), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, java.util.List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (level != null) {
            String displayName = getTeamDisplayName(level, stack);
            int memberCount = getStoredMemberCount(stack);
            tooltip.add(Component.literal(displayName.isEmpty() ? "No team bound" : "Team " + displayName)
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("Stored members: " + memberCount).withStyle(ChatFormatting.GRAY));
        }
    }

    public static boolean addTeamMember(ItemStack stack, ServerPlayer player) {
        PlayerTeam team = getTeamFromItem(player.level(), stack);
        if (team == null) {
            team = getOrCreateTeam(player.level(), stack, player.getScoreboardName());
        }
        if (team == null || team.getPlayers().contains(player.getScoreboardName())) {
            return false;
        }
        player.level().getScoreboard().addPlayerToTeam(player.getScoreboardName(), team);
        rememberMember(stack, player);
        return true;
    }

    public static boolean removeTeamMember(ItemStack stack, ServerPlayer player) {
        PlayerTeam team = getTeamFromItem(player.level(), stack);
        if (team == null || !team.getPlayers().contains(player.getScoreboardName())) {
            return false;
        }
        player.level().getScoreboard().removePlayerFromTeam(player.getScoreboardName(), team);
        forgetMember(stack, player);
        return true;
    }

    public static Collection<String> getTeamMembers(Level level, ItemStack stack) {
        PlayerTeam team = getTeamFromItem(level, stack);
        return team != null ? team.getPlayers() : Collections.emptyList();
    }

    public static String getTeamDisplayName(Level level, ItemStack stack) {
        PlayerTeam team = getTeamFromItem(level, stack);
        return team != null ? team.getDisplayName().getString() : "";
    }

    public static void setTeamDisplayName(Level level, ItemStack stack, String newName) {
        PlayerTeam team = getTeamFromItem(level, stack);
        if (team != null && !level.isClientSide) {
            team.setDisplayName(Component.literal(newName));
            stack.getOrCreateTag().putString(TEAM_DISPLAY_NAME_TAG, newName);
        }
    }

    @Nullable
    public static PlayerTeam getOrCreateTeam(Level level, ItemStack stack, String teamName) {
        CompoundTag tag = stack.getOrCreateTag();
        Scoreboard scoreboard = level.getScoreboard();
        PlayerTeam team = scoreboard.getPlayerTeam(teamName);
        if (team == null && !level.isClientSide) {
            team = scoreboard.addPlayerTeam(teamName);
        }
        if (team != null) {
            tag.putString(TEAM_NAME_TAG, teamName);
            tag.putString(TEAM_DISPLAY_NAME_TAG, teamName);
            if (!level.isClientSide) {
                team.setDisplayName(Component.literal(teamName));
            }
        }
        return team;
    }

    @Nullable
    private static PlayerTeam getTeamFromItem(Level level, ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TEAM_NAME_TAG)) {
            return null;
        }
        Scoreboard scoreboard = level.getScoreboard();
        String teamName = tag.getString(TEAM_NAME_TAG);
        String teamDisplayName = tag.getString(TEAM_DISPLAY_NAME_TAG);
        PlayerTeam team = scoreboard.getPlayerTeam(teamName);
        if (team == null && !level.isClientSide) {
            team = scoreboard.addPlayerTeam(teamName);
        }
        if (team == null) {
            return null;
        }
        if (!teamDisplayName.isEmpty() && !team.getDisplayName().getString().equals(teamDisplayName) && !level.isClientSide) {
            team.setDisplayName(Component.literal(teamDisplayName));
        }
        pruneMovedMembers(level, stack, team);
        return team;
    }

    private static void rememberMember(ItemStack stack, ServerPlayer player) {
        CompoundTag tag = stack.getOrCreateTag();
        ListTag members = tag.contains(TEAM_MEMBERS_TAG, Tag.TAG_LIST)
                ? tag.getList(TEAM_MEMBERS_TAG, Tag.TAG_COMPOUND)
                : new ListTag();
        for (int i = 0; i < members.size(); i++) {
            CompoundTag member = members.getCompound(i);
            if (member.hasUUID(MEMBER_UUID_TAG) && member.getUUID(MEMBER_UUID_TAG).equals(player.getUUID())) {
                return;
            }
        }
        CompoundTag member = new CompoundTag();
        member.putUUID(MEMBER_UUID_TAG, player.getUUID());
        members.add(member);
        tag.put(TEAM_MEMBERS_TAG, members);
    }

    private static void forgetMember(ItemStack stack, ServerPlayer player) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TEAM_MEMBERS_TAG, Tag.TAG_LIST)) {
            return;
        }
        ListTag members = tag.getList(TEAM_MEMBERS_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < members.size(); i++) {
            CompoundTag member = members.getCompound(i);
            if (member.hasUUID(MEMBER_UUID_TAG) && member.getUUID(MEMBER_UUID_TAG).equals(player.getUUID())) {
                members.remove(i);
                tag.put(TEAM_MEMBERS_TAG, members);
                return;
            }
        }
    }

    private static void pruneMovedMembers(Level level, ItemStack stack, PlayerTeam team) {
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TEAM_MEMBERS_TAG, Tag.TAG_LIST)) {
            return;
        }
        ListTag members = tag.getList(TEAM_MEMBERS_TAG, Tag.TAG_COMPOUND);
        boolean changed = false;
        for (int i = members.size() - 1; i >= 0; i--) {
            CompoundTag memberTag = members.getCompound(i);
            if (!memberTag.hasUUID(MEMBER_UUID_TAG)) {
                continue;
            }
            Player member = serverLevel.getPlayerByUUID(memberTag.getUUID(MEMBER_UUID_TAG));
            if (member == null) {
                continue;
            }
            PlayerTeam memberTeam = level.getScoreboard().getPlayersTeam(member.getScoreboardName());
            if (memberTeam == null && !team.getPlayers().contains(member.getScoreboardName())) {
                level.getScoreboard().addPlayerToTeam(member.getScoreboardName(), team);
            } else if (memberTeam != null && !team.isAlliedTo(memberTeam)) {
                members.remove(i);
                changed = true;
            }
        }
        if (changed) {
            tag.put(TEAM_MEMBERS_TAG, members);
        }
    }

    private static int getStoredMemberCount(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(TEAM_MEMBERS_TAG, Tag.TAG_LIST)
                ? tag.getList(TEAM_MEMBERS_TAG, Tag.TAG_COMPOUND).size()
                : 0;
    }
}
