package net.klouse.kteams.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.klouse.kteams.KTeams;
import net.klouse.kteams.gui.ManagedGui;
import net.klouse.kteams.gui.TeamGUI;
import net.klouse.kteams.gui.TeamGuiContext;
import net.klouse.kteams.gui.TeamGuiState;
import net.klouse.kteams.gui.confirm.ConfirmContext;
import net.klouse.kteams.gui.confirm.ConfirmGUI;
import net.klouse.kteams.gui.confirm.ConfirmType;
import net.klouse.kteams.gui.invite.InviteGUI;
import net.klouse.kteams.gui.member.MemberGUI;
import net.klouse.kteams.gui.member.MemberGuiContext;
import net.klouse.kteams.gui.member.MemberSettingsContext;
import net.klouse.kteams.gui.member.MemberSettingsGUI;
import net.klouse.kteams.gui.settings.TeamSettingsGUI;
import net.klouse.kteams.manager.TeamChatManager;
import net.klouse.kteams.manager.TeamManager;
import net.klouse.kteams.manager.TeamPermission;
import net.klouse.kteams.util.ChatUtil;
import net.klouse.kteams.util.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.UUID;

public final class TeamListener implements Listener {

    private final KTeams plugin;
    private final TeamManager manager;
    private final TeamGUI gui;
    private final MemberGUI memberGUI;
    private final ConfirmGUI confirmGUI;
    private final InviteGUI inviteGUI;
    private final MemberSettingsGUI memberSettingsGUI;
    private final TeamSettingsGUI teamSettingsGUI;
    private final TeamChatManager teamChatManager;

    public TeamListener(KTeams plugin) {
        this.plugin = plugin;
        this.manager = plugin.getTeamManager();
        this.gui = plugin.getTeamGUI();
        this.memberGUI = plugin.getMemberGUI();
        this.confirmGUI = plugin.getConfirmGUI();
        this.inviteGUI = plugin.getInviteGUI();
        this.memberSettingsGUI = plugin.getMemberSettingsGUI();
        this.teamSettingsGUI = plugin.getTeamSettingsGUI();
        this.teamChatManager = plugin.getTeamChatManager();
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ManagedGui.Type guiType = ManagedGui.getType(event.getView());
        if (guiType == null) {
            return;
        }

        switch (guiType) {
            case TEAM_MAIN, TEAM_NO_TEAM -> handleMainGui(event, player);
            case MEMBER_LIST -> handleMemberGui(event, player);
            case CONFIRM -> handleConfirmGui(event, player);
            case MEMBER_SETTINGS -> handleMemberSettingsGui(event, player);
            case TEAM_SETTINGS -> handleTeamSettingsGui(event, player);
            case INVITE -> handleInviteGui(event, player);
            case ALLY_MANAGER -> {
            }
        }
    }

