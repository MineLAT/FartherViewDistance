package xuan.cat.fartherviewdistance.module.server;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheRadiusPacket;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import xuan.cat.fartherviewdistance.api.server.packet.PacketEvent;
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

    public static boolean write(final Player player, final Packet<?> packet) {
        try {
            final PacketEvent event;
            switch (packet) {
                case ClientboundForgetLevelChunkPacket forgetLevelChunk ->
                        event = new PacketUnloadChunkEvent(player, forgetLevelChunk.pos().x, forgetLevelChunk.pos().z);
                case ClientboundSetChunkCacheRadiusPacket setChunkCacheRadius ->
                        event = new PacketViewDistanceEvent(player, setChunkCacheRadius.getRadius());
                case ClientboundLevelChunkWithLightPacket levelChunkWithLight ->
                        event = new PacketMapChunkEvent(player, levelChunkWithLight.getX(), levelChunkWithLight.getZ());
                case null, default -> {
                    return true;
                }
            }
            Bukkit.getPluginManager().callEvent(event);
            return !event.isCancelled();
        } catch (final Exception ex) {
            ex.printStackTrace();
            return true;
        }
    }
}
