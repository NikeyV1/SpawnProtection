package de.nikey.spawnProtection.Managers;

import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ProtectionManager {

    private final Plugin plugin;
    private final Map<UUID, Integer> protectionTime = new HashMap<>();
    private final Map<UUID, BukkitTask> timers = new HashMap<>();
    private final int defaultProtectionMinutes;
    private final int firstJoinProtectionMinutes;

    public ProtectionManager(Plugin plugin) {
        this.plugin = plugin;
        FileConfiguration config = plugin.getConfig();
        this.defaultProtectionMinutes = config.getInt("protection.default-minutes", 5);
        this.firstJoinProtectionMinutes = config.getInt("protection.first-join-minutes", 10);
    }

    public void startProtection(Player player, boolean firstJoin) {
        int minutes = firstJoin ? firstJoinProtectionMinutes : defaultProtectionMinutes;
        if (minutes <= 0) return;

        protectionTime.put(player.getUniqueId(), minutes * 60);

        if (timers.containsKey(player.getUniqueId())) {
            timers.get(player.getUniqueId()).cancel();
        }

        timers.put(player.getUniqueId(), new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;

                int timeLeft = protectionTime.getOrDefault(player.getUniqueId(), 0) - 1;
                if (timeLeft <= 0) {
                    protectionTime.remove(player.getUniqueId());
                    timers.remove(player.getUniqueId());
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
                    player.sendActionBar(plugin.getConfig().getString("messages.protection-ended", "&aYour spawn protection has ended."));
                    cancel();
                } else {
                    protectionTime.put(player.getUniqueId(), timeLeft);
                    player.sendActionBar("§b" + formatTime(timeLeft));
                }
            }
        }.runTaskTimer(plugin, 20L, 20L));
    }

    public boolean isProtected(Player player) {
        return protectionTime.containsKey(player.getUniqueId());
    }
    public boolean hasProtection(UUID player) {
        return protectionTime.containsKey(player);
    }

    public void endProtection(Player player) {
        protectionTime.remove(player.getUniqueId());
        if (timers.containsKey(player.getUniqueId())) {
            timers.get(player.getUniqueId()).cancel();
            timers.remove(player.getUniqueId());
        }
    }

    public void endProtection(UUID player) {
        protectionTime.remove(player);
        if (timers.containsKey(player)) {
            timers.get(player).cancel();
            timers.remove(player);
        }
    }

    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }

    public Plugin getPlugin() {
        return plugin;
    }
}
