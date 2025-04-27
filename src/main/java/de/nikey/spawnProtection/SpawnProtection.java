package de.nikey.spawnProtection;

import de.nikey.spawnProtection.Commands.AttackConfirm;
import de.nikey.spawnProtection.Commands.EndProtection;
import de.nikey.spawnProtection.Listener.ProtectionListener;
import de.nikey.spawnProtection.Listener.ProximityWatcher;
import de.nikey.spawnProtection.Managers.ProtectionManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class SpawnProtection extends JavaPlugin {
    private static SpawnProtection plugin;
    private ProtectionManager protectionManager;

    @Override
    public void onEnable() {
        plugin = this;
        saveDefaultConfig();
        protectionManager = new ProtectionManager(this);
        getCommand("confirmattack").setExecutor(new AttackConfirm(protectionManager));
        getCommand("endprotection").setExecutor(new EndProtection(protectionManager));
        new ProtectionListener(protectionManager);
        new ProximityWatcher(protectionManager);
    }

    public ProtectionManager getProtectionManager() {
        return protectionManager;
    }

    public static SpawnProtection getPlugin() {
        return plugin;
    }
}
