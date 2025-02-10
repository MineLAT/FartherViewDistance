package xuan.cat.fartherviewdistance.module.server;

import java.lang.reflect.Field;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheRadiusPacket;
import net.minecraft.world.level.ChunkPos;
import xuan.cat.fartherviewdistance.api.server.packet.PacketKeepAliveEvent;
import xuan.cat.fartherviewdistance.api.server.packet.PacketMapChunkEvent;
import xuan.cat.fartherviewdistance.api.server.packet.PacketUnloadChunkEvent;
import xuan.cat.fartherviewdistance.api.server.packet.PacketViewDistanceEvent;

public final class ProxyPlayerConnection {

    public static boolean read(final Player player, final Packet<?> packet) {
        if (packet instanceof ServerboundKeepAlivePacket) {
            final PacketKeepAliveEvent event = new PacketKeepAliveEvent(player, ((ServerboundKeepAlivePacket) packet).getId());
            Bukkit.getPluginManager().callEvent(event);
            return !event.isCancelled();
        } else {
            return true;
        }
    }

    private static Field field_ClientboundForgetLevelChunkPacket_chunkPos;
    private static Field field_ClientboundSetChunkCacheRadiusPacket_distance;
    private static Field field_ClientboundLevelChunkWithLightPacket_chunkX;
    private static Field field_ClientboundLevelChunkWithLightPacket_chunkZ;

    static {
        try {
            ProxyPlayerConnection.field_ClientboundForgetLevelChunkPacket_chunkPos = ClientboundForgetLevelChunkPacket.class
                    .getDeclaredField("pos");
            ProxyPlayerConnection.field_ClientboundSetChunkCacheRadiusPacket_distance = ClientboundSetChunkCacheRadiusPacket.class
                    .getDeclaredField("radius");
            ProxyPlayerConnection.field_ClientboundLevelChunkWithLightPacket_chunkX = ClientboundLevelChunkWithLightPacket.class
                    .getDeclaredField("x");
            ProxyPlayerConnection.field_ClientboundLevelChunkWithLightPacket_chunkZ = ClientboundLevelChunkWithLightPacket.class
                    .getDeclaredField("z");
            ProxyPlayerConnection.field_ClientboundForgetLevelChunkPacket_chunkPos.setAccessible(true);
            ProxyPlayerConnection.field_ClientboundSetChunkCacheRadiusPacket_distance.setAccessible(true);
            ProxyPlayerConnection.field_ClientboundLevelChunkWithLightPacket_chunkX.setAccessible(true);
            ProxyPlayerConnection.field_ClientboundLevelChunkWithLightPacket_chunkZ.setAccessible(true);
        } catch (final Exception ex) {
            ex.printStackTrace();
        }
    }

    public static boolean write(final Player player, final Packet<?> packet) {
        try {
            if (packet instanceof ClientboundForgetLevelChunkPacket) {
                final PacketUnloadChunkEvent event = new PacketUnloadChunkEvent(player,
                        (ChunkPos) ProxyPlayerConnection.field_ClientboundForgetLevelChunkPacket_chunkPos.get(packet));
                Bukkit.getPluginManager().callEvent(event);
                return !event.isCancelled();
            } else if (packet instanceof ClientboundSetChunkCacheRadiusPacket) {
                final PacketViewDistanceEvent event = new PacketViewDistanceEvent(player,
                        ProxyPlayerConnection.field_ClientboundSetChunkCacheRadiusPacket_distance.getInt(packet));
                Bukkit.getPluginManager().callEvent(event);
                return !event.isCancelled();
            } else if (packet instanceof ClientboundLevelChunkWithLightPacket) {
                final PacketMapChunkEvent event = new PacketMapChunkEvent(player,
                        ProxyPlayerConnection.field_ClientboundLevelChunkWithLightPacket_chunkX.getInt(packet),
                        ProxyPlayerConnection.field_ClientboundLevelChunkWithLightPacket_chunkZ.getInt(packet));
                Bukkit.getPluginManager().callEvent(event);
                return !event.isCancelled();
            } else {
                return true;
            }
        } catch (final Exception ex) {
            ex.printStackTrace();
            return true;
        }
    }
}
