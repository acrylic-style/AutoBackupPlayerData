package xyz.acrylicstyle.backup;

import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import xyz.acrylicstyle.backup.commands.PlayerDataCommand;
import xyz.acrylicstyle.backup.event.PlayerPreDeathEvent;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

public class AutoBackupPlayerData extends JavaPlugin implements Listener {
    public static String world = "world";
    public static int interval = 10; // minutes
    public static int keepFiles = 100;

    public static @NotNull AutoBackupPlayerData getInstance() {
        return AutoBackupPlayerData.getPlugin(AutoBackupPlayerData.class);
    }

    public static String timestampToDate(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        String m = Integer.toString(calendar.get(Calendar.MINUTE));
        String s = Integer.toString(calendar.get(Calendar.SECOND));
        return calendar.get(Calendar.YEAR) + "/"
                + (calendar.get(Calendar.MONTH) + 1) + "/"
                + calendar.get(Calendar.DAY_OF_MONTH) + " "
                + calendar.get(Calendar.HOUR_OF_DAY) + ":"
                + (m.length() == 1 ? "0" + m : m) + ":"
                + (s.length() == 1 ? "0" + s : s);
    }

    @Override
    public void onEnable() {
        world = getConfig().getString("world", "world");
        interval = getConfig().getInt("interval", 10);
        keepFiles = getConfig().getInt("keepFiles", 10000);
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(new PlayerDataRestoreGui.EventListener(), AutoBackupPlayerData.getInstance());
        Objects.requireNonNull(getCommand("playerdata")).setExecutor(new PlayerDataCommand());
        new BukkitRunnable() {
            @Override
            public void run() {
                getLogger().info("Saving player data...");
                File folder = new File("./backupplayerdata");
                if (folder.listFiles() != null) {
                    List<File> files = Arrays.asList(Objects.requireNonNull(folder.listFiles()));
                    files.sort(Comparator.comparingLong(File::lastModified));
                    files.sort(Comparator.reverseOrder());
                    for (int i = 0; i < files.size(); i++) {
                        File file = files.get(i);
                        if (i > keepFiles) {
                            getLogger().info("Deleting " + file.getAbsolutePath());
                            try {
                                FileUtils.deleteDirectory(file);
                            } catch (IOException ignore) {}
                        }
                    }
                }
                saveNow();
                getLogger().info("Saved player data for " + Bukkit.getOnlinePlayers().size() + " players");
            }
        }.runTaskTimerAsynchronously(this, (long) interval * 60 * 20, (long) interval * 60 * 20);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @EventHandler
    public void onPlayerPreDeath(PlayerPreDeathEvent e) {
        new Thread(() -> {
            getSLF4JLogger().info("Saving {} data before they die", e.getPlayer().getName());
            File folder = new File("./backupplayerdata/deaths");
            folder.mkdirs();
            File src = new File("./" + world + "/playerdata/" + e.getPlayer().getUniqueId() + ".dat");
            File dest = new File("./backupplayerdata/deaths/" + new Date().getTime() + "/" + e.getPlayer().getUniqueId() + ".dat");
            dest.mkdirs();
            dest.delete();
            try {
                FileUtils.copyFile(src, dest);
            } catch (IOException ignore) {}
            getLogger().info("Saved data for " + e.getPlayer().getName());
        }).start();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private Consumer<UUID> saveConsumer(long time) {
        return uuid -> {
            if (Bukkit.getPlayer(uuid) != null) Objects.requireNonNull(Bukkit.getPlayer(uuid)).saveData();
            File src = new File("./" + world + "/playerdata/" + uuid + ".dat");
            File dest = new File("./backupplayerdata/" + time + "/" + uuid + ".dat");
            dest.mkdirs();
            dest.delete();
            try {
                FileUtils.copyFile(src, dest);
            } catch (IOException ignore) {}
        };
    }

    public void saveNow() {
        long time = new Date().getTime();
        Bukkit.getOnlinePlayers().stream().map(Player::getUniqueId).forEach(saveConsumer(time));
    }

    @Override
    public void onDisable() {
        getLogger().info("Saving player data...");
        saveNow();
        getLogger().info("Saved player data for " + Bukkit.getOnlinePlayers().size() + " players, going to shutdown.");
    }
}
