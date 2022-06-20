package moe.caa.bukkit.dynamicsigns.handler.placelholder;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

public class UsePlaceholderAPIHandler extends PlaceholderHandler {

    @Override
    public String setPlaceholders(String s, Player player) {
        return PlaceholderAPI.setPlaceholders(player, s);
    }
}
