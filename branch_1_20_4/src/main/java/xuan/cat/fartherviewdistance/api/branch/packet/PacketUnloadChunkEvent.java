package xuan.cat.fartherviewdistance.api.branch.packet;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import net.minecraft.world.level.ChunkPos;

public final class PacketUnloadChunkEvent extends PacketEvent {

    private static final HandlerList handlers = new HandlerList();

    @Override
    public HandlerList getHandlers() { return handlers; }

    public static HandlerList getHandlerList() { return handlers; }

    private final int chunkX;
    private final int chunkZ;
    // The backtick character (`) in programming is often used as a delimiter for
    // code blocks or inline
    // code snippets. It is commonly used in Markdown, for example, to format code
    // within a text document.
    // In the context of your code snippet, the backticks are not being used for any
    // specific purpose as
    // they are just part of the text and do not have any special meaning in Java
    // code.

    public PacketUnloadChunkEvent(final Player player, final int chunkX, final int chunkZ) {
        super(player);
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    public PacketUnloadChunkEvent(final Player player, final ChunkPos chunkPos) {
        super(player);
        this.chunkX = chunkPos.x;
        this.chunkZ = chunkPos.z;
    }

    public int getChunkX() { return this.chunkX; }

    public int getChunkZ() { return this.chunkZ; }
}
