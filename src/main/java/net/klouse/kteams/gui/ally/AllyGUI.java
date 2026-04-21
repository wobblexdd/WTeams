package net.klouse.kteams.gui.ally;

import net.kyori.adventure.text.Component;
import net.klouse.kteams.KTeams;
import net.klouse.kteams.gui.ManagedGui;
import net.klouse.kteams.manager.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class AllyGUI {

    public static final int SLOT_INFO = 4;
    public static final int SLOT_BACK = 49;

    private final KTeams plugin;
    private final TeamManager manager;

    public AllyGUI(KTeams plugin, TeamManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    public void open(Player player, String teamName) {
        Inventory inventory = ManagedGui.createInventory(ManagedGui.Type.ALLY_MANAGER, 54, "Ally Manager");
        fillBackground(inventory);

        List<String> allies = manager.getAllies(teamName);
        List<String> requests = manager.getPendingAllyRequestsForTeam(teamName);

        inventory.setItem(SLOT_INFO, simpleItem(
                Material.BOOK,
                msg("ally-gui-info-name"),
                List.of(
                        mm(msg("ally-gui-info-lore-1").replace("{count}", String.valueOf(allies.size()))),
                        mm(msg("ally-gui-info-lore-2").replace("{requests}", String.valueOf(requests.size())))
                )
        ));

        int allySlot = 10;
        for (String ally : allies) {
            if (allySlot >= 17) break;
            inventory.setItem(allySlot++, simpleItem(
                    Material.LIME_DYE,
                    msg("ally-gui-allies-name") + " §f" + ally,
                    List.of(mm(msg("ally-gui-ally-lore-1")))
            ));
        }

        if (allies.isEmpty()) {
            inventory.setItem(13, simpleItem(
                    Material.GRAY_DYE,
                    msg("ally-gui-allies-name"),
                    List.of(mm(msg("ally-gui-empty-allies")))
            ));
        }

        int requestSlot = 28;
        for (String request : requests) {
            if (requestSlot >= 35) break;
            inventory.setItem(requestSlot++, simpleItem(
                    Material.NAME_TAG,
                    msg("ally-gui-requests-name") + " §f" + request,
                    List.of(
                            mm(msg("ally-gui-request-lore-1")),
                            mm(msg("ally-gui-request-lore-2"))
                    )
            ));
        }

        if (requests.isEmpty()) {
            inventory.setItem(31, simpleItem(
                    Material.GRAY_DYE,
                    msg("ally-gui-requests-name"),
                    List.of(mm(msg("ally-gui-empty-requests")))
            ));
        }

        inventory.setItem(SLOT_BACK, simpleItem(
                Material.BARRIER,
                msg("ally-gui-back-name"),
                List.of(Component.text("§7Go back"))
        ));

        plugin.getAllyGuiContext().put(player.getUniqueId(), teamName);
        player.openInventory(inventory);
    }

    public String getRequestBySlot(String teamName, int slot) {
        List<String> requests = manager.getPendingAllyRequestsForTeam(teamName);
        int index = slot - 28;
        if (index < 0 || index >= requests.size() || slot > 34) {
            return null;
        }
        return requests.get(index);
    }

    private void fillBackground(Inventory inventory) {
        ItemStack filler = simpleItem(Material.BLACK_STAINED_GLASS_PANE, "&0 ", List.of());
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }
    }

    private ItemStack simpleItem(Material material, String name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(mm(name));
        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
        return item;
    }

    private Component mm(String text) {
        return Component.text(color(text));
    }

    private String msg(String path) {
        return plugin.getMessagesConfig().getString(path, "&cMissing message: " + path);
    }

    private String color(String text) {
        return text == null ? "" : text.replace("&", "§");
    }
}
