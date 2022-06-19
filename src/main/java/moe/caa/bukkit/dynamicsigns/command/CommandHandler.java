package moe.caa.bukkit.dynamicsigns.command;

import moe.caa.bukkit.dynamicsigns.config.DynamicConfig;
import moe.caa.bukkit.dynamicsigns.handler.SignPacketHandler;
import moe.caa.bukkit.dynamicsigns.main.DynamicSigns;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandHandler implements CommandExecutor, TabCompleter {
    private final DynamicSigns plugin;

    public CommandHandler(DynamicSigns plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("command.dynamicsign")) {
            sender.sendMessage(plugin.getConfig().getString("message.noPerms"));
            return true;
        }
        if (args.length != 5) {
            sender.sendMessage(plugin.getConfig().getString("message.paramNotEnough"));
            return true;
        }
        try {
            final World world = Bukkit.getWorld(args[0]);
            if (world == null) {
                sender.sendMessage(plugin.getConfig().getString("message.worldNotFound").replace("%world%", args[0]));
                return true;
            }
            Location loc = new Location(world, Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]));
            if (!SignPacketHandler.isSign(loc.getBlock().getType())) {
                sender.sendMessage(plugin.getConfig().getString("message.notSign")
                        .replace("%world%", args[0])
                        .replace("%x%", args[1])
                        .replace("%y%", args[2])
                        .replace("%z%", args[3])

                );
                return true;
            }
            DynamicConfig dynamicConfig = plugin.getDynamicSignsMap().get(args[4]);
            if (dynamicConfig == null) {
                sender.sendMessage(plugin.getConfig().getString("message.dynamicNotFound").replace("%dynamic%", args[4]));
                return true;
            }
            plugin.getDataEntry().remove(loc);
            plugin.getDataEntry().put(loc, dynamicConfig);
            sender.sendMessage(plugin.getConfig().getString("message.added")
                    .replace("%world%", args[0])
                    .replace("%x%", args[1])
                    .replace("%y%", args[2])
                    .replace("%z%", args[3])
                    .replace("%dynamic%", args[4])

            );
            return true;

        } catch (Exception e) {
            sender.sendMessage(plugin.getConfig().getString("message.digitalParseException"));
            return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 5) {
            return plugin.getDynamicSignsMap().keySet().stream().filter(s -> s.toLowerCase().contains(args[4].toLowerCase())).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
