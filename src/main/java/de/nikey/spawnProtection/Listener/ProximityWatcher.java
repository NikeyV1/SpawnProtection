package de.nikey.spawnProtection.Listener;

import de.nikey.spawnProtection.Managers.ProtectionManager;
import de.nikey.spawnProtection.SpawnProtection;
import de.nikey.trust.Trust;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ProximityWatcher {

    private final ProtectionManager manager;
    private final Plugin plugin;
    private final Map<UUID, Map<UUID, Integer>> proximityTime = new HashMap<>();
    private final int radius;
    private final int minTime;

    public ProximityWatcher(ProtectionManager manager) {
        this.manager = manager;
        this.plugin = manager.getPlugin();
        FileConfiguration config = plugin.getConfig();
        this.radius = config.getInt("proximity-detection.radius", 50);
        this.minTime = config.getInt("proximity-detection.min-time-seconds", 120);
        startWatching();
    }

    private void startWatching() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player protectedPlayer : Bukkit.getOnlinePlayers()) {
                if (!manager.isProtected(protectedPlayer)) {
                    proximityTime.remove(protectedPlayer.getUniqueId());
                    continue;
                }

                Collection<Player> nearbyPlayers = protectedPlayer.getWorld()
                        .getNearbyPlayers(protectedPlayer.getLocation(), radius);

                Map<UUID, Integer> nearbyMap = proximityTime.computeIfAbsent(protectedPlayer.getUniqueId(), k -> new HashMap<>());

                for (Player other : nearbyPlayers) {
                    if (other.equals(protectedPlayer)) continue;
                    if (manager.isProtected(other)) continue;
                    if (SpawnProtection.hasTrustEnabled) {
                        if (Trust.isTrusted(protectedPlayer.getUniqueId(),other.getUniqueId()))continue;
                    }

                    int seconds = nearbyMap.getOrDefault(other.getUniqueId(), 0) + 2;
                    nearbyMap.put(other.getUniqueId(), seconds);

                    if (seconds >= minTime) {
                        FileConfiguration config = plugin.getConfig();

                        Component protectedMsg = LegacyComponentSerializer.legacyAmpersand()
                                .deserialize(config.getString("messages.protected-nearby", "&eYou are too close to players who could harm you!"));

                        Component aggressorMsg = LegacyComponentSerializer.legacyAmpersand()
                                .deserialize(config.getString("messages.aggressor-nearby", "&cDo not chase spawn protected players!"));

                        protectedPlayer.sendMessage(protectedMsg);
                        other.sendMessage(aggressorMsg);

                        nearbyMap.put(other.getUniqueId(), -9999);
                    }
                }

                nearbyMap.keySet().removeIf(uuid ->
                        Bukkit.getPlayer(uuid) == null ||
                                !nearbyPlayers.contains(Bukkit.getPlayer(uuid))
                );
            }
        }, 0L, 40L);
    }
}
