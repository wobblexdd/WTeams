package net.wobble.wteams.gui.confirm;

import net.kyori.adventure.text.Component;
import net.wobble.wteams.WTeams;
import net.wobble.wteams.gui.ManagedGui;
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

public final class ConfirmGUI {

    public static final int SLOT_CONFIRM = 11;
    public static final int SLOT_INFO = 13;
    public static final int SLOT_CANCEL = 15;

    private final WTeams plugin;
    private final Map<UUID, ConfirmContext> contexts = new HashMap<>();

    public ConfirmGUI(WTeams plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, ConfirmContext context) {
        String title = switch (context.type()) {
            case LEAVE -> msg("confirm-title-leave");
            case KICK -> msg("confirm-title-kick");
            case DISBAND -> msg("confirm-title-disband");
        };

        Inventory inventory = ManagedGui.createInventory(ManagedGui.Type.CONFIRM, 27, color(title));

        fillBackground(inventory);

        inventory.setItem(SLOT_CONFIRM, simpleItem(
                Material.LIME_WOOL,
                msg("confirm-accept-name"),
                List.of(mm(msg("confirm-accept-lore-1")))
        ));

        inventory.setItem(SLOT_INFO, simpleItem(
                Material.BOOK,
                msg("confirm-info-name"),
                List.of(
                        mm(msg("confirm-info-lore-1").replace("{action}", actionName(context.type()))),
                        mm(msg("confirm-info-lore-2").replace("{target}", context.targetName() == null ? "None" : context.targetName())),
                        mm(msg("confirm-info-lore-3")),
                        mm(msg("confirm-info-lore-4"))
                )
        ));

        inventory.setItem(SLOT_CANCEL, simpleItem(
                Material.RED_WOOL,
                msg("confirm-cancel-name"),
                List.of(mm(msg("confirm-cancel-lore-1")))
        ));

        contexts.put(player.getUniqueId(), context);
        player.openInventory(inventory);
    }

    public ConfirmContext getContext(UUID uuid) {
        return contexts.get(uuid);
    }

    public void clearContext(UUID uuid) {
        contexts.remove(uuid);
    }

    private String actionName(ConfirmType type) {
        return switch (type) {
            case LEAVE -> "Leave Team";
            case KICK -> "Kick Player";
            case DISBAND -> "Disband Team";
        };
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
