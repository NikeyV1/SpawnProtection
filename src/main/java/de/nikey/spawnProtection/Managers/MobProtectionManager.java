package de.nikey.spawnProtection.Managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles a separate, time-limited protection against mobs/monsters.
 * Mainly used to give players a grace period after they join the server for the
 * first time, so they don't get attacked by monsters right away (e.g. at night).
 */
public class MobProtectionManager {

    private final Plugin plugin;
    private final Map<UUID, Integer> mobProtectionTime = new HashMap<>();
    private final Map<UUID, BukkitTask> timers = new HashMap<>();

    public MobProtectionManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("mob-protection.enabled", true);
    }

    public boolean hasMobProtection(Player player) {
        return mobProtectionTime.containsKey(player.getUniqueId());
    }

    /**
     * Grants the configured first-join mob protection if the feature is enabled.
     */
    public void startFirstJoinProtection(Player player) {
        if (!isEnabled()) return;
        int seconds = plugin.getConfig().getInt("mob-protection.first-join-seconds", 300);
        grantProtection(player, seconds, true);
    }

    public void grantProtection(Player player, int seconds, boolean announce) {
        if (seconds <= 0) return;

        UUID uuid = player.getUniqueId();
        mobProtectionTime.put(uuid, seconds);

        if (timers.containsKey(uuid)) {
            timers.get(uuid).cancel();
        }

        if (announce) {
            String raw = plugin.getConfig().getString("messages.mob-protection-started",
                    "&aMonsters can't attack you for &e%time%&a after your first join.");
            Component message = LegacyComponentSerializer.legacyAmpersand()
                    .deserialize(raw.replace("%time%", formatTime(seconds)));
            player.sendMessage(message);
        }

        timers.put(uuid, new BukkitRunnable() {
            @Override
            public void run() {
                int timeLeft = mobProtectionTime.getOrDefault(uuid, 0) - 1;
                if (timeLeft <= 0) {
                    mobProtectionTime.remove(uuid);
                    timers.remove(uuid);
                    cancel();

                    if (player.isOnline()) {
                        Component message = LegacyComponentSerializer.legacyAmpersand()
                                .deserialize(plugin.getConfig().getString("messages.mob-protection-ended",
                                        "&aThe protection against monsters has ended."));
                        player.sendMessage(message);
                    }
                } else {
                    mobProtectionTime.put(uuid, timeLeft);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L));
    }

    public void endProtection(Player player) {
        UUID uuid = player.getUniqueId();
        mobProtectionTime.remove(uuid);
        if (timers.containsKey(uuid)) {
            timers.get(uuid).cancel();
            timers.remove(uuid);
        }
    }

    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        if (minutes > 0) {
            return minutes + "m " + secs + "s";
        }
        return secs + "s";
    }
}
