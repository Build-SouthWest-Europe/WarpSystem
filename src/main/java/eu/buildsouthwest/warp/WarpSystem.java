package eu.buildsouthwest.warp;

import com.alpsbte.alpslib.io.database.DatabaseConnection;
import eu.buildsouthwest.warp.core.command.CreateWarpCommand;
import eu.buildsouthwest.warp.core.command.SetWarpCommand;
import eu.buildsouthwest.warp.core.command.WarpCommand;
import eu.buildsouthwest.warp.core.database.DBWarpRepository;
import eu.buildsouthwest.warp.core.database.DatabaseService;
import eu.buildsouthwest.warp.core.storage.CaffeineWarpCache;
import eu.buildsouthwest.warp.core.storage.WarpRepository;
import eu.buildsouthwest.warp.core.storage.WarpService;
import eu.buildsouthwest.warp.util.WarpConfig;
import org.bukkit.plugin.java.JavaPlugin;
import space.arim.dazzleconf.Configuration;
import space.arim.dazzleconf.StandardErrorPrint;
import space.arim.dazzleconf.backend.Backend;
import space.arim.dazzleconf.backend.PathRoot;
import space.arim.dazzleconf.backend.yaml.YamlBackend;

import java.io.IOException;
import java.nio.file.Path;

public final class WarpSystem extends JavaPlugin {
    private static WarpSystem plugin;
    private static WarpConfig warpConfig;
    private static DatabaseService databaseService;
    private static WarpService cacheService;

    @Override
    public void onEnable() {
        plugin = this;

        // Splash Screen
        getComponentLogger().info("Enabling Build SouthWest Europe - Warp System");
        getComponentLogger().info("- - - - - - - - - - - - - - - - - - - - - - - - - - - - - -");
        getComponentLogger().info("");
        getComponentLogger().info(" __    __                   __           _                 ");
        getComponentLogger().info("/ / /\\ \\ \\__ _ _ __ _ __   / _\\_   _ ___| |_ ___ _ __ ___  ");
        getComponentLogger().info("\\ \\/  \\/ / _` | '__| '_ \\  \\ \\| | | / __| __/ _ \\ '_ ` _ \\ ");
        getComponentLogger().info(" \\  /\\  / (_| | |  | |_) | _\\ \\ |_| \\__ \\ ||  __/ | | | | |");
        getComponentLogger().info("  \\/  \\/ \\__,_|_|  | .__/  \\__/\\__, |___/\\__\\___|_| |_| |_|");
        getComponentLogger().info("");
        getComponentLogger().info("- - - - - - - - - - - - - - - - - - - - - - - - - - - - - -");

        // Initialize Config
        getComponentLogger().info("Inizializing Config");
        saveDefaultConfig();
        Configuration<WarpConfig> configuration = Configuration.defaultBuilder(WarpConfig.class).build();
        Path configPath = getDataPath().resolve("config.yml");
        Backend backend = new YamlBackend(new PathRoot(configPath));
        warpConfig = configuration.configureOrFallback(backend, new StandardErrorPrint(output -> {
            try {
                output.printTo(System.out);
            } catch (IOException e) {
                getComponentLogger().warn("Could not print the error",e);
                this.getServer().getPluginManager().disablePlugin(this);
                return;
            }
        }));

        //Start DB bridge
        databaseService = new DatabaseService(this);
        databaseService.init();
        WarpRepository repo = new DBWarpRepository(databaseService);
        //Start and populate warp cache
        cacheService = new WarpService(this, repo, new CaffeineWarpCache());
        cacheService.refresh();
        cacheService.startRefreshTask();

        //Register warp commands
        plugin.getCommand("createwarp").setExecutor(new CreateWarpCommand(plugin));
//        plugin.getCommand("setwarp").setExecutor(new SetWarpCommand(plugin));
        plugin.getCommand("warp").setExecutor(new WarpCommand(plugin));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        DatabaseConnection.shutdown();
    }

    public static WarpConfig getWarpConfig() {
        return warpConfig;
    }
    public static DatabaseService getDatabaseService() { return databaseService; }
    public WarpService getCacheService() { return cacheService; }
}
