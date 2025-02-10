package xuan.cat.fartherviewdistance.core.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import xuan.cat.fartherviewdistance.core.ChunkServer;
import xuan.cat.fartherviewdistance.core.data.ConfigData;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public final class CommandSuggest implements TabCompleter {

    private final ChunkServer chunkServer;
    private final ConfigData configData;

    public CommandSuggest(final ChunkServer chunkServer, final ConfigData configData) {
        this.chunkServer = chunkServer;
        this.configData = configData;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String s, final String[] parameters) {
        if (!sender.hasPermission("command.viewdistance")) {
            return new ArrayList();
        } else {
            final List<String> list = new ArrayList();
            if (parameters.length == 1) {
                list.add("start");
                list.add("stop");
                list.add("reload");
                list.add("report");
                list.add("permissionCheck");
                list.add("debug");
            } else if (parameters.length == 2) {
                final String var6 = parameters[0];
                switch (var6) {
                    case "report":
                        list.add("server");
                        list.add("thread");
                        list.add("world");
                        list.add("player");
                        break;
                    case "permissionCheck":
                        Bukkit.getOnlinePlayers().forEach(player -> list.add(player.getName()));
                        break;
                    case "debug":
                        list.add("view");
                }
            } else if (parameters.length == 3) {
                final String var10 = parameters[0];
                switch (var10) {
                    case "report":
                    case "permissionCheck":
                    default:
                        break;
                    case "debug":
                        final String var8 = parameters[1];
                        byte var9 = -1;
                        switch (var8.hashCode()) {
                            case 3619493:
                                if (var8.equals("view")) {
                                    var9 = 0;
                                }
                            default:
                                switch (var9) {
                                    case 0:
                                        Bukkit.getOnlinePlayers().forEach(player -> list.add(player.getName()));
                                }
                        }
                }
            }

            return list;
        }
    }
}
