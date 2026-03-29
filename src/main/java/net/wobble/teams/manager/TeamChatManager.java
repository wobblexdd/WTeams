package net.wobble.teams.manager;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class TeamChatManager {

    private final Set<UUID> toggledPlayers = new HashSet<>();

    public boolean isEnabled(UUID uuid) {
        return toggledPlayers.contains(uuid);
    }

    public boolean toggle(UUID uuid) {
        if (toggledPlayers.contains(uuid)) {
            toggledPlayers.remove(uuid);
            return false;
        }

        toggledPlayers.add(uuid);
        return true;
    }

    public void disable(UUID uuid) {
        toggledPlayers.remove(uuid);
    }
}
