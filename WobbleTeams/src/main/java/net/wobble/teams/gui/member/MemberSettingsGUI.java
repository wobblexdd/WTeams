package net.wobble.teams.gui.member;

import net.kyori.adventure.text.Component;
import net.wobble.teams.WobbleTeams;
import net.wobble.teams.manager.TeamManager;
import net.wobble.teams.manager.TeamPermission;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class MemberSettingsGUI {

    public static final int SLOT_INVITE = 11;
    public static final int SLOT_KICK = 12;
    public static final int SLOT_CHAT = 14;
    public static final int SLOT_HOME = 15;
    public static final int SLOT_INFO = 13;
    public static final int SLOT_BACK = 22;

    private final WobbleTeams plugin;
    private final TeamManager manager;
    private final Map<UUID, MemberSettingsContext> contexts = new HashMap<>();

    public MemberSettingsGUI(WobbleTeams plugin, TeamManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    public void open(Player viewer, String teamName, UUID targetId, int returnPage) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetId);
        String targetName = target.getName() == null ? targetId.toString() : target.getName();
        boolean leader = manager.isLeader(targetId, teamName);

        Inventory inventory = Bukkit.createInventory(null, 27, "Member Settings");
        fillBackground(inventory);

        inventory.setItem(SLOT_INFO, simpleItem(
                Material.BOOK,
                msg("perm-info-name"),
                List.of(
                        mm(msg("perm-info-lore-1").replace("{name}", targetName)),
                        mm(msg("perm-info-lore-2").replace("{role}", leader ? "Leader" : "Member")),
                        mm(msg("perm-info-lore-3"))
                )
        ));

        inventory.setItem(SLOT_INVITE, permissionItem(targetId, teamName, TeamPermission.INVITE, msg("perm-invite-name")));
        inventory.setItem(SLOT_KICK, permissionItem(targetId, teamName, TeamPermission.KICK, msg("perm-kick-name")));
        inventory.setItem(SLOT_CHAT, permissionItem(targetId, teamName, TeamPermission.CHAT, msg("perm-chat-name")));
        inventory.setItem(SLOT_HOME, permissionItem(targetId, teamName, TeamPermission.HOME, msg("perm-home-name")));

        inventory.setItem(SLOT_BACK, simpleItem(
                Material.BARRIER,
                msg("pagination-back"),
                List.of(Component.text("§7Go back"))
        ));

        contexts.put(viewer.getUniqueId(), new MemberSettingsContext(viewer.getUniqueId(), teamName, targetId, targetName, returnPage));
        viewer.openInventory(inventory);
    }

    public MemberSettingsContext getContext(UUID uuid) {
        return contexts.get(uuid);
    }

    public void clearContext(UUID uuid) {
        contexts.remove(uuid);
    }

    private ItemStack permissionItem(UUID targetId, String teamName, TeamPermission permission, String display) {
        boolean enabled = manager.hasPermission(targetId, teamName, permission);
        Material material = enabled ? Material.LIME_DYE : Material.GRAY_DYE;
        String state = enabled ? "ON" : "OFF";

        return simpleItem(
                material,
                display,
                List.of(
                        mm(msg("perm-toggle-lore-1").replace("{state}", state)),
                        mm(msg("perm-toggle-lore-2"))
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
