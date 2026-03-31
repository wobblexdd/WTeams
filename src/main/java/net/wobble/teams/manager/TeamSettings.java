package net.wobble.teams.manager;

public record TeamSettings(
        boolean teamInvitesEnabled,
        boolean friendlyFireEnabled,
        boolean chatIsolationEnabled
) {
    public static TeamSettings defaults() {
        return new TeamSettings(true, false, false);
    }
}
