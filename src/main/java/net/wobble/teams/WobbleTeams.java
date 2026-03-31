package net.wobble.teams;



import net.wobble.teams.command.AllyGuiCommand;

import net.wobble.teams.command.TeamCommand;

import net.wobble.teams.command.TeamSettingsCommand;

import net.wobble.teams.gui.TeamGUI;

import net.wobble.teams.gui.ally.AllyGUI;

import net.wobble.teams.gui.confirm.ConfirmGUI;
import net.wobble.teams.gui.invite.InviteGUI;

import net.wobble.teams.gui.member.MemberGUI;

import net.wobble.teams.gui.member.MemberSettingsGUI;

import net.wobble.teams.gui.settings.TeamSettingsGUI;

import net.wobble.teams.listener.*;

import net.wobble.teams.manager.AllyChatManager;

import net.wobble.teams.manager.TeamChatManager;

import net.wobble.teams.manager.TeamHomeTeleportManager;

import net.wobble.teams.manager.TeamManager;

import org.bukkit.command.PluginCommand;

import org.bukkit.configuration.file.FileConfiguration;

import org.bukkit.configuration.file.YamlConfiguration;

import org.bukkit.plugin.java.JavaPlugin;



import java.io.File;

import java.io.IOException;

import java.util.HashMap;

import java.util.Map;

import java.util.Objects;

import java.util.UUID;

import java.util.logging.Level;



public final class WobbleTeams extends JavaPlugin {



    private TeamManager teamManager;

    private TeamGUI teamGUI;

    private MemberGUI memberGUI;

    private ConfirmGUI confirmGUI;
    private InviteGUI inviteGUI;

    private MemberSettingsGUI memberSettingsGUI;

    private TeamSettingsGUI teamSettingsGUI;

    private AllyGUI allyGUI;

    private TeamChatManager teamChatManager;

    private AllyChatManager allyChatManager;

    private TeamHomeTeleportManager teamHomeTeleportManager;



    private final Map<UUID, String> allyGuiContext = new HashMap<>();



    private File messagesFile;

    private FileConfiguration messagesConfig;



    @Override

    public void onEnable() {

        saveDefaultConfig();

        saveResourceIfNotExists("messages.yml");

        saveResourceIfNotExists("teams.yml");

        loadMessages();



        this.teamManager = new TeamManager(this);

        this.teamGUI = new TeamGUI(this, teamManager);

        this.memberGUI = new MemberGUI(this, teamManager);

        this.confirmGUI = new ConfirmGUI(this);
        this.inviteGUI = new InviteGUI(this, teamManager);

        this.memberSettingsGUI = new MemberSettingsGUI(this, teamManager);

        this.teamSettingsGUI = new TeamSettingsGUI(this, teamManager);

        this.allyGUI = new AllyGUI(this, teamManager);

        this.teamChatManager = new TeamChatManager();

        this.allyChatManager = new AllyChatManager();

        this.teamHomeTeleportManager = new TeamHomeTeleportManager(this);



        TeamCommand teamCommand = new TeamCommand(this, teamManager, teamGUI);

        PluginCommand team = getCommand("team");

        if (team != null) {

            team.setExecutor(teamCommand);

            team.setTabCompleter(teamCommand);

        }



        PluginCommand teamSettings = getCommand("teamsettings");

        if (teamSettings != null) {

            teamSettings.setExecutor(new TeamSettingsCommand(this, teamManager, teamSettingsGUI));

        }



        PluginCommand allyGui = getCommand("allygui");

        if (allyGui != null) {

            allyGui.setExecutor(new AllyGuiCommand(this, teamManager, this.allyGUI));

        }



        getServer().getPluginManager().registerEvents(new TeamListener(this), this);

        getServer().getPluginManager().registerEvents(new TeamChatListener(this), this);

        getServer().getPluginManager().registerEvents(new AllyChatListener(this), this);

        getServer().getPluginManager().registerEvents(new InventorySafetyListener(), this);

        getServer().getPluginManager().registerEvents(new MoveCancelListener(this), this);

        getServer().getPluginManager().registerEvents(new AllyGuiListener(this), this);



        Bootstrap.init(this);

        getLogger().info("WobbleTeams v2 enabled.");

    }



    @Override

    public void onDisable() {

        if (teamManager != null) {

            teamManager.save();

        }

    }



    public void reloadPlugin() {

        reloadConfig();

        loadMessages();

        if (teamManager != null) {

            teamManager.reload();

        }

    }



    private void saveResourceIfNotExists(String resourcePath) {

        File file = new File(getDataFolder(), resourcePath);

        if (!file.exists()) {

            saveResource(resourcePath, false);

        }

    }



    private void loadMessages() {

        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {

            getLogger().warning("Could not create plugin data folder.");

        }



        this.messagesFile = new File(getDataFolder(), "messages.yml");

        this.messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

    }



    public FileConfiguration getMessagesConfig() {

        return Objects.requireNonNull(messagesConfig, "messagesConfig");

    }



    public TeamManager getTeamManager() {

        return teamManager;

    }



    public TeamGUI getTeamGUI() {

        return teamGUI;

    }



    public MemberGUI getMemberGUI() {

        return memberGUI;

    }



    public ConfirmGUI getConfirmGUI() {

        return confirmGUI;

    }



    public MemberSettingsGUI getMemberSettingsGUI() {

        return memberSettingsGUI;

    }



    public TeamSettingsGUI getTeamSettingsGUI() {

        return teamSettingsGUI;

    }



    public AllyGUI getAllyGUI() {

        return allyGUI;

    }



    public TeamChatManager getTeamChatManager() {

        return teamChatManager;

    }



    public AllyChatManager getAllyChatManager() {

        return allyChatManager;

    }



    public TeamHomeTeleportManager getTeamHomeTeleportManager() {

        return teamHomeTeleportManager;

    }



    public Map<UUID, String> getAllyGuiContext() {

        return allyGuiContext;

    }



    public void saveMessages() {

        if (messagesFile == null || messagesConfig == null) return;



        try {

            messagesConfig.save(messagesFile);

        } catch (IOException exception) {

            getLogger().log(Level.SEVERE, "Could not save messages.yml", exception);

        }

    }



    public InviteGUI getInviteGUI() {
        return inviteGUI;
    }

}

