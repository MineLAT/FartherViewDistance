package xuan.cat.fartherviewdistance;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import xuan.cat.fartherviewdistance.api.server.ServerPacket;
import xuan.cat.fartherviewdistance.api.server.ServerWorld;
import xuan.cat.fartherviewdistance.core.ChunkServer;
import xuan.cat.fartherviewdistance.core.command.Command;
import xuan.cat.fartherviewdistance.core.command.CommandSuggest;
import xuan.cat.fartherviewdistance.core.data.ConfigData;
import xuan.cat.fartherviewdistance.core.data.viewmap.ViewShape;
import xuan.cat.fartherviewdistance.module.hook.ChunkPlaceholder;
import xuan.cat.fartherviewdistance.module.listener.ChunkListener;
import xuan.cat.fartherviewdistance.module.server.MinecraftPacket;
import xuan.cat.fartherviewdistance.module.server.MinecraftWorld;

public final class FVD extends JavaPlugin {

    private static FVD instance;

    public static FVD get() {
        return instance;
    }

    private ConfigData configData;
    private ServerPacket serverPacket;
    private ServerWorld serverWorld;
    private ChunkServer chunkServer;

    private boolean placeholderRegistered;

    @Override
    public void onLoad() {
        instance = this;

        saveDefaultConfig();
        configData = new ConfigData(this, this.getConfig());
    }

    @Override
    public void onEnable() {
        serverPacket = new MinecraftPacket();
        serverWorld = new MinecraftWorld();
        chunkServer = new ChunkServer(configData, this, ViewShape.ROUND, serverWorld, serverPacket);

        // Init current server data
        for (final Player player : Bukkit.getOnlinePlayers()) {
            chunkServer.initView(player);
        }
        for (final World world : Bukkit.getWorlds()) {
            chunkServer.initWorld(world);
        }

        Bukkit.getPluginManager().registerEvents(new ChunkListener(chunkServer, serverPacket, serverWorld), this);

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            ChunkPlaceholder.registerPlaceholder();
            placeholderRegistered = true;
        }

        // Register command
        final PluginCommand command = this.getCommand("viewdistance");
        if (command != null) {
            command.setExecutor(new Command(chunkServer, configData));
            command.setTabCompleter(new CommandSuggest(chunkServer, configData));
        }
    }

    @Override
    public void onDisable() {
        if (placeholderRegistered) {
            ChunkPlaceholder.unregisterPlaceholder();
        }
        if (chunkServer != null) {
            chunkServer.close();
        }
    }

    public ConfigData getConfigData() {
        return configData;
    }

    public ServerPacket getServerPacket() {
        return serverPacket;
    }

    public ServerWorld getServerWorld() {
        return serverWorld;
    }

    public ChunkServer getChunkServer() {
        return chunkServer;
    }
}
