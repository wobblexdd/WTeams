package net.wobble.teams.gui;

import java.util.UUID;

public record TeamGuiContext(UUID playerId, TeamGuiState state, String teamName) {
}
