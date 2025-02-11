package xuan.cat.fartherviewdistance.api.server;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Player;

public interface ServerWorld {
    ServerChunk getChunkFromDisk(World world, int chunkX, int chunkZ);

    ServerChunk getChunkFromMemoryCache(World world, int chunkX, int chunkZ);

    ServerChunk getChunkOrLoad(World world, Chunk chunk);

    void injectPlayer(Player player);
}
