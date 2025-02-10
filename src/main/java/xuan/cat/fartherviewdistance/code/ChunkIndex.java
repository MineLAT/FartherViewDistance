package xuan.cat.fartherviewdistance.code;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import xuan.cat.fartherviewdistance.api.branch.BranchMinecraft;
import xuan.cat.fartherviewdistance.api.branch.BranchPacket;
import xuan.cat.fartherviewdistance.code.branch.v121.Branch_121_Minecraft;
import xuan.cat.fartherviewdistance.code.branch.v121.Branch_121_Packet;
import xuan.cat.fartherviewdistance.code.command.Command;
import xuan.cat.fartherviewdistance.code.command.CommandSuggest;
import xuan.cat.fartherviewdistance.code.data.ConfigData;
import xuan.cat.fartherviewdistance.code.data.viewmap.ViewShape;

public final class ChunkIndex extends JavaPlugin {

    // private static ProtocolManager protocolManager;
    private static Plugin plugin;
    private static ChunkServer chunkServer;
    private static ConfigData configData;
    private static BranchPacket branchPacket;
    private static BranchMinecraft branchMinecraft;

    @Override
    public void onEnable() {
        // protocolManager = ProtocolLibrary.getProtocolManager();

        ((ChunkIndex) (ChunkIndex.plugin = (Plugin) this)).saveDefaultConfig();
        ChunkIndex.configData = new ConfigData(this, this.getConfig());

        // 1.21
        ChunkIndex.branchPacket = new Branch_121_Packet();
        ChunkIndex.branchMinecraft = new Branch_121_Minecraft();
        ChunkIndex.chunkServer = new ChunkServer(ChunkIndex.configData, (Plugin) this, ViewShape.ROUND, ChunkIndex.branchMinecraft,
                ChunkIndex.branchPacket);

        // 初始化一些資料
        for (final Player player : Bukkit.getOnlinePlayers()) {
            ChunkIndex.chunkServer.initView(player);
        }
        for (final World world : Bukkit.getWorlds()) {
            ChunkIndex.chunkServer.initWorld(world);
        }

        Bukkit.getPluginManager()
                .registerEvents(new ChunkEvent(ChunkIndex.chunkServer, ChunkIndex.branchPacket, ChunkIndex.branchMinecraft), (Plugin) this);
        // protocolManager.addPacketListener(new ChunkPacketEvent(plugin, chunkServer));

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            ChunkPlaceholder.registerPlaceholder();
        }

        // 指令
        final PluginCommand command = this.getCommand("viewdistance");
        if (command != null) {
            command.setExecutor((CommandExecutor) new Command(ChunkIndex.chunkServer, ChunkIndex.configData));
            command.setTabCompleter((TabCompleter) new CommandSuggest(ChunkIndex.chunkServer, ChunkIndex.configData));
        }
    }

    @Override
    public void onDisable() {
        // ChunkPlaceholder.unregisterPlaceholder();
        if (ChunkIndex.chunkServer != null)
            ChunkIndex.chunkServer.close();
    }

    public ChunkIndex() { super(); }

    public static ChunkServer getChunkServer() { return ChunkIndex.chunkServer; }

    public static ConfigData getConfigData() { return ChunkIndex.configData; }

    public static Plugin getPlugin() { return ChunkIndex.plugin; }
}
