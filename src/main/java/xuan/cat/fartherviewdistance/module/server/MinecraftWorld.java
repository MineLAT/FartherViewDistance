package xuan.cat.fartherviewdistance.module.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftChunk;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import xuan.cat.fartherviewdistance.api.server.ServerChunk;
import xuan.cat.fartherviewdistance.api.server.ServerWorld;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public final class MinecraftWorld implements ServerWorld {

    @Override
    public ServerChunk getChunkFromDisk(World world, int chunkX, int chunkZ) {
        try {
            final ServerLevel level = ((CraftWorld) world).getHandle();
            final CompletableFuture<Optional<CompoundTag>> futureNBT = level.getChunkSource().chunkMap.read(new ChunkPos(chunkX, chunkZ));
            final Optional<CompoundTag> optionalNBT = futureNBT.get();
            final CompoundTag tag = optionalNBT.orElse(null);
            if (tag != null) {
                return new FileChunk(level, MinecraftChunkData.parse(level, level.registryAccess(), tag));
            }
        } catch (InterruptedException | ExecutionException ignored) { }
        return null;
    }

    @Override
    public ServerChunk getChunkFromMemoryCache(World world, int chunkX, int chunkZ) {
        try {
            // 適用於 paper
            final ServerLevel level = ((CraftWorld) world).getHandle();
            final ChunkHolder playerChunk = level.getChunkSource().chunkMap.getVisibleChunkIfPresent((long) chunkZ << 32 | (long) chunkX & 4294967295L);
            if (playerChunk != null) {
                final LevelChunk chunk = playerChunk.getFullChunkNow();
                if (chunk != null && !(chunk instanceof EmptyLevelChunk)) {
                    return new MemoryChunk(level, chunk);
                }
            }
            return null;
        } catch (NoSuchMethodError ignored) { }
        return null;
    }

    @Override
    public ServerChunk getChunkOrLoad(World world, org.bukkit.Chunk chunk) {
        return new MemoryChunk(((CraftChunk) chunk).getCraftWorld().getHandle(), (LevelChunk) ((CraftChunk) chunk).getHandle(ChunkStatus.FULL));
    }

    @Override
    public void injectPlayer(Player player) {
        final ServerPlayer entityPlayer = ((CraftPlayer) player).getHandle();
        final ServerGamePacketListenerImpl connection = entityPlayer.connection;
        final Channel channel = connection.connection.channel;
        final ChannelPipeline pipeline = channel.pipeline();
        pipeline.addAfter("packet_handler", "farther_view_distance_write", new ChannelDuplexHandler() {
            @Override
            public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                if (msg instanceof Packet) {
                    if (!ProxyPlayerConnection.write(player, (Packet<?>) msg))
                        return;
                }
                super.write(ctx, msg, promise);
            }
        });
        pipeline.addAfter("encoder", "farther_view_distance_read", new ChannelInboundHandlerAdapter() {
            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                if (msg instanceof Packet) {
                    if (!ProxyPlayerConnection.read(player, (Packet<?>) msg))
                        return;
                }
                super.channelRead(ctx, msg);
            }
        });
    }
}
