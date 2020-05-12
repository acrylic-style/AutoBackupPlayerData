package xyz.acrylicstyle.backup;

import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import util.CollectionList;
import util.ICollectionList;
import xyz.acrylicstyle.tomeito_api.providers.ConfigProvider;
import xyz.acrylicstyle.tomeito_api.utils.Log;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class AutoBackupPlayerData extends JavaPlugin implements Listener {
    public static ConfigProvider config = null;
    public static Set<UUID> uuids = new HashSet<>();
    public static int period = 10; // minutes
    public static int keepFiles = 100;

    @Override
    public void onEnable() {
        config = new ConfigProvider("./plugins/AutoBackupPlayerData/config.yml");
        period = config.getInt("delayMinute", 10);
        keepFiles = config.getInt("keepFiles", 100);
        Bukkit.getPluginManager().registerEvents(this, this);
        new BukkitRunnable() {
            @SuppressWarnings("ResultOfMethodCallIgnored")
            @Override
            public void run() {
                Log.info("プレイヤーデータをバックアップしています...");
                File folder = new File("./backupplayerdata");
                if (folder.listFiles() != null) {
                    CollectionList<File> files = ICollectionList.asList(folder.listFiles());
                    files.sort(Comparator.comparingLong(File::lastModified));
                    files.sort(Comparator.reverseOrder());
                    files.foreach((file, i) -> {
                        if (i > keepFiles) {
                            Log.info("Deleting " + file.getAbsolutePath());
                            try {
                                FileUtils.deleteDirectory(file);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
                long time = new Date().getTime();
                uuids.forEach(uuid -> {
                    File src = new File("./world/playerdata/" + uuid.toString() + ".dat");
                    File dest = new File("./backupplayerdata/" + time + "/" + uuid.toString() + ".dat");
                    dest.mkdirs();
                    dest.delete();
                    try {
                        FileUtils.copyFile(src, dest);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                uuids.clear();
                Log.info("プレイヤーデータのバックアップが完了しました。");
            }
        }.runTaskTimerAsynchronously(this, period * 60 * 20, period * 60 * 20);
        Bukkit.getOnlinePlayers().forEach(p -> uuids.add(p.getUniqueId()));
    }

    @Override
    public void onDisable() {
        Log.info("プレイヤーデータをバックアップしています...");

        Log.info("バックアップが完了しました。");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        uuids.add(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        uuids.add(e.getPlayer().getUniqueId());
    }
}
