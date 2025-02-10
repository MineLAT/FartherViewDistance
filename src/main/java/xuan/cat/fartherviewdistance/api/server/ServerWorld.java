package xuan.cat.fartherviewdistance.api.server;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.io.IOException;

public interface ServerWorld {
    ServerNBT getChunkNBTFromDisk(World world, int chunkX, int chunkZ) throws IOException;

    ServerChunk getChunkFromMemoryCache(World world, int chunkX, int chunkZ);

    ServerChunk fromChunk(World world, int chunkX, int chunkZ, ServerNBT nbt, boolean integralHeightmap);

    ServerChunkLight fromLight(World world, ServerNBT nbt);

    ServerChunkLight fromLight(World world);

    ServerChunk fromChunk(World world, Chunk chunk);

    ServerChunk.Status fromStatus(ServerNBT nbt);

    void injectPlayer(Player player);
}
