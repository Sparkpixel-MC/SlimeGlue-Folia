package com.xzavier0722.mc.plugin.slimeglue.scheduler;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Scheduler wrapper routing between the Paper (single main thread) path and
 * the Folia (regionized) path. All scheduling entry points of the plugin must
 * go through this class; direct BukkitScheduler calls in business code are
 * prohibited.
 */
public final class SchedulerUtil {

    private static final boolean FOLIA = detectFolia();

    private SchedulerUtil() {
    }

    /**
     * One-time Folia detection, cached for the whole plugin lifetime.
     */
    private static boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static boolean isFolia() {
        return FOLIA;
    }

    /**
     * Handle of a scheduled repeating task, cancellable from any thread.
     */
    public interface TaskHandle {
        void cancel();
    }

    /**
     * Run a task on the global region (Folia) / main thread (Paper).
     */
    public static TaskHandle runGlobal(Plugin plugin, Runnable task) {
        if (FOLIA) {
            ScheduledTask t = Bukkit.getGlobalRegionScheduler().run(plugin, st -> task.run());
            return t::cancel;
        }
        BukkitTask t = Bukkit.getScheduler().runTask(plugin, task);
        return t::cancel;
    }

    /**
     * Run a task on the global region after at least 1 tick.
     */
    public static TaskHandle runGlobalDelayed(Plugin plugin, Runnable task, long delayTicks) {
        delayTicks = Math.max(1, delayTicks);
        if (FOLIA) {
            ScheduledTask t = Bukkit.getGlobalRegionScheduler().runDelayed(plugin, st -> task.run(), delayTicks);
            return t::cancel;
        }
        BukkitTask t = Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        return t::cancel;
    }

    /**
     * Run a task on the global region periodically. Delay and period are
     * forced to be at least 1 tick.
     */
    public static TaskHandle runGlobalTimer(Plugin plugin, Runnable task, long delayTicks, long periodTicks) {
        delayTicks = Math.max(1, delayTicks);
        periodTicks = Math.max(1, periodTicks);
        if (FOLIA) {
            ScheduledTask t = Bukkit.getGlobalRegionScheduler()
                    .runAtFixedRate(plugin, st -> task.run(), delayTicks, periodTicks);
            return t::cancel;
        }
        BukkitTask t = Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
        return t::cancel;
    }

    /**
     * Run a task in the context owning the given entity. Note: on Folia this
     * always incurs at least 1 tick of delay.
     */
    public static void runAtEntity(Plugin plugin, Entity entity, Runnable task) {
        if (FOLIA) {
            entity.getScheduler().run(plugin, st -> task.run(), null);
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    /**
     * Run a task in the context owning the given entity after at least 1 tick.
     */
    public static void runAtEntityDelayed(Plugin plugin, Entity entity, Runnable task, long delayTicks) {
        delayTicks = Math.max(1, delayTicks);
        if (FOLIA) {
            entity.getScheduler().runDelayed(plugin, st -> task.run(), null, delayTicks);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        }
    }

    /**
     * Run a task in the context owning the given location's region. Note: on
     * Folia this always incurs at least 1 tick of delay.
     */
    public static void runAtRegion(Plugin plugin, Location loc, Runnable task) {
        if (FOLIA) {
            Bukkit.getRegionScheduler().run(plugin, loc, st -> task.run());
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    /**
     * Run a task in the context owning the given location's region after at
     * least 1 tick.
     */
    public static void runAtRegionDelayed(Plugin plugin, Location loc, Runnable task, long delayTicks) {
        delayTicks = Math.max(1, delayTicks);
        if (FOLIA) {
            Bukkit.getRegionScheduler().runDelayed(plugin, loc, st -> task.run(), delayTicks);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        }
    }

    /**
     * Run a task asynchronously. The task must not touch region-owned live
     * state (Player, Entity, World, Block, ...).
     */
    public static void runAsync(Plugin plugin, Runnable task) {
        if (FOLIA) {
            Bukkit.getAsyncScheduler().runNow(plugin, st -> task.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        }
    }

    /**
     * Teleport asynchronously; the returned future completes in the target
     * region's context. Supported on both Paper and Folia.
     */
    public static void teleport(Entity entity, Location loc) {
        entity.teleportAsync(loc);
    }

}
