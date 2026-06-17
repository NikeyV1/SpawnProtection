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
    private final Map<UUID, Integer> baseSeconds = new HashMap<>();
    private final Map<UUID, Integer> sessionSeconds = new HashMap<>();
    private final String today;

    public TodayPlaytimeManager(Plugin plugin) {
        this.file = new File(plugin.getDataFolder(), "todayPlaytime.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
        this.today = LocalDate.now().toString();

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    UUID uuid = player.getUniqueId();
                    sessionSeconds.put(uuid, sessionSeconds.getOrDefault(uuid, 0) + 1);
                    data.set(today + "." + uuid, getTodaySeconds(uuid));
                }

                // Persist once a minute to avoid excessive disk writes.
                if (++ticks >= 60) {
                    ticks = 0;
                    save();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public int getTodaySeconds(UUID uuid) {
        return getStoredSeconds(uuid) + sessionSeconds.getOrDefault(uuid, 0);
    }

    private int getStoredSeconds(UUID uuid) {
        return baseSeconds.computeIfAbsent(uuid, u -> data.getInt(today + "." + u, 0));
    }

    private void save() {
        try {
            data.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
