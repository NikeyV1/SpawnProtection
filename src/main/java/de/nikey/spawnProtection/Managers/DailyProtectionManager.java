package de.nikey.spawnProtection.Managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DailyProtectionManager {

    private final Plugin plugin;
    private final File file;
    private final FileConfiguration config;
    private final Map<UUID, LocalDate> claimedDates = new HashMap<>();

    public DailyProtectionManager(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "dailyclaims.yml");
        this.config = YamlConfiguration.loadConfiguration(file);
        load();
    }

    public boolean hasClaimedToday(UUID uuid) {
        return claimedDates.containsKey(uuid) && claimedDates.get(uuid).isEqual(LocalDate.now());
    }

    public void setClaimedToday(UUID uuid) {
        claimedDates.put(uuid, LocalDate.now());
        config.set(uuid.toString(), LocalDate.now().toString());
        save();
    }

    private void load() {
        for (String key : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                LocalDate date = LocalDate.parse(config.getString(key));
                claimedDates.put(uuid, date);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load daily claim for UUID: " + key);
            }
        }
    }

    private void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save dailyclaims.yml!");
        }
    }
}
