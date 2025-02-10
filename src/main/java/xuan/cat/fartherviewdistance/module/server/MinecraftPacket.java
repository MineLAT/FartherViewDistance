package xuan.cat.fartherviewdistance.module.server;

import java.util.function.Consumer;

import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import io.netty.buffer.Unpooled;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheRadiusPacket;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.lighting.LevelLightEngine;
import xuan.cat.fartherviewdistance.api.server.ServerChunk;
import xuan.cat.fartherviewdistance.api.server.ServerChunkLight;
import xuan.cat.fartherviewdistance.api.server.ServerPacket;

public final class MinecraftPacket implements ServerPacket {

    private final PacketHandleChunk handleChunk = new PacketHandleChunk();
    private final PacketHandleLightUpdate handleLightUpdate = new PacketHandleLightUpdate();

    public void sendPacket(final Player player, final Packet<?> packet) {
        try {
            final Connection container = ((CraftPlayer) player).getHandle().connection.connection;
            container.send(packet);
        } catch (final IllegalArgumentException ignored) {
        }
    }

    @Override
    public void sendViewDistance(final Player player, final int viewDistance) {
        this.sendPacket(player, new ClientboundSetChunkCacheRadiusPacket(viewDistance));
    }

    @Override
    public void sendUnloadChunk(final Player player, final int chunkX, final int chunkZ) {
        this.sendPacket(player, new ClientboundForgetLevelChunkPacket(new ChunkPos(chunkX, chunkZ)));
    }

    @Override
    public Consumer<Player> sendChunkAndLight(final ServerChunk chunk, final ServerChunkLight light, final boolean needTile,
                                              final Consumer<Integer> consumeTraffic) {
        final FriendlyByteBuf serializer = new FriendlyByteBuf(Unpooled.buffer().writerIndex(0));
        final LevelLightEngine levelLight = ((MinecraftChunk) chunk).getLevelChunk().level.getLightEngine();
        serializer.writeInt(chunk.getX());
        serializer.writeInt(chunk.getZ());
        this.handleChunk.write(serializer, ((MinecraftChunk) chunk).getLevelChunk(), needTile);
        this.handleLightUpdate.write(serializer, (MinecraftChunkLight) light);
        consumeTraffic.accept(serializer.readableBytes());
        final ClientboundLevelChunkWithLightPacket packet = new ClientboundLevelChunkWithLightPacket(
                ((MinecraftChunk) chunk).getLevelChunk(), levelLight, null, null, false);
        try {
            // 適用於 paper
            packet.setReady(true);
        } catch (final NoSuchMethodError noSuchMethodError) {
            // 適用於 spigot (不推薦)
        }
        return player -> this.sendPacket(player, packet);
    }

    @Override
    public void sendKeepAlive(final Player player, final long id) { this.sendPacket(player, new ClientboundKeepAlivePacket(id)); }
}
