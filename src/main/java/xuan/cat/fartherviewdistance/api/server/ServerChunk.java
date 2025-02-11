package xuan.cat.fartherviewdistance.api.server;

import org.bukkit.World;

public interface ServerChunk {

    World getWorld();

    int getX();

    int getZ();

    Status getStatus();

    /**
     * Chunk block status
     */
    enum Status {
        EMPTY,
        STRUCTURE_STARTS,
        STRUCTURE_REFERENCES,
        BIOMES,
        NOISE,
        SURFACE,
        CARVERS,
        FEATURES,
        LIGHT,
        SPAWN,
        FULL;

        public boolean isAbove(Status status) {
            return this.ordinal() >= status.ordinal();
        }

        public boolean isUnder(Status status) {
            return this.ordinal() <= status.ordinal();
        }
    }
}
