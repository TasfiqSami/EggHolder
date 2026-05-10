package me.tasfiq.eggholder;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class EggHolderPlugin extends JavaPlugin {

    private EggHolderService eggHolderService;
    private DeadPlayerService deadPlayerService;
    private TeleportMenuListener teleportMenuListener;
    private MessageManager messageManager;
    private PluginConfig pluginConfig;
    private ManagedEndWorldService managedEndWorldService;
    private TeamService teamService;
    private KitService kitService;
    private SidebarService sidebarService;
    private EndWarService endWarService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.messageManager = new MessageManager(this);
        this.messageManager.load();
        this.pluginConfig = PluginConfig.load(this);
        this.managedEndWorldService = new ManagedEndWorldService(this);
        this.managedEndWorldService.start();

        this.deadPlayerService = new DeadPlayerService(this);
        this.eggHolderService = new EggHolderService(this, deadPlayerService);
        this.teleportMenuListener = new TeleportMenuListener(this, deadPlayerService);
        this.teamService = new TeamService(this);
        this.kitService = new KitService(this);
        this.sidebarService = new SidebarService(this, teamService);
        this.endWarService = new EndWarService(this, teamService, kitService, sidebarService);

        registerCommands();

        getServer().getPluginManager().registerEvents(deadPlayerService, this);
        getServer().getPluginManager().registerEvents(eggHolderService, this);
        getServer().getPluginManager().registerEvents(teleportMenuListener, this);
        getServer().getPluginManager().registerEvents(teamService, this);
        getServer().getPluginManager().registerEvents(kitService, this);
        getServer().getPluginManager().registerEvents(sidebarService, this);
        getServer().getPluginManager().registerEvents(endWarService, this);

        deadPlayerService.reloadSettings();
        eggHolderService.start();
        endWarService.start();
    }

    @Override
    public void onDisable() {
        if (eggHolderService != null) {
            eggHolderService.shutdown();
        }
        if (endWarService != null) {
            endWarService.shutdown();
        }
        if (deadPlayerService != null) {
            deadPlayerService.shutdown();
        }
        if (sidebarService != null) {
            sidebarService.shutdown();
        }
        if (teamService != null) {
            teamService.shutdown();
        }
        if (managedEndWorldService != null) {
            managedEndWorldService.shutdown();
        }
    }

    public void reloadPluginFiles() {
        reloadConfig();
        messageManager.load();
        pluginConfig = PluginConfig.load(this);
        if (managedEndWorldService != null) {
            managedEndWorldService.reload();
        }
        if (teamService != null) {
            teamService.reload();
        }
        if (kitService != null) {
            kitService.reload();
        }
        if (sidebarService != null) {
            sidebarService.reload();
        }
        if (endWarService != null) {
            endWarService.reload();
        }
        deadPlayerService.reloadSettings();
        eggHolderService.reloadSettings();
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    public ManagedEndWorldService getManagedEndWorldService() {
        return managedEndWorldService;
    }

    public EggHolderService getEggHolderService() {
        return eggHolderService;
    }

    public DeadPlayerService getDeadPlayerService() {
        return deadPlayerService;
    }

    public TeamService getTeamService() {
        return teamService;
    }

    public KitService getKitService() {
        return kitService;
    }

    public SidebarService getSidebarService() {
        return sidebarService;
    }

    public EndWarService getEndWarService() {
        return endWarService;
    }

    private void registerCommands() {
        EggHolderCommand eggHolderCommand = new EggHolderCommand(this, eggHolderService, deadPlayerService);
        PluginCommand holderCommand = getCommand("eggholder");
        if (holderCommand == null) {
            throw new IllegalStateException("The /eggholder command is missing from plugin.yml.");
        }
        holderCommand.setExecutor(eggHolderCommand);
        holderCommand.setTabCompleter(eggHolderCommand);

        ReviveCommand reviveCommand = new ReviveCommand(this, deadPlayerService);
        PluginCommand revivePluginCommand = getCommand("revive");
        if (revivePluginCommand == null) {
            throw new IllegalStateException("The /revive command is missing from plugin.yml.");
        }
        revivePluginCommand.setExecutor(reviveCommand);
        revivePluginCommand.setTabCompleter(reviveCommand);

        TeamCommand teamCommand = new TeamCommand(this, teamService);
        PluginCommand teamPluginCommand = getCommand("team");
        if (teamPluginCommand == null) {
            throw new IllegalStateException("The /team command is missing from plugin.yml.");
        }
        teamPluginCommand.setExecutor(teamCommand);
        teamPluginCommand.setTabCompleter(teamCommand);

        StartCommand startCommand = new StartCommand(this, endWarService);
        PluginCommand startPluginCommand = getCommand("start");
        if (startPluginCommand == null) {
            throw new IllegalStateException("The /start command is missing from plugin.yml.");
        }
        startPluginCommand.setExecutor(startCommand);
        startPluginCommand.setTabCompleter(startCommand);

        EndWarAdminCommand endWarAdminCommand = new EndWarAdminCommand(this, endWarService);
        PluginCommand endWarPluginCommand = getCommand("endwar");
        if (endWarPluginCommand == null) {
            throw new IllegalStateException("The /endwar command is missing from plugin.yml.");
        }
        endWarPluginCommand.setExecutor(endWarAdminCommand);
        endWarPluginCommand.setTabCompleter(endWarAdminCommand);
    }
}
