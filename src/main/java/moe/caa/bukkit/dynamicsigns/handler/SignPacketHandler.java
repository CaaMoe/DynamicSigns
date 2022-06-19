package moe.caa.bukkit.dynamicsigns.handler;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;
import me.clip.placeholderapi.PlaceholderAPI;
import moe.caa.bukkit.dynamicsigns.main.UnsupportedServer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Set;

public class SignPacketHandler {
    private final Plugin plugin;
    private Object tileEntityDataTwoObject;

    public SignPacketHandler(Plugin plugin) {
        this.plugin = plugin;
    }

    public static boolean isSign(Material material) {
        return material.name().toLowerCase().contains("sign");
    }

    public static boolean isWall(Material material) {
        return material.name().toLowerCase().contains("wall");
    }

    public void init() throws IllegalAccessException, UnsupportedServer {

        // Tile Entity Data
        PacketContainer container = new PacketContainer(PacketType.Play.Server.TILE_ENTITY_DATA);
        final Class<?> integerOrTileEntityTypes = container.getModifier().getField(1).getType();
        if (integerOrTileEntityTypes == int.class) {
            tileEntityDataTwoObject = 9;
        } else {
            Class<?> blockSignClass = MinecraftReflection.getMinecraftClass("world.level.block.BlockSign", "BlockSign");
            // TileEntityTypes
            Field tileEntityTypes_blocks_field = null;
            for (Field field : integerOrTileEntityTypes.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                if (!Set.class.isAssignableFrom(field.getType())) continue;
                field.setAccessible(true);
                tileEntityTypes_blocks_field = field;
                break;
            }
            if (tileEntityTypes_blocks_field == null) {
                throw new UnsupportedServer(Bukkit.getVersion(), new NoSuchFieldException("Set<Block>"));
            }
            a:
            for (Field field : integerOrTileEntityTypes.getDeclaredFields()) {
                if (field.getType() != integerOrTileEntityTypes) continue;
                if (!Modifier.isStatic(field.getModifiers())) continue;
                field.setAccessible(true);
                final Object tileEntityTypes = field.get(null);
                final Set<?> blocks = (Set<?>) tileEntityTypes_blocks_field.get(tileEntityTypes);
                for (Object block : blocks) {
                    if (!blockSignClass.isInstance(block)) continue a;
                }
                this.tileEntityDataTwoObject = tileEntityTypes;
                break;
            }
        }
        if (tileEntityDataTwoObject == null) {
            throw new UnsupportedServer(Bukkit.getVersion());
        }
    }

    public void sendSignDirectionUpdate(Player player, Location location, int direction) {
        if (Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin,
                    () -> sendSignDirectionUpdate(player, location, direction)
            );
            return;
        }
        final Block block = location.getBlock();
        if (block == null) return;
        if (!isSign(block.getType())) return;
        if (isWall(block.getType())) return;
        if (direction < 0) return;
        if (direction > 15) return;

        PacketContainer packet = new PacketContainer(PacketType.Play.Server.BLOCK_CHANGE);
        BlockPosition blockPosition = new BlockPosition(block.getX(), block.getY(), block.getZ());
        WrappedBlockData wbd = WrappedBlockData.createData(block.getType(), direction);

        packet.getBlockPositionModifier().write(0, blockPosition);
        packet.getBlockData().write(0, wbd);

        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            plugin.getLogger().severe(String.format("An exception occurs when sending %s packets to %s.", "BLOCK_CHANGE", player.getName()));
        }
    }

    public void sendSignContentUpdate(Player player, Location location, List<String> content, boolean glowingText) {
        if (Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin,
                    () -> sendSignContentUpdate(player, location, content, glowingText)
            );
            return;
        }
        final Block block = location.getBlock();
        if (block == null) return;
        if (!isSign(block.getType())) return;
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.TILE_ENTITY_DATA);
        BlockPosition blockPosition = new BlockPosition(block.getX(), block.getY(), block.getZ());

        NbtCompound nbtBases = NbtFactory.ofCompound("");
        nbtBases.put("GlowingText", glowingText ? (byte) 1 : (byte) 0);
        nbtBases.put("Text1", NbtFactory.of("text", getText(content.get(0), player)));
        nbtBases.put("Text2", NbtFactory.of("text", getText(content.get(1), player)));
        nbtBases.put("Text3", NbtFactory.of("text", getText(content.get(2), player)));
        nbtBases.put("Text4", NbtFactory.of("text", getText(content.get(3), player)));

        nbtBases.put("x", block.getX());
        nbtBases.put("y", block.getY());
        nbtBases.put("z", block.getZ());

        packet.getBlockPositionModifier().write(0, blockPosition);
        packet.getModifier().write(1, this.tileEntityDataTwoObject);
        packet.getNbtModifier().write(0, nbtBases);

        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            plugin.getLogger().severe(String.format("An exception occurs when sending %s packets to %s.", "TILE_ENTITY_DATA", player.getName()));
        }
    }

    public String getText(String message, Player player) {
        message = PlaceholderAPI.setPlaceholders(player, message);

        try {
            new JSONParser().parse(message);
            return message;
        } catch (ParseException e) {
            return String.format("{\"extra\":[{\"text\":\"%s\"}],\"text\":\"\"}", message);
        }
    }
}
