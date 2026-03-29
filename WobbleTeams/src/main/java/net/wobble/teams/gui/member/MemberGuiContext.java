package net.wobble.teams.gui.member;

import java.util.UUID;

public record MemberGuiContext(UUID viewer, String teamName, int page, MemberGuiState state) {
}
