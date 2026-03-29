package net.wobble.teams.gui.member;

import java.util.UUID;

public record MemberSettingsContext(
        UUID viewer,
        String teamName,
        UUID targetId,
        String targetName,
        int returnPage
) {
}
