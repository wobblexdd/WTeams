package net.wobble.teams.gui.member;

import net.kyori.adventure.text.Component;
import net.wobble.teams.WobbleTeams;
import net.wobble.teams.manager.TeamManager;
import net.wobble.teams.util.Pagination;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class MemberGUI {

    public static final int[] MEMBER_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    public static final int SLOT_PREV = 45;
    public static final int SLOT_BACK = 49;
    public static final int SLOT_NEXT = 53;

    private final WobbleTeams plugin;
    private final TeamManager manager;
    private final Pagination<UUID> pagination = new Pagination<>();
    private final Map<UUID, MemberGuiContext> contexts = new HashMap<>();

    public MemberGUI(WobbleTeams plugin, TeamManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    public void open(Player viewer, String teamName, int page) {
        List<UUID> members = manager.getMembers(teamName);
        int maxPage = pagination.maxPage(members, MEMBER_SLOTS.length);

        if (page < 1) page = 1;
        if (page > maxPage) page = maxPage;

        Inventory inventory = Bukkit.createInventory(null, 54, "Member Manager");
        fillBackground(inventory);
        placeFrame(inventory);

        List<UUID> pageMembers = pagination.page(members, page, MEMBER_SLOTS.length);

        for (int i = 0; i < Math.min(pageMembers.size(), MEMBER_SLOTS.length); i++) {
            UUID memberId = pageMembers.get(i);
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(memberId);
            String name = offlinePlayer.getName() == null ? memberId.toString() : offlinePlayer.getName();
            boolean leader = manager.isLeader(memberId, teamName);

            inventory.setItem(MEMBER_SLOTS[i], playerHead(
                    offlinePlayer,
                    name,
                    leader ? "Leader" : "Member"
            ));
        }

        inventory.setItem(SLOT_PREV, navItem(Material.ARROW, msg("pagination-prev")));
        inventory.setItem(SLOT_BACK, navItem(Material.BARRIER, msg("pagination-back")));
        inventory.setItem(SLOT_NEXT, navItem(Material.ARROW, msg("pagination-next")));

        contexts.put(viewer.getUniqueId(), new MemberGuiContext(viewer.getUniqueId(), teamName, page, MemberGuiState.LIST));
        viewer.openInventory(inventory);
    }

    public MemberGuiContext getContext(UUID uuid) {
        return contexts.get(uuid);
    }

    public void clearContext(UUID uuid) {
        contexts.remove(uuid);
    }

    public UUID getMemberBySlot(String teamName, int slot, int page) {
        List<UUID> members = manager.getMembers(teamName);
        List<UUID> pageMembers = pagination.page(members, page, MEMBER_SLOTS.length);

        for (int i = 0; i < MEMBER_SLOTS.length && i < pageMembers.size(); i++) {
            if (MEMBER_SLOTS[i] == slot) {
                return pageMembers.get(i);
            }
        }

        return null;
    }

    public int getMaxPage(String teamName) {
        return pagination.maxPage(manager.getMembers(teamName), MEMBER_SLOTS.length);
    }

    private ItemStack playerHead(OfflinePlayer target, String name, String role) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(target);
        meta.displayName(mm("&e" + name));
        meta.lore(List.of(
                mm(msg("member-lore-name").replace("{name}", name)),
                mm(msg("member-lore-role").replace("{role}", role)),
                mm(msg("member-lore-1")),
                mm(msg("member-lore-2")),
                mm(msg("member-lore-3"))
        ));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack navItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(mm(name));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
        return item;
    }

    private void fillBackground(Inventory inventory) {
        ItemStack filler = navItem(Material.BLACK_STAINED_GLASS_PANE, "&0 ");
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }
    }

    private void placeFrame(Inventory inventory) {
        ItemStack accent = navItem(Material.GRAY_STAINED_GLASS_PANE, "&7 ");
        int[] slots = {
                0,1,2,3,4,5,6,7,8,
                9,17,18,26,27,35,36,44,
                46,47,48,50,51,52
        };

        for (int slot : slots) {
            inventory.setItem(slot, accent);
        }
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
