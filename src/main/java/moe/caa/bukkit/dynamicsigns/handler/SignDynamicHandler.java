package moe.caa.bukkit.dynamicsigns.handler;

import moe.caa.bukkit.dynamicsigns.config.DynamicConfig;
import moe.caa.bukkit.dynamicsigns.main.DynamicSigns;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Collection;
import java.util.Map;

public class SignDynamicHandler {
    private final DynamicSigns plugin;
    private int tickNum;

    private Listener listener = new Listener() {
        @EventHandler(ignoreCancelled = true)
        private void onMove(PlayerMoveEvent event) {

        }
    };

    public SignDynamicHandler(DynamicSigns plugin) {
        this.plugin = plugin;
    }

    private void tick() {
        tickNum++;
        if (tickNum % 100 == 0) {
            plugin.getDataEntry().forEach((l, e) -> {
                if (l.getBlock() == null || !SignPacketHandler.isSign(l.getBlock().getType())) {
                    plugin.getLogger().info("Remove the dynamic sign at " + l);
                    plugin.getDataEntry().remove(l);
                }
            });
        }

        for (Map.Entry<Location, DynamicConfig> entry : plugin.getDataEntry().entrySet()) {
            int visualRange = entry.getValue().getVisualRange();
            final int lookMeRange = entry.getValue().getLookMeRange();
            final Collection<Entity> nearbyEntities = entry.getKey().getWorld().getNearbyEntities(entry.getKey(), visualRange, visualRange, visualRange);


            final DynamicConfig.Entry contentAT = entry.getValue().getContentAT(tickNum);
            for (Entity entity : nearbyEntities) {
                if (!(entity instanceof Player)) continue;
                plugin.getSignPacketHandler().sendSignContentUpdate(((Player) entity), entry.getKey(), contentAT.getContent(), contentAT.isGlowingText());
                if (entity.getLocation().distance(entry.getKey()) < lookMeRange) {
                    plugin.getSignPacketHandler().sendSignDirectionUpdate(((Player) entity), entry.getKey(), getRotation(entry.getKey(), entity.getLocation()));
                } else {
                    plugin.getSignPacketHandler().sendSignDirectionUpdate(((Player) entity), entry.getKey(), contentAT.getRotation());
                }
            }
        }
    }

    private int getRotation(Location src, Location target) {
        double dx = target.getX() - src.getX() - 0.5;
        double dz = target.getZ() - src.getZ() - 0.5;

        double angle = Math.acos(dz / Math.sqrt(dx * dx + dz * dz));
        int x = (int) (Math.toDegrees(angle) / 22.5);
        if (dx > 0) {
            x = 16 - x;
        }
        return x;
    }

    public void register() {
        Bukkit.getPluginManager().registerEvents(listener, plugin);
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 0, 1);
    }
}
