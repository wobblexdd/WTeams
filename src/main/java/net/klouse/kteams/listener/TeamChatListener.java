package net.klouse.kteams.listener;

import net.klouse.kteams.KTeams;
import net.klouse.kteams.manager.TeamChatManager;
import net.klouse.kteams.manager.TeamManager;
import net.klouse.kteams.manager.TeamPermission;
import net.klouse.kteams.util.ChatUtil;
import net.klouse.kteams.util.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import io.papermc.paper.event.player.AsyncChatEvent;

import java.util.UUID;

public final class TeamChatListener implements Listener {

    private final KTeams plugin;
    private final TeamManager manager;
    private final TeamChatManager chatManager;

    public TeamChatListener(KTeams plugin) {
        this.plugin = plugin;
        this.manager = plugin.getTeamManager();
        this.chatManager = plugin.getTeamChatManager();
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        if (!chatManager.isEnabled(player.getUniqueId())) {
            return;
        }

        String teamName = manager.getTeamName(player.getUniqueId());
        if (teamName == null) {
            chatManager.disable(player.getUniqueId());
            return;
        }

        if (!manager.isLeader(player.getUniqueId(), teamName)
                && !manager.hasPermission(player.getUniqueId(), teamName, TeamPermission.CHAT)) {
            return;
        }

        event.setCancelled(true);
        String plainMessage = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(event.message());

        String formatted = ChatUtil.message(plugin, "team-chat-format",
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
    }
}
