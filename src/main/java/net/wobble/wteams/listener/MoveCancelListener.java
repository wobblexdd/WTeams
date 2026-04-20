package net.wobble.wteams.listener;

import net.wobble.wteams.WTeams;
import net.wobble.wteams.manager.TeamHomeTeleportManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public final class MoveCancelListener implements Listener {

    private final WTeams plugin;
    private final TeamHomeTeleportManager teleportManager;

    public MoveCancelListener(WTeams plugin) {
        this.plugin = plugin;
        this.teleportManager = plugin.getTeamHomeTeleportManager();
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!plugin.getConfig().getBoolean("homes.cancel-on-move", true)) {
            return;
        }

        if (event.getTo() == null) {
            return;
        }

        Player player = event.getPlayer();
        if (!teleportManager.isTeleporting(player.getUniqueId())) {
            return;
        }

        if (event.getFrom().getBlockX() != event.getTo().getBlockX()
                || event.getFrom().getBlockY() != event.getTo().getBlockY()
                || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
            teleportManager.cancel(player);
        }
    }
}
