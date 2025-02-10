package xuan.cat.fartherviewdistance.module.server;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.VarInt;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public final class PacketHandleLightUpdate {

    public PacketHandleLightUpdate() {
    }

    public void write(final FriendlyByteBuf serializer, final MinecraftChunkLight light) {
        final List<byte[]> dataSky = new ArrayList<>();
        final List<byte[]> dataBlock = new ArrayList<>();
        final BitSet notSkyEmpty = new BitSet();
        final BitSet notBlockEmpty = new BitSet();
        final BitSet isSkyEmpty = new BitSet();
        final BitSet isBlockEmpty = new BitSet();

        for (int index = 0; index < light.getArrayLength(); ++index) {
            PacketHandleLightUpdate.saveBitSet(light.getSkyLights(), index, notSkyEmpty, isSkyEmpty, dataSky);
            PacketHandleLightUpdate.saveBitSet(light.getBlockLights(), index, notBlockEmpty, isBlockEmpty, dataBlock);
        }

        serializer.writeBitSet(notSkyEmpty);
        serializer.writeBitSet(notBlockEmpty);
        serializer.writeBitSet(isSkyEmpty);
        serializer.writeBitSet(isBlockEmpty);
        serializer.writeCollection(dataBlock, (buf, byteArray) -> {
            VarInt.write(buf, byteArray.length);
            buf.writeBytes(byteArray);
        });
        serializer.writeCollection(dataBlock, (buf, byteArray) -> {
            VarInt.write(buf, byteArray.length);
            buf.writeBytes(byteArray);
        });
    }

    private static void saveBitSet(final byte[][] nibbleArrays, final int index, final BitSet notEmpty, final BitSet isEmpty,
                                   final List<byte[]> list) {
        final byte[] nibbleArray = nibbleArrays[index];
        if (nibbleArray != MinecraftChunkLight.EMPTY) {
            if (nibbleArray == null) {
                isEmpty.set(index);
            } else {
                notEmpty.set(index);
                list.add(nibbleArray);
            }
        }
    }
}
