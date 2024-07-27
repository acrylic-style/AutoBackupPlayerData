package xyz.acrylicstyle.backup.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import xyz.acrylicstyle.backup.event.PlayerPreDeathEvent;

public class DamageListener implements Listener {
    private void firePreDeathEvent(double dmg, Entity e) {
        if (e instanceof LivingEntity) {
            LivingEntity entity = (LivingEntity) e;
            if ((entity.getHealth() - dmg) <= 0) {
                if (entity instanceof Player) {
                    PlayerPreDeathEvent event = new PlayerPreDeathEvent((Player) entity);
                    Bukkit.getPluginManager().callEvent(event);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent e) {
        firePreDeathEvent(e.getFinalDamage(), e.getEntity());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        firePreDeathEvent(e.getFinalDamage(), e.getEntity());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
        firePreDeathEvent(e.getFinalDamage(), e.getEntity());
    }
}
