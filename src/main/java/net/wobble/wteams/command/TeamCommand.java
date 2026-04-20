package net.wobble.wteams.command;

import net.wobble.wteams.WTeams;
import net.wobble.wteams.gui.TeamGUI;
import net.wobble.wteams.gui.invite.InviteGUI;
import net.wobble.wteams.manager.InviteData;
import net.wobble.wteams.manager.TeamChatManager;
import net.wobble.wteams.manager.TeamHomeTeleportManager;
import net.wobble.wteams.manager.TeamManager;
import net.wobble.wteams.manager.TeamPermission;
import net.wobble.wteams.util.ChatUtil;
import net.wobble.wteams.util.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public final class TeamCommand implements CommandExecutor, TabCompleter {

    private final WTeams plugin;
    private final TeamManager manager;
    private final TeamGUI gui;
    private final InviteGUI inviteGUI;
    private final TeamChatManager chatManager;
    private final TeamHomeTeleportManager homeTeleportManager;

    public TeamCommand(WTeams plugin, TeamManager manager, TeamGUI gui) {
        this.plugin = plugin;
        this.manager = manager;
        this.gui = gui;
        this.inviteGUI = plugin.getInviteGUI();
        this.chatManager = plugin.getTeamChatManager();
        this.homeTeleportManager = plugin.getTeamHomeTeleportManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtil.message(plugin, "only-player"));
            return true;
        }

        manager.cleanupExpiredInvites();

        if (args.length == 0) {
            gui.open(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "menu" -> gui.open(player);
            case "create" -> {
                if (args.length < 2) {
                    player.sendMessage(ChatUtil.message(plugin, "usage-create"));
                    SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
                    return true;
                }

                String name = args[1];

                if (manager.getTeamName(player.getUniqueId()) != null) {
                    player.sendMessage(ChatUtil.message(plugin, "already-in-team"));
                    SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
                    return true;
                }

                if (manager.getTeam(name) != null) {
                    player.sendMessage(ChatUtil.message(plugin, "team-already-exists"));
                    SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
                    return true;
                }

                boolean success = manager.createTeam(player, name);
                if (!success) {
                    player.sendMessage(ChatUtil.message(plugin, "create-failed"));
                    SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
                    return true;
                }

                player.sendMessage(ChatUtil.message(plugin, "create-success", "{name}", name));
                SoundUtil.play(player, plugin.getConfig().getString("sounds.success"), 1f, 1f);
            }
            case "invitegui" -> {
                String teamName = manager.getTeamName(player.getUniqueId());
                if (teamName == null) {
                    player.sendMessage(ChatUtil.message(plugin, "no-team"));
                    SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
                    return true;
                }
                if (!manager.isLeader(player.getUniqueId(), teamName)) {
                    player.sendMessage(ChatUtil.message(plugin, "gui-only-leader"));
                    SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
                    return true;
                }
                inviteGUI.open(player);
                SoundUtil.play(player, plugin.getConfig().getString("sounds.click"), 1f, 1f);
            }
            case "invite" -> {
                if (args.length < 2) {
                    player.sendMessage(ChatUtil.message(plugin, "usage-invite"));
                    SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
                    return true;
                }

                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    player.sendMessage(ChatUtil.message(plugin, "player-not-found"));
                    SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
                    return true;
                }

                if (target.getUniqueId().equals(player.getUniqueId())) {
                    player.sendMessage(ChatUtil.message(plugin, "cannot-invite-self"));
                    SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
                    return true;
                }

                if (manager.getTeamName(target.getUniqueId()) != null) {
                    player.sendMessage(ChatUtil.message(plugin, "target-already-in-team"));
                    SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
                    return true;
                }

                boolean success = manager.invitePlayer(player, target);
                if (!success) {
                    player.sendMessage(ChatUtil.message(plugin, "invite-failed"));
                    SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
                    return true;
                }

                String teamName = manager.getTeamName(player.getUniqueId());
                player.sendMessage(ChatUtil.message(plugin, "invite-sent", "{player}", target.getName()));
                target.sendMessage(ChatUtil.message(plugin, "invite-received",
                        "{player}", player.getName(),
                        "{team}", teamName == null ? "Unknown" : teamName));
                target.sendMessage(net.kyori.adventure.text.Component.text("[ACCEPT INVITE]")
                        .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/team accept")));
                SoundUtil.play(player, plugin.getConfig().getString("sounds.success"), 1f, 1f);
                SoundUtil.play(target, plugin.getConfig().getString("sounds.notify"), 1f, 1.15f);
            }
            case "accept" -> {
                InviteData invite = manager.getInvite(player.getUniqueId());
                if (invite == null) {
                    player.sendMessage(ChatUtil.message(plugin, "no-pending-invite"));
                    SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
                    return true;
                }

                if (invite.isExpired()) {
                    player.sendMessage(ChatUtil.message(plugin, "invite-expired"));
                    SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
                    return true;
                }

                String joinedTeam = manager.acceptInvite(player);
                if (joinedTeam == null) {
                    player.sendMessage(ChatUtil.message(plugin, "accept-failed"));
                    SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
                    return true;
                }

                player.sendMessage(ChatUtil.message(plugin, "accept-success", "{team}", joinedTeam));
                broadcastToTeam(joinedTeam, ChatUtil.message(plugin, "team-broadcast-join", "{player}", player.getName()), null);
                SoundUtil.play(player, plugin.getConfig().getString("sounds.success"), 1f, 1f);
            }
            case "deny" -> {
                InviteData invite = manager.getInvite(player.getUniqueId());
                if (invite == null) {
                    player.sendMessage(ChatUtil.message(plugin, "no-pending-invite"));
                    SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
                    return true;
                }

                boolean success = manager.denyInvite(player);
                if (!success) {
                    player.sendMessage(ChatUtil.message(plugin, "deny-failed"));
                    SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
                    return true;
                }

                player.sendMessage(ChatUtil.message(plugin, "deny-success"));
                SoundUtil.play(player, plugin.getConfig().getString("sounds.success"), 1f, 1f);
            }
            case "ally" -> handleAlly(player, args);
            case "chat" -> {
                String teamName = manager.getTeamName(player.getUniqueId());
                if (teamName == null) {
                    player.sendMessage(ChatUtil.message(plugin, "team-chat-no-team"));
                    SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
                    return true;
                }

                if (!manager.isLeader(player.getUniqueId(), teamName)
                        && !manager.hasPermission(player.getUniqueId(), teamName, TeamPermission.CHAT)) {
                    player.sendMessage(ChatUtil.message(plugin, "team-chat-no-permission"));
                    SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
                    return true;
                }

                if (args.length == 1) {
                    boolean enabled = chatManager.toggle(player.getUniqueId());
                    player.sendMessage(ChatUtil.message(plugin, enabled ? "team-chat-enabled" : "team-chat-disabled"));
                    SoundUtil.play(player, plugin.getConfig().getString(enabled ? "sounds.success" : "sounds.click"), 1f, 1f);
                    return true;
                }

                String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                String formatted = ChatUtil.message(plugin, "team-chat-format",
                        "{player}", player.getName(),
                        "{message}", message);

                broadcastToTeam(teamName, formatted, player.getUniqueId());
                player.sendMessage(formatted);
                SoundUtil.play(player, plugin.getConfig().getString("sounds.chat"), 1f, 1f);
            }
            case "sethome" -> {
                boolean success = manager.setTeamHome(player);
                if (!success) {
                    player.sendMessage(ChatUtil.message(plugin, "team-home-no-permission"));
                    SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
                    return true;
                }

                player.sendMessage(ChatUtil.message(plugin, "team-home-set"));
                SoundUtil.play(player, plugin.getConfig().getString("sounds.success"), 1f, 1f);
            }
            case "home" -> {
                String teamName = manager.getTeamName(player.getUniqueId());
                if (teamName == null) {
                    player.sendMessage(ChatUtil.message(plugin, "team-home-no-permission"));
                    SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
                    return true;
                }

                if (!manager.isLeader(player.getUniqueId(), teamName)
                        && !manager.hasPermission(player.getUniqueId(), teamName, TeamPermission.HOME)) {
                    player.sendMessage(ChatUtil.message(plugin, "team-home-no-permission"));
                    SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
                    return true;
                }

                Location location = manager.getTeamHomeLocation(player.getUniqueId());
                if (location == null) {
                    player.sendMessage(ChatUtil.message(plugin, "team-home-not-set"));
                    SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
                    return true;
                }

                homeTeleportManager.startTeleport(player, location);
            }
            case "delhome" -> {
                boolean success = manager.deleteTeamHome(player);
                if (!success) {
                    player.sendMessage(ChatUtil.message(plugin, "team-home-no-permission"));
                    SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
                    return true;
                }

                player.sendMessage(ChatUtil.message(plugin, "team-home-deleted"));
                SoundUtil.play(player, plugin.getConfig().getString("sounds.success"), 1f, 1f);
            }
            case "rename" -> {
                if (args.length < 2) {
                    player.sendMessage(ChatUtil.message(plugin, "usage-rename"));
                    SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
                    return true;
                }

                if (!player.hasPermission("wteams.rename") && !player.hasPermission("wteams.admin")) {
                    player.sendMessage(ChatUtil.message(plugin, "rename-no-permission"));
                    SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
                    return true;
                }

                boolean success = manager.renameTeam(player.getUniqueId(), args[1]);
                if (!success) {
                    player.sendMessage(ChatUtil.message(plugin, "rename-failed"));
                    SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
                    return true;
                }

                player.sendMessage(ChatUtil.message(plugin, "rename-success", "{name}", args[1]));
                SoundUtil.play(player, plugin.getConfig().getString("sounds.success"), 1f, 1f);
            }
            case "admin" -> handleAdmin(player, args);
            case "info" -> {
                String teamName = manager.getTeamName(player.getUniqueId());
                if (teamName == null) {
                    player.sendMessage(ChatUtil.message(plugin, "no-team"));
                    SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
                    return true;
                }

                player.sendMessage(ChatUtil.message(plugin, "team-info-header", "{name}", teamName));
                player.sendMessage(ChatUtil.message(plugin, "team-info-leader", "{leader}", manager.getLeaderName(teamName)));
                player.sendMessage(ChatUtil.message(plugin, "team-info-members", "{members}", String.join(", ", manager.getMemberNames(teamName))));
                SoundUtil.play(player, plugin.getConfig().getString("sounds.click"), 1f, 1f);
            }
            case "leave" -> {
                String teamName = manager.getTeamName(player.getUniqueId());
                boolean success = manager.leaveTeam(player);
                if (!success) {
                    player.sendMessage(ChatUtil.message(plugin, "leave-failed"));
                    SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
                    return true;
                }

                chatManager.disable(player.getUniqueId());
                plugin.getAllyChatManager().disable(player.getUniqueId());
                player.sendMessage(ChatUtil.message(plugin, "leave-success"));
                if (teamName != null) {
                    broadcastToTeam(teamName, ChatUtil.message(plugin, "team-broadcast-leave", "{player}", player.getName()), null);
                }
                SoundUtil.play(player, plugin.getConfig().getString("sounds.success"), 1f, 1f);
            }
            case "disband" -> {
                String teamName = manager.getTeamName(player.getUniqueId());
                boolean success = manager.disbandTeam(player);
                if (!success) {
                    player.sendMessage(ChatUtil.message(plugin, "disband-failed"));
                    SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
                    return true;
                }

                chatManager.disable(player.getUniqueId());
                plugin.getAllyChatManager().disable(player.getUniqueId());
                player.sendMessage(ChatUtil.message(plugin, "disband-success"));
                if (teamName != null) {
                    broadcastToTeam(teamName, ChatUtil.message(plugin, "team-broadcast-disband"), null);
                }
                SoundUtil.play(player, plugin.getConfig().getString("sounds.success"), 1f, 1f);
            }
            case "reload" -> {
                if (!player.hasPermission("wteams.reload") && !player.hasPermission("wteams.admin")) {
                    player.sendMessage(ChatUtil.message(plugin, "admin-no-permission"));
                    SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
                    return true;
                }

                plugin.reloadPlugin();
                player.sendMessage(ChatUtil.message(plugin, "reload-success"));
                SoundUtil.play(player, plugin.getConfig().getString("sounds.success"), 1f, 1f);
            }
            default -> gui.open(player);
        }

        return true;
    }

    private void handleAlly(Player player, String[] args) {
        if (!plugin.getConfig().getBoolean("allies.enabled", true)) {
            player.sendMessage(ChatUtil.message(plugin, "ally-disabled"));
            SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
            return;
        }

        String teamName = manager.getTeamName(player.getUniqueId());
        if (teamName == null) {
            player.sendMessage(ChatUtil.message(plugin, "ally-no-team"));
            SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
            return;
        }

        if (!manager.isLeader(player.getUniqueId(), teamName)) {
            player.sendMessage(ChatUtil.message(plugin, "ally-only-leader"));
            SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ChatUtil.message(plugin, "usage-ally"));
            SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
            return;
        }

        String sub = args[1].toLowerCase();

        if (sub.equals("list")) {
            List<String> allies = manager.getAllies(teamName);
            if (allies.isEmpty()) {
                player.sendMessage(ChatUtil.message(plugin, "ally-list-empty"));
            } else {
                player.sendMessage(ChatUtil.message(plugin, "ally-list-header", "{allies}", String.join(", ", allies)));
            }
            SoundUtil.play(player, plugin.getConfig().getString("sounds.click"), 1f, 1f);
            return;
        }

        if (sub.equals("chat")) {
            List<String> allies = manager.getAllies(teamName);
            if (allies.isEmpty()) {
                player.sendMessage(ChatUtil.message(plugin, "ally-chat-no-allies"));
                SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
                return;
            }

            if (args.length == 2) {
                boolean enabled = plugin.getAllyChatManager().toggle(player.getUniqueId());
                player.sendMessage(ChatUtil.message(plugin, enabled ? "ally-chat-enabled" : "ally-chat-disabled"));
                SoundUtil.play(player, plugin.getConfig().getString(enabled ? "sounds.success" : "sounds.click"), 1f, 1f);
                return;
            }

            String message = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            String formatted = ChatUtil.message(plugin, "ally-chat-format",
                    "{team}", teamName,
                    "{player}", player.getName(),
                    "{message}", message);

            broadcastToAlliance(teamName, formatted, player.getUniqueId());
            player.sendMessage(formatted);
            SoundUtil.play(player, plugin.getConfig().getString("sounds.chat"), 1f, 1f);
            return;
        }

        if (args.length < 3) {
            player.sendMessage(ChatUtil.message(plugin, "usage-ally"));
            SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
            return;
        }

        String targetTeam = args[2];
        if (manager.getTeam(targetTeam) == null) {
            player.sendMessage(ChatUtil.message(plugin, "ally-team-not-found", "{team}", targetTeam));
            SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
            return;
        }

        if (teamName.equalsIgnoreCase(targetTeam)) {
            player.sendMessage(ChatUtil.message(plugin, "ally-cannot-self"));
            SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
            return;
        }

        switch (sub) {
            case "invite" -> {
                if (manager.areAllies(teamName, targetTeam)) {
                    player.sendMessage(ChatUtil.message(plugin, "ally-already", "{team}", targetTeam));
                    SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
                    return;
                }

                boolean success = manager.sendAllyRequest(teamName, targetTeam);
                if (!success) {
                    player.sendMessage(ChatUtil.message(plugin, "ally-request-already", "{team}", targetTeam));
                    SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
                    return;
                }

                player.sendMessage(ChatUtil.message(plugin, "ally-invite-sent", "{team}", targetTeam));
                SoundUtil.play(player, plugin.getConfig().getString("sounds.success"), 1f, 1f);
            }
            case "accept" -> {
                if (!manager.hasPendingAllyRequest(teamName, targetTeam)) {
                    player.sendMessage(ChatUtil.message(plugin, "ally-no-request", "{team}", targetTeam));
                    SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
                    return;
                }

                manager.acceptAllyRequest(teamName, targetTeam);
                player.sendMessage(ChatUtil.message(plugin, "ally-accept-success", "{team}", targetTeam));
                SoundUtil.play(player, plugin.getConfig().getString("sounds.success"), 1f, 1f);
            }
            case "deny" -> {
                if (!manager.denyAllyRequest(teamName, targetTeam)) {
                    player.sendMessage(ChatUtil.message(plugin, "ally-no-request", "{team}", targetTeam));
                    SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
                    return;
                }

                player.sendMessage(ChatUtil.message(plugin, "ally-deny-success", "{team}", targetTeam));
                SoundUtil.play(player, plugin.getConfig().getString("sounds.success"), 1f, 1f);
            }
            default -> {
                player.sendMessage(ChatUtil.message(plugin, "usage-ally"));
                SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
            }
        }
    }

    private void handleAdmin(Player player, String[] args) {
        if (!player.hasPermission("wteams.admin")) {
            player.sendMessage(ChatUtil.message(plugin, "admin-no-permission"));
            SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
            return;
        }

        if (args.length < 3) {
            player.sendMessage(ChatUtil.message(plugin, "usage-admin"));
            SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
            return;
        }

        String sub = args[1].toLowerCase();
        String teamName = args[2];

        switch (sub) {
            case "info" -> {
                if (manager.getTeam(teamName) == null) {
                    player.sendMessage(ChatUtil.message(plugin, "admin-team-not-found", "{team}", teamName));
                    return;
                }

                player.sendMessage(ChatUtil.message(plugin, "team-info-header", "{name}", teamName));
                player.sendMessage(ChatUtil.message(plugin, "team-info-leader", "{leader}", manager.getLeaderName(teamName)));
                player.sendMessage(ChatUtil.message(plugin, "team-info-members", "{members}", String.join(", ", manager.getMemberNames(teamName))));
            }
            case "disband" -> {
                boolean success = manager.adminDisbandTeam(teamName);
                if (!success) {
                    player.sendMessage(ChatUtil.message(plugin, "admin-team-not-found", "{team}", teamName));
                    return;
                }

                player.sendMessage(ChatUtil.message(plugin, "admin-disband-success", "{team}", teamName));
            }
            case "rename" -> {
                if (args.length < 4) {
                    player.sendMessage(ChatUtil.message(plugin, "usage-admin"));
                    return;
                }

                boolean success = manager.adminRenameTeam(teamName, args[3]);
                if (!success) {
                    player.sendMessage(ChatUtil.message(plugin, "rename-failed"));
                    return;
                }

                player.sendMessage(ChatUtil.message(plugin, "admin-rename-success", "{name}", args[3]));
            }
            case "setleader" -> {
                if (args.length < 4) {
                    player.sendMessage(ChatUtil.message(plugin, "usage-admin"));
                    return;
                }

                OfflinePlayer target = Bukkit.getOfflinePlayer(args[3]);
                boolean success = manager.adminSetLeader(teamName, target.getUniqueId());
                if (!success) {
                    player.sendMessage(ChatUtil.message(plugin, "rename-failed"));
                    return;
                }

                player.sendMessage(ChatUtil.message(plugin, "admin-setleader-success",
                        "{team}", teamName,
                        "{player}", args[3]));
            }
            default -> player.sendMessage(ChatUtil.message(plugin, "usage-admin"));
        }

        SoundUtil.play(player, plugin.getConfig().getString("sounds.success"), 1f, 1f);
    }

    private void broadcastToTeam(String teamName, String message, UUID exclude) {
        for (UUID memberId : manager.getMembers(teamName)) {
            if (exclude != null && exclude.equals(memberId)) continue;

            Player target = Bukkit.getPlayer(memberId);
            if (target != null && target.isOnline()) {
                target.sendMessage(message);
            }
        }
    }

    private void broadcastToAlliance(String teamName, String message, UUID exclude) {
        broadcastToTeam(teamName, message, exclude);

        for (String ally : manager.getAllies(teamName)) {
            for (UUID memberId : manager.getMembers(ally)) {
                if (exclude != null && exclude.equals(memberId)) continue;

                Player target = Bukkit.getPlayer(memberId);
                if (target != null && target.isOnline()) {
                    target.sendMessage(message);
                }
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();

        if (args.length == 1) {
            list.add("menu");
            list.add("create");
            list.add("invite");
            list.add("invitegui");
            list.add("accept");
            list.add("deny");
            list.add("ally");
            list.add("chat");
            list.add("sethome");
            list.add("home");
            list.add("delhome");
            list.add("rename");
            list.add("admin");
            list.add("info");
            list.add("leave");
            list.add("disband");
            list.add("reload");
            return list.stream().filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("invite")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("admin")) {
            return List.of("info", "disband", "rename", "setleader").stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .toList();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("ally")) {
            return List.of("invite", "accept", "deny", "list", "chat").stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .toList();
        }

        return List.of();
    }
}
