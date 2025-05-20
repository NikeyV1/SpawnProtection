package de.nikey.spawnProtection.Managers;

import de.nikey.buffSMP.General.ShowCooldown;
import de.nikey.spawnProtection.SpawnProtection;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
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
    public final Map<UUID, Integer> protectionTime = new HashMap<>();
    private final Map<UUID, BukkitTask> timers = new HashMap<>();
    private final Map<UUID, BossBar> bossBars = new HashMap<>();
    private final int defaultProtectionMinutes;
    private final int firstJoinProtectionMinutes;

    public ProtectionManager(Plugin plugin) {
        this.plugin = plugin;
        FileConfiguration config = plugin.getConfig();
        this.defaultProtectionMinutes = config.getInt("protection.default-minutes", 5);
        this.firstJoinProtectionMinutes = config.getInt("protection.first-join-minutes", 10);
    }


    public void startProtection(Player player, boolean firstJoin) {
        int sec = firstJoin ? firstJoinProtectionMinutes : defaultProtectionMinutes;
        if (sec <= 0) return;

        protectionTime.put(player.getUniqueId(), sec * 60);
        applyNameProtectionState(player,true);

        if (timers.containsKey(player.getUniqueId())) {
            timers.get(player.getUniqueId()).cancel();
        }
        BossBar bar = bossBars.remove(player.getUniqueId());
        if (bar != null) {
            bar.removeViewer(player);
        }

        String displayMode = plugin.getConfig().getString("protection.protection-display", "actionbar");
        if (displayMode.equalsIgnoreCase("bossbar")) {
            BossBar bossBar = BossBar.bossBar(
                    Component.text("Spawn Protection").color(NamedTextColor.AQUA),
                    1.0f,
                    BossBar.Color.BLUE,
                    BossBar.Overlay.PROGRESS
            );
            player.showBossBar(bossBar);
            bossBars.put(player.getUniqueId(), bossBar);
        }

        int totalSeconds = sec * 60;

        timers.put(player.getUniqueId(), new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;

                int timeLeft = protectionTime.getOrDefault(player.getUniqueId(), 0) - 1;
                if (timeLeft <= 0) {
                    protectionTime.remove(player.getUniqueId());
                    timers.remove(player.getUniqueId());

                    BossBar bossBar = bossBars.remove(player.getUniqueId());
                    if (bossBar != null) {
                        player.hideBossBar(bossBar);
                    }

                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
                    Component message = LegacyComponentSerializer.legacyAmpersand().deserialize(plugin.getConfig().getString("messages.protection-ended", "&aYour spawn protection has ended."));
                    player.sendActionBar(message);
                    applyNameProtectionState(player, false);
                    cancel();
                } else {
                    protectionTime.put(player.getUniqueId(), timeLeft);

                    String formatted = formatTime(timeLeft);

                    if (displayMode.equalsIgnoreCase("bossbar")) {
                        BossBar bossBar = bossBars.get(player.getUniqueId());
                        if (bossBar != null) {
                            bossBar.name(Component.text("Spawn Protection: " + formatted).color(NamedTextColor.AQUA));
                            bossBar.progress((float) timeLeft / totalSeconds);
                        }
                    } else {
                        if (SpawnProtection.hasBuffSMPEnabled) {
                            if (!ShowCooldown.viewingPlayers.containsKey(player.getUniqueId())) {
                                player.sendActionBar(Component.text(formatted).color(NamedTextColor.DARK_AQUA));
                            }
                        } else {
                            player.sendActionBar(Component.text(formatted).color(NamedTextColor.DARK_AQUA));
                        }
                    }
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
        BossBar bar = bossBars.remove(player.getUniqueId());
        if (bar != null) {
            bar.removeViewer(player);
        }
        applyNameProtectionState(player, false);
    }

    public void endProtection(UUID player) {
        protectionTime.remove(player);
        if (timers.containsKey(player)) {
            timers.get(player).cancel();
            timers.remove(player);
        }
    }

    public void grantOfflineProtection(OfflinePlayer player, int seconds) {
        if (seconds <= 0) return;

        UUID uuid = player.getUniqueId();
        protectionTime.put(uuid, seconds);
    }

    public int getProtectionTime(Player player) {
        return protectionTime.get(player.getUniqueId());
    }

    public void grantProtection(Player player, int seconds) {
        if (seconds <= 0) return;

        UUID uuid = player.getUniqueId();
        protectionTime.put(uuid, seconds);

        if (timers.containsKey(uuid)) {
            timers.get(uuid).cancel();
        }
        BossBar bar = bossBars.remove(player.getUniqueId());
        if (bar != null) {
            bar.removeViewer(player);
        }


        String displayMode = plugin.getConfig().getString("protection.protection-display", "actionbar");
        if (displayMode.equalsIgnoreCase("bossbar")) {
            BossBar bossBar = BossBar.bossBar(
                    Component.text("Spawn Protection").color(NamedTextColor.AQUA),
                    1.0f,
                    BossBar.Color.BLUE,
                    BossBar.Overlay.PROGRESS
            );
            player.showBossBar(bossBar);
            bossBars.put(player.getUniqueId(), bossBar);
        }

        timers.put(uuid, new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;

                int timeLeft = protectionTime.getOrDefault(uuid, 0) - 1;
                if (timeLeft <= 0) {
                    protectionTime.remove(uuid);
                    timers.remove(uuid);
                    BossBar bossBar = bossBars.remove(player.getUniqueId());
                    if (bossBar != null) {
                        player.hideBossBar(bossBar);
                    }
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
                    Component message = LegacyComponentSerializer.legacyAmpersand().deserialize(plugin.getConfig().getString("messages.protection-ended", "&aYour spawn protection has ended."));
                    player.sendActionBar(message);
                    applyNameProtectionState(player, false);
                    cancel();
                } else {
                    protectionTime.put(player.getUniqueId(), timeLeft);
                    String formatted = formatTime(timeLeft);
                    if (displayMode.equalsIgnoreCase("bossbar")) {
                        BossBar bossBar = bossBars.get(player.getUniqueId());
                        if (bossBar != null) {
                            bossBar.name(Component.text("Spawn Protection: " + formatted).color(NamedTextColor.AQUA));
                            bossBar.progress((float) timeLeft / seconds);
                        }
                    } else {
                        if (SpawnProtection.hasBuffSMPEnabled) {
                            if (!ShowCooldown.viewingPlayers.containsKey(player.getUniqueId())) {
                                player.sendActionBar(Component.text(formatted).color(NamedTextColor.DARK_AQUA));
                            }
                        } else {
                            player.sendActionBar(Component.text(formatted).color(NamedTextColor.DARK_AQUA));
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L));

        applyNameProtectionState(player, true);
    }

    public void applyNameProtectionState(Player player, boolean hasProtection) {
        String playerName = player.getName();
        Component teamPrefix = Component.empty();

        if (player.getScoreboard().getEntryTeam(playerName) != null) {
            teamPrefix = player.getScoreboard().getEntryTeam(playerName).prefix();
        }

        Component baseName = Component.text(playerName);
        Component finalName = teamPrefix.append(baseName.color(NamedTextColor.WHITE));

        if (hasProtection) {
            finalName = finalName.append(Component.text(" (S)").color(NamedTextColor.DARK_AQUA));
        }

        player.playerListName(finalName);
    }

    public void emptyProtection() {
        protectionTime.clear();
        for (Player player : Bukkit.getOnlinePlayers()) {
            BossBar bar = bossBars.remove(player.getUniqueId());
            if (bar != null) {
                bar.removeViewer(player);
            }
            applyNameProtectionState(player,false);
        }
    }

    public String formatTime(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public void pauseProtection(Player player) {
        UUID uuid = player.getUniqueId();
        if (timers.containsKey(uuid)) {
            timers.get(uuid).cancel();
            timers.remove(uuid);
        }
    }

    public void resumeProtection(Player player) {
        UUID uuid = player.getUniqueId();
        if (!protectionTime.containsKey(uuid)) return; // Kein Schutz aktiv

        int timeLeft = protectionTime.get(uuid);
        if (timeLeft <= 0) {
            endProtection(player);
            return;
        }

        String displayMode = plugin.getConfig().getString("protection.protection-display", "actionbar");
        BossBar bar = bossBars.remove(player.getUniqueId());
        if (bar != null) {
            bar.removeViewer(player);
        }

        if (displayMode.equalsIgnoreCase("bossbar")) {
            BossBar bossBar = BossBar.bossBar(
                    Component.text("Spawn Protection").color(NamedTextColor.AQUA),
                    1.0f,
                    BossBar.Color.BLUE,
                    BossBar.Overlay.PROGRESS
            );
            player.showBossBar(bossBar);
            bossBars.put(player.getUniqueId(), bossBar);
        }

        int finalTime = timeLeft;

        timers.put(uuid, new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;

                int timeLeft = protectionTime.getOrDefault(uuid, 0) - 1;
                if (timeLeft <= 0) {
                    protectionTime.remove(uuid);
                    timers.remove(uuid);
                    BossBar bossBar = bossBars.remove(player.getUniqueId());
                    if (bossBar != null) {
                        player.hideBossBar(bossBar);
                    }
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
                    Component message = LegacyComponentSerializer.legacyAmpersand().deserialize(plugin.getConfig().getString("messages.protection-ended", "&aYour spawn protection has ended."));
                    player.sendActionBar(message);
                    applyNameProtectionState(player, false);
                    cancel();
                } else {
                    protectionTime.put(player.getUniqueId(), timeLeft);
                    String formatted = formatTime(timeLeft);
                    if (displayMode.equalsIgnoreCase("bossbar")) {
                        BossBar bossBar = bossBars.get(player.getUniqueId());
                        if (bossBar != null) {
                            bossBar.name(Component.text("Spawn Protection: " + formatted).color(NamedTextColor.AQUA));
                            bossBar.progress((float) timeLeft / finalTime);
                        }
                    } else {
                        if (SpawnProtection.hasBuffSMPEnabled) {
                            if (!ShowCooldown.viewingPlayers.containsKey(player.getUniqueId())) {
                                player.sendActionBar(Component.text(formatted).color(NamedTextColor.DARK_AQUA));
                            }
                        } else {
                            player.sendActionBar(Component.text(formatted).color(NamedTextColor.DARK_AQUA));
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L));

        applyNameProtectionState(player, true);
    }

}