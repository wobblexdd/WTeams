package net.klouse.kteams;

import net.klouse.kteams.hook.TeamsPlaceholder;
import net.klouse.kteams.manager.TeamManager;
import org.bukkit.Bukkit;

public final class Bootstrap {

    public static void init(KTeams plugin) {

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
