package de.nikey.spawnProtection.Managers;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Grants configurable potion effects to a player when they die,
 * e.g. as a temporary debuff/buff after respawning.
 */
public class DeathEffectsManager {

    private final Plugin plugin;

    public DeathEffectsManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("death-effects.enabled", false);
    }

    public void applyDeathEffects(Player player) {
        if (!isEnabled()) return;

        ConfigurationSection effectsSection = plugin.getConfig().getConfigurationSection("death-effects.effects");
        if (effectsSection == null) return;

        for (String key : effectsSection.getKeys(false)) {
            ConfigurationSection effectSection = effectsSection.getConfigurationSection(key);
            if (effectSection == null) continue;

            if (!effectSection.getBoolean("enabled", true)) continue;

            String typeName = effectSection.getString("type");
            if (typeName == null) {
                plugin.getLogger().warning("death-effects.effects." + key + " is missing a 'type', skipping.");
                continue;
            }

            PotionEffectType type = PotionEffectType.getByName(typeName.toUpperCase());
            if (type == null) {
                plugin.getLogger().warning("Unknown potion effect type '" + typeName + "' in death-effects.effects." + key);
                continue;
            }

            int durationSeconds = effectSection.getInt("duration-seconds", 30);
            int amplifier = effectSection.getInt("amplifier", 0);
            boolean ambient = effectSection.getBoolean("ambient", true);
            boolean particles = effectSection.getBoolean("particles", true);
            boolean icon = effectSection.getBoolean("icon", true);

            player.addPotionEffect(new PotionEffect(
                    type,
                    durationSeconds * 20,
                    amplifier,
                    ambient,
                    particles,
                    icon
            ));
        }
    }
}
