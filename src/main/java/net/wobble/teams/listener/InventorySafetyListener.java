package net.wobble.teams.listener;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class InventorySafetyListener implements Listener {

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (isProtected(title)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onUnsafeClick(InventoryClickEvent event) {
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!isProtected(title)) {
            return;
        }

        ClickType click = event.getClick();
        InventoryAction action = event.getAction();

        if (event.isShiftClick()
                || click == ClickType.NUMBER_KEY
                || click == ClickType.DOUBLE_CLICK
                || action == InventoryAction.COLLECT_TO_CURSOR
                || action == InventoryAction.HOTBAR_SWAP
                || action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            event.setCancelled(true);
        }
    }

    private boolean isProtected(String title) {
        return title.equalsIgnoreCase("TEAM")
                || title.equalsIgnoreCase("MEMBER MANAGER")
                || title.equalsIgnoreCase("MEMBER SETTINGS")
                || title.equalsIgnoreCase("TEAM SETTINGS")
                || title.equalsIgnoreCase("ALLY MANAGER")
                || title.equalsIgnoreCase("CONFIRM LEAVING TEAM")
                || title.equalsIgnoreCase("CONFIRM KICKING PLAYER")
                || title.equalsIgnoreCase("CONFIRM DISBANDING TEAM");
    }
}
