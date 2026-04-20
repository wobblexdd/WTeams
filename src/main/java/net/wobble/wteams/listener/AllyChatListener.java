package net.wobble.wteams.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.wobble.wteams.WTeams;
import net.wobble.wteams.manager.AllyChatManager;
import net.wobble.wteams.manager.TeamManager;
import net.wobble.wteams.util.ChatUtil;
import net.wobble.wteams.util.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.UUID;

public final class AllyChatListener implements Listener {

    private final WTeams plugin;
    private final TeamManager manager;
    private final AllyChatManager allyChatManager;

    public AllyChatListener(WTeams plugin) {
        this.plugin = plugin;
        this.manager = plugin.getTeamManager();
        this.allyChatManager = plugin.getAllyChatManager();
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        if (!allyChatManager.isEnabled(player.getUniqueId())) {
            return;
        }

        String teamName = manager.getTeamName(player.getUniqueId());
        if (teamName == null) {
            allyChatManager.disable(player.getUniqueId());
            return;
        }

        if (manager.getAllies(teamName).isEmpty()) {
            allyChatManager.disable(player.getUniqueId());
            return;
        }

        event.setCancelled(true);
        String plainMessage = PlainTextComponentSerializer.plainText().serialize(event.message());

        String formatted = ChatUtil.message(plugin, "ally-chat-format",
                "{team}", teamName,
                "{player}", player.getName(),
                "{message}", plainMessage);

        for (UUID memberId : manager.getMembers(teamName)) {
            Player target = Bukkit.getPlayer(memberId);
            if (target != null && target.isOnline()) {
                target.sendMessage(formatted);
                if (!target.getUniqueId().equals(player.getUniqueId())) {
                    SoundUtil.play(target, plugin.getConfig().getString("sounds.chat"), 1f, 1.15f);
                }
            }
        }

        for (String ally : manager.getAllies(teamName)) {
            for (UUID memberId : manager.getMembers(ally)) {
                Player target = Bukkit.getPlayer(memberId);
                if (target != null && target.isOnline()) {
                    target.sendMessage(formatted);
                    SoundUtil.play(target, plugin.getConfig().getString("sounds.chat"), 1f, 1.15f);
                }
            }
        }
    }
}
