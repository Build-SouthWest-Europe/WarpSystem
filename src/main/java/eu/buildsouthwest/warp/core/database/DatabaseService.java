package eu.buildsouthwest.warp.core.database;

import com.alpsbte.alpslib.io.database.DatabaseConnection;
import com.alpsbte.alpslib.io.database.DatabaseSection;
import eu.buildsouthwest.warp.WarpSystem;
import eu.buildsouthwest.warp.core.command.CreateWarpCommand;
import eu.buildsouthwest.warp.util.WarpConfig;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DatabaseService {
    private final WarpSystem plugin;
    private ExecutorService executor;

    public DatabaseService(WarpSystem plugin) {
        this.plugin = plugin;
    }

    public void init(){
        plugin.getLogger().info("[DatabaseService] Initializing Database Service");

        WarpConfig config = WarpSystem.getWarpConfig();
        DatabaseSection databaseSection = new DatabaseSection(config.database().url(),
                config.database().dbname(),
                config.database().username(),
                config.database().password(),
                config.database().maxLifetime(),
                config.database().connectionTimeout(),
                config.database().keepaliveTime(),
                config.database().maximumPoolSize(),
                0,
                "warpSystem-hikari");
        DatabaseConnection.initializeDatabase(databaseSection, true);

        executor = Executors.newFixedThreadPool(4, r ->{
            Thread t = new Thread(r, "BSWE-DB");
            t.setDaemon(true);
            return t;
        });

//        plugin.getCommand("create").setExecutor(new CreateWarpCommand(plugin));

        CommandMap cmdMap = Bukkit.getServer().getCommandMap();
        CreateWarpCommand createCmd = new CreateWarpCommand(plugin);
//        SetWarpCommand setCmd = new SetWarpCommand(plugin);

        Command create = cmdMap.getCommand("createwarp");
        Command set =  cmdMap.getCommand("setwarp");
        if (create != null) create.unregister(cmdMap);
        if (set != null) set.unregister(cmdMap);

        cmdMap.register("btewarp", createCmd);
//        cmdMap.register("btewarp", setCmd);

        plugin.getLogger().info("Database initialization complete.");
    }

    public Executor executor(){ return executor; }

    public Connection getConnection(){
        try{
            return DatabaseConnection.getConnection();
        }
        catch(SQLException sqle){
            sqle.printStackTrace();
            return null;
        }
    }

    public void shutdown(){
//        if(executor != null) executor.shutdown();
    }
}
