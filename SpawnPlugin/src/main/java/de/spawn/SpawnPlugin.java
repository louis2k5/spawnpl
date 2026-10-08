package de.spawn;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SpawnPlugin extends JavaPlugin {

    private final Set<UUID> teleporting = new HashSet<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getLogger().info("SpawnPlugin aktiviert.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(msg("only-players", null));
            return true;
        }

        switch (command.getName().toLowerCase()) {
            case "setspawn" -> {
                Location loc = player.getLocation();
                getConfig().set("spawn.world", loc.getWorld().getName());
                getConfig().set("spawn.x", loc.getX());
                getConfig().set("spawn.y", loc.getY());
                getConfig().set("spawn.z", loc.getZ());
                getConfig().set("spawn.yaw", (double) loc.getYaw());
                getConfig().set("spawn.pitch", (double) loc.getPitch());
                saveConfig();
                player.sendMessage(msg("spawn-set", null));
            }
            case "spawn" -> {
                if (loadSpawn() == null) {
                    player.sendMessage(msg("spawn-not-set", null));
                    return true;
                }
                if (!teleporting.add(player.getUniqueId())) {
                    player.sendMessage(msg("already-teleporting", null));
                    return true;
                }
                startCountdown(player);
            }
        }
        return true;
    }

    private void startCountdown(Player player) {
        final int seconds = Math.max(1, getConfig().getInt("countdown-seconds", 3));

        new BukkitRunnable() {
            int remaining = seconds;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    teleporting.remove(player.getUniqueId());
                    cancel();
                    return;
                }

                if (remaining > 0) {
                    // Anzeige direkt über der Hotbar/Levelanzeige
                    player.sendActionBar(msg("countdown", String.valueOf(remaining)));
                    remaining--;
                    return;
                }

                cancel();
                teleporting.remove(player.getUniqueId());

                Location spawn = loadSpawn();
                if (spawn == null) {
                    player.sendMessage(msg("spawn-not-set", null));
                    return;
                }
                player.teleportAsync(spawn).thenAccept(success -> {
                    if (success) {
                        player.sendMessage(msg("spawn-teleport", null));
                    }
                });
            }
        }.runTaskTimer(this, 0L, 20L);
    }

    private Location loadSpawn() {
        if (!getConfig().contains("spawn.world")) {
            return null;
        }
        World world = Bukkit.getWorld(getConfig().getString("spawn.world", ""));
        if (world == null) {
            return null;
        }
        return new Location(
                world,
                getConfig().getDouble("spawn.x"),
                getConfig().getDouble("spawn.y"),
                getConfig().getDouble("spawn.z"),
                (float) getConfig().getDouble("spawn.yaw"),
                (float) getConfig().getDouble("spawn.pitch")
        );
    }

    private Component msg(String key, String time) {
        String text = getConfig().getString("messages." + key, key);
        if (time != null) {
            text = text.replace("{time}", time);
        }
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }
}
