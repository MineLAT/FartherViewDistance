package xuan.cat.fartherviewdistance.module.server;

import net.minecraft.nbt.CompoundTag;
import xuan.cat.fartherviewdistance.api.server.ServerNBT;

public final class MinecraftNBT implements ServerNBT {

    protected CompoundTag tag;

    public MinecraftNBT() {
        this.tag = new CompoundTag();
    }

    public MinecraftNBT(final CompoundTag tag) {
        this.tag = tag;
    }

    public CompoundTag getNMSTag() {
        return this.tag;
    }

    @Override
    public String toString() {
        return this.tag.toString();
    }
}
