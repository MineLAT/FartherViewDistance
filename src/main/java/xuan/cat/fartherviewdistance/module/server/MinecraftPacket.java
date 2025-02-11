package xuan.cat.fartherviewdistance.module.server;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheRadiusPacket;
import net.minecraft.world.level.ChunkPos;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import xuan.cat.fartherviewdistance.api.server.ServerChunk;
import xuan.cat.fartherviewdistance.api.server.ServerPacket;
import xuan.cat.fartherviewdistance.core.data.ConfigData;

import java.util.BitSet;
import java.util.function.Consumer;

public final class MinecraftPacket implements ServerPacket {

    @Override
    public void sendViewDistance(Player player, int viewDistance) {
        this.sendPacket(player, new ClientboundSetChunkCacheRadiusPacket(viewDistance));
    }

    @Override
    public void sendUnloadChunk(Player player, int chunkX, int chunkZ) {
        this.sendPacket(player, new ClientboundForgetLevelChunkPacket(new ChunkPos(chunkX, chunkZ)));
    }

    @Override
    public Consumer<Player> sendChunkAndLight(ConfigData.World configWorld, ServerChunk chunk, Consumer<Integer> consumeTraffic) {
        final ClientboundLevelChunkWithLightPacket packet;
        if (chunk instanceof MemoryChunk memoryChunk) {
            packet = new ClientboundLevelChunkWithLightPacket(
                    memoryChunk.getChunk(),
                    memoryChunk.getLevel().getLightEngine(),
                    null,
                    null,
                    configWorld.preventXray != null && !configWorld.preventXray.isEmpty()
            );
        } else if (chunk instanceof FileChunk fileChunk) {
            packet = new ClientboundLevelChunkWithLightPacket(
                    fileChunk.getChunk(),
                    fileChunk.getLevel().getLightEngine(),
                    null,
                    null,
                    configWorld.preventXray != null && !configWorld.preventXray.isEmpty()
            );
        } else {
            return player -> {};
        }

        if (!configWorld.sendTitleData) {
            packet.getExtraPackets().clear();
        }

        consumeTraffic.accept(packet.getChunkData().getReadBuffer().array().length);

        consumeTraffic.accept(size(packet.getLightData().getSkyYMask()));
        consumeTraffic.accept(size(packet.getLightData().getEmptySkyYMask()));
        for (byte[] update : packet.getLightData().getSkyUpdates()) {
            consumeTraffic.accept(update.length);
        }
        consumeTraffic.accept(size(packet.getLightData().getBlockYMask()));
        consumeTraffic.accept(size(packet.getLightData().getEmptyBlockYMask()));
        for (byte[] update : packet.getLightData().getBlockUpdates()) {
            consumeTraffic.accept(update.length);
        }

        try {
            // Paper method
            packet.setReady(true);
        } catch (NoSuchMethodError ignored) {
            // Spigot compatibility (not recommended)
        }
        return player -> this.sendPacket(player, packet);
    }

    private static int size(BitSet bitSet) {
        return (bitSet.cardinality() + 7) / 8;
    }

    @Override
    public void sendKeepAlive(Player player, long id) {
        this.sendPacket(player, new ClientboundKeepAlivePacket(id));
    }

    public void sendPacket(Player player, Packet<?> packet) {
        try {
            final Connection container = ((CraftPlayer) player).getHandle().connection.connection;
            container.send(packet);
        } catch (IllegalArgumentException ignored) { }
    }
}
