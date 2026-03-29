package net.wobble.teams.manager;

import net.wobble.teams.WobbleTeams;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public final class TeamManager {

    public record TeamData(String name, UUID leader, LinkedHashSet<UUID> members) {}

    private final WobbleTeams plugin;

    private final Map<String, TeamData> teams = new HashMap<>();
    private final Map<UUID, String> playerTeams = new HashMap<>();
    private final Map<UUID, InviteData> pendingInvites = new HashMap<>();
    private final Map<String, EnumSet<TeamPermission>> memberPermissions = new HashMap<>();
    private final Map<String, TeamHome> teamHomes = new HashMap<>();
    private final Map<String, TeamSettings> teamSettings = new HashMap<>();
    private final Map<String, Set<String>> allies = new HashMap<>();
    private final Map<String, Set<String>> pendingAllyRequests = new HashMap<>();

    private File dataFile;
    private FileConfiguration dataConfig;

    public TeamManager(WobbleTeams plugin) {
        this.plugin = plugin;
        load();
    }

    public void reload() {
        load();
        cleanupExpiredInvites();
    }

    private void load() {
        this.dataFile = new File(plugin.getDataFolder(), "teams.yml");
        this.dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        teams.clear();
        playerTeams.clear();
        memberPermissions.clear();
        teamHomes.clear();
        teamSettings.clear();
        allies.clear();
        pendingAllyRequests.clear();

        ConfigurationSection teamsSection = dataConfig.getConfigurationSection("teams");
        if (teamsSection != null) {
            for (String teamName : teamsSection.getKeys(false)) {
                String base = "teams." + teamName + ".";
                String leaderRaw = dataConfig.getString(base + "leader");

                if (leaderRaw == null) continue;

                UUID leader;
                try {
                    leader = UUID.fromString(leaderRaw);
                } catch (IllegalArgumentException exception) {
                    continue;
                }

                LinkedHashSet<UUID> members = new LinkedHashSet<>();
                for (String raw : dataConfig.getStringList(base + "members")) {
                    try {
                        members.add(UUID.fromString(raw));
                    } catch (IllegalArgumentException ignored) {
                    }
                }

                if (!members.contains(leader)) {
                    members.add(leader);
                }

                TeamData teamData = new TeamData(teamName, leader, members);
                teams.put(teamName.toLowerCase(Locale.ROOT), teamData);

                for (UUID member : members) {
                    playerTeams.put(member, teamName);

                    List<String> storedPermissions = dataConfig.getStringList(base + "permissions." + member);
                    EnumSet<TeamPermission> parsed = EnumSet.noneOf(TeamPermission.class);
                    for (String rawPerm : storedPermissions) {
                        try {
                            parsed.add(TeamPermission.valueOf(rawPerm.toUpperCase(Locale.ROOT)));
                        } catch (IllegalArgumentException ignored) {
                        }
                    }

                    if (member.equals(leader)) {
                        parsed = EnumSet.allOf(TeamPermission.class);
                    }

                    memberPermissions.put(permissionKey(teamName, member), parsed);
                }

                String homeBase = base + "home.";
                String worldName = dataConfig.getString(homeBase + "world");
                if (worldName != null) {
                    World world = Bukkit.getWorld(worldName);
                    if (world != null) {
                        double x = dataConfig.getDouble(homeBase + "x");
                        double y = dataConfig.getDouble(homeBase + "y");
                        double z = dataConfig.getDouble(homeBase + "z");
                        float yaw = (float) dataConfig.getDouble(homeBase + "yaw");
                        float pitch = (float) dataConfig.getDouble(homeBase + "pitch");
                        teamHomes.put(teamName.toLowerCase(Locale.ROOT), new TeamHome(new Location(world, x, y, z, yaw, pitch)));
                    }
                }

                boolean invites = dataConfig.getBoolean(base + "settings.team-invites-enabled", true);
                boolean pvp = dataConfig.getBoolean(base + "settings.friendly-fire-enabled", false);
                boolean isolation = dataConfig.getBoolean(base + "settings.chat-isolation-enabled", false);
                teamSettings.put(teamName.toLowerCase(Locale.ROOT), new TeamSettings(invites, pvp, isolation));

                Set<String> allySet = new HashSet<>();
                for (String ally : dataConfig.getStringList(base + "allies")) {
                    allySet.add(ally.toLowerCase(Locale.ROOT));
                }
                allies.put(teamName.toLowerCase(Locale.ROOT), allySet);

                Set<String> requestSet = new HashSet<>();
                for (String req : dataConfig.getStringList(base + "pending-ally-requests")) {
                    requestSet.add(req.toLowerCase(Locale.ROOT));
                }
                pendingAllyRequests.put(teamName.toLowerCase(Locale.ROOT), requestSet);
            }
        }
    }

    public void save() {
        dataConfig.set("teams", null);
        dataConfig.set("players", null);

        for (TeamData team : teams.values()) {
            String base = "teams." + team.name() + ".";
            dataConfig.set(base + "leader", team.leader().toString());

            List<String> members = new ArrayList<>();
            for (UUID uuid : team.members()) {
                members.add(uuid.toString());
                dataConfig.set("players." + uuid, team.name());

                EnumSet<TeamPermission> permissions = memberPermissions.getOrDefault(
                        permissionKey(team.name(), uuid),
                        uuid.equals(team.leader()) ? EnumSet.allOf(TeamPermission.class) : EnumSet.noneOf(TeamPermission.class)
                );

                List<String> permNames = permissions.stream().map(Enum::name).toList();
                dataConfig.set(base + "permissions." + uuid, permNames);
            }

            dataConfig.set(base + "members", members);

            TeamHome home = teamHomes.get(team.name().toLowerCase(Locale.ROOT));
            if (home != null && home.location() != null && home.location().getWorld() != null) {
                String homeBase = base + "home.";
                dataConfig.set(homeBase + "world", home.location().getWorld().getName());
                dataConfig.set(homeBase + "x", home.location().getX());
                dataConfig.set(homeBase + "y", home.location().getY());
                dataConfig.set(homeBase + "z", home.location().getZ());
                dataConfig.set(homeBase + "yaw", home.location().getYaw());
                dataConfig.set(homeBase + "pitch", home.location().getPitch());
            }

            TeamSettings settings = teamSettings.getOrDefault(team.name().toLowerCase(Locale.ROOT), TeamSettings.defaults());
            dataConfig.set(base + "settings.team-invites-enabled", settings.teamInvitesEnabled());
            dataConfig.set(base + "settings.friendly-fire-enabled", settings.friendlyFireEnabled());
            dataConfig.set(base + "settings.chat-isolation-enabled", settings.chatIsolationEnabled());

            dataConfig.set(base + "allies", new ArrayList<>(allies.getOrDefault(team.name().toLowerCase(Locale.ROOT), Set.of())));
            dataConfig.set(base + "pending-ally-requests", new ArrayList<>(pendingAllyRequests.getOrDefault(team.name().toLowerCase(Locale.ROOT), Set.of())));
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Could not save teams.yml", exception);
        }
    }

    public List<String> getPendingAllyRequestsForTeam(String teamName) {
        if (teamName == null) return List.of();
        List<String> result = new ArrayList<>();
        for (String requestKey : pendingAllyRequests.getOrDefault(teamName.toLowerCase(Locale.ROOT), Set.of())) {
            TeamData requestTeam = teams.get(requestKey);
            result.add(requestTeam == null ? requestKey : requestTeam.name());
        }
        result.sort(String.CASE_INSENSITIVE_ORDER);
        return result;
    }

    public boolean createTeam(Player player, String rawName) {
        if (rawName == null) return false;
        String name = rawName.trim();
        if (name.isEmpty()) return false;
        if (playerTeams.containsKey(player.getUniqueId())) return false;

        String regex = plugin.getConfig().getString("teams.name-regex", "[A-Za-z0-9_]+");
        int maxLength = plugin.getConfig().getInt("teams.max-name-length", 16);

        if (name.length() > maxLength || !name.matches(regex)) return false;
        if (teams.containsKey(name.toLowerCase(Locale.ROOT))) return false;

        LinkedHashSet<UUID> members = new LinkedHashSet<>();
        members.add(player.getUniqueId());

        TeamData teamData = new TeamData(name, player.getUniqueId(), members);
        teams.put(name.toLowerCase(Locale.ROOT), teamData);
        playerTeams.put(player.getUniqueId(), name);
        memberPermissions.put(permissionKey(name, player.getUniqueId()), EnumSet.allOf(TeamPermission.class));
        teamSettings.put(name.toLowerCase(Locale.ROOT), TeamSettings.defaults());
        allies.put(name.toLowerCase(Locale.ROOT), new HashSet<>());
        pendingAllyRequests.put(name.toLowerCase(Locale.ROOT), new HashSet<>());
        save();
        return true;
    }

    public boolean renameTeam(UUID actorId, String newRawName) {
        String oldName = getTeamName(actorId);
        if (oldName == null) return false;
        if (!isLeader(actorId, oldName)) return false;
        return adminRenameTeam(oldName, newRawName);
    }

    public boolean adminRenameTeam(String oldTeamName, String newRawName) {
        TeamData oldTeam = getTeam(oldTeamName);
        if (oldTeam == null || newRawName == null) return false;

        String newName = newRawName.trim();
        if (newName.isEmpty()) return false;

        String regex = plugin.getConfig().getString("teams.name-regex", "[A-Za-z0-9_]+");
        int maxLength = plugin.getConfig().getInt("teams.max-name-length", 16);

        if (newName.length() > maxLength || !newName.matches(regex)) return false;

        String oldKey = oldTeam.name().toLowerCase(Locale.ROOT);
        String newKey = newName.toLowerCase(Locale.ROOT);

        if (!oldKey.equals(newKey) && teams.containsKey(newKey)) return false;

        TeamData renamed = new TeamData(newName, oldTeam.leader(), new LinkedHashSet<>(oldTeam.members()));
        teams.remove(oldKey);
        teams.put(newKey, renamed);

        for (UUID member : oldTeam.members()) {
            playerTeams.put(member, newName);

            EnumSet<TeamPermission> perms = memberPermissions.remove(permissionKey(oldTeam.name(), member));
            if (perms != null) memberPermissions.put(permissionKey(newName, member), perms);
        }

        TeamHome home = teamHomes.remove(oldKey);
        if (home != null) teamHomes.put(newKey, home);

        TeamSettings settings = teamSettings.remove(oldKey);
        if (settings != null) teamSettings.put(newKey, settings);

        Set<String> movedAllies = allies.remove(oldKey);
        if (movedAllies != null) allies.put(newKey, movedAllies);

        Set<String> movedRequests = pendingAllyRequests.remove(oldKey);
        if (movedRequests != null) pendingAllyRequests.put(newKey, movedRequests);

        for (Map.Entry<String, Set<String>> entry : allies.entrySet()) {
            if (entry.getValue().remove(oldKey)) entry.getValue().add(newKey);
        }
        for (Map.Entry<String, Set<String>> entry : pendingAllyRequests.entrySet()) {
            if (entry.getValue().remove(oldKey)) entry.getValue().add(newKey);
        }

        save();
        return true;
    }

    public boolean adminSetLeader(String teamName, UUID newLeader) {
        TeamData team = getTeam(teamName);
        if (team == null || newLeader == null || !team.members().contains(newLeader)) return false;

        TeamData updated = new TeamData(team.name(), newLeader, new LinkedHashSet<>(team.members()));
        teams.put(team.name().toLowerCase(Locale.ROOT), updated);

        for (UUID member : team.members()) {
            if (member.equals(newLeader)) {
                memberPermissions.put(permissionKey(team.name(), member), EnumSet.allOf(TeamPermission.class));
            }
        }

        save();
        return true;
    }

    public boolean adminDisbandTeam(String teamName) {
        TeamData team = getTeam(teamName);
        if (team == null) return false;

        for (UUID member : team.members()) {
            playerTeams.remove(member);
            pendingInvites.remove(member);
            memberPermissions.remove(permissionKey(team.name(), member));
        }

        String key = team.name().toLowerCase(Locale.ROOT);
        for (String ally : allies.getOrDefault(key, Set.of())) {
            allies.getOrDefault(ally, new HashSet<>()).remove(key);
        }
        for (Map.Entry<String, Set<String>> entry : pendingAllyRequests.entrySet()) {
            entry.getValue().remove(key);
        }

        teamHomes.remove(key);
        teamSettings.remove(key);
        allies.remove(key);
        pendingAllyRequests.remove(key);
        teams.remove(key);
        save();
        return true;
    }

    public boolean invitePlayer(Player inviter, Player target) {
        cleanupExpiredInvites();

        String teamName = getTeamName(inviter.getUniqueId());
        if (teamName == null) return false;
        if (!isLeader(inviter.getUniqueId(), teamName) && !hasPermission(inviter.getUniqueId(), teamName, TeamPermission.INVITE)) return false;
        if (inviter.getUniqueId().equals(target.getUniqueId())) return false;
        if (getTeamName(target.getUniqueId()) != null) return false;
        if (!getTeamSettings(teamName).teamInvitesEnabled()) return false;

        TeamData team = getTeam(teamName);
        if (team == null) return false;

        int maxSize = plugin.getConfig().getInt("teams.max-size", 5);
        if (team.members().size() >= maxSize) return false;

        long expireSeconds = plugin.getConfig().getLong("invites.expire-seconds", 60L);
        long expiresAt = System.currentTimeMillis() + (expireSeconds * 1000L);

        pendingInvites.put(target.getUniqueId(), new InviteData(inviter.getUniqueId(), teamName, expiresAt));
        return true;
    }

    public boolean sendAllyRequest(String fromTeam, String toTeam) {
        if (fromTeam == null || toTeam == null) return false;

        String from = fromTeam.toLowerCase(Locale.ROOT);
        String to = toTeam.toLowerCase(Locale.ROOT);

        if (!teams.containsKey(from) || !teams.containsKey(to)) return false;
        if (from.equals(to)) return false;
        if (areAllies(from, to)) return false;
        if (pendingAllyRequests.getOrDefault(to, Set.of()).contains(from)) return false;
        if (pendingAllyRequests.getOrDefault(from, Set.of()).contains(to)) return false;

        pendingAllyRequests.computeIfAbsent(to, k -> new HashSet<>()).add(from);
        save();
        return true;
    }

    public boolean acceptAllyRequest(String targetTeam, String fromTeam) {
        if (targetTeam == null || fromTeam == null) return false;

        String target = targetTeam.toLowerCase(Locale.ROOT);
        String from = fromTeam.toLowerCase(Locale.ROOT);

        Set<String> requests = pendingAllyRequests.getOrDefault(target, new HashSet<>());
        if (!requests.remove(from)) return false;

        allies.computeIfAbsent(target, k -> new HashSet<>()).add(from);
        allies.computeIfAbsent(from, k -> new HashSet<>()).add(target);
        save();
        return true;
    }

    public boolean denyAllyRequest(String targetTeam, String fromTeam) {
        if (targetTeam == null || fromTeam == null) return false;

        String target = targetTeam.toLowerCase(Locale.ROOT);
        String from = fromTeam.toLowerCase(Locale.ROOT);

        Set<String> requests = pendingAllyRequests.getOrDefault(target, new HashSet<>());
        boolean removed = requests.remove(from);
        if (removed) save();
        return removed;
    }

    public boolean hasPendingAllyRequest(String targetTeam, String fromTeam) {
        if (targetTeam == null || fromTeam == null) return false;
        return pendingAllyRequests.getOrDefault(targetTeam.toLowerCase(Locale.ROOT), Set.of())
                .contains(fromTeam.toLowerCase(Locale.ROOT));
    }

    public boolean areAllies(String firstTeam, String secondTeam) {
        if (firstTeam == null || secondTeam == null) return false;
        return allies.getOrDefault(firstTeam.toLowerCase(Locale.ROOT), Set.of())
                .contains(secondTeam.toLowerCase(Locale.ROOT));
    }

    public List<String> getAllies(String teamName) {
        if (teamName == null) return List.of();
        List<String> result = new ArrayList<>();
        for (String allyKey : allies.getOrDefault(teamName.toLowerCase(Locale.ROOT), Set.of())) {
            TeamData allyTeam = teams.get(allyKey);
            result.add(allyTeam == null ? allyKey : allyTeam.name());
        }
        result.sort(String.CASE_INSENSITIVE_ORDER);
        return result;
    }

    public InviteData getInvite(UUID targetId) {
        cleanupExpiredInvites();
        InviteData invite = pendingInvites.get(targetId);
        if (invite != null && invite.isExpired()) {
            pendingInvites.remove(targetId);
            return null;
        }
        return invite;
    }

    public boolean denyInvite(Player player) {
        cleanupExpiredInvites();
        InviteData invite = pendingInvites.get(player.getUniqueId());
        if (invite == null) return false;
        pendingInvites.remove(player.getUniqueId());
        return true;
    }

    public String acceptInvite(Player player) {
        cleanupExpiredInvites();

        if (getTeamName(player.getUniqueId()) != null) return null;

        InviteData invite = pendingInvites.get(player.getUniqueId());
        if (invite == null || invite.isExpired()) {
            pendingInvites.remove(player.getUniqueId());
            return null;
        }

        TeamData team = getTeam(invite.teamName());
        if (team == null) {
            pendingInvites.remove(player.getUniqueId());
            return null;
        }

        int maxSize = plugin.getConfig().getInt("teams.max-size", 5);
        if (team.members().size() >= maxSize) {
            pendingInvites.remove(player.getUniqueId());
            return null;
        }

        LinkedHashSet<UUID> members = new LinkedHashSet<>(team.members());
        members.add(player.getUniqueId());

        TeamData updated = new TeamData(team.name(), team.leader(), members);
        teams.put(team.name().toLowerCase(Locale.ROOT), updated);
        playerTeams.put(player.getUniqueId(), team.name());
        memberPermissions.put(permissionKey(team.name(), player.getUniqueId()), EnumSet.noneOf(TeamPermission.class));
        pendingInvites.remove(player.getUniqueId());
        save();
        return team.name();
    }

    public boolean hasPendingInvite(UUID targetId) {
        return getInvite(targetId) != null;
    }

    public void cleanupExpiredInvites() {
        Iterator<Map.Entry<UUID, InviteData>> iterator = pendingInvites.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, InviteData> entry = iterator.next();
            if (entry.getValue() == null || entry.getValue().isExpired()) {
                iterator.remove();
            }
        }
    }

    public String getTeamName(UUID playerId) {
        return playerTeams.get(playerId);
    }

    public TeamData getTeam(String teamName) {
        if (teamName == null) return null;
        return teams.get(teamName.toLowerCase(Locale.ROOT));
    }

    public List<UUID> getMembers(String teamName) {
        TeamData team = getTeam(teamName);
        if (team == null) return List.of();
        return new ArrayList<>(team.members());
    }

    public List<String> getMemberNames(String teamName) {
        TeamData team = getTeam(teamName);
        if (team == null) return List.of();

        List<String> result = new ArrayList<>();
        for (UUID uuid : team.members()) {
            String name = plugin.getServer().getOfflinePlayer(uuid).getName();
            result.add(name == null ? uuid.toString() : name);
        }
        return result;
    }

    public String getLeaderName(String teamName) {
        TeamData team = getTeam(teamName);
        if (team == null) return null;

        String name = plugin.getServer().getOfflinePlayer(team.leader()).getName();
        return name == null ? team.leader().toString() : name;
    }

    public boolean isLeader(UUID playerId, String teamName) {
        TeamData team = getTeam(teamName);
        return team != null && team.leader().equals(playerId);
    }

    public boolean hasPermission(UUID targetId, String teamName, TeamPermission permission) {
        if (targetId == null || teamName == null || permission == null) return false;
        if (isLeader(targetId, teamName)) return true;
        return memberPermissions.getOrDefault(permissionKey(teamName, targetId), EnumSet.noneOf(TeamPermission.class)).contains(permission);
    }

    public boolean togglePermission(UUID actorId, String teamName, UUID targetId, TeamPermission permission) {
        if (actorId == null || targetId == null || teamName == null || permission == null) return false;
        if (!isLeader(actorId, teamName)) return false;
        if (isLeader(targetId, teamName)) return false;

        String key = permissionKey(teamName, targetId);
        EnumSet<TeamPermission> set = memberPermissions.getOrDefault(key, EnumSet.noneOf(TeamPermission.class));

        if (set.contains(permission)) set.remove(permission);
        else set.add(permission);

        memberPermissions.put(key, set);
        save();
        return true;
    }

    public TeamSettings getTeamSettings(String teamName) {
        if (teamName == null) return TeamSettings.defaults();
        return teamSettings.getOrDefault(teamName.toLowerCase(Locale.ROOT), TeamSettings.defaults());
    }

    public boolean toggleTeamInvites(String teamName) {
        if (teamName == null) return false;
        TeamSettings current = getTeamSettings(teamName);
        teamSettings.put(teamName.toLowerCase(Locale.ROOT),
                new TeamSettings(!current.teamInvitesEnabled(), current.friendlyFireEnabled(), current.chatIsolationEnabled()));
        save();
        return true;
    }

    public boolean toggleFriendlyFire(String teamName) {
        if (teamName == null) return false;
        TeamSettings current = getTeamSettings(teamName);
        teamSettings.put(teamName.toLowerCase(Locale.ROOT),
                new TeamSettings(current.teamInvitesEnabled(), !current.friendlyFireEnabled(), current.chatIsolationEnabled()));
        save();
        return true;
    }

    public boolean toggleChatIsolation(String teamName) {
        if (teamName == null) return false;
        TeamSettings current = getTeamSettings(teamName);
        teamSettings.put(teamName.toLowerCase(Locale.ROOT),
                new TeamSettings(current.teamInvitesEnabled(), current.friendlyFireEnabled(), !current.chatIsolationEnabled()));
        save();
        return true;
    }

    public boolean setTeamHome(Player actor) {
        String teamName = getTeamName(actor.getUniqueId());
        if (teamName == null) return false;
        if (!isLeader(actor.getUniqueId(), teamName) && !hasPermission(actor.getUniqueId(), teamName, TeamPermission.HOME)) return false;

        teamHomes.put(teamName.toLowerCase(Locale.ROOT), new TeamHome(actor.getLocation()));
        save();
        return true;
    }

    public boolean deleteTeamHome(Player actor) {
        String teamName = getTeamName(actor.getUniqueId());
        if (teamName == null) return false;
        if (!isLeader(actor.getUniqueId(), teamName) && !hasPermission(actor.getUniqueId(), teamName, TeamPermission.HOME)) return false;

        TeamHome removed = teamHomes.remove(teamName.toLowerCase(Locale.ROOT));
        save();
        return removed != null;
    }

    public Location getTeamHomeLocation(UUID playerId) {
        String teamName = getTeamName(playerId);
        if (teamName == null) return null;
        TeamHome home = teamHomes.get(teamName.toLowerCase(Locale.ROOT));
        return home == null ? null : home.location();
    }

    public boolean leaveTeam(Player player) {
        String teamName = getTeamName(player.getUniqueId());
        if (teamName == null) return false;

        TeamData team = getTeam(teamName);
        if (team == null) return false;
        if (team.leader().equals(player.getUniqueId())) return false;

        LinkedHashSet<UUID> members = new LinkedHashSet<>(team.members());
        members.remove(player.getUniqueId());

        teams.put(teamName.toLowerCase(Locale.ROOT), new TeamData(team.name(), team.leader(), members));
        playerTeams.remove(player.getUniqueId());
        memberPermissions.remove(permissionKey(teamName, player.getUniqueId()));
        save();
        return true;
    }

    public boolean kickPlayer(Player actor, UUID targetId) {
        String teamName = getTeamName(actor.getUniqueId());
        if (teamName == null) return false;

        TeamData team = getTeam(teamName);
        if (team == null) return false;

        boolean allowed = team.leader().equals(actor.getUniqueId()) || hasPermission(actor.getUniqueId(), teamName, TeamPermission.KICK);
        if (!allowed) return false;

        if (targetId == null || targetId.equals(actor.getUniqueId()) || targetId.equals(team.leader())) return false;
        if (!team.members().contains(targetId)) return false;

        LinkedHashSet<UUID> members = new LinkedHashSet<>(team.members());
        members.remove(targetId);

        teams.put(teamName.toLowerCase(Locale.ROOT), new TeamData(team.name(), team.leader(), members));
        playerTeams.remove(targetId);
        memberPermissions.remove(permissionKey(teamName, targetId));
        save();
        return true;
    }

    public boolean disbandTeam(Player player) {
        String teamName = getTeamName(player.getUniqueId());
        if (teamName == null) return false;

        TeamData team = getTeam(teamName);
        if (team == null || !team.leader().equals(player.getUniqueId())) return false;

        for (UUID member : team.members()) {
            playerTeams.remove(member);
            pendingInvites.remove(member);
            memberPermissions.remove(permissionKey(teamName, member));
        }

        String key = team.name().toLowerCase(Locale.ROOT);
        for (String ally : allies.getOrDefault(key, Set.of())) {
            allies.getOrDefault(ally, new HashSet<>()).remove(key);
        }
        for (Map.Entry<String, Set<String>> entry : pendingAllyRequests.entrySet()) {
            entry.getValue().remove(key);
        }

        teamHomes.remove(key);
        teamSettings.remove(key);
        allies.remove(key);
        pendingAllyRequests.remove(key);
        teams.remove(key);
        save();
        return true;
    }

    private String permissionKey(String teamName, UUID uuid) {
        return teamName.toLowerCase(Locale.ROOT) + ":" + uuid;
    }
}
