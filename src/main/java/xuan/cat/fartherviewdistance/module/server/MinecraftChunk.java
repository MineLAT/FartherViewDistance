package xuan.cat.fartherviewdistance.module.server;

import net.minecraft.world.level.chunk.status.ChunkStatus;
import xuan.cat.fartherviewdistance.api.server.ServerChunk;

public class MinecraftChunk {

    public static ServerChunk.Status getStatus(ChunkStatus chunkStatus) {
        if (chunkStatus == ChunkStatus.EMPTY) {
            return ServerChunk.Status.EMPTY;
        } else if (chunkStatus == ChunkStatus.STRUCTURE_STARTS) {
            return ServerChunk.Status.STRUCTURE_STARTS;
        } else if (chunkStatus == ChunkStatus.STRUCTURE_REFERENCES) {
            return ServerChunk.Status.STRUCTURE_REFERENCES;
        } else if (chunkStatus == ChunkStatus.BIOMES) {
            return ServerChunk.Status.BIOMES;
        } else if (chunkStatus == ChunkStatus.NOISE) {
            return ServerChunk.Status.NOISE;
        } else if (chunkStatus == ChunkStatus.SURFACE) {
            return ServerChunk.Status.SURFACE;
        } else if (chunkStatus == ChunkStatus.CARVERS) {
            return ServerChunk.Status.CARVERS;
        } else if (chunkStatus == ChunkStatus.FEATURES) {
            return ServerChunk.Status.FEATURES;
        } else if (chunkStatus == ChunkStatus.LIGHT) {
            return ServerChunk.Status.LIGHT;
        } else if (chunkStatus == ChunkStatus.SPAWN) {
            return ServerChunk.Status.SPAWN;
        } else {
            return chunkStatus == ChunkStatus.FULL ? ServerChunk.Status.FULL : ServerChunk.Status.EMPTY;
        }
    }
}
