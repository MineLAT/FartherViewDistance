package xuan.cat.fartherviewdistance.api.server;

import org.bukkit.entity.Player;

import java.util.function.Consumer;

public interface ServerPacket {
    void sendViewDistance(Player player, int viewDistance);

    void sendUnloadChunk(Player player, int chunkX, int chunkZ);

    Consumer<Player> sendChunkAndLight(ServerChunk chunk, ServerChunkLight light, boolean needTile, Consumer<Integer> consumeTraffic);

    void sendKeepAlive(Player player, long id);
}