    private void handleMainGui(InventoryClickEvent event, Player player) {
        if (event.getClickedInventory() == null) return;
        if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;

        event.setCancelled(true);

        TeamGuiContext context = gui.getContext(player.getUniqueId());
        if (context == null) return;
        if (context.state() == TeamGuiState.NO_TEAM) {
            SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
            return;
        }

        String teamName = context.teamName();
        if (teamName == null) return;

        switch (event.getRawSlot()) {
            case TeamGUI.SLOT_INFO -> {
                player.sendMessage(ChatUtil.message(plugin, "team-info-header", "{name}", teamName));
                player.sendMessage(ChatUtil.message(plugin, "team-info-leader", "{leader}", manager.getLeaderName(teamName)));
                player.sendMessage(ChatUtil.message(plugin, "team-info-members", "{members}", String.join(", ", manager.getMemberNames(teamName))));
                SoundUtil.play(player, plugin.getConfig().getString("sounds.click"), 1f, 1f);
            }
            case TeamGUI.SLOT_MEMBERS -> {
                memberGUI.open(player, teamName, 1);
                player.sendMessage(ChatUtil.message(plugin, "gui-open-members"));
                SoundUtil.play(player, plugin.getConfig().getString("sounds.click"), 1f, 1f);
            }
            case TeamGUI.SLOT_INVITE -> {
                if (!manager.isLeader(player.getUniqueId(), teamName) && !manager.hasPermission(player.getUniqueId(), teamName, TeamPermission.INVITE)) {
                    player.sendMessage(ChatUtil.message(plugin, "gui-only-leader"));
                    SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
                    return;
                }

                inviteGUI.open(player);
                SoundUtil.play(player, plugin.getConfig().getString("sounds.click"), 1f, 1f);
            }
            case TeamGUI.SLOT_SETTINGS -> {
                if (!manager.isLeader(player.getUniqueId(), teamName)) {
                    player.sendMessage(ChatUtil.message(plugin, "settings-only-leader"));
                    SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
                    return;
                }

                teamSettingsGUI.open(player, teamName);
                player.sendMessage(ChatUtil.message(plugin, "gui-open-settings"));
                SoundUtil.play(player, plugin.getConfig().getString("sounds.click"), 1f, 1f);
            }
            case TeamGUI.SLOT_LEAVE -> {
                if (manager.isLeader(player.getUniqueId(), teamName)) {
                    player.sendMessage(ChatUtil.message(plugin, "disband-failed"));
                    SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
                    return;
                }

                confirmGUI.open(player, new ConfirmContext(player.getUniqueId(), ConfirmType.LEAVE, teamName, null, teamName, 1));
                SoundUtil.play(player, plugin.getConfig().getString("sounds.click"), 1f, 1f);
            }
            case TeamGUI.SLOT_DISBAND -> {
                if (!manager.isLeader(player.getUniqueId(), teamName)) {
                    player.sendMessage(ChatUtil.message(plugin, "gui-only-leader"));
                    SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
                    return;
                }

                confirmGUI.open(player, new ConfirmContext(player.getUniqueId(), ConfirmType.DISBAND, teamName, null, teamName, 1));
                SoundUtil.play(player, plugin.getConfig().getString("sounds.click"), 1f, 1f);
            }
            case TeamGUI.SLOT_CLOSE -> {
                player.closeInventory();
                SoundUtil.play(player, plugin.getConfig().getString("sounds.click"), 1f, 1f);
            }
            default -> SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
        }
    }

