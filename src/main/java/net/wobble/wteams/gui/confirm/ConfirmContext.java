package net.wobble.wteams.gui.confirm;

import java.util.UUID;

public record ConfirmContext(
        UUID viewer,
        ConfirmType type,
        String teamName,
        UUID targetPlayer,
        String targetName,
        int returnPage
) {
}
