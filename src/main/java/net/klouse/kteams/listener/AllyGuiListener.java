package net.klouse.kteams.listener;

import net.klouse.kteams.KTeams;
import net.klouse.kteams.gui.ManagedGui;
import net.klouse.kteams.gui.TeamGUI;
import net.klouse.kteams.gui.ally.AllyGUI;
import net.klouse.kteams.manager.TeamManager;
import net.klouse.kteams.util.ChatUtil;
import net.klouse.kteams.util.SoundUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public final class AllyGuiListener implements Listener {

    private final KTeams plugin;
    private final TeamManager manager;
    private final AllyGUI gui;
    private final TeamGUI teamGUI;

    public AllyGuiListener(KTeams plugin) {
        this.plugin = plugin;
        this.manager = plugin.getTeamManager();
        this.gui = plugin.getAllyGUI();
        this.teamGUI = plugin.getTeamGUI();
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (ManagedGui.getType(event.getView()) != ManagedGui.Type.ALLY_MANAGER) return;
        if (event.getClickedInventory() == null) return;
        if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;

        event.setCancelled(true);

        String teamName = plugin.getAllyGuiContext().get(player.getUniqueId());
        if (teamName == null) return;

        if (event.getRawSlot() == AllyGUI.SLOT_BACK) {
            teamGUI.open(player);
            SoundUtil.play(player, plugin.getConfig().getString("sounds.click"), 1f, 1f);
            return;
        }

        String request = gui.getRequestBySlot(teamName, event.getRawSlot());
        if (request == null) {
            SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
            return;
        }

        if (event.getClick() == ClickType.RIGHT) {
            boolean success = manager.denyAllyRequest(teamName, request);
            if (!success) {
                player.sendMessage(ChatUtil.message(plugin, "ally-no-request", "{team}", request));
                SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
                return;
            }

            player.sendMessage(ChatUtil.message(plugin, "ally-deny-success", "{team}", request));
            gui.open(player, teamName);
            SoundUtil.play(player, plugin.getConfig().getString("sounds.success"), 1f, 1f);
            return;
        }

        boolean success = manager.acceptAllyRequest(teamName, request);
        if (!success) {
            player.sendMessage(ChatUtil.message(plugin, "ally-no-request", "{team}", request));
            SoundUtil.play(player, plugin.getConfig().getString("sounds.error"), 1f, 1f);
            return;
        }

        player.sendMessage(ChatUtil.message(plugin, "ally-accept-success", "{team}", request));
        gui.open(player, teamName);
        SoundUtil.play(player, plugin.getConfig().getString("sounds.success"), 1f, 1f);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (ManagedGui.getType(event.getInventory()) != ManagedGui.Type.ALLY_MANAGER) return;

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (ManagedGui.getType(player.getOpenInventory()) == ManagedGui.Type.ALLY_MANAGER) {
                return;
            }
            plugin.getAllyGuiContext().remove(player.getUniqueId());
        });
    }
}
