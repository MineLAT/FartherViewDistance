package xuan.cat.fartherviewdistance.code.branch.v120_4;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import net.minecraft.network.FriendlyByteBuf;

public final class Branch_120_4_PacketHandleLightUpdate {

    public Branch_120_4_PacketHandleLightUpdate() {}

    public void write(final FriendlyByteBuf serializer, final Branch_120_4_ChunkLight light) {
        final List<byte[]> dataSky = new ArrayList<>();
        final List<byte[]> dataBlock = new ArrayList<>();
        final BitSet notSkyEmpty = new BitSet();
        final BitSet notBlockEmpty = new BitSet();
        final BitSet isSkyEmpty = new BitSet();
        final BitSet isBlockEmpty = new BitSet();

        for (int index = 0; index < light.getArrayLength(); ++index) {
            Branch_120_4_PacketHandleLightUpdate.saveBitSet(light.getSkyLights(), index, notSkyEmpty, isSkyEmpty, dataSky);
            Branch_120_4_PacketHandleLightUpdate.saveBitSet(light.getBlockLights(), index, notBlockEmpty, isBlockEmpty, dataBlock);
        }

        serializer.writeBitSet(notSkyEmpty);
        serializer.writeBitSet(notBlockEmpty);
        serializer.writeBitSet(isSkyEmpty);
        serializer.writeBitSet(isBlockEmpty);
        serializer.writeCollection(dataSky, FriendlyByteBuf::writeByteArray);
        serializer.writeCollection(dataBlock, FriendlyByteBuf::writeByteArray);
    }

    private static void saveBitSet(final byte[][] nibbleArrays, final int index, final BitSet notEmpty, final BitSet isEmpty,
            final List<byte[]> list) {
        final byte[] nibbleArray = nibbleArrays[index];
        if (nibbleArray != Branch_120_4_ChunkLight.EMPTY) {
            if (nibbleArray == null) {
                isEmpty.set(index);
            } else {
                notEmpty.set(index);
                list.add(nibbleArray);
            }
        }
    }
}
