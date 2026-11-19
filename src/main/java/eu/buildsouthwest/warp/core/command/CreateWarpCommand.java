package eu.buildsouthwest.warp.core.command;

import eu.buildsouthwest.warp.WarpSystem;
import eu.buildsouthwest.warp.core.api.ApiService;
import eu.buildsouthwest.warp.core.database.WarpRepository;
import eu.buildsouthwest.warp.core.model.Author;
import eu.buildsouthwest.warp.core.model.GeoMetadata;
import eu.buildsouthwest.warp.core.model.Warp;
import eu.buildsouthwest.warp.util.NameSanitizer;
import eu.buildsouthwest.warp.util.NameValidator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class CreateWarpCommand extends Command implements CommandExecutor {
    private final Component NONPLAYER = Component.text("This command can only be run by a player.", NamedTextColor.RED);
    private final Component INVALID_NAME = Component.text("Invalid warp name! Please use standard characters from the Latin alphabet.", NamedTextColor.RED);
    private final Component INVALID_COORD = Component.text("No coordinates were received from the BTE API! Warp creation will carry on but please contact server staff about this error.", NamedTextColor.YELLOW);
    private final Component ERROR = Component.text("Failed to reach APIs for warp creation, please contact staff about this message.", NamedTextColor.RED);
    private final WarpSystem plugin;
    private final ApiService api;
    private WarpRepository repo;

    public CreateWarpCommand(WarpSystem plugin) {
        super("createwarp");

        this.plugin = plugin;
        this.repo = new WarpRepository(plugin.getDatabaseService());
        this.api = new ApiService();
    }

    @Override
    public boolean onCommand(CommandSender cs, Command cm, String label, String[] args) {
        if(!(cs instanceof Player pl)){
            cs.sendMessage(NONPLAYER);
            return true;
        }

        UUID uuid = UUID.randomUUID();
        Optional<String> possibleName = parseName(args);
        if(possibleName.isEmpty()){
            pl.sendMessage(INVALID_NAME);
            return false;
        }
        plugin.getLogger().info("[Warp_System_Create_Warp] Getting new warp info");
        String warpName = possibleName.get();

        double x = Math.round(pl.getLocation().getX() * 100.0) / 100.0;
        double y = Math.round(pl.getLocation().getY() * 100.0) / 100.0;
        double z = Math.round(pl.getLocation().getZ() * 100.0) / 100.0;
        double pitch = Math.round(pl.getLocation().getPitch() * 100.0) / 100.0;
        double yaw = Math.round(pl.getLocation().getYaw() * 100.0) / 100.0;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try{
                Optional<GeoMetadata> geo = api.fetchCoords(x, z).join();
                if(geo.isEmpty()){
                    pl.sendMessage(ERROR);
                    return;
                }

                Optional<String> loc = api.fetchLocation(geo.get()).join();
                if(loc.isEmpty()){
                    pl.sendMessage(ERROR);
                    return;
                }
                Optional<String> opt = parseName(loc.get().split(" "));
                if(opt.isEmpty()){
                    pl.sendMessage(ERROR);
                    return;
                }

                plugin.getLogger().info(opt.get() + " and geo data: " + geo.get().latitude() + ", " + geo.get().longitude() );
                Author newAuthor = new Author(pl.getUniqueId(), pl.getName());
                Warp newWarp = new Warp(uuid, opt.get() + "_" + warpName, pl.getWorld().getName(), "", "warp", x, y, z, pitch, yaw, "minecraft:yellow_wool", geo, Optional.empty(), Optional.empty());

                plugin.getLogger().info("[Warp_System_Create_Warp] Found geo and location data, sending to DB");
                repo.newWarp(newWarp, newAuthor, geo.get());
            } catch (Exception e) {
                pl.sendMessage(ERROR);
                throw new RuntimeException(e);
            }
        });

        return true;
    }

    private Optional<String> parseName(String[] args) {
        if(args.length < 1) return Optional.empty();

        String joined = Arrays.stream(args)
                .map(NameSanitizer::sanitize)
                .filter(w -> w.chars().anyMatch(Character::isLetterOrDigit))
                .map(w -> capitalize(w.toLowerCase(Locale.ENGLISH)))
                .collect(Collectors.joining("-"));

        plugin.getLogger().info("[WarpSystemParseName] args=" + Arrays.toString(args)
                + " joined='" + joined + "' length=" + joined.length());

        return NameValidator.validate(joined);
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.substring(0, 1).toUpperCase(Locale.ENGLISH) + s.substring(1);
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String @NotNull [] args) {
        plugin.getLogger().info("[createwarp] execute called by " + sender.getName() + " args=" + Arrays.toString(args));
        return onCommand(sender,this,commandLabel,args);
    }
}
