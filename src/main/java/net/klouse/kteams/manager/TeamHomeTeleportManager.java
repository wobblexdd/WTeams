package net.klouse.kteams.manager;

import net.kyori.adventure.text.Component;
import net.klouse.kteams.KTeams;
import net.klouse.kteams.util.ChatUtil;
import net.klouse.kteams.util.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TeamHomeTeleportManager {

    private final KTeams plugin;
    private final Map<UUID, BukkitTask> pendingTasks = new HashMap<>();
    private final Map<UUID, Location> pendingLocations = new HashMap<>();
    private final Map<UUID, Integer> pendingSeconds = new HashMap<>();

    public TeamHomeTeleportManager(KTeams plugin) {
        this.plugin = plugin;
    }

    public boolean isTeleporting(UUID uuid) {
        return pendingTasks.containsKey(uuid);
    }

    public void startTeleport(Player player, Location location) {
        cancel(player);

        int delay = Math.max(0, plugin.getConfig().getInt("homes.teleport-delay-seconds", 5));
        if (delay <= 0) {
            completeTeleport(player, location);
            return;
        }

        UUID uuid = player.getUniqueId();
        pendingLocations.put(uuid, location);
        pendingSeconds.put(uuid, delay);

        player.sendMessage(ChatUtil.message(plugin, "team-home-teleport-start", "{time}", String.valueOf(delay)));
        player.sendActionBar(Component.text("Teleports in " + delay + "..."));
        SoundUtil.play(player, plugin.getConfig().getString("sounds.click"), 1f, 1f);

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel(player);
                    return;
                }

                int remaining = pendingSeconds.getOrDefault(uuid, 0);
                if (remaining <= 0) {
                    Location pending = pendingLocations.remove(uuid);
                    BukkitTask current = pendingTasks.remove(uuid);
                    pendingSeconds.remove(uuid);

                    if (current != null) {
                        current.cancel();
                    }

                    if (pending != null) {
                        completeTeleport(player, pending);
                    }
                    return;
                }

                player.sendActionBar(Component.text("Teleports in " + remaining + "..."));
                pendingSeconds.put(uuid, remaining - 1);
            }
        }, 0L, 20L);

        pendingTasks.put(uuid, task);
    }

    public void cancel(Player player) {
        UUID uuid = player.getUniqueId();
        BukkitTask task = pendingTasks.remove(uuid);
        pendingLocations.remove(uuid);
        pendingSeconds.remove(uuid);

        if (task != null) {
            task.cancel();
            if (player.isOnline()) {
                player.sendMessage(ChatUtil.message(plugin, "team-home-teleport-cancelled"));
                player.sendActionBar(Component.text("Teleport cancelled"));
                SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
            }
        }
    }

    private void completeTeleport(Player player, Location location) {
        player.teleport(location);
        player.sendMessage(ChatUtil.message(plugin, "team-home-teleported"));
        player.sendActionBar(Component.text("Teleported"));
        SoundUtil.play(player, plugin.getConfig().getString("sounds.teleport"), 1f, 1f);
    }
}
