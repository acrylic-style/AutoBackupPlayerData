package xyz.acrylicstyle.backup.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PlayerPreDeathEvent extends Event {
    private static final HandlerList HANDLER_LIST = new HandlerList();
    private final Player player;

    public PlayerPreDeathEvent(@NotNull Player player) {
        this.player = player;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    public @NotNull Player getPlayer() {
        return player;
    }
}
