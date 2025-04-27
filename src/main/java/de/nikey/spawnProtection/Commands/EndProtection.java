package de.nikey.spawnProtection.Commands;

import de.nikey.spawnProtection.Managers.ProtectionManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class EndProtection implements CommandExecutor {
    private final ProtectionManager protectionManager;

    public EndProtection(ProtectionManager protectionManager) {
        this.protectionManager = protectionManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) return true;

        if (!protectionManager.hasProtection(player.getUniqueId())) {
            player.sendMessage(Component.text("You don't have spawn protection active.", NamedTextColor.RED));
            return true;
        }

        protectionManager.endProtection(player.getUniqueId());
        player.sendMessage(Component.text("Your spawn protection has been disabled.", NamedTextColor.GREEN));
        return true;
    }
}
