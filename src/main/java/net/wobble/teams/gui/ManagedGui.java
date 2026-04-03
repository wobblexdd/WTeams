package net.wobble.teams.gui;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public final class ManagedGui {

    public enum Type {
        TEAM_MAIN,
        TEAM_NO_TEAM,
        MEMBER_LIST,
        MEMBER_SETTINGS,
        TEAM_SETTINGS,
        CONFIRM,
        INVITE,
        ALLY_MANAGER
    }

    public static final class Holder implements InventoryHolder {

        private final Type type;
        private Inventory inventory;

        private Holder(Type type) {
            this.type = type;
        }

        public Type type() {
            return type;
        }

        private void attach(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private ManagedGui() {
    }

    public static Inventory createInventory(Type type, int size, String title) {
        Holder holder = new Holder(type);
        Inventory inventory = Bukkit.createInventory(holder, size, title);
        holder.attach(inventory);
        return inventory;
    }

    public static @Nullable Type getType(InventoryView view) {
        if (view == null) {
            return null;
        }

        Type holderType = getType(view.getTopInventory());
        if (holderType != null) {
            return holderType;
        }

        return fromLegacyTitle(PlainTextComponentSerializer.plainText().serialize(view.title()));
    }

    public static @Nullable Type getType(Inventory inventory) {
        if (inventory == null) {
            return null;
        }

        InventoryHolder holder = inventory.getHolder();
        if (holder instanceof Holder managedHolder) {
            return managedHolder.type();
        }

        return null;
    }

    public static boolean isManaged(InventoryView view) {
        return getType(view) != null;
    }

    private static @Nullable Type fromLegacyTitle(String rawTitle) {
        String title = normalize(rawTitle);

        return switch (title) {
            case "TEAM" -> Type.TEAM_MAIN;
            case "MEMBER MANAGER" -> Type.MEMBER_LIST;
            case "MEMBER SETTINGS" -> Type.MEMBER_SETTINGS;
            case "TEAM SETTINGS" -> Type.TEAM_SETTINGS;
            case "TEAM INVITES" -> Type.INVITE;
            case "ALLY MANAGER" -> Type.ALLY_MANAGER;
            case "CONFIRM LEAVING TEAM", "CONFIRM KICKING PLAYER", "CONFIRM DISBANDING TEAM" -> Type.CONFIRM;
            default -> null;
        };
    }

    private static String normalize(String rawTitle) {
        if (rawTitle == null) {
            return "";
        }

        return rawTitle
                .replace("\u00C2\u00A7", "\u00A7")
                .replaceAll("(?i)\u00A7[0-9A-FK-OR]", "")
                .trim()
                .toUpperCase(Locale.ROOT);
    }
}
