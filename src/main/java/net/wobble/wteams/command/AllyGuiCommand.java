package net.wobble.wteams.command;

import net.wobble.wteams.WTeams;
import net.wobble.wteams.gui.ally.AllyGUI;
import net.wobble.wteams.manager.TeamManager;
import net.wobble.wteams.util.ChatUtil;
import net.wobble.wteams.util.SoundUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class AllyGuiCommand implements CommandExecutor {

    private final WTeams plugin;
    private final TeamManager manager;
    private final AllyGUI gui;

    public AllyGuiCommand(WTeams plugin, TeamManager manager, AllyGUI gui) {
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

        if (!plugin.getConfig().getBoolean("allies.enabled", true)) {
            player.sendMessage(ChatUtil.message(plugin, "ally-disabled"));
            SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
            return true;
        }

        String teamName = manager.getTeamName(player.getUniqueId());
        if (teamName == null) {
            player.sendMessage(ChatUtil.message(plugin, "ally-no-team"));
            SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
            return true;
        }

        if (!manager.isLeader(player.getUniqueId(), teamName)) {
            player.sendMessage(ChatUtil.message(plugin, "ally-only-leader"));
            SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
            return true;
        }

        gui.open(player, teamName);
        player.sendMessage(ChatUtil.message(plugin, "ally-gui-opened"));
        SoundUtil.play(player, plugin.getConfig().getString("sounds.click"), 1f, 1f);
        return true;
    }
}
