package moe.caa.bukkit.dynamicsigns.handler.placelholder;

import org.bukkit.entity.Player;

public class EmptyPlaceholderHandler extends PlaceholderHandler {
    @Override
    public String setPlaceholders(String s, Player player) {
        return s;
    }
}
