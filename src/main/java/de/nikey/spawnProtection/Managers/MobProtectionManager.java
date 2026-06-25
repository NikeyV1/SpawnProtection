package de.nikey.spawnProtection.Managers;

import de.nikey.spawnProtection.SpawnProtection;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
    private final Map<UUID, BossBar> bossBars = new HashMap<>();

    public MobProtectionManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("mob-protection.enabled", true);
    }

    public boolean hasMobProtection(Player player) {
        return mobProtectionTime.containsKey(player.getUniqueId());
    }

    public String getDisplay() {
        return plugin.getConfig().getString("mob-protection.protection-display", "bossbar");
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
        BossBar existing = bossBars.remove(uuid);
        if (existing != null) {
            player.hideBossBar(existing);
        }

        if (announce) {
            String raw = plugin.getConfig().getString("messages.mob-protection-started",
                    "&aMonsters can't attack you for &e%time%&a after your first join.");
            Component message = LegacyComponentSerializer.legacyAmpersand()
                    .deserialize(raw.replace("%time%", formatPretty(seconds)));
            player.sendMessage(message);
        }

        String displayMode = getDisplay();
        if (displayMode.equalsIgnoreCase("bossbar")) {
            BossBar bossBar = BossBar.bossBar(
                    Component.text("Mob Protection").color(NamedTextColor.GREEN),
                    1.0f,
                    BossBar.Color.GREEN,
                    BossBar.Overlay.PROGRESS
            );
            player.showBossBar(bossBar);
            bossBars.put(uuid, bossBar);
        }

        int totalSeconds = seconds;

        timers.put(uuid, new BukkitRunnable() {
            @Override
            public void run() {
                int timeLeft = mobProtectionTime.getOrDefault(uuid, 0) - 1;
                if (timeLeft <= 0) {
                    mobProtectionTime.remove(uuid);
                    timers.remove(uuid);

                    BossBar bossBar = bossBars.remove(uuid);
                    if (bossBar != null && player.isOnline()) {
                        player.hideBossBar(bossBar);
                    }
                    cancel();

                    if (player.isOnline()) {
                        Component message = LegacyComponentSerializer.legacyAmpersand()
                                .deserialize(plugin.getConfig().getString("messages.mob-protection-ended",
                                        "&aThe protection against monsters has ended."));
                        player.sendMessage(message);
                    }
                } else {
                    mobProtectionTime.put(uuid, timeLeft);

                    if (!player.isOnline()) return;
                    updateDisplay(player, displayMode, timeLeft, totalSeconds);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L));
    }

    private void updateDisplay(Player player, String displayMode, int timeLeft, int totalSeconds) {
        String formatted = formatTime(timeLeft);
        if (displayMode.equalsIgnoreCase("bossbar")) {
            BossBar bossBar = bossBars.get(player.getUniqueId());
            if (bossBar != null) {
                bossBar.name(Component.text("Mob Protection: " + formatted).color(NamedTextColor.GREEN));
                bossBar.progress(Math.max(0f, Math.min(1f, (float) timeLeft / totalSeconds)));
            }
        } else if (displayMode.equalsIgnoreCase("actionbar")) {
            // Let an active spawn protection keep ownership of the action bar so the
            // two countdowns don't fight over the same line.
            if (SpawnProtection.getProtectionManager().isProtected(player)) return;
            player.sendActionBar(Component.text("Mob: " + formatted).color(NamedTextColor.GREEN));
        }
    }

    public void endProtection(Player player) {
        UUID uuid = player.getUniqueId();
        mobProtectionTime.remove(uuid);
        if (timers.containsKey(uuid)) {
            timers.get(uuid).cancel();
            timers.remove(uuid);
        }
        BossBar bossBar = bossBars.remove(uuid);
        if (bossBar != null) {
            player.hideBossBar(bossBar);
        }
    }

    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }

    private String formatPretty(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        if (minutes > 0) {
            return minutes + "m " + secs + "s";
        }
        return secs + "s";
    }
}
