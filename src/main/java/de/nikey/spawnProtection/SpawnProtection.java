package de.nikey.spawnProtection;

import de.nikey.spawnProtection.Commands.SpawnProtectionCommand;
import de.nikey.spawnProtection.Listener.ProtectionListener;
import de.nikey.spawnProtection.Listener.ProximityWatcher;
import de.nikey.spawnProtection.Managers.DailyProtectionManager;
import de.nikey.spawnProtection.Managers.ProtectionManager;
import de.nikey.spawnProtection.Managers.TodayPlaytimeManager;
import de.nikey.spawnProtection.Util.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public final class SpawnProtection extends JavaPlugin {
    private static SpawnProtection plugin;
    private static ProtectionManager protectionManager;
    private static DailyProtectionManager dailyProtectionManager;
    private static TodayPlaytimeManager todayPlaytimeManager;

    public static boolean hasTrustEnabled = false;
    public static boolean hasBuffSMPEnabled = false;

    @Override
    public void onEnable() {
        plugin = this;
        saveDefaultConfig();
        protectionManager = new ProtectionManager(this);
        dailyProtectionManager = new DailyProtectionManager(this);
        todayPlaytimeManager = new TodayPlaytimeManager(this);
        getCommand("spawnprotection").setExecutor(new SpawnProtectionCommand());
        getCommand("spawnprotection").setTabCompleter(new SpawnProtectionCommand());
        new ProtectionListener(protectionManager);
        new ProximityWatcher(protectionManager);
        loadProtectionTimes();

        if (getServer().getPluginManager().getPlugin("Trust") != null) {
            hasTrustEnabled = true;
            getLogger().info("Trust detected! Compatibility features enabled");
        }

        if (getServer().getPluginManager().getPlugin("BuffSMP") != null) {
            hasBuffSMPEnabled = true;
            getLogger().info("BuffSMP detected! Compatibility features enabled");
        }
        Metrics metrics = new Metrics(this,	28280);
    }

    @Override
    public void onDisable() {
        saveProtectionTimes();
    }

    public void saveProtectionTimes() {
        File file = new File(plugin.getDataFolder(), "protection_data.yml");
        FileConfiguration data = YamlConfiguration.loadConfiguration(file);

        for (Map.Entry<UUID, Integer> entry : protectionManager.protectionTime.entrySet()) {
            data.set(entry.getKey().toString(), entry.getValue());
        }

        try {
            data.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadProtectionTimes() {
        File file = new File(plugin.getDataFolder(), "protection_data.yml");
        if (!file.exists()) return;

        FileConfiguration data = YamlConfiguration.loadConfiguration(file);

        for (String key : data.getKeys(false)) {
            UUID uuid = UUID.fromString(key);
            int seconds = data.getInt(key);
            protectionManager.protectionTime.put(uuid, seconds);
        }

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (protectionManager.protectionTime.containsKey(online.getUniqueId())) {
                protectionManager.grantProtection(online, protectionManager.protectionTime.get(online.getUniqueId()));
            }
        }

        // Datei nach dem Laden löschen, um alten Stand nicht zu behalten
        file.delete();
    }

    public static TodayPlaytimeManager getTodayPlaytimeManager() {
        return todayPlaytimeManager;
    }

    public static DailyProtectionManager getDailyProtectionManager() {
        return dailyProtectionManager;
    }

    public static ProtectionManager getProtectionManager() {
        return protectionManager;
    }

    public static SpawnProtection getPlugin() {
        return plugin;
    }
}
