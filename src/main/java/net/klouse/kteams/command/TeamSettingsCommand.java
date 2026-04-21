package net.klouse.kteams.command;

import net.klouse.kteams.KTeams;
import net.klouse.kteams.gui.settings.TeamSettingsGUI;
import net.klouse.kteams.manager.TeamManager;
import net.klouse.kteams.util.ChatUtil;
import net.klouse.kteams.util.SoundUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class TeamSettingsCommand implements CommandExecutor {

    private final KTeams plugin;
    private final TeamManager manager;
    private final TeamSettingsGUI gui;

    public TeamSettingsCommand(KTeams plugin, TeamManager manager, TeamSettingsGUI gui) {
        this.plugin = plugin;
        this.manager = manager;
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtil.message(plugin, "only-player"));
            return true;
        }

        String teamName = manager.getTeamName(player.getUniqueId());
        if (teamName == null) {
            player.sendMessage(ChatUtil.message(plugin, "settings-no-team"));
            SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
            return true;
        }

        if (!manager.isLeader(player.getUniqueId(), teamName)) {
            player.sendMessage(ChatUtil.message(plugin, "settings-only-leader"));
            SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
            return true;
        }

        gui.open(player, teamName);
        SoundUtil.play(player, plugin.getConfig().getString("sounds.click"), 1f, 1f);
        return true;
    }
}
