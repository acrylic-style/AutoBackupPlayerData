package xyz.acrylicstyle.backup;

import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import util.CollectionList;
import util.ICollectionList;
import xyz.acrylicstyle.shared.BaseMojangAPI;
import xyz.acrylicstyle.tomeito_api.TomeitoAPI;
import xyz.acrylicstyle.tomeito_api.command.PlayerCommandExecutor;
import xyz.acrylicstyle.tomeito_api.events.player.PlayerPreDeathEvent;
import xyz.acrylicstyle.tomeito_api.gui.PerPlayerInventory;
import xyz.acrylicstyle.tomeito_api.providers.ConfigProvider;
import xyz.acrylicstyle.tomeito_api.utils.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

public class AutoBackupPlayerData extends JavaPlugin implements Listener {
    public static ConfigProvider config = null;
    public static int period = 10; // minutes
    public static int keepFiles = 100;
    public static AutoBackupPlayerData instance = null;

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

    public static final PerPlayerInventory<PlayerDataRestoreGui> gui = new PerPlayerInventory<>(PlayerDataRestoreGui::new);

    @Override
    public void onEnable() {
        instance = this;
        config = new ConfigProvider("./plugins/AutoBackupPlayerData/config.yml");
        period = config.getInt("delayMinute", 10);
        keepFiles = config.getInt("keepFiles", 100);
        Bukkit.getPluginManager().registerEvents(this, this);
        TomeitoAPI.registerCommand("playerdata", new PlayerCommandExecutor() {
            @Override
            public void onCommand(Player player, String[] args) {
                UUID uuid;
                try {
                    uuid = BaseMojangAPI.getUniqueId(args[0]);
                    if (uuid == null) throw new NullPointerException();
                } catch (RuntimeException ex) {
                    player.sendMessage(ChatColor.RED + "プレイヤーが見つかりません。");
                    return;
                }
                player.openInventory(gui.get(uuid).getInventory());
            }
        });
        new BukkitRunnable() {
            @Override
            public void run() {
                Log.info("Saving player data...");
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
                Log.info("Saved player data for " + Bukkit.getOnlinePlayers().size() + " players");
            }
        }.runTaskTimerAsynchronously(this, period * 60 * 20, period * 60 * 20);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @EventHandler
    public void onPlayerPreDeath(PlayerPreDeathEvent e) {
        new Thread(() -> {
            Log.info("Saving " + e.getPlayer().getName() + "'s data before they die");
            File folder = new File("./backupplayerdata/deaths");
            folder.mkdirs();
            File src = new File("./world/playerdata/" + e.getPlayer().getUniqueId().toString() + ".dat");
            File dest = new File("./backupplayerdata/deaths/" + new Date().getTime() + "/" + e.getPlayer().getUniqueId().toString() + ".dat");
            dest.mkdirs();
            dest.delete();
            try {
                FileUtils.copyFile(src, dest);
            } catch (IOException ignore) {}
            Log.info("Saved data for " + e.getPlayer().getName());
        }).start();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private Consumer<UUID> saveConsumer(long time) {
        return uuid -> {
            if (Bukkit.getPlayer(uuid) != null) Objects.requireNonNull(Bukkit.getPlayer(uuid)).saveData();
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
        Log.info("Saving player data...");
        long time = new Date().getTime();
        ICollectionList.asList(new ArrayList<>(Bukkit.getOnlinePlayers())).map(Player::getUniqueId).forEach(saveConsumer(time));
        Log.info("Saved player data for " + Bukkit.getOnlinePlayers().size() + " players, going to shutdown.");
    }
}
