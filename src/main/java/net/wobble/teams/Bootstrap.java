package net.wobble.teams;

import net.wobble.teams.hook.TeamsPlaceholder;
import net.wobble.teams.manager.TeamManager;
import org.bukkit.Bukkit;

public final class Bootstrap {

    public static void init(WobbleTeams plugin) {

        // PlaceholderAPI hook
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new TeamsPlaceholder(plugin).register();
            plugin.getLogger().info("PlaceholderAPI hooked.");
        }

        // Preload cache
        TeamManager manager = plugin.getTeamManager();
        Bukkit.getOnlinePlayers().forEach(p -> manager.getTeamName(p.getUniqueId()));
    }
}
