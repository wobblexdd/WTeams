package net.wobble.teams.manager;

import net.wobble.teams.WobbleTeams;
import net.wobble.teams.util.ChatUtil;
import net.wobble.teams.util.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TeamHomeTeleportManager {

    private final WobbleTeams plugin;
    private final Map<UUID, Integer> pendingTasks = new HashMap<>();
    private final Map<UUID, Location> pendingLocations = new HashMap<>();

    public TeamHomeTeleportManager(WobbleTeams plugin) {
        this.plugin = plugin;
    }

    public boolean isTeleporting(UUID uuid) {
        return pendingTasks.containsKey(uuid);
    }

    public void startTeleport(Player player, Location location) {
        cancel(player);

        int delay = plugin.getConfig().getInt("homes.teleport-delay-seconds", 5);
        if (delay <= 0) {
            completeTeleport(player, location);
            return;
        }

        pendingLocations.put(player.getUniqueId(), location);
        player.sendMessage(ChatUtil.message(plugin, "team-home-teleport-start", "{time}", String.valueOf(delay)));
        SoundUtil.play(player, plugin.getConfig().getString("sounds.click"), 1f, 1f);

        int taskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Location pending = pendingLocations.remove(player.getUniqueId());
            pendingTasks.remove(player.getUniqueId());

            if (!player.isOnline() || pending == null) {
                return;
            }

            completeTeleport(player, pending);
        }, delay * 20L).getTaskId();

        pendingTasks.put(player.getUniqueId(), taskId);
    }

    public void cancel(Player player) {
        Integer taskId = pendingTasks.remove(player.getUniqueId());
        pendingLocations.remove(player.getUniqueId());

        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
            player.sendMessage(ChatUtil.message(plugin, "team-home-teleport-cancelled"));
            SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
        }
    }

    private void completeTeleport(Player player, Location location) {
        player.teleport(location);
        player.sendMessage(ChatUtil.message(plugin, "team-home-teleported"));
        SoundUtil.play(player, plugin.getConfig().getString("sounds.teleport"), 1f, 1f);
    }
}
