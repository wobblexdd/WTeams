package net.wobble.wteams;

import net.wobble.wteams.hook.TeamsPlaceholder;
import net.wobble.wteams.manager.TeamManager;
import org.bukkit.Bukkit;

public final class Bootstrap {

    public static void init(WTeams plugin) {

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
