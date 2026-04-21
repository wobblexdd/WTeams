package net.klouse.kteams.gui.settings;

import net.kyori.adventure.text.Component;
import net.klouse.kteams.KTeams;
import net.klouse.kteams.gui.ManagedGui;
import net.klouse.kteams.manager.TeamManager;
import net.klouse.kteams.manager.TeamSettings;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TeamSettingsGUI {

    public static final int SLOT_TEAM_INVITES = 11;
    public static final int SLOT_FRIENDLY_FIRE = 13;
    public static final int SLOT_CHAT_ISOLATION = 15;
    public static final int SLOT_INFO = 4;
    public static final int SLOT_BACK = 22;

    private final KTeams plugin;
    private final TeamManager manager;
    private final Map<UUID, String> contexts = new HashMap<>();

    public TeamSettingsGUI(KTeams plugin, TeamManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    public void open(Player player, String teamName) {
        TeamSettings settings = manager.getTeamSettings(teamName);

        Inventory inventory = ManagedGui.createInventory(ManagedGui.Type.TEAM_SETTINGS, 27, "Team Settings");
        fillBackground(inventory);

        inventory.setItem(SLOT_INFO, simpleItem(
                Material.BOOK,
                msg("settings-info-name"),
                List.of(
                        mm(msg("settings-info-lore-1")),
                        mm(msg("settings-info-lore-2"))
                )
        ));

        inventory.setItem(SLOT_TEAM_INVITES, toggleItem(
                msg("settings-team-invites-name"),
                settings.teamInvitesEnabled()
        ));

        inventory.setItem(SLOT_FRIENDLY_FIRE, toggleItem(
                msg("settings-friendly-fire-name"),
                settings.friendlyFireEnabled()
        ));

        inventory.setItem(SLOT_CHAT_ISOLATION, toggleItem(
                msg("settings-chat-isolation-name"),
                settings.chatIsolationEnabled()
        ));

        inventory.setItem(SLOT_BACK, simpleItem(
                Material.BARRIER,
                msg("pagination-back"),
                List.of(Component.text("§7Go back"))
        ));

        contexts.put(player.getUniqueId(), teamName);
        player.openInventory(inventory);
    }

    public String getContext(UUID uuid) {
        return contexts.get(uuid);
    }

    public void clearContext(UUID uuid) {
        contexts.remove(uuid);
    }

    private ItemStack toggleItem(String name, boolean enabled) {
        Material material = enabled ? Material.LIME_DYE : Material.GRAY_DYE;
        String state = enabled ? "ON" : "OFF";

        return simpleItem(
                material,
                name,
                List.of(
                        mm(msg("settings-toggle-lore-1").replace("{state}", state)),
                        mm(msg("settings-toggle-lore-2"))
                )
        );
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
