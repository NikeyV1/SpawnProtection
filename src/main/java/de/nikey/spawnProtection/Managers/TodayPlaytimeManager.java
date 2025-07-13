package de.nikey.spawnProtection.Managers;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TodayPlaytimeManager {
    private final File file;
    private final YamlConfiguration data;
    private final Map<UUID, Integer> sessionMinutes = new HashMap<>();
    private final String today;

    public TodayPlaytimeManager(Plugin plugin) {
        this.file = new File(plugin.getDataFolder(), "todayPlaytime.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
        this.today = LocalDate.now().toString();

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    UUID uuid = player.getUniqueId();
                    sessionMinutes.put(uuid, sessionMinutes.getOrDefault(uuid, 0) + 1);
                    int total = getStoredMinutes(uuid) + sessionMinutes.get(uuid);
                    data.set(today + "." + uuid, total);
                }
                save();
            }
        }.runTaskTimer(plugin, 20 * 60L, 20 * 60L);
    }

    public int getTodayMinutes(UUID uuid) {
        return getStoredMinutes(uuid) + sessionMinutes.getOrDefault(uuid, 0);
    }

    private int getStoredMinutes(UUID uuid) {
        return data.getInt(today + "." + uuid.toString(), 0);
    }

    private void save() {
        try {
            data.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}