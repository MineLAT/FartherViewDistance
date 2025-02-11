package xuan.cat.fartherviewdistance.module.server;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import org.bukkit.World;
import xuan.cat.fartherviewdistance.api.server.ServerChunk;

public class FileChunk implements ServerChunk {

    private final ServerLevel level;
    private final MinecraftChunkData data;

    private transient LevelChunk chunk;

    public FileChunk(ServerLevel level, MinecraftChunkData data) {
        this.level = level;
        this.data = data;
    }

    public ServerLevel getLevel() {
        return level;
    }

    public MinecraftChunkData getData() {
        return data;
    }

    public LevelChunk getChunk() {
        if (chunk == null) {
            final ProtoChunk protoChunk = data.read(
                    level,
                    null,
                    new RegionStorageInfo(level.levelStorageAccess.getLevelId(), level.dimension(), "chunk"),
                    data.chunkPos()
            );
            chunk = new LevelChunk(level, protoChunk, chunk -> {});
        }
        return chunk;
    }

    @Override
    public World getWorld() {
        return level.getWorld();
    }

    @Override
    public int getX() {
        return data.chunkPos().x;
    }

    @Override
    public int getZ() {
        return data.chunkPos().z;
    }

    @Override
    public Status getStatus() {
        return MinecraftChunk.getStatus(data.chunkStatus());
    }
}
