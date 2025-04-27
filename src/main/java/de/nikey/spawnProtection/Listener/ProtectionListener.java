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
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPlayedBefore()) {
            manager.startProtection(player, true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (manager.isProtected(player)) {
            manager.endProtection(player);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!(event.getEntity() instanceof Player victim)) return;

        if (manager.isProtected(victim)) {
            event.setCancelled(true);
            return;
        }

        if (manager.isProtected(damager)) {
            event.setCancelled(true);
        }
    }
    public static final Map<UUID, UUID> pendingConfirmations = new HashMap<>();

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!(event.getEntity() instanceof Player victim)) return;

        ProtectionManager protectionManager = SpawnProtection.getPlugin().getProtectionManager();

        if (protectionManager.isProtected(damager)) {
            event.setCancelled(true);

            damager.sendMessage(Component.text()
                    .append(Component.text("⚠ ", NamedTextColor.RED))
                    .append(Component.text("You cannot attack while under spawn protection! ", NamedTextColor.RED))
                    .append(Component.text("[End Protection]", NamedTextColor.YELLOW)
                            .clickEvent(ClickEvent.runCommand("/endprotection"))
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
                        .clickEvent(ClickEvent.runCommand("/confirmattack " + victim.getUniqueId()))
                        .hoverEvent(HoverEvent.showText(Component.text("Click to attack at your own risk!", NamedTextColor.RED))))
        );
    }
}