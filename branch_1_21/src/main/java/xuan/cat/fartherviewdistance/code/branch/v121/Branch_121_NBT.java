package xuan.cat.fartherviewdistance.code.branch.v121;

import net.minecraft.nbt.CompoundTag;
import xuan.cat.fartherviewdistance.api.branch.BranchNBT;

public final class Branch_121_NBT implements BranchNBT {

    protected CompoundTag tag;

    public Branch_121_NBT() { this.tag = new CompoundTag(); }

    public Branch_121_NBT(final CompoundTag tag) { this.tag = tag; }

    public CompoundTag getNMSTag() { return this.tag; }

    @Override
    public String toString() { return this.tag.toString(); }
}
