package net.wobble.teams.gui.invite;

import net.kyori.adventure.text.Component;
import net.wobble.teams.WobbleTeams;
import net.wobble.teams.manager.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class InviteGUI {

    public static final int SLOT_BACK = 49;
    private static final int[] PLAYER_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private final WobbleTeams plugin;
    private final TeamManager teamManager;
    private final Map<UUID, Map<Integer, UUID>> contexts = new HashMap<>();

    public InviteGUI(WobbleTeams plugin, TeamManager teamManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
    }

    public void open(Player viewer) {
        Inventory inventory = Bukkit.createInventory(null, 54, "TEAM INVITES");
        fill(inventory);
        inventory.setItem(4, simple(Material.BOOK, "§aInvite Online Players", "§7Click a head to invite"));
        inventory.setItem(SLOT_BACK, simple(Material.BARRIER, "§cBack", "§7Return to team menu"));

        Map<Integer, UUID> slotMap = new HashMap<>();
        int index = 0;

        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target.getUniqueId().equals(viewer.getUniqueId())) continue;
            if (teamManager.getTeamName(target.getUniqueId()) != null) continue;
            if (index >= PLAYER_SLOTS.length) break;

            int slot = PLAYER_SLOTS[index++];
            inventory.setItem(slot, head(target));
            slotMap.put(slot, target.getUniqueId());
        }

        contexts.put(viewer.getUniqueId(), slotMap);
        viewer.openInventory(inventory);
    }

    public Player getTarget(UUID viewerId, int slot) {
        Map<Integer, UUID> slotMap = contexts.get(viewerId);
        if (slotMap == null) return null;

        UUID targetId = slotMap.get(slot);
        if (targetId == null) return null;

        return Bukkit.getPlayer(targetId);
    }

    public void clear(UUID viewerId) {
        contexts.remove(viewerId);
    }

    private void fill(Inventory inventory) {
        ItemStack filler = simple(Material.BLACK_STAINED_GLASS_PANE, "§0 ", null);
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }
    }

    private ItemStack head(Player target) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(target);
        meta.displayName(Component.text("§a" + target.getName()));
        meta.lore(java.util.List.of(
                Component.text("§7Click to invite"),
                Component.text("§8Online player")
        ));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack simple(Material material, String name, String lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name));
        if (lore != null) {
            meta.lore(java.util.List.of(Component.text(lore)));
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
        return item;
    }
}
