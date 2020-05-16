package xyz.acrylicstyle.backup;

import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import util.CollectionList;
import util.ICollectionList;
import xyz.acrylicstyle.tomeito_api.providers.ConfigProvider;
import xyz.acrylicstyle.tomeito_api.utils.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.UUID;
import java.util.function.Consumer;

public class AutoBackupPlayerData extends JavaPlugin implements Listener {
    public static ConfigProvider config = null;
    public static int period = 10; // minutes
    public static int keepFiles = 100;

    @Override
    public void onEnable() {
        config = new ConfigProvider("./plugins/AutoBackupPlayerData/config.yml");
        period = config.getInt("delayMinute", 10);
        keepFiles = config.getInt("keepFiles", 100);
        Bukkit.getPluginManager().registerEvents(this, this);
        new BukkitRunnable() {
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
                            } catch (IOException ignore) {}
                        }
                    });
                }
                long time = new Date().getTime();
                ICollectionList.asList(new ArrayList<>(Bukkit.getOnlinePlayers())).map(Player::getUniqueId).forEach(saveConsumer(time));
                Log.info("プレイヤーデータのバックアップが完了しました。");
            }
        }.runTaskTimerAsynchronously(this, period * 60 * 20, period * 60 * 20);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private Consumer<UUID> saveConsumer(long time) {
        return uuid -> {
            File src = new File("./world/playerdata/" + uuid.toString() + ".dat");
            File dest = new File("./backupplayerdata/" + time + "/" + uuid.toString() + ".dat");
            dest.mkdirs();
            dest.delete();
            try {
                FileUtils.copyFile(src, dest);
            } catch (IOException ignore) {}
        };
    }

    @Override
    public void onDisable() {
        Log.info("プレイヤーデータをバックアップしています...");
        long time = new Date().getTime();
        ICollectionList.asList(new ArrayList<>(Bukkit.getOnlinePlayers())).map(Player::getUniqueId).forEach(saveConsumer(time));
        Log.info("バックアップが完了しました。");
    }
}
