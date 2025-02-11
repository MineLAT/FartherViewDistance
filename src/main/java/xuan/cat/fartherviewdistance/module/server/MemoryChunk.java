package xuan.cat.fartherviewdistance.module.server;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.bukkit.World;
import xuan.cat.fartherviewdistance.api.server.ServerChunk;

public final class MemoryChunk implements ServerChunk {

    private final ServerLevel level;
    private final LevelChunk chunk;

    public MemoryChunk(ServerLevel level, LevelChunk chunk) {
        this.level = level;
        this.chunk = chunk;
    }

    public LevelChunk getChunk() {
        return this.chunk;
    }

    public ServerLevel getLevel() {
        return level;
    }

    @Override
    public World getWorld() {
        return level.getWorld();
    }

    @Override
    public int getX() {
        return this.chunk.getPos().x;
    }

    @Override
    public int getZ() {
        return this.chunk.getPos().z;
    }

    @Override
    public Status getStatus() {
        return MinecraftChunk.getStatus(chunk.getPersistedStatus());
    }
}
