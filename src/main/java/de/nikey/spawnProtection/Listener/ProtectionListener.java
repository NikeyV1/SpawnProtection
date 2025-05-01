package de.nikey.spawnProtection.Listener;

import de.nikey.spawnProtection.Managers.ProtectionManager;
import de.nikey.spawnProtection.SpawnProtection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ProtectionListener implements Listener {
    private final ProtectionManager manager;

    public ProtectionListener(ProtectionManager manager) {
        this.manager = manager;
        manager.getPlugin().getServer().getPluginManager().registerEvents(this, manager.getPlugin());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        manager.startProtection(player, false);

        UUID deadPlayerUUID = player.getUniqueId();

        pendingConfirmations.entrySet().removeIf(entry ->
                entry.getKey().equals(deadPlayerUUID) || entry.getValue().equals(deadPlayerUUID)
        );
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        manager.pauseProtection(player);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!player.hasPlayedBefore()) {
            manager.startProtection(player, true);
        } else if (manager.hasProtection(player.getUniqueId())) {
            manager.resumeProtection(player);
        }
    }


    public static final Map<UUID, UUID> pendingConfirmations = new HashMap<>();

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!(event.getEntity() instanceof Player victim)) return;

        ProtectionManager protectionManager = SpawnProtection.getProtectionManager();

        if (protectionManager.isProtected(damager)) {
            event.setCancelled(true);

            damager.sendMessage(Component.text()
                    .append(Component.text("⚠ ", NamedTextColor.RED))
                    .append(Component.text("You cannot attack while under spawn protection! ", NamedTextColor.RED))
                    .append(Component.text("[End Protection]", NamedTextColor.YELLOW)
                            .clickEvent(ClickEvent.runCommand("/spawnprotection end"))
                            .hoverEvent(HoverEvent.showText(Component.text("Click to end your protection!", NamedTextColor.RED))))
            );
            return;
        }

        if (!protectionManager.isProtected(victim)) return;

        if (pendingConfirmations.getOrDefault(damager.getUniqueId(), null) != null
                && pendingConfirmations.get(damager.getUniqueId()).equals(victim.getUniqueId())) {
            return;
        }

        event.setCancelled(true);

        damager.sendMessage(Component.text()
                .append(Component.text("⚠ ", NamedTextColor.RED))
                .append(Component.text("This player has spawn protection! ", NamedTextColor.RED))
                .append(Component.text("[Attack Anyway]", NamedTextColor.YELLOW)
                        .clickEvent(ClickEvent.runCommand("/spawnprotection confirm " + victim.getName()))
                        .hoverEvent(HoverEvent.showText(Component.text("Click to attack at your own risk!", NamedTextColor.RED))))
        );
    }
}