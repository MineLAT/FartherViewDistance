package xuan.cat.fartherviewdistance.api.server.packet;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

public final class PacketMapChunkEvent extends PacketEvent {
    private static final HandlerList handlers = new HandlerList();
    public HandlerList getHandlers() {
        return handlers;
    }
    public static HandlerList getHandlerList() {
        return handlers;
    }

    private final int chunkX;
    private final int chunkZ;

    public PacketMapChunkEvent(Player player, int chunkX, int chunkZ) {
        super(player);
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    public int getChunkX() {
        return chunkX;
    }
    public int getChunkZ() {
        return chunkZ;
    }
}