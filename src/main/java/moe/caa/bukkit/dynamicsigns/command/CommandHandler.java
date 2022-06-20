package moe.caa.bukkit.dynamicsigns.command;

import lombok.SneakyThrows;
import moe.caa.bukkit.dynamicsigns.config.DynamicConfig;
import moe.caa.bukkit.dynamicsigns.handler.SignPacketHandler;
import moe.caa.bukkit.dynamicsigns.main.DynamicSigns;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CommandHandler implements CommandExecutor, TabCompleter {
    private final DynamicSigns plugin;

    public CommandHandler(DynamicSigns plugin) {
        this.plugin = plugin;
    }

    @Override
    @SneakyThrows
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("reload")) {
                if (testPermission(sender, "command.dynamicsigns.reload")) {
                    plugin.reload();
                    sender.sendMessage(plugin.getConfig().getString("message.reloaded"));
                }
                return true;
            }
            if (args[0].equalsIgnoreCase("destroy")) {
                if (testPermission(sender, "command.dynamicsigns.destroy") && testPlayer(sender)) {
                    Block block = testGetSignBlock(((Player) sender));
                    if (block != null) {
                        final Sign state = (Sign) block.getState();
                        if (plugin.getDataEntry().containsKey(block.getLocation())) {
                            plugin.getDataEntry().remove(block.getLocation());
                            sender.sendMessage(plugin.getConfig().getString("message.removeDigital")
                                    .replace("%world%", block.getWorld().getName())
                                    .replace("%x%", block.getX() + "")
                                    .replace("%y%", block.getY() + "")
                                    .replace("%z%", block.getZ() + "")
                            );
                            Bukkit.getScheduler().runTaskLater(plugin, state::update, 10);
                        } else {
                            sender.sendMessage(plugin.getConfig().getString("message.noDigital")
                                    .replace("%world%", block.getWorld().getName())
                                    .replace("%x%", block.getX() + "")
                                    .replace("%y%", block.getY() + "")
                                    .replace("%z%", block.getZ() + "")
                            );
                        }
                    }
                }
                return true;
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("set")) {
                if (testPermission(sender, "command.dynamicsigns.set") && testPlayer(sender)) {
                    Block block = testGetSignBlock(((Player) sender));
                    if (block != null) {
                        DynamicConfig dynamicConfig = testGetDynamicConfig(sender, args[1]);
                        if (dynamicConfig != null) {
                            DynamicConfig old = plugin.getDataEntry().get(block.getLocation());
                            if (old != null) {
                                sender.sendMessage(plugin.getConfig().getString("message.repeatSet")
                                        .replace("%world%", block.getWorld().getName())
                                        .replace("%x%", block.getX() + "")
                                        .replace("%y%", block.getY() + "")
                                        .replace("%z%", block.getZ() + "")
                                        .replace("%dynamic%", old.getPath())
                                );
                            } else {
                                plugin.getDataEntry().put(block.getLocation(), dynamicConfig);
                                sender.sendMessage(plugin.getConfig().getString("message.setDigital")
                                        .replace("%world%", block.getWorld().getName())
                                        .replace("%x%", block.getX() + "")
                                        .replace("%y%", block.getY() + "")
                                        .replace("%z%", block.getZ() + "")
                                        .replace("%dynamic%", dynamicConfig.getPath())
                                );
                            }
                        }
                    }
                }
                return true;
            } else if (args[0].equalsIgnoreCase("reset")) {
                if (testPermission(sender, "command.dynamicsigns.reset") && testPlayer(sender)) {
                    Block block = testGetSignBlock(((Player) sender));
                    if (block != null) {
                        DynamicConfig dynamicConfig = testGetDynamicConfig(sender, args[1]);
                        if (dynamicConfig != null) {
                            DynamicConfig old = plugin.getDataEntry().get(block.getLocation());
                            if (old == null) {
                                sender.sendMessage(plugin.getConfig().getString("message.resetBreak")
                                        .replace("%world%", block.getWorld().getName())
                                        .replace("%x%", block.getX() + "")
                                        .replace("%y%", block.getY() + "")
                                        .replace("%z%", block.getZ() + "")
                                );
                            } else {
                                if (old.getPath().equals(dynamicConfig.getPath())) {
                                    sender.sendMessage(plugin.getConfig().getString("message.resetEqual")
                                            .replace("%dynamic%", dynamicConfig.getPath()));
                                } else {
                                    plugin.getDataEntry().remove(block.getLocation());
                                    plugin.getDataEntry().put(block.getLocation(), dynamicConfig);
                                    sender.sendMessage(plugin.getConfig().getString("message.resetDigital")
                                            .replace("%world%", block.getWorld().getName())
                                            .replace("%x%", block.getX() + "")
                                            .replace("%y%", block.getY() + "")
                                            .replace("%z%", block.getZ() + "")
                                            .replace("%old_dynamic%", old.getPath())
                                            .replace("%new_dynamic%", dynamicConfig.getPath()));
                                }
                            }
                        }
                    }
                }
                return true;
            }
        }
        if (testPermission(sender, "command.dynamicsigns.help")) {
            sender.sendMessage(plugin.getConfig().getString("message.help"));
        }
        return true;
    }

    public boolean testPermission(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) return true;
        sender.sendMessage(plugin.getConfig().getString("message.noPerms"));
        return false;
    }

    public boolean testPlayer(CommandSender sender) {
        if (sender instanceof Player) return true;
        sender.sendMessage(plugin.getConfig().getString("message.notIsPlayer"));
        return false;
    }

    public Block testGetSignBlock(Player player) {
        final BlockIterator blockIterator = new BlockIterator(player, 5);
        while (blockIterator.hasNext()) {
            final Block next = blockIterator.next();
            if (SignPacketHandler.isSign(next)) return next;
            if (next.getType() != Material.AIR) break;
        }
        player.sendMessage(plugin.getConfig().getString("message.notIsSign"));
        return null;
    }

    public DynamicConfig testGetDynamicConfig(CommandSender sender, String name) {
        final DynamicConfig dynamicConfig = plugin.getDynamicSignsMap().get(name);
        if (dynamicConfig != null) return dynamicConfig;
        sender.sendMessage(plugin.getConfig().getString("message.dynamicNotFound")
                .replace("%dynamic%", name)
        );
        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("command.dynamicsigns.tabcomplete")) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            return Stream.of("reload", "destroy", "set", "reset").filter(s -> s.toLowerCase().contains(args[0].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("reset"))
                return plugin.getDynamicSignsMap().keySet().stream().filter(s -> s.toLowerCase().contains(args[1].toLowerCase())).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