    private void handleMemberGui(InventoryClickEvent event, Player player) {
        if (event.getClickedInventory() == null) return;
        if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;

        event.setCancelled(true);

        MemberGuiContext context = memberGUI.getContext(player.getUniqueId());
        if (context == null) return;

        String teamName = context.teamName();
        int page = context.page();

        switch (event.getRawSlot()) {
            case MemberGUI.SLOT_PREV -> {
                if (page <= 1) {
                    player.sendMessage(ChatUtil.message(plugin, "pagination-no-more-pages"));
                    SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
                    return;
                }
                memberGUI.open(player, teamName, page - 1);
                SoundUtil.play(player, plugin.getConfig().getString("sounds.click"), 1f, 1f);
                return;
            }
            case MemberGUI.SLOT_BACK -> {
                gui.open(player);
                SoundUtil.play(player, plugin.getConfig().getString("sounds.click"), 1f, 1f);
                return;
            }
            case MemberGUI.SLOT_NEXT -> {
                int maxPage = memberGUI.getMaxPage(teamName);
                if (page >= maxPage) {
                    player.sendMessage(ChatUtil.message(plugin, "pagination-no-more-pages"));
                    SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
                    return;
                }
                memberGUI.open(player, teamName, page + 1);
                SoundUtil.play(player, plugin.getConfig().getString("sounds.click"), 1f, 1f);
                return;
            }
        }

        UUID selected = memberGUI.getMemberBySlot(teamName, event.getRawSlot(), page);
        if (selected == null) {
            SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(selected);
        String targetName = target.getName() == null ? selected.toString() : target.getName();
        boolean leader = manager.isLeader(selected, teamName);

        if (event.getClick() == ClickType.RIGHT) {
            boolean allowed = manager.isLeader(player.getUniqueId(), teamName) || manager.hasPermission(player.getUniqueId(), teamName, TeamPermission.KICK);
            if (!allowed) {
                player.sendMessage(ChatUtil.message(plugin, "gui-only-leader"));
                SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
                return;
            }

            if (player.getUniqueId().equals(selected)) {
                player.sendMessage(ChatUtil.message(plugin, "member-cannot-kick-self"));
                SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
                return;
            }

            if (leader) {
                player.sendMessage(ChatUtil.message(plugin, "member-cannot-kick-leader"));
                SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
                return;
            }

            confirmGUI.open(player, new ConfirmContext(
                    player.getUniqueId(),
                    ConfirmType.KICK,
                    teamName,
                    selected,
                    targetName,
                    page
            ));
            player.sendMessage(ChatUtil.message(plugin, "member-kick-prepared", "{player}", targetName));
            SoundUtil.play(player, plugin.getConfig().getString("sounds.click"), 1f, 1f);
            return;
        }

        memberSettingsGUI.open(player, teamName, selected, page);
        SoundUtil.play(player, plugin.getConfig().getString("sounds.click"), 1f, 1f);
    }

    private void handleInviteGui(InventoryClickEvent event, Player player) {
        if (event.getClickedInventory() == null) return;
        if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;

        event.setCancelled(true);

        if (event.getRawSlot() == InviteGUI.SLOT_BACK) {
            gui.open(player);
            SoundUtil.play(player, plugin.getConfig().getString("sounds.click"), 1f, 1f);
            return;
        }

        Player target = inviteGUI.getTarget(player.getUniqueId(), event.getRawSlot());
        if (target == null) {
            SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
            return;
        }

        boolean success = manager.invitePlayer(player, target);
        if (!success) {
            player.sendMessage(ChatUtil.message(plugin, "invite-failed"));
            inviteGUI.open(player);
            SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
            return;
        }

        String teamName = manager.getTeamName(player.getUniqueId());
        player.sendMessage(ChatUtil.message(plugin, "invite-sent", "{player}", target.getName()));
        target.sendMessage(ChatUtil.message(plugin, "invite-received",
                "{player}", player.getName(),
                "{team}", teamName == null ? "Unknown" : teamName));
        target.sendMessage(Component.text("[ACCEPT INVITE]")
                .clickEvent(ClickEvent.runCommand("/team accept")));

        inviteGUI.open(player);
        SoundUtil.play(player, plugin.getConfig().getString("sounds.success"), 1f, 1f);
        SoundUtil.play(target, plugin.getConfig().getString("sounds.notify"), 1f, 1.15f);
    }

    private void handleConfirmGui(InventoryClickEvent event, Player player) {
        if (event.getClickedInventory() == null) return;
        if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;

        event.setCancelled(true);

        ConfirmContext context = confirmGUI.getContext(player.getUniqueId());
        if (context == null) return;

        switch (event.getRawSlot()) {
            case ConfirmGUI.SLOT_CANCEL -> {
                confirmGUI.clearContext(player.getUniqueId());

                if (context.type() == ConfirmType.KICK) {
                    memberGUI.open(player, context.teamName(), context.returnPage());
                } else {
                    gui.open(player);
                }

                SoundUtil.play(player, plugin.getConfig().getString("sounds.click"), 1f, 1f);
            }

            case ConfirmGUI.SLOT_CONFIRM -> {
                boolean success = false;

                switch (context.type()) {
                    case LEAVE -> success = manager.leaveTeam(player);
                    case DISBAND -> success = manager.disbandTeam(player);
                    case KICK -> {
                        if (context.targetPlayer() != null) {
                            success = manager.kickPlayer(player, context.targetPlayer());
                        }
                    }
                }

                confirmGUI.clearContext(player.getUniqueId());

                if (!success) {
                    switch (context.type()) {
                        case LEAVE -> player.sendMessage(ChatUtil.message(plugin, "leave-failed"));
                        case DISBAND -> player.sendMessage(ChatUtil.message(plugin, "disband-failed"));
                        case KICK -> player.sendMessage(ChatUtil.message(plugin, "kick-failed"));
                    }
                    SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
                    return;
                }

                switch (context.type()) {
                    case LEAVE -> {
                        teamChatManager.disable(player.getUniqueId());
                        player.sendMessage(ChatUtil.message(plugin, "leave-success"));
                    }
                    case DISBAND -> {
                        teamChatManager.disable(player.getUniqueId());
                        player.sendMessage(ChatUtil.message(plugin, "disband-success"));
                    }
                    case KICK -> {
                        if (context.targetPlayer() != null) {
                            teamChatManager.disable(context.targetPlayer());
                        }
                        player.sendMessage(ChatUtil.message(plugin, "kick-success", "{player}", context.targetName()));
                    }
                }

                SoundUtil.play(player, plugin.getConfig().getString("sounds.success"), 1f, 1f);

                if (context.type() == ConfirmType.KICK) {
                    memberGUI.open(player, context.teamName(), context.returnPage());
                } else {
                    player.closeInventory();
                }
            }

            default -> SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
        }
    }

    private void handleMemberSettingsGui(InventoryClickEvent event, Player player) {
        if (event.getClickedInventory() == null) return;
        if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;

        event.setCancelled(true);

        MemberSettingsContext context = memberSettingsGUI.getContext(player.getUniqueId());
        if (context == null) return;

        switch (event.getRawSlot()) {
            case MemberSettingsGUI.SLOT_BACK -> {
                memberGUI.open(player, context.teamName(), context.returnPage());
                SoundUtil.play(player, plugin.getConfig().getString("sounds.click"), 1f, 1f);
            }
            case MemberSettingsGUI.SLOT_INVITE ->
                    togglePermission(player, context, TeamPermission.INVITE, "INVITE");
            case MemberSettingsGUI.SLOT_KICK ->
                    togglePermission(player, context, TeamPermission.KICK, "KICK");
            case MemberSettingsGUI.SLOT_CHAT ->
                    togglePermission(player, context, TeamPermission.CHAT, "CHAT");
            case MemberSettingsGUI.SLOT_HOME ->
                    togglePermission(player, context, TeamPermission.HOME, "HOME");
            default -> SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
        }
    }

    private void handleTeamSettingsGui(InventoryClickEvent event, Player player) {
        if (event.getClickedInventory() == null) return;
        if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;

        event.setCancelled(true);

        String teamName = teamSettingsGUI.getContext(player.getUniqueId());
        if (teamName == null) return;

        switch (event.getRawSlot()) {
            case TeamSettingsGUI.SLOT_BACK -> {
                gui.open(player);
                SoundUtil.play(player, plugin.getConfig().getString("sounds.click"), 1f, 1f);
            }

            case TeamSettingsGUI.SLOT_TEAM_INVITES -> {
                manager.toggleTeamInvites(teamName);
                boolean state = manager.getTeamSettings(teamName).teamInvitesEnabled();
                player.sendMessage(ChatUtil.message(plugin, "settings-updated", "{setting}", "TEAM_INVITES", "{state}", state ? "ON" : "OFF"));
                teamSettingsGUI.open(player, teamName);
                SoundUtil.play(player, plugin.getConfig().getString("sounds.success"), 1f, 1f);
            }

            case TeamSettingsGUI.SLOT_FRIENDLY_FIRE -> {
                manager.toggleFriendlyFire(teamName);
                boolean state = manager.getTeamSettings(teamName).friendlyFireEnabled();
                player.sendMessage(ChatUtil.message(plugin, "settings-updated", "{setting}", "FRIENDLY_FIRE", "{state}", state ? "ON" : "OFF"));
                teamSettingsGUI.open(player, teamName);
                SoundUtil.play(player, plugin.getConfig().getString("sounds.success"), 1f, 1f);
            }

            case TeamSettingsGUI.SLOT_CHAT_ISOLATION -> {
                manager.toggleChatIsolation(teamName);
                boolean state = manager.getTeamSettings(teamName).chatIsolationEnabled();
                player.sendMessage(ChatUtil.message(plugin, "settings-updated", "{setting}", "CHAT_ISOLATION", "{state}", state ? "ON" : "OFF"));
                teamSettingsGUI.open(player, teamName);
                SoundUtil.play(player, plugin.getConfig().getString("sounds.success"), 1f, 1f);
            }

            default -> SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
        }
    }

    private void togglePermission(Player player, MemberSettingsContext context, TeamPermission permission, String permissionName) {
        boolean success = manager.togglePermission(player.getUniqueId(), context.teamName(), context.targetId(), permission);
        if (!success) {
            SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
            return;
        }

        boolean state = manager.hasPermission(context.targetId(), context.teamName(), permission);
        player.sendMessage(ChatUtil.message(plugin, "perm-updated",
                "{permission}", permissionName,
                "{player}", context.targetName(),
                "{state}", state ? "ON" : "OFF"));

        memberSettingsGUI.open(player, context.teamName(), context.targetId(), context.returnPage());
        SoundUtil.play(player, plugin.getConfig().getString("sounds.success"), 1f, 1f);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        ManagedGui.Type closedType = ManagedGui.getType(event.getInventory());
        if (closedType == null) {
            return;
        }

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (ManagedGui.getType(player.getOpenInventory()) == closedType) {
                return;
            }
            clearContext(player.getUniqueId(), closedType);
        });
    }

    private void clearContext(UUID playerId, ManagedGui.Type closedType) {
        switch (closedType) {
            case TEAM_MAIN, TEAM_NO_TEAM -> gui.clearContext(playerId);
            case MEMBER_LIST -> memberGUI.clearContext(playerId);
            case CONFIRM -> confirmGUI.clearContext(playerId);
            case MEMBER_SETTINGS -> memberSettingsGUI.clearContext(playerId);
            case TEAM_SETTINGS -> teamSettingsGUI.clearContext(playerId);
            case INVITE -> inviteGUI.clear(playerId);
            case ALLY_MANAGER -> {
            }
        }
    }
}
