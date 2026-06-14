package mc.mkay.scythe;

import org.bukkit.plugin.java.JavaPlugin;

public class ScythePlugin extends JavaPlugin {

    private static ScythePlugin instance;

    @Override
    public void onEnable() {
        instance = this;
        ScytheListener listener = new ScytheListener(this);
        getServer().getPluginManager().registerEvents(listener, this);
        getCommand("givescythe").setExecutor(new GiveScytheCommand(this));
        getCommand("scythekills").setExecutor(new ScytheKillsCommand(listener));
        getLogger().info("[Scythe] Cherry Blossom Relic awakened for mkaymc.");
    }

    @Override
    public void onDisable() {
        getLogger().info("[Scythe] Relic sealed.");
    }

    public static ScythePlugin getInstance() { return instance; }
}
