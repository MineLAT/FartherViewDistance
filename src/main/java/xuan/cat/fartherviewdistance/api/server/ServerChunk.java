package xuan.cat.fartherviewdistance.api.server;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Map;

public interface ServerChunk {
    ServerNBT toNBT(ServerChunkLight light, List<Runnable> asyncRunnable);

    Chunk getChunk();

    World getWorld();

    boolean equalsBlockData(int x, int y, int z, BlockData blockData);
    BlockData getBlockData(int x, int y, int z);
    void setBlockData(int x, int y, int z, BlockData blockData);
    Map<Vector, BlockData> getBlockDataMap();

    Material getMaterial(int x, int y, int z);
    void setMaterial(int x, int y, int z, Material material);

    int getX();
    int getZ();

    Status getStatus();

    void replaceAllMaterial(BlockData[] target, BlockData to);

    @Deprecated
    Biome getBiome(int x, int z);
    Biome getBiome(int x, int y, int z);
    @Deprecated
    void setBiome(int x, int z, Biome biome);
    void setBiome(int x, int y, int z, Biome biome);

    boolean hasFluid(int x, int y, int z);
    boolean isAir(int x, int y, int z);

    int getHighestY(int x, int z);

    /**
     * 區塊狀態
     */
    enum Status {
        EMPTY               (0),
        STRUCTURE_STARTS    (1),
        STRUCTURE_REFERENCES(2),
        BIOMES              (3),
        NOISE               (4),
        SURFACE             (5),
        CARVERS             (6),
        LIQUID_CARVERS      (7),
        FEATURES            (8),
        LIGHT               (9),
        SPAWN               (10),
        HEIGHTMAPS          (11),
        FULL                (12);

        private final int sequence;
        Status(int sequence) {
            this.sequence = sequence;
        }

        public boolean isAbove(Status status) {
            return this.sequence >= status.sequence;
        }
        public boolean isUnder(Status status) {
            return this.sequence <= status.sequence;
        }
    }
}
