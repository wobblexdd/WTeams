package net.wobble.teams.listener;

import net.wobble.teams.WobbleTeams;
import net.wobble.teams.gui.TeamGUI;
import net.wobble.teams.gui.TeamGuiContext;
import net.wobble.teams.gui.TeamGuiState;
import net.wobble.teams.gui.confirm.ConfirmContext;
import net.wobble.teams.gui.confirm.ConfirmGUI;
import net.wobble.teams.gui.confirm.ConfirmType;
import net.wobble.teams.gui.member.MemberGUI;
import net.wobble.teams.gui.member.MemberGuiContext;
import net.wobble.teams.gui.member.MemberSettingsContext;
import net.wobble.teams.gui.member.MemberSettingsGUI;
import net.wobble.teams.gui.settings.TeamSettingsGUI;
import net.wobble.teams.manager.TeamChatManager;
import net.wobble.teams.manager.TeamManager;
import net.wobble.teams.manager.TeamPermission;
import net.wobble.teams.manager.TeamSettings;
import net.wobble.teams.util.ChatUtil;
import net.wobble.teams.util.SoundUtil;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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

    private final WobbleTeams plugin;
    private final TeamManager manager;
    private final TeamGUI gui;
    private final MemberGUI memberGUI;
    private final ConfirmGUI confirmGUI;
    private final MemberSettingsGUI memberSettingsGUI;
    private final TeamSettingsGUI teamSettingsGUI;
    private final TeamChatManager teamChatManager;

    public TeamListener(WobbleTeams plugin) {
        this.plugin = plugin;
        this.manager = plugin.getTeamManager();
        this.gui = plugin.getTeamGUI();
        this.memberGUI = plugin.getMemberGUI();
        this.confirmGUI = plugin.getConfirmGUI();
        this.memberSettingsGUI = plugin.getMemberSettingsGUI();
        this.teamSettingsGUI = plugin.getTeamSettingsGUI();
        this.teamChatManager = plugin.getTeamChatManager();
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getView().title() == null) return;

        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());

        if (title.equalsIgnoreCase("TEAM")) {
            handleMainGui(event, player);
            return;
        }

        if (title.equalsIgnoreCase("MEMBER MANAGER")) {
            handleMemberGui(event, player);
            return;
        }

        if (title.equalsIgnoreCase("CONFIRM LEAVING TEAM")
                || title.equalsIgnoreCase("CONFIRM KICKING PLAYER")
                || title.equalsIgnoreCase("CONFIRM DISBANDING TEAM")) {
            handleConfirmGui(event, player);
            return;
        }

        if (title.equalsIgnoreCase("MEMBER SETTINGS")) {
            handleMemberSettingsGui(event, player);
            return;
        }

        if (title.equalsIgnoreCase("TEAM SETTINGS")) {
            handleTeamSettingsGui(event, player);
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
                player.closeInventory();
                player.sendMessage(ChatUtil.message(plugin, "gui-open-invite-help"));
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
        if (!(event.getPlayer() instanceof Player player)) return;
        gui.clearContext(player.getUniqueId());
        memberGUI.clearContext(player.getUniqueId());
        confirmGUI.clearContext(player.getUniqueId());
        memberSettingsGUI.clearContext(player.getUniqueId());
        teamSettingsGUI.clearContext(player.getUniqueId());
    }
}
