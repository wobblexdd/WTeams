package net.wobble.teams.hook;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.wobble.teams.WobbleTeams;
import net.wobble.teams.manager.TeamManager;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class TeamsPlaceholder extends PlaceholderExpansion {

    private final WobbleTeams plugin;
    private final TeamManager manager;

    public TeamsPlaceholder(WobbleTeams plugin) {
        this.plugin = plugin;
        this.manager = plugin.getTeamManager();
    }

    @Override
    public @NotNull String getIdentifier() {
        return "wobbleteams";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Wobble";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";

        String team = manager.getTeamName(player.getUniqueId());

        if (params.equalsIgnoreCase("team")) {
            return team == null ? "None" : team;
        }

        if (params.equalsIgnoreCase("leader")) {
            return team == null ? "None" : manager.getLeaderName(team);
        }

        if (params.equalsIgnoreCase("members")) {
            return team == null ? "0" : String.valueOf(manager.getMembers(team).size());
        }

        if (params.equalsIgnoreCase("allies")) {
            return team == null ? "0" : String.valueOf(manager.getAllies(team).size());
        }

        return null;
    }
}
