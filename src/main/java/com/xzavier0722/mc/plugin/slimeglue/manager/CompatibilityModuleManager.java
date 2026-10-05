package com.xzavier0722.mc.plugin.slimeglue.manager;

import com.xzavier0722.mc.plugin.slimeglue.SlimeGlue;
import com.xzavier0722.mc.plugin.slimeglue.api.ACompatibilityModule;
import com.xzavier0722.mc.plugin.slimeglue.api.listener.IListener;
import com.xzavier0722.mc.plugin.slimeglue.api.listener.SubscriptionType;
import com.xzavier0722.mc.plugin.slimeglue.api.protection.IProtectionHandler;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CompatibilityModuleManager {

    private final Map<String, ACompatibilityModule> disabledModules;
    private final Map<String, ACompatibilityModule> enabledModules;

    public CompatibilityModuleManager() {
        // Read from region threads (protection/event checks) while the global
        // thread may load/unload modules, so both maps must be thread-safe.
        disabledModules = new ConcurrentHashMap<>();
        enabledModules = new ConcurrentHashMap<>();
    }

    public void register(ACompatibilityModule module) {
        disabledModules.put(module.getCompatibilityPluginName(), module);
    }

    public void load() {
        disabledModules.values().removeIf(this::load);
    }

    public void load(String pluginName) {
        ACompatibilityModule module = disabledModules.get(pluginName);
        if (module == null) {
            return;
        }

        load(module);
    }

    public void unload(String pluginName) {
        ACompatibilityModule module = enabledModules.remove(pluginName);
        if (module == null) {
            return;
        }

        module.disable();
        disabledModules.put(module.getCompatibilityPluginName(), module);
    }

    public Set<IListener> getListeners(String sfId) {
        Set<IListener> re = new HashSet<>();
        enabledModules.values().forEach(module -> module.getListeners().forEach(l -> {
            if (l.getSubscriptionType() == SubscriptionType.SUBSCRIBE_TYPE_ALL || l.getSubscribedId().contains(sfId)) {
                re.add(l);
            }
        }));
        return re;
    }

    public Set<IProtectionHandler> getProtectionHandlers() {
        Set<IProtectionHandler> re = new HashSet<>();
        enabledModules.values().forEach(module -> re.addAll(module.getProtectionHandlers()));
        return re;
    }

    private boolean load(ACompatibilityModule module) {
        String name = module.getCompatibilityPluginName();
        SlimeGlue.logger().i("Loading compatibility module for " + name);
        Plugin plugin = SlimeGlue.instance().getServer().getPluginManager().getPlugin(name);

        if (plugin == null || !plugin.isEnabled()) {
            SlimeGlue.logger().w("Plugin " + name + " is not installed or enabled, module ignored.");
            return false;
        }

        try {
            module.enable(plugin);
            enabledModules.put(name, module);
            return true;
        } catch (Throwable e) {
            SlimeGlue.logger().e("Exception thrown while loading the compatibility module for " + name);
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Disable every enabled module, releasing resources held by them.
     * Called from {@code onDisable}.
     */
    public void disableAll() {
        enabledModules.values().forEach(module -> {
            try {
                module.disable();
            } catch (Throwable e) {
                SlimeGlue.logger().e("Exception thrown while disabling the compatibility module for "
                        + module.getCompatibilityPluginName());
                e.printStackTrace();
            }
        });
        enabledModules.clear();
    }

}
