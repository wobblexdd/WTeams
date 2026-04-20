package net.wobble.wteams.util;

import net.wobble.wteams.manager.TeamManager;
import org.bukkit.entity.Player;

public final class TeamDataProvider {

    private final TeamManager manager;

    public TeamDataProvider(TeamManager manager) {
        this.manager = manager;
    }

    public String getTeam(Player player) {
        String t = manager.getTeamName(player.getUniqueId());
        return t == null ? "None" : t;
    }

    public int getSize(Player player) {
        String t = manager.getTeamName(player.getUniqueId());
        return t == null ? 0 : manager.getMembers(t).size();
    }

    public int getAllies(Player player) {
        String t = manager.getTeamName(player.getUniqueId());
        return t == null ? 0 : manager.getAllies(t).size();
    }
}
