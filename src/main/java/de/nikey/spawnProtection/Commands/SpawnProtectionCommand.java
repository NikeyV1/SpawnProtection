package de.nikey.spawnProtection.Commands;

import de.nikey.spawnProtection.Listener.ProtectionListener;
import de.nikey.spawnProtection.Managers.DailyProtectionManager;
import de.nikey.spawnProtection.Managers.ProtectionManager;
import de.nikey.spawnProtection.Managers.TodayPlaytimeManager;
import de.nikey.spawnProtection.SpawnProtection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class SpawnProtectionCommand implements TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player))return true;

        if (command.getName().equalsIgnoreCase("Spawnprotection")) {
            if (args.length == 1) {
                if (args[0].equalsIgnoreCase("end")) {
                    ProtectionManager protectionManager = SpawnProtection.getProtectionManager();

                    if (!protectionManager.hasProtection(player.getUniqueId())) {
                        player.sendMessage(Component.text("You don't have spawn protection active.", NamedTextColor.RED));
                        return true;
                    }

                    protectionManager.endProtection(player);
                    player.sendMessage(Component.text("Your spawn protection has been disabled.", NamedTextColor.GREEN));
                    return true;
                }else if (args[0].equalsIgnoreCase("clear")) {
                    if (!player.isOp())return true;
                    ProtectionManager protectionManager = SpawnProtection.getProtectionManager();

                    protectionManager.emptyProtection();

                    player.sendMessage(Component.text("Cleared spawn protection from everyone").color(NamedTextColor.GREEN));
                    return true;
                }else if (args[0].equalsIgnoreCase("claim")) {
                    ProtectionManager protectionManager = SpawnProtection.getProtectionManager();
                    DailyProtectionManager dailyProtectionManager = SpawnProtection.getDailyProtectionManager();
                    TodayPlaytimeManager playtimeManager = SpawnProtection.getTodayPlaytimeManager();

                    if (!protectionManager.getPlugin().getConfig().getBoolean("claim.enabled", true)) {
                        player.sendMessage("§cClaiming spawn protection is currently disabled.");
                        return true;
                    }

                    int requiredMinutes = protectionManager.getPlugin().getConfig().getInt("claim.required-online-minutes", 15);
                    int todayMinutes = playtimeManager.getTodayMinutes(player.getUniqueId());

                    if (todayMinutes < requiredMinutes) {
                        player.sendMessage("§cYou must play at least " + requiredMinutes + " minutes today to claim spawn protection.");
                        return true;
                    }

                    if (dailyProtectionManager.hasClaimedToday(player.getUniqueId())) {
                        player.sendMessage("§cYou have already claimed your daily spawn protection.");
                        return true;
                    }

                    dailyProtectionManager.setClaimedToday(player.getUniqueId());
                    protectionManager.grantProtection(player, SpawnProtection.getPlugin().getConfig().getInt("claim.duration",30) * 60);
                    player.sendMessage(Component.text("You have received your daily spawn protection!", NamedTextColor.GREEN));
                    player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 2f);
                    return true;
                }
            }else if (args.length == 2) {
                if (args[0].equalsIgnoreCase("end")) {
                    if (!player.isOp())return true;
                    UUID playerUniqueId = Bukkit.getPlayerUniqueId(args[1]);

                    ProtectionManager protectionManager = SpawnProtection.getProtectionManager();

                    if (!protectionManager.hasProtection(playerUniqueId)) {
                        player.sendMessage(Component.text("You don't have spawn protection active.", NamedTextColor.RED));
                        return true;
                    }

                    protectionManager.endProtection(playerUniqueId);
                    player.sendMessage(Component.text("Your spawn protection has been disabled.", NamedTextColor.GREEN));
                    return true;
                }else if (args[0].equalsIgnoreCase("confirm")) {
                    UUID playerUniqueId = Bukkit.getPlayerUniqueId(args[1]);
                    ProtectionListener.pendingConfirmations.put(player.getUniqueId(), playerUniqueId);

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            UUID confirmedVictim = ProtectionListener.pendingConfirmations.get(player.getUniqueId());
                            if (confirmedVictim != null && confirmedVictim.equals(playerUniqueId)) {
                                ProtectionListener.pendingConfirmations.remove(player.getUniqueId());
                            }
                        }
                    }.runTaskLater(SpawnProtection.getPlugin(), 10 * 60 * 20);

                    player.sendMessage(Component.text("You have chosen to attack this player for 10 min. Be careful!", NamedTextColor.YELLOW));
                    return true;
                }else if (args[0].equalsIgnoreCase("add")) {
                    if (!player.isOp())return true;
                    ProtectionManager protectionManager = SpawnProtection.getProtectionManager();

                    Player target = Bukkit.getPlayer(args[1]);
                    if (target != null) {
                        protectionManager.startProtection(target,false);
                    }else {
                        player.sendMessage(Component.text("This player isn't online").color(NamedTextColor.RED));
                    }
                    return true;
                }else if (args[0].equalsIgnoreCase("clear")) {
                    if (!player.isOp())return true;
                    ProtectionManager protectionManager = SpawnProtection.getProtectionManager();

                    if (args[1].equalsIgnoreCase("@a")) {
                        protectionManager.emptyProtection();
                        player.sendMessage(Component.text("Cleared spawn protection for everyone").color(NamedTextColor.GREEN));
                        return true;
                    }

                    Player target = Bukkit.getPlayer(args[1]);
                    if (target != null) {
                        protectionManager.endProtection(target);
                    }else {
                        UUID t = Bukkit.getPlayerUniqueId(args[1]);
                        protectionManager.endProtection(t);
                    }

                    player.sendMessage(Component.text("Cleared spawn protection from " + args[1]).color(NamedTextColor.GREEN));
                    return true;
                }
            }else if (args.length == 3) {
                if (args[0].equalsIgnoreCase("add")) {
                    if (!player.isOp())return true;
                    ProtectionManager protectionManager = SpawnProtection.getProtectionManager();

                    Player target = Bukkit.getPlayer(args[1]);
                    if (target != null) {
                        protectionManager.grantProtection(target, Integer.parseInt(args[2]));

                        player.sendMessage(Component.text("Added " + args[2] + " sek spawn protection time").color(NamedTextColor.GREEN));
                    }else {
                        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[1]);
                        protectionManager.grantOfflineProtection(offlinePlayer, Integer.parseInt(args[2]));

                        player.sendMessage(Component.text("Added " + args[2] + " sek spawn protection time").color(NamedTextColor.GREEN));
                    }
                    return true;
                }
            }
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return Collections.emptyList();

        if (command.getName().equalsIgnoreCase("spawnprotection")) {
            if (args.length == 1) {
                return Arrays.asList("end", "confirm", "add", "clear", "claim");
            } else if (args.length == 2) {
                if (args[0].equalsIgnoreCase("end") || args[0].equalsIgnoreCase("confirm") || args[0].equalsIgnoreCase("add")
                        || args[0].equalsIgnoreCase("clear")) {
                    return Arrays.stream(Bukkit.getOfflinePlayers())
                            .map(OfflinePlayer::getName)
                            .filter(Objects::nonNull)
                            .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                            .toList();

                }
            }else if (args.length == 3) {
                if (args[0].equalsIgnoreCase("add")) {
                    return List.of("sek");
                }
            }
        }

        return Collections.emptyList();
    }
}