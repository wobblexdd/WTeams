package net.klouse.kteams.manager;

import java.util.UUID;

public record InviteData(UUID inviter, String teamName, long expiresAt) {

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }
}
