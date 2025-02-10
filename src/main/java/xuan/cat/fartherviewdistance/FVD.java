package xuan.cat.fartherviewdistance;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
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

    // private static ProtocolManager protocolManager;
    private static Plugin plugin;
    private static ChunkServer chunkServer;
    private static ConfigData configData;
    private static ServerPacket serverPacket;
    private static ServerWorld serverWorld;

    @Override
    public void onLoad() {
        plugin = this;
    }

    @Override
    public void onEnable() {
        // protocolManager = ProtocolLibrary.getProtocolManager();

        saveDefaultConfig();
        FVD.configData = new ConfigData(this, this.getConfig());

        // 1.21
        FVD.serverPacket = new MinecraftPacket();
        FVD.serverWorld = new MinecraftWorld();
        FVD.chunkServer = new ChunkServer(FVD.configData, this, ViewShape.ROUND, FVD.serverWorld,
                FVD.serverPacket);

        // 初始化一些資料
        for (final Player player : Bukkit.getOnlinePlayers()) {
            FVD.chunkServer.initView(player);
        }
        for (final World world : Bukkit.getWorlds()) {
            FVD.chunkServer.initWorld(world);
        }

        Bukkit.getPluginManager()
                .registerEvents(new ChunkListener(FVD.chunkServer, FVD.serverPacket, FVD.serverWorld), (Plugin) this);
        // protocolManager.addPacketListener(new ChunkPacketEvent(plugin, chunkServer));

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            ChunkPlaceholder.registerPlaceholder();
        }

        // 指令
        final PluginCommand command = this.getCommand("viewdistance");
        if (command != null) {
            command.setExecutor(new Command(FVD.chunkServer, FVD.configData));
            command.setTabCompleter(new CommandSuggest(FVD.chunkServer, FVD.configData));
        }
    }

    @Override
    public void onDisable() {
        // ChunkPlaceholder.unregisterPlaceholder();
        if (FVD.chunkServer != null)
            FVD.chunkServer.close();
    }

    public static ChunkServer getChunkServer() {
        return FVD.chunkServer;
    }

    public static ConfigData getConfigData() {
        return FVD.configData;
    }

    public static Plugin getPlugin() {
        return FVD.plugin;
    }
}
