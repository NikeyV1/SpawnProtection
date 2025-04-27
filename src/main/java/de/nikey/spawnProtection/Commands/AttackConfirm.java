package de.nikey.spawnProtection.Commands;

import de.nikey.spawnProtection.Listener.ProtectionListener;
import de.nikey.spawnProtection.Managers.ProtectionManager;
import de.nikey.spawnProtection.SpawnProtection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class AttackConfirm implements CommandExecutor {
    private final ProtectionManager protectionManager;

    public AttackConfirm(ProtectionManager protectionManager) {
        this.protectionManager = protectionManager;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (args.length != 1) {
            player.sendMessage(Component.text("Usage: /confirmattack <playerUUID>", NamedTextColor.RED));
            return true;
        }

        UUID victimUUID;
        try {
            victimUUID = UUID.fromString(args[0]);
        } catch (IllegalArgumentException e) {
            player.sendMessage(Component.text("Invalid player ID!", NamedTextColor.RED));
            return true;
        }

        ProtectionListener.pendingConfirmations.put(player.getUniqueId(), victimUUID);

        new BukkitRunnable() {
            @Override
            public void run() {
                UUID confirmedVictim = ProtectionListener.pendingConfirmations.get(player.getUniqueId());
                if (confirmedVictim != null && confirmedVictim.equals(victimUUID)) {
                    ProtectionListener.pendingConfirmations.remove(player.getUniqueId());
                }
            }
        }.runTaskLater(SpawnProtection.getPlugin(), 10 * 60 * 20);

        player.sendMessage(Component.text("You have chosen to attack this player. Be careful!", NamedTextColor.YELLOW));
        return true;
    }
}
