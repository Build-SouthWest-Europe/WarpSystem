package eu.buildsouthwest.warp.core.command;

import eu.buildsouthwest.warp.WarpSystem;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class SetWarpCommand extends Command {
    private final WarpSystem plugin;

    public SetWarpCommand(WarpSystem plugin) {
        super("setwarp");

        this.plugin = plugin;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String @NotNull [] args) {
        return false;
    }
}
