package eu.buildsouthwest.warp.core.command;

import eu.buildsouthwest.warp.WarpSystem;
import eu.buildsouthwest.warp.core.model.Warp;
import eu.buildsouthwest.warp.core.storage.WarpService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class WarpCommand implements CommandExecutor, TabCompleter {
    private final Component NONPLAYER = Component.text("This command can only be run by a player.", NamedTextColor.RED);
    private final Component NOWARP1 = Component.text("Warp ", NamedTextColor.RED);
    private final Component NOWARP2 = Component.text(" not found.", NamedTextColor.RED);
    private final Component TP = Component.text("Teleporting to ", NamedTextColor.GRAY);

    private final WarpSystem plugin;
    private WarpService cache;

    public WarpCommand(WarpSystem plugin) {
        this.plugin = plugin;
        this.cache = plugin.getCacheService();
    }

    @Override
    public boolean onCommand(CommandSender cs, Command cm, String label, String[] args) {
        if(!(cs instanceof Player pl)){
            cs.sendMessage(NONPLAYER);
            return true;
        }
        if (args.length == 1) {
            Optional<Warp> warpOpt = cache.getAll().values().stream()
                    .filter(warp -> warp.displayName().toLowerCase(Locale.ENGLISH).equals(args[0].toLowerCase(Locale.ENGLISH)))
                    .findFirst();

            if (warpOpt.isEmpty()) {
                cs.sendMessage(NOWARP1
                        .append(Component.text(args[0], NamedTextColor.RED))
                        .append(NOWARP2));
            }

            Warp warpTo  = warpOpt.get();
            World world = Bukkit.getWorld(warpTo.world());
            if (world == null) {
                pl.sendMessage(Component.text("The world for this warp isn't loaded.", NamedTextColor.RED));
                return true;
            }

            Location tp = new Location(world, warpTo.x(), warpTo.y(), warpTo.z(), warpTo.yaw(), warpTo.pitch());
            pl.teleportAsync(tp);
            pl.sendMessage(TP.append(Component.text(warpTo.displayName(), NamedTextColor.GREEN)));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase(Locale.ENGLISH);
            return cache.getAll().values().stream()
                    .map(Warp::displayName)
                    .filter(name -> name.toLowerCase(Locale.ENGLISH).startsWith(partial))
                    .sorted().toList();
        }

        return Collections.emptyList();
    }
}
