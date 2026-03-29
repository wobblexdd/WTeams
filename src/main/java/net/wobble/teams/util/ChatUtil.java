package net.wobble.teams.util;

import net.wobble.teams.WobbleTeams;

public final class ChatUtil {

    public static String message(WobbleTeams plugin, String key, String... replacements) {
        String msg = plugin.getMessagesConfig().getString(key, "&cMissing: " + key);

        if (replacements != null) {
            for (int i = 0; i < replacements.length - 1; i += 2) {
                msg = msg.replace(replacements[i], replacements[i + 1]);
            }
        }

        return color(plugin.getMessagesConfig().getString("prefix", "") + msg);
    }

    public static String color(String text) {
        return text == null ? "" : text.replace("&", "§");
    }
}
