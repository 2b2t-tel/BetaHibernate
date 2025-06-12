package net.utils.betahibernate;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.Properties;

public class BetaHibernate extends JavaPlugin {

    private int chunkRange = 5;
    private long cleanupIntervalTicks = 100;
    private boolean unloadChunks = true;
    private boolean removeMonsters = true;
    private boolean removeAnimals = true;
    private boolean removeItems = true;

    private final File configFile = new File(getDataFolder(), "config.yml");

    @Override
    public void onEnable() {
        loadSettings();

        getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            public void run() {
                hibernateWorlds();
            }
        }, cleanupIntervalTicks, cleanupIntervalTicks);

        System.out.println("[BetaHibernate] Enabled with interval " + (cleanupIntervalTicks / 20) + "s");
    }

    @Override
    public void onDisable() {
        System.out.println("[BetaHibernate] Disabled.");
    }

    private void loadSettings() {
        try {
            // Ensure plugin data folder exists
            File folder = getDataFolder();
            if (!folder.exists()) {
                if (folder.mkdirs()) {
                    System.out.println("[BetaHibernate] Created plugin data folder.");
                } else {
                    System.out.println("[BetaHibernate] Failed to create data folder!");
                }
            }

            // Create default config if missing
            if (!configFile.exists()) {
                Properties defaultProps = new Properties();
                defaultProps.setProperty("chunk-range", "5");
                defaultProps.setProperty("cleanup-interval-seconds", "5");
                defaultProps.setProperty("unload-chunks", "true");
                defaultProps.setProperty("remove-monsters", "true");
                defaultProps.setProperty("remove-animals", "true");
                defaultProps.setProperty("remove-items", "true");

                FileOutputStream out = new FileOutputStream(configFile);
                defaultProps.store(out, "BetaHibernate Configuration");
                out.flush();
                out.close();
                System.out.println("[BetaHibernate] Created default config.yml");
            }

            // Load config values
            Properties props = new Properties();
            FileInputStream in = new FileInputStream(configFile);
            props.load(in);
            in.close();

            chunkRange = Integer.parseInt(props.getProperty("chunk-range", "5"));
            cleanupIntervalTicks = Integer.parseInt(props.getProperty("cleanup-interval-seconds", "5")) * 20L;
            unloadChunks = Boolean.parseBoolean(props.getProperty("unload-chunks", "true"));
            removeMonsters = Boolean.parseBoolean(props.getProperty("remove-monsters", "true"));
            removeAnimals = Boolean.parseBoolean(props.getProperty("remove-animals", "true"));
            removeItems = Boolean.parseBoolean(props.getProperty("remove-items", "true"));

            System.out.println("[BetaHibernate] Settings loaded:");
            System.out.println("  chunkRange = " + chunkRange);
            System.out.println("  interval = " + cleanupIntervalTicks + " ticks");
            System.out.println("  unloadChunks = " + unloadChunks);
            System.out.println("  removeMonsters = " + removeMonsters);
            System.out.println("  removeAnimals = " + removeAnimals);
            System.out.println("  removeItems = " + removeItems);

        } catch (Exception e) {
            System.out.println("[BetaHibernate] Error loading config: " + e.getMessage());
        }
    }


    private void hibernateWorlds() {
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                if (isChunkFarFromPlayers(chunk)) {
                    removeEntitiesInChunk(chunk);
                    if (unloadChunks) {
                        world.unloadChunk(chunk.getX(), chunk.getZ(), false, false);
                        System.out.println("[BetaHibernate] Unloaded chunk (" + chunk.getX() + "," + chunk.getZ() + ") in world '" + world.getName() + "'");
                    }
                }
            }
        }
    }

    private boolean isChunkFarFromPlayers(Chunk chunk) {
        for (Player player : chunk.getWorld().getPlayers()) {
            int px = player.getLocation().getBlockX() >> 4;
            int pz = player.getLocation().getBlockZ() >> 4;

            int dx = Math.abs(px - chunk.getX());
            int dz = Math.abs(pz - chunk.getZ());
            if (dx <= chunkRange && dz <= chunkRange) return false;
        }
        return true;
    }

    private void removeEntitiesInChunk(Chunk chunk) {
        Entity[] entities = chunk.getEntities();
        int removed = 0;

        for (Entity entity : entities) {
            if ((removeMonsters && entity instanceof Monster)
                    || (removeAnimals && entity instanceof Animals)
                    || (removeItems && entity instanceof Item)) {
                entity.remove();
                removed++;
            }
        }

        if (removed > 0) {
            System.out.println("[BetaHibernate] Removed " + removed + " entities in chunk (" + chunk.getX() + "," + chunk.getZ() + ")");
        }
    }

    @Override
    public boolean onCommand(org.bukkit.command.CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("betahibernate")) {
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                loadSettings();
                sender.sendMessage("[BetaHibernate] Configuration reloaded.");
                return true;
            } else {
                sender.sendMessage("Usage: /betahibernate reload");
                return true;
            }
        }
        return false;
    }
}
