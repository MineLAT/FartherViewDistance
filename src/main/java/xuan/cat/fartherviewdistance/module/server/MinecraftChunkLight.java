package xuan.cat.fartherviewdistance.module.server;

import java.util.Arrays;

import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;

import net.minecraft.server.level.ServerLevel;
import xuan.cat.fartherviewdistance.api.server.ServerChunkLight;

public final class MinecraftChunkLight implements ServerChunkLight {

    public static final byte[] EMPTY = new byte[0];

    private final ServerLevel worldServer;
    private final byte[][] blockLights;
    private final byte[][] skyLights;

    public MinecraftChunkLight(final World world) { this(((CraftWorld) world).getHandle()); }

    public MinecraftChunkLight(final ServerLevel worldServer) {
        this(worldServer, new byte[worldServer.getSectionsCount() + 2][], new byte[worldServer.getSectionsCount() + 2][]);
    }

    public MinecraftChunkLight(final ServerLevel worldServer, final byte[][] blockLights, final byte[][] skyLights) {
        this.worldServer = worldServer;
        this.blockLights = blockLights;
        this.skyLights = skyLights;
        Arrays.fill(blockLights, MinecraftChunkLight.EMPTY);
        Arrays.fill(skyLights, MinecraftChunkLight.EMPTY);
    }

    public ServerLevel getWorldServer() { return this.worldServer; }

    public int getArrayLength() { return this.blockLights.length; }

    public static int indexFromSectionY(final ServerLevel worldServer, final int sectionY) {
        return sectionY - worldServer.getMinSection() + 1;
    }

    public void setBlockLight(final int sectionY, final byte[] blockLight) {
        this.blockLights[MinecraftChunkLight.indexFromSectionY(this.worldServer, sectionY)] = blockLight;
    }

    public void setSkyLight(final int sectionY, final byte[] skyLight) {
        this.skyLights[MinecraftChunkLight.indexFromSectionY(this.worldServer, sectionY)] = skyLight;
    }

    public byte[] getBlockLight(final int sectionY) {
        return this.blockLights[MinecraftChunkLight.indexFromSectionY(this.worldServer, sectionY)];
    }

    public byte[] getSkyLight(final int sectionY) {
        return this.skyLights[MinecraftChunkLight.indexFromSectionY(this.worldServer, sectionY)];
    }

    public byte[][] getBlockLights() { return this.blockLights; }

    public byte[][] getSkyLights() { return this.skyLights; }
}
