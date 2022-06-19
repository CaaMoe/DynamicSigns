package moe.caa.bukkit.dynamicsigns.main;

import lombok.Getter;
import lombok.SneakyThrows;
import moe.caa.bukkit.dynamicsigns.command.CommandHandler;
import moe.caa.bukkit.dynamicsigns.config.DynamicConfig;
import moe.caa.bukkit.dynamicsigns.handler.SignDynamicHandler;
import moe.caa.bukkit.dynamicsigns.handler.SignPacketHandler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public final class DynamicSigns extends JavaPlugin {
    private final SignPacketHandler signPacketHandler = new SignPacketHandler(this);
    private final File dataFile = new File(getDataFolder(), "data.yml");
    private Map<Location, DynamicConfig> dataEntry = new ConcurrentHashMap<>();
    private Map<String, DynamicConfig> dynamicSignsMap = new ConcurrentHashMap<>();

    @Override
    @SneakyThrows
    public void onEnable() {
        signPacketHandler.init();
        reload();
        readData();
        CommandHandler commandHandler = new CommandHandler(this);
        PluginCommand command = getCommand(getDescription().getName());
        command.setExecutor(commandHandler);
        command.setTabCompleter(commandHandler);
        new SignDynamicHandler(this).register();
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, this::saveData, 20 * 60 * 5, 20 * 60 * 5);
    }

    public void reload() throws IOException {
        saveDefaultConfig();
        reloadConfig();
        File dynamicFolder = new File(getDataFolder(), "dynamic");

        if (!dynamicFolder.exists()) {
            Files.createDirectory(dynamicFolder.toPath());
            final File file = new File(dynamicFolder, "dynamic_template.yml");
            Files.createFile(file.toPath());
            try (InputStream resource = getResource("dynamic_template.yml"); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                byte[] buf = new byte[1024];
                int num = 0;
                while ((num = resource.read(buf)) != -1) {
                    baos.write(buf, 0, num);
                }
                baos.flush();

                Files.write(file.toPath(), baos.toByteArray());
            }
        }

        Map<String, DynamicConfig> dynamicSignsMap = new ConcurrentHashMap<>();
        {
            final List<File> childrenFiles = getChildrenFiles(dynamicFolder);
            for (File file : childrenFiles) {
                final String name = file.getAbsolutePath().substring(dynamicFolder.getAbsolutePath().length() + 1);
                try {
                    final DynamicConfig of = DynamicConfig.of(name, YamlConfiguration.loadConfiguration(file));
                    if (of.getCountTicks() != 0) {
                        dynamicSignsMap.put(name, of);
                        getLogger().info("Loaded " + name);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    getLogger().severe("An exception occurred on reading the dynamic file " + file.getAbsolutePath());
                }
            }
        }

        getLogger().info(dynamicSignsMap.size() + " dynamic files are loaded.");

        this.dynamicSignsMap = dynamicSignsMap;
    }

    public void readData() {
        try {
            if (!getDataFolder().exists()) Files.createDirectory(getDataFolder().toPath());
            if (!dataFile.exists()) Files.createFile(dataFile.toPath());

            final YamlConfiguration section = YamlConfiguration.loadConfiguration(dataFile);
            Map<Location, DynamicConfig> dataEntry = new ConcurrentHashMap<>();
            for (String key : section.getKeys(false)) {
                String[] split = key.split("\\*");
                if (split.length != 4) {
                    getLogger().warning("Remove invalid data " + key);
                    continue;
                }
                Location location = new Location(Bukkit.getWorld(split[0]), Integer.parseInt(split[1]),
                        Integer.parseInt(split[2]), Integer.parseInt(split[3]));
                if (location.getWorld() == null || location.getBlock() == null || !SignPacketHandler.isSign(location.getBlock().getType())) {
                    getLogger().warning("Remove invalid data " + key);
                    continue;
                }
                final DynamicConfig dynamicConfig = this.dynamicSignsMap.get(section.getString(key));
                if (dynamicConfig == null) {
                    getLogger().warning("Remove invalid data " + key);
                    continue;
                }
                dataEntry.put(location, dynamicConfig);
            }
            this.dataEntry = dataEntry;
        } catch (Exception e) {
            e.printStackTrace();
            getLogger().severe("Data file is broken and will be overwritten.");
        }
    }

    public synchronized void saveData() {
        YamlConfiguration configuration = new YamlConfiguration();
        for (Map.Entry<Location, DynamicConfig> entry : dataEntry.entrySet()) {
            configuration.set(String.format("%s*%d*%d*%d*", entry.getKey().getWorld().getName(),
                    entry.getKey().getBlockX(),
                    entry.getKey().getBlockY(),
                    entry.getKey().getBlockZ()
            ), entry.getValue().getPath());
        }

        try {
            configuration.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
            getLogger().severe("Unable to save data file.");
        }
    }

    public List<File> getChildrenFiles(File folderFile) throws IOException {
        List<File> childrenFiles = new ArrayList<>();
        BasicFileAttributes attributes = Files.readAttributes(folderFile.toPath(), BasicFileAttributes.class);
        if (attributes.isDirectory()) {
            File[] files = folderFile.listFiles();
            if (files != null) {
                for (File file : files) {
                    childrenFiles.addAll(getChildrenFiles(file));
                }
            }
        }
        if (attributes.isRegularFile()) {
            childrenFiles.add(folderFile);
        }
        return childrenFiles;
    }

    @Override
    public void onDisable() {
        saveData();
    }
}
