package xuan.cat.fartherviewdistance.module.server;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.bukkit.World;
import org.bukkit.craftbukkit.CraftChunk;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import ca.spottedleaf.moonrise.patches.starlight.util.SaveUtil;
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
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import xuan.cat.fartherviewdistance.api.server.ServerChunk;
import xuan.cat.fartherviewdistance.api.server.ServerChunkLight;
import xuan.cat.fartherviewdistance.api.server.ServerWorld;
import xuan.cat.fartherviewdistance.api.server.ServerNBT;

@SuppressWarnings("resource")
public final class MinecraftWorld implements ServerWorld {

    /**
     * 參考 XuanCatAPI.CodeExtendWorld
     */
    @Override
    public ServerNBT getChunkNBTFromDisk(final World world, final int chunkX, final int chunkZ) throws IOException {
        CompoundTag nbt = null;
        try {
            final CompletableFuture<Optional<CompoundTag>> futureNBT = (((CraftWorld) world).getHandle()).getChunkSource().chunkMap
                    .read(new ChunkPos(chunkX, chunkZ));
            final Optional<CompoundTag> optionalNBT = futureNBT.get();
            nbt = optionalNBT.orElse(null);
        } catch (InterruptedException | ExecutionException ignored) {
        }
        return nbt != null ? new MinecraftNBT(nbt) : null;
    }

    /**
     * 參考 XuanCatAPI.CodeExtendWorld
     */
    @Override
    public ServerChunk getChunkFromMemoryCache(final World world, final int chunkX, final int chunkZ) {
        try {
            // 適用於 paper
            final ServerLevel level = ((CraftWorld) world).getHandle();
            final ChunkHolder playerChunk = level.getChunkSource().chunkMap
                    .getVisibleChunkIfPresent((long) chunkZ << 32 | (long) chunkX & 4294967295L);
            if (playerChunk != null) {
                final ChunkAccess chunk = playerChunk.getAvailableChunkNow();
                if (chunk != null && !(chunk instanceof EmptyLevelChunk) && chunk instanceof LevelChunk) {
                    return new MinecraftChunk(level, (LevelChunk) chunk);
                }
            }
            return null;
        } catch (final NoSuchMethodError ignored) {
            return null;
        }
    }

    /**
     * 參考 XuanCatAPI.CodeExtendWorld
     */
    @Override
    public ServerChunk fromChunk(final World world, final int chunkX, final int chunkZ, final ServerNBT nbt,
                                 final boolean integralHeightmap) {
        return ChunkRegionLoader.loadChunk(((CraftWorld) world).getHandle(), chunkX, chunkZ, ((MinecraftNBT) nbt).getNMSTag(),
                integralHeightmap);
    }

    /**
     * 參考 XuanCatAPI.CodeExtendWorld
     */
    @Override
    public ServerChunkLight fromLight(final World world, final ServerNBT nbt) {
        final ServerLevel level = ((CraftWorld) world).getHandle();
        final CompoundTag tag = ((MinecraftNBT) nbt).getNMSTag();
        final ChunkPos pos = new ChunkPos(tag.getInt("xPos"), tag.getInt("zPos"));
        SaveUtil.loadLightHook(level, pos, tag, level.getChunk(pos.getMiddleBlockPosition(0)));
        return ChunkRegionLoader.loadLight(level, tag);
    }

    /**
     * 參考 XuanCatAPI.CodeExtendWorld
     */
    @Override
    public ServerChunkLight fromLight(final World world) { return new MinecraftChunkLight(((CraftWorld) world).getHandle()); }

    /**
     * 參考 XuanCatAPI.CodeExtendWorld
     */
    @Override
    public ServerChunk.Status fromStatus(final ServerNBT nbt) {
        return ChunkRegionLoader.loadStatus(((MinecraftNBT) nbt).getNMSTag());
    }

    /**
     * 參考 XuanCatAPI.CodeExtendWorld
     */
    @Override
    public ServerChunk fromChunk(final World world, final org.bukkit.Chunk chunk) {
        return new MinecraftChunk(((CraftChunk) chunk).getCraftWorld().getHandle(),
                (LevelChunk) ((CraftChunk) chunk).getHandle(ChunkStatus.FULL));
    }

    @Override
    public void injectPlayer(final Player player) {
        final ServerPlayer entityPlayer = ((CraftPlayer) player).getHandle();
        final ServerGamePacketListenerImpl connection = entityPlayer.connection;
        final Channel channel = connection.connection.channel;
        final ChannelPipeline pipeline = channel.pipeline();
        pipeline.addAfter("packet_handler", "farther_view_distance_write", new ChannelDuplexHandler() {
            @Override
            public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) throws Exception {
                if (msg instanceof Packet) {
                    if (!ProxyPlayerConnection.write(player, (Packet<?>) msg))
                        return;
                }
                super.write(ctx, msg, promise);
            }
        });
        pipeline.addAfter("encoder", "farther_view_distance_read", new ChannelInboundHandlerAdapter() {
            @Override
            public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
                if (msg instanceof Packet) {
                    if (!ProxyPlayerConnection.read(player, (Packet<?>) msg))
                        return;
                }
                super.channelRead(ctx, msg);
            }
        });
    }
}
