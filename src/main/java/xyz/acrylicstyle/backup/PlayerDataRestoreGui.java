package xyz.acrylicstyle.backup;

import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class PlayerDataRestoreGui implements InventoryHolder {
    private final UUID uuid;
    private static final ItemStack bigBlack;
    private final List<Inventory> inventories = new ArrayList<>();
    private final Map<Integer, Map<Integer, File>> fi = new HashMap<>();

    static {
        bigBlack = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = bigBlack.getItemMeta();
        assert glassMeta != null;
        glassMeta.setDisplayName(" ");
        bigBlack.setItemMeta(glassMeta);
    }

    public PlayerDataRestoreGui(UUID uuid) {
        this.uuid = uuid;
        buildGui();
    }

    private void buildGui() {
        inventories.clear();
        fi.clear();
        AtomicInteger page = new AtomicInteger(-1);
        AtomicInteger index = new AtomicInteger();
        File folder = new File("./backupplayerdata");
        AtomicReference<Inventory> inventory = new AtomicReference<>(setItems(Bukkit.createInventory(this, 54, "プレイヤーデータ復元 - Page " + page.incrementAndGet())));
        inventory.get().setItem(45, bigBlack);
        if (folder.listFiles() != null) {
            List<File> files = Arrays.asList(Objects.requireNonNull(folder.listFiles()));
            files.sort(Comparator.comparingLong(File::lastModified));
            files.sort(Comparator.reverseOrder());
            files.forEach(file -> {
                if (index.get() > 44) {
                    inventories.add(inventory.get());
                    inventory.set(setItems(Bukkit.createInventory(this, 54, "プレイヤーデータ復元 - Page " + page.incrementAndGet())));
                    index.set(0);
                }
                File f = new File(file.getAbsolutePath() + "/");
                if (new File(f.getAbsolutePath() + "/" + uuid.toString() + ".dat").exists()) {
                    if (!fi.containsKey(page.get())) fi.put(page.get(), new HashMap<>());
                    fi.get(page.get()).put(index.get(), f);
                    inventory.get().setItem(index.getAndIncrement(), getItemStack(file));
                }
            });
        }
        inventory.get().setItem(53, bigBlack);
        inventories.add(inventory.get());
    }

    private ItemStack getItemStack(File file) {
        ItemStack item = new ItemStack(Material.CAKE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + AutoBackupPlayerData.timestampToDate(file.lastModified()));
        item.setItemMeta(meta);
        return item;
    }

    private Inventory setItems(Inventory inventory) {
        inventory.setItem(45, arrowItem("← 戻る"));
        inventory.setItem(46, bigBlack);
        inventory.setItem(47, bigBlack);
        inventory.setItem(48, bigBlack);
        inventory.setItem(49, arrowItem("データを更新する"));
        inventory.setItem(50, bigBlack);
        inventory.setItem(51, bigBlack);
        inventory.setItem(52, bigBlack);
        inventory.setItem(53, arrowItem("次へ →"));
        return inventory;
    }

    private ItemStack arrowItem(String name) {
        ItemStack arrow = new ItemStack(Material.ARROW);
        ItemMeta meta = arrow.getItemMeta();
        assert meta != null;
        meta.setDisplayName(ChatColor.GREEN + name);
        arrow.setItemMeta(meta);
        return arrow;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventories.get(0);
    }

    private int page = 0;

    public static class EventListener implements Listener {
        @EventHandler
        public void onInventoryClick(InventoryClickEvent e) {
            if (!(e.getInventory().getHolder() instanceof PlayerDataRestoreGui)) return;
            e.setCancelled(true);
            if (e.getClick() == ClickType.SHIFT_LEFT || e.getClick() == ClickType.SHIFT_RIGHT) {
                return;
            }
            if (e.getClickedInventory() == null || !(e.getClickedInventory().getHolder() instanceof PlayerDataRestoreGui)) {
                return;
            }
            PlayerDataRestoreGui gui = (PlayerDataRestoreGui) e.getInventory().getHolder();
            if (e.getSlot() == 45) {
                if (gui.page - 1 < 0) {
                    e.setCancelled(true);
                    return;
                }
                e.getWhoClicked().openInventory(gui.inventories.get(--gui.page));
            } else if (e.getSlot() == 49) {
                e.getWhoClicked().closeInventory();
                gui.page = 1;
                gui.buildGui();
                e.getWhoClicked().openInventory(gui.getInventory());
            } else if (e.getSlot() == 53) {
                if (gui.page + 1 >= gui.inventories.size()) {
                    e.setCancelled(true);
                    return;
                }
                e.getWhoClicked().openInventory(gui.inventories.get(++gui.page));
            }
            if (e.getSlot() >= 45) return;
            File f = gui.fi.get(gui.page).get(e.getSlot());
            File dest = new File("./world/playerdata/" + gui.uuid.toString() + ".dat");
            try {
                FileUtils.copyFile(new File(f.getAbsolutePath() + "/" + gui.uuid + ".dat"), dest);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            Player player = Bukkit.getPlayer(gui.uuid);
            if (player != null) player.loadData();
            e.getWhoClicked().sendMessage(ChatColor.GREEN + Bukkit.getOfflinePlayer(gui.uuid).getName() + "のデータを復元しました。");
            e.getWhoClicked().sendMessage(ChatColor.GREEN + "復元したバックアップ: " + AutoBackupPlayerData.timestampToDate(f.lastModified()));
            ((Player) e.getWhoClicked()).playSound(e.getWhoClicked().getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 100F, 2F);
        }

        @EventHandler
        public void onInventoryDrag(InventoryDragEvent e) {
            if (e.getInventory().getHolder() != this) return;
            e.setCancelled(true);
        }

        @EventHandler
        public void onInventoryClose(InventoryCloseEvent e) {
            if (!(e.getInventory().getHolder() instanceof PlayerDataRestoreGui)) return;
            PlayerDataRestoreGui gui = (PlayerDataRestoreGui) e.getInventory().getHolder();
            gui.page = 0;
        }
    }
}
