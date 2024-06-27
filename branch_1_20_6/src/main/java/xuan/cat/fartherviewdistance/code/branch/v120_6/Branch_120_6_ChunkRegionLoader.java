package xuan.cat.fartherviewdistance.code.branch.v120_6;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;

import ca.spottedleaf.starlight.common.light.SWMRNibbleArray;
import ca.spottedleaf.starlight.common.light.StarLightEngine;
import ca.spottedleaf.starlight.common.util.SaveUtil;
import io.papermc.paper.util.WorldUtil;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.shorts.ShortList;
import it.unimi.dsi.fastutil.shorts.ShortListIterator;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainer.Strategy;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.status.ChunkType;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.chunk.storage.ChunkSerializer.AsyncSaveData;
import net.minecraft.world.level.levelgen.BelowZeroRetrogen;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.GenerationStep.Carving;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.ProtoChunkTicks;
import xuan.cat.fartherviewdistance.api.branch.BranchChunk;
import xuan.cat.fartherviewdistance.api.branch.BranchChunkLight;

/**
 * @see ChunkSerializer 參考 XuanCatAPI.CodeExtendChunkLight
 */
@SuppressWarnings({ "unchecked" })
public final class Branch_120_6_ChunkRegionLoader {

    private static final int CURRENT_DATA_VERSION = SharedConstants.getCurrentVersion().getDataVersion().getVersion();
    private static final boolean JUST_CORRUPT_IT = Boolean.getBoolean("Paper.ignoreWorldDataVersion");
    public static final Codec<PalettedContainer<BlockState>> BLOCK_STATE_CODEC = ChunkSerializer.BLOCK_STATE_CODEC;

    public static BranchChunk.Status loadStatus(final CompoundTag nbt) {
        try {
            // 適用於 paper
            return Branch_120_6_Chunk.ofStatus(ChunkStatus.getStatus(nbt.getString("Status")));
        } catch (final NoSuchMethodError noSuchMethodError) {
            // 適用於 spigot (不推薦)
            return Branch_120_6_Chunk.ofStatus(ChunkStatus.byName(nbt.getString("Status")));
        }
    }

    private static Codec<PalettedContainerRO<Holder<Biome>>> makeBiomeCodec(final Registry<Biome> biomeRegistry) {
        return PalettedContainer.codecRO(biomeRegistry.asHolderIdMap(), biomeRegistry.holderByNameCodec(),
                PalettedContainer.Strategy.SECTION_BIOMES, biomeRegistry.getHolderOrThrow(Biomes.PLAINS));
    }

    private static Method method_ChunkSerializer_makeBiomeCodecRW;

    static {
        try {
            Branch_120_6_ChunkRegionLoader.method_ChunkSerializer_makeBiomeCodecRW = ChunkSerializer.class
                    .getDeclaredMethod("makeBiomeCodecRW", Registry.class);
            Branch_120_6_ChunkRegionLoader.method_ChunkSerializer_makeBiomeCodecRW.setAccessible(true);
        } catch (final NoSuchMethodException ex) {
            ex.printStackTrace();
        }
    }
    private static Field DimensionDataStorage_Provider;

    static {
        try {
            Branch_120_6_ChunkRegionLoader.DimensionDataStorage_Provider = DimensionDataStorage.class.getDeclaredField("registries");
            Branch_120_6_ChunkRegionLoader.DimensionDataStorage_Provider.setAccessible(true);
        } catch (final NoSuchFieldException ex) {
            ex.printStackTrace();
        }
    }

    private static Codec<PalettedContainer<Holder<Biome>>> makeBiomeCodecRW(final Registry<Biome> biomeRegistry) {
        try {
            return (Codec<PalettedContainer<Holder<Biome>>>) Branch_120_6_ChunkRegionLoader.method_ChunkSerializer_makeBiomeCodecRW
                    .invoke(Branch_120_6_ChunkRegionLoader.method_ChunkSerializer_makeBiomeCodecRW, biomeRegistry);
        } catch (InvocationTargetException | IllegalAccessException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static BranchChunk loadChunk(final ServerLevel world, final int chunkX, final int chunkZ, final CompoundTag nbt,
            final boolean integralHeightmap) {
        if (nbt.contains("DataVersion", 99)) {
            final int dataVersion = nbt.getInt("DataVersion");
            if (!Branch_120_6_ChunkRegionLoader.JUST_CORRUPT_IT && dataVersion > Branch_120_6_ChunkRegionLoader.CURRENT_DATA_VERSION) {
                (new RuntimeException("Server attempted to load chunk saved with newer version of minecraft! " + dataVersion + " > "
                        + Branch_120_6_ChunkRegionLoader.CURRENT_DATA_VERSION)).printStackTrace();
                System.exit(1);
            }
        }

        final ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
        final UpgradeData upgradeData = nbt.contains("UpgradeData", 10) ? new UpgradeData(nbt.getCompound("UpgradeData"), world)
                : UpgradeData.EMPTY;
        boolean isLightOn = Branch_120_6_ChunkRegionLoader.getStatus(nbt) != null
                && Branch_120_6_ChunkRegionLoader.getStatus(nbt).isOrAfter(ChunkStatus.LIGHT) && nbt.get("isLightOn") != null
                && nbt.getInt("starlight.light_version") == 9;
        final ListTag sectionArrayNBT = nbt.getList("sections", 10);
        final int sectionsCount = world.getSectionsCount();
        final LevelChunkSection[] sections = new LevelChunkSection[sectionsCount];
        final boolean hasSkyLight = world.dimensionType().hasSkyLight();
        final ServerChunkCache chunkSource = world.getChunkSource();
        final LevelLightEngine lightEngine = chunkSource.getLightEngine();

        final SWMRNibbleArray[] blockNibbles = StarLightEngine.getFilledEmptyLight(world);
        final SWMRNibbleArray[] skyNibbles = StarLightEngine.getFilledEmptyLight(world);
        final int minSection = world.getMinSection() - 1;
        final Registry<Biome> biomeRegistry = world.registryAccess().registryOrThrow(Registries.BIOME);
        final Codec<PalettedContainer<Holder<Biome>>> paletteCodec = Branch_120_6_ChunkRegionLoader.makeBiomeCodecRW(biomeRegistry);

        for (int sectionIndex = 0; sectionIndex < sectionArrayNBT.size(); ++sectionIndex) {
            final CompoundTag sectionNBT = sectionArrayNBT.getCompound(sectionIndex);
            final byte locationY = sectionNBT.getByte("Y");
            final int sectionY = world.getSectionIndexFromSectionY(locationY);
            if (sectionY >= 0 && sectionY < sections.length) {
                final BlockState[] presetBlockStates = world.chunkPacketBlockController.getPresetBlockStates(world, chunkPos, locationY);
                PalettedContainer<BlockState> paletteBlock;
                if (sectionNBT.contains("block_states", 10)) {

                    final Codec<PalettedContainer<BlockState>> blockStateCodec = presetBlockStates == null
                            ? ChunkSerializer.BLOCK_STATE_CODEC
                            : PalettedContainer.codecRW(Block.BLOCK_STATE_REGISTRY, BlockState.CODEC, Strategy.SECTION_STATES,
                                    Blocks.AIR.defaultBlockState(), presetBlockStates);
                    paletteBlock = (PalettedContainer) blockStateCodec.parse(NbtOps.INSTANCE, sectionNBT.getCompound("block_states"))
                            .promotePartial(sx -> {
                            }).getOrThrow(Branch_120_6_NothingException::new);
                } else {
                    paletteBlock = new PalettedContainer<>(Block.BLOCK_STATE_REGISTRY, Blocks.AIR.defaultBlockState(),
                            PalettedContainer.Strategy.SECTION_STATES,
                            world.chunkPacketBlockController.getPresetBlockStates(world, chunkPos, locationY));
                }

                // 生態轉換器
                PalettedContainer<Holder<Biome>> paletteBiome;
                if (sectionNBT.contains("biomes", 10)) {
                    paletteBiome = paletteCodec.parse(NbtOps.INSTANCE, sectionNBT.getCompound("biomes")).promotePartial(sx -> {
                    }).getOrThrow(Branch_120_6_NothingException::new);
                } else {
                    try {
                        // 適用於 paper
                        paletteBiome = new PalettedContainer<>(biomeRegistry.asHolderIdMap(), biomeRegistry.getHolderOrThrow(Biomes.PLAINS),
                                PalettedContainer.Strategy.SECTION_BIOMES, null);
                    } catch (final NoSuchMethodError noSuchMethodError) {
                        // 適用於 spigot (不推薦)
                        paletteBiome = new PalettedContainer<>(biomeRegistry.asHolderIdMap(), biomeRegistry.getHolderOrThrow(Biomes.PLAINS),
                                PalettedContainer.Strategy.SECTION_BIOMES, null);
                    }
                }

                final LevelChunkSection chunkSection = new LevelChunkSection(paletteBlock, paletteBiome);
                sections[sectionY] = chunkSection;
                SectionPos.of(chunkPos, sectionY);
            }
            final boolean isBlockLight = sectionNBT.contains("BlockLight", 7);
            final boolean isSkyLight = isLightOn && sectionNBT.contains("SkyLight", 7);
            if (isLightOn) {
                try {
                    final int y = sectionNBT.getByte("Y");
                    if (isBlockLight) {
                        blockNibbles[y - minSection] = new SWMRNibbleArray((byte[]) sectionNBT.getByteArray("BlockLight").clone(),
                                sectionNBT.getInt("starlight.blocklight_state"));
                    } else {
                        blockNibbles[y - minSection] = new SWMRNibbleArray((byte[]) null, sectionNBT.getInt("starlight.blocklight_state"));
                    }

                    if (isSkyLight) {
                        skyNibbles[y - minSection] = new SWMRNibbleArray((byte[]) sectionNBT.getByteArray("SkyLight").clone(),
                                sectionNBT.getInt("starlight.skylight_state"));
                    } else if (isLightOn) {
                        skyNibbles[y - minSection] = new SWMRNibbleArray((byte[]) null, sectionNBT.getInt("starlight.skylight_state"));
                    }
                } catch (final Exception e) {
                    isLightOn = false;
                }
            }

        }

        final long inhabitedTime = nbt.getLong("InhabitedTime");
        final ChunkType chunkType = Branch_120_6_ChunkRegionLoader.getChunkTypeFromTag(nbt);
        final BlendingData blendingData;
        if (nbt.contains("blending_data", 10)) {
            blendingData = BlendingData.CODEC.parse(new Dynamic<>(NbtOps.INSTANCE, nbt.getCompound("blending_data")))
                    .resultOrPartial(sx -> {
                    }).orElse(null);
        } else {
            blendingData = null;
        }

        Object chunk;
        if (chunkType == ChunkType.LEVELCHUNK) {
            final LevelChunkTicks<Block> ticksBlock = LevelChunkTicks.load(nbt.getList("block_ticks", 10),
                    sx -> BuiltInRegistries.BLOCK.getOptional(ResourceLocation.tryParse(sx)), chunkPos);
            final LevelChunkTicks<Fluid> ticksFluid = LevelChunkTicks.load(nbt.getList("fluid_ticks", 10),
                    sx -> BuiltInRegistries.FLUID.getOptional(ResourceLocation.tryParse(sx)), chunkPos);
            final LevelChunk levelChunk = new LevelChunk(world.getLevel(), chunkPos, upgradeData, ticksBlock, ticksFluid, inhabitedTime,
                    sections, Branch_120_6_ChunkRegionLoader.postLoadChunk(world, nbt), blendingData);
            chunk = levelChunk;
            ((LevelChunk) chunk).setBlockNibbles(blockNibbles);
            ((LevelChunk) chunk).setSkyNibbles(skyNibbles);

        } else {
            final ProtoChunkTicks<Block> ticksBlock = ProtoChunkTicks.load(nbt.getList("block_ticks", 10),
                    sx -> BuiltInRegistries.BLOCK.getOptional(ResourceLocation.tryParse(sx)), chunkPos);
            final ProtoChunkTicks<Fluid> ticksFluid = ProtoChunkTicks.load(nbt.getList("fluid_ticks", 10),
                    sx -> BuiltInRegistries.FLUID.getOptional(ResourceLocation.tryParse(sx)), chunkPos);
            final ProtoChunk protochunk = new ProtoChunk(chunkPos, upgradeData, sections, ticksBlock, ticksFluid, world, biomeRegistry,
                    blendingData);
            chunk = protochunk;
            ((ProtoChunk) chunk).setBlockNibbles(blockNibbles);
            ((ProtoChunk) chunk).setSkyNibbles(skyNibbles);
            ((ProtoChunk) chunk).setInhabitedTime(inhabitedTime);
            if (nbt.contains("below_zero_retrogen", 10)) {
                BelowZeroRetrogen.CODEC.parse(new Dynamic<>(NbtOps.INSTANCE, nbt.getCompound("below_zero_retrogen")))
                        .resultOrPartial(sx -> {
                        }).ifPresent(protochunk::setBelowZeroRetrogen);
            }

            final ChunkStatus chunkStatus = ChunkStatus.byName(nbt.getString("Status"));
            ((ProtoChunk) chunk).setStatus(chunkStatus);
            if (chunkStatus.isOrAfter(ChunkStatus.INITIALIZE_LIGHT)) {
                ((ProtoChunk) chunk).setLightEngine(lightEngine);
            }
        }

        final Tag persistentBase = nbt.get("ChunkBukkitValues");
        if (persistentBase instanceof CompoundTag) {
            ((ChunkAccess) chunk).persistentDataContainer.putAll((CompoundTag) persistentBase);
        }

        ((ChunkAccess) chunk).setLightCorrect(isLightOn);
        final CompoundTag nbttagcompound2 = nbt.getCompound("Heightmaps");
        final EnumSet<Heightmap.Types> enumHeightmapType = EnumSet.noneOf(Heightmap.Types.class);
        final Iterator iterator = ((ChunkAccess) chunk).getStatus().heightmapsAfter().iterator();

        while (iterator.hasNext()) {
            final Heightmap.Types heightmap_type = (Heightmap.Types) iterator.next();
            final String s = heightmap_type.getSerializationKey();
            if (nbttagcompound2.contains(s, 12)) {
                ((ChunkAccess) chunk).setHeightmap(heightmap_type, nbttagcompound2.getLongArray(s));
            } else {
                enumHeightmapType.add(heightmap_type);
            }
        }
        Heightmap.primeHeightmaps((ChunkAccess) chunk, enumHeightmapType);

        final CompoundTag nbttagcompound3 = nbt.getCompound("structures");
        ((ChunkAccess) chunk).setAllStarts(Branch_120_6_ChunkRegionLoader
                .unpackStructureStart(StructurePieceSerializationContext.fromLevel(world), nbttagcompound3, world.getSeed()));
        ((ChunkAccess) chunk).setAllReferences(
                Branch_120_6_ChunkRegionLoader.unpackStructureReferences(world.registryAccess(), chunkPos, nbttagcompound3));
        if (nbt.getBoolean("shouldSave")) {
            ((ChunkAccess) chunk).setUnsaved(true);
        }

        final ListTag processListNBT = nbt.getList("PostProcessing", 9);
        for (int indexList = 0; indexList < processListNBT.size(); ++indexList) {
            final ListTag processNBT = processListNBT.getList(indexList);
            for (int index = 0; index < processNBT.size(); ++index) {
                ((ChunkAccess) chunk).addPackedPostProcess(processNBT.getShort(index), indexList);
            }
        }

        if (chunkType == ChunkType.LEVELCHUNK) {
            return new Branch_120_6_Chunk(world, (LevelChunk) chunk, nbt);
        } else {
            final ProtoChunk protoChunk = (ProtoChunk) chunk;
            return new Branch_120_6_Chunk(world, new LevelChunk(world, protoChunk, v -> {
            }), nbt);
        }
    }

    public static ChunkType getChunkTypeFromTag(@javax.annotation.Nullable final CompoundTag nbt) {
        return nbt != null ? ChunkStatus.byName(nbt.getString("Status")).getChunkType() : ChunkType.PROTOCHUNK;
    }

    @Nullable
    private static LevelChunk.PostLoadProcessor postLoadChunk(final ServerLevel world, final CompoundTag nbt) {
        final ListTag nbttaglist = Branch_120_6_ChunkRegionLoader.getListOfCompoundsOrNull(nbt, "entities");
        final ListTag nbttaglist1 = Branch_120_6_ChunkRegionLoader.getListOfCompoundsOrNull(nbt, "block_entities");
        return nbttaglist == null && nbttaglist1 == null ? null : chunk -> {
            if (nbttaglist != null) {
                world.addLegacyChunkEntities(EntityType.loadEntitiesRecursive(nbttaglist, world), chunk.getPos());
            }

            if (nbttaglist1 != null) {
                for (int i = 0; i < nbttaglist1.size(); ++i) {
                    final CompoundTag nbttagcompound1 = nbttaglist1.getCompound(i);
                    final boolean flag = nbttagcompound1.getBoolean("keepPacked");
                    final BlockPos blockposition = BlockEntity.getPosFromTag(nbttagcompound1);
                    final ChunkPos chunkPos = chunk.getPos();
                    if (blockposition.getX() >> 4 == chunkPos.x && blockposition.getZ() >> 4 == chunkPos.z) {
                        if (flag) {
                            chunk.setBlockEntityNbt(nbttagcompound1);
                        } else {
                            final BlockEntity tileentity = BlockEntity.loadStatic(blockposition, chunk.getBlockState(blockposition),
                                    nbttagcompound1, world.registryAccess());
                            if (tileentity != null) {
                                chunk.setBlockEntity(tileentity);
                            }
                        }
                    } else {
                    }
                }
            }

        };
    }

    @Nullable
    private static ListTag getListOfCompoundsOrNull(final CompoundTag nbt, final String key) {
        final ListTag nbttaglist = nbt.getList(key, 10);
        return nbttaglist.isEmpty() ? null : nbttaglist;
    }

    @javax.annotation.Nullable
    public static ChunkStatus getStatus(@javax.annotation.Nullable final CompoundTag compound) {
        return compound == null ? null : ChunkStatus.getStatus(compound.getString("Status"));
    }

    public static BranchChunkLight loadLight(final ServerLevel world, final CompoundTag nbt) {
        // 檢查資料版本
        if (nbt.contains("DataVersion", 99)) {
            final int dataVersion = nbt.getInt("DataVersion");
            if (!Branch_120_6_ChunkRegionLoader.JUST_CORRUPT_IT && dataVersion > Branch_120_6_ChunkRegionLoader.CURRENT_DATA_VERSION) {
                (new RuntimeException("Server attempted to load chunk saved with newer version of minecraft! " + dataVersion + " > "
                        + Branch_120_6_ChunkRegionLoader.CURRENT_DATA_VERSION)).printStackTrace();
                System.exit(1);
            }
        }

        final boolean isLightOn = Branch_120_6_ChunkRegionLoader.getStatus(nbt) != null
                && Branch_120_6_ChunkRegionLoader.getStatus(nbt).isOrAfter(ChunkStatus.LIGHT) && nbt.get("isLightOn") != null
                && nbt.getInt("starlight.light_version") == 9;
        final boolean hasSkyLight = world.dimensionType().hasSkyLight();
        final ListTag sectionArrayNBT = nbt.getList("sections", 10);
        final Branch_120_6_ChunkLight chunkLight = new Branch_120_6_ChunkLight(world);
        for (int sectionIndex = 0; sectionIndex < sectionArrayNBT.size(); ++sectionIndex) {
            final CompoundTag sectionNBT = sectionArrayNBT.getCompound(sectionIndex);
            final byte locationY = sectionNBT.getByte("Y");
            if (isLightOn) {
                if (sectionNBT.contains("BlockLight", 7)) {
                    chunkLight.setBlockLight(locationY, sectionNBT.getByteArray("BlockLight"));
                }
                if (hasSkyLight) {
                    if (sectionNBT.contains("SkyLight", 7)) {
                        chunkLight.setSkyLight(locationY, sectionNBT.getByteArray("SkyLight"));
                    }
                }
            }
        }

        return chunkLight;
    }

    public static CompoundTag saveChunk(final ServerLevel world, final ChunkAccess chunk, @Nullable final AsyncSaveData asyncsavedata) {
        final int minSection = WorldUtil.getMinLightSection(world);
        final int maxSection = WorldUtil.getMaxLightSection(world);
        final SWMRNibbleArray[] blockNibbles = chunk.getBlockNibbles();
        final SWMRNibbleArray[] skyNibbles = chunk.getSkyNibbles();
        final ChunkPos chunkcoordintpair = chunk.getPos();
        final CompoundTag nbttagcompound = NbtUtils.addCurrentDataVersion(new CompoundTag());
        nbttagcompound.putInt("xPos", chunkcoordintpair.x);
        nbttagcompound.putInt("yPos", chunk.getMinSection());
        nbttagcompound.putInt("zPos", chunkcoordintpair.z);
        nbttagcompound.putLong("LastUpdate", world.getGameTime());
        nbttagcompound.putLong("InhabitedTime", chunk.getInhabitedTime());
        nbttagcompound.putString("Status", BuiltInRegistries.CHUNK_STATUS.getKey(chunk.getStatus()).toString());
        final BlendingData blendingdata = chunk.getBlendingData();
        DataResult dataresult;
        if (blendingdata != null) {
            dataresult = BlendingData.CODEC.encodeStart(NbtOps.INSTANCE, blendingdata);
            dataresult.resultOrPartial(Branch_120_6_NothingException::new).ifPresent(nbtbase -> {
                nbttagcompound.put("blending_data", (Tag) nbtbase);
            });
        }

        final BelowZeroRetrogen belowzeroretrogen = chunk.getBelowZeroRetrogen();
        if (belowzeroretrogen != null) {
            dataresult = BelowZeroRetrogen.CODEC.encodeStart(NbtOps.INSTANCE, belowzeroretrogen);
            dataresult.resultOrPartial(Branch_120_6_NothingException::new).ifPresent(nbtbase -> {
                nbttagcompound.put("below_zero_retrogen", (Tag) nbtbase);
            });
        }

        final UpgradeData chunkconverter = chunk.getUpgradeData();
        if (!chunkconverter.isEmpty()) {
            nbttagcompound.put("UpgradeData", chunkconverter.write());
        }

        final LevelChunkSection[] achunksection = chunk.getSections();
        final ListTag nbttaglist = new ListTag();
        final ThreadedLevelLightEngine lightenginethreaded = world.getChunkSource().getLightEngine();
        final Registry<Biome> iregistry = world.registryAccess().registryOrThrow(Registries.BIOME);
        final Codec<PalettedContainerRO<Holder<Biome>>> codec = Branch_120_6_ChunkRegionLoader.makeBiomeCodec(iregistry);
        final boolean flag = chunk.isLightCorrect();

        for (int i = lightenginethreaded.getMinLightSection(); i < lightenginethreaded.getMaxLightSection(); ++i) {
            final int j = chunk.getSectionIndexFromSectionY(i);
            final boolean flag1 = j >= 0 && j < achunksection.length;
            final SWMRNibbleArray.SaveState blockNibble = blockNibbles[i - minSection].getSaveState();
            final SWMRNibbleArray.SaveState skyNibble = skyNibbles[i - minSection].getSaveState();
            if (flag1 || blockNibble != null || skyNibble != null) {
                final CompoundTag nbttagcompound1 = new CompoundTag();
                if (flag1) {
                    final LevelChunkSection chunksection = achunksection[j];
                    nbttagcompound1.put("block_states", (Tag) Branch_120_6_ChunkRegionLoader.BLOCK_STATE_CODEC
                            .encodeStart(NbtOps.INSTANCE, chunksection.getStates()).getOrThrow());
                    nbttagcompound1.put("biomes", (Tag) codec.encodeStart(NbtOps.INSTANCE, chunksection.getBiomes()).getOrThrow());
                }

                if (blockNibble != null) {
                    if (blockNibble.data != null) {
                        nbttagcompound1.putByteArray("BlockLight", blockNibble.data);
                    }

                    nbttagcompound1.putInt("starlight.blocklight_state", blockNibble.state);
                }

                if (skyNibble != null) {
                    if (skyNibble.data != null) {
                        nbttagcompound1.putByteArray("SkyLight", skyNibble.data);
                    }

                    nbttagcompound1.putInt("starlight.skylight_state", skyNibble.state);
                }

                if (!nbttagcompound1.isEmpty()) {
                    nbttagcompound1.putByte("Y", (byte) i);
                    nbttaglist.add(nbttagcompound1);
                }
            }
        }

        nbttagcompound.put("sections", nbttaglist);
        if (flag) {
            nbttagcompound.putInt("starlight.light_version", 9);
            nbttagcompound.putBoolean("isLightOn", false);
        }

        ListTag nbttaglist1;
        Iterator iterator;
        nbttaglist1 = new ListTag();
        iterator = chunk.getBlockEntitiesPos().iterator();

        CompoundTag nbttagcompound2;
        while (iterator.hasNext()) {
            final BlockPos blockposition = (BlockPos) iterator.next();
            nbttagcompound2 = chunk.getBlockEntityNbtForSaving(blockposition, world.registryAccess());
            if (nbttagcompound2 != null) {
                nbttaglist1.add(nbttagcompound2);
            }
        }

        nbttagcompound.put("block_entities", nbttaglist1);
        if (chunk.getStatus().getChunkType() == ChunkType.PROTOCHUNK) {
            final ProtoChunk protochunk = (ProtoChunk) chunk;
            final ListTag nbttaglist2 = new ListTag();
            nbttaglist2.addAll(protochunk.getEntities());
            nbttagcompound.put("entities", nbttaglist2);
            nbttagcompound2 = new CompoundTag();
            final GenerationStep.Carving[] aworldgenstage_features = Carving.values();
            final int k = aworldgenstage_features.length;

            for (int l = 0; l < k; ++l) {
                final GenerationStep.Carving worldgenstage_features = aworldgenstage_features[l];
                final CarvingMask carvingmask = protochunk.getCarvingMask(worldgenstage_features);
                if (carvingmask != null) {
                    nbttagcompound2.putLongArray(worldgenstage_features.toString(), carvingmask.toArray());
                }
            }

            nbttagcompound.put("CarvingMasks", nbttagcompound2);
        }

        Branch_120_6_ChunkRegionLoader.saveTicks(world, nbttagcompound, chunk.getTicksForSerialization());

        nbttagcompound.put("PostProcessing", Branch_120_6_ChunkRegionLoader.packOffsets(chunk.getPostProcessing()));
        final CompoundTag nbttagcompound3 = new CompoundTag();
        final Iterator iterator1 = chunk.getHeightmaps().iterator();

        while (iterator1.hasNext()) {
            final Map.Entry<Heightmap.Types, Heightmap> entry = (Map.Entry) iterator1.next();
            if (chunk.getStatus().heightmapsAfter().contains(entry.getKey())) {
                nbttagcompound3.put(((Heightmap.Types) entry.getKey()).getSerializationKey(),
                        new LongArrayTag(((Heightmap) entry.getValue()).getRawData()));
            }
        }

        nbttagcompound.put("Heightmaps", nbttagcompound3);
        nbttagcompound.put("structures", Branch_120_6_ChunkRegionLoader.packStructureData(
                StructurePieceSerializationContext.fromLevel(world), chunkcoordintpair, chunk.getAllStarts(), chunk.getAllReferences()));
        if (!chunk.persistentDataContainer.isEmpty()) {
            nbttagcompound.put("ChunkBukkitValues", chunk.persistentDataContainer.toTagCompound());
        }

        return nbttagcompound;
    }

    private static CompoundTag packStructureData(final StructurePieceSerializationContext context, final ChunkPos pos,
            final Map<Structure, StructureStart> starts, final Map<Structure, LongSet> references) {
        final CompoundTag nbttagcompound = new CompoundTag();
        final CompoundTag nbttagcompound1 = new CompoundTag();
        final Registry<Structure> iregistry = context.registryAccess().registryOrThrow(Registries.STRUCTURE);
        final Iterator iterator = starts.entrySet().iterator();

        while (iterator.hasNext()) {
            final Map.Entry<Structure, StructureStart> entry = (Map.Entry) iterator.next();
            final ResourceLocation minecraftkey = iregistry.getKey((Structure) entry.getKey());
            nbttagcompound1.put(minecraftkey.toString(), ((StructureStart) entry.getValue()).createTag(context, pos));
        }

        nbttagcompound.put("starts", nbttagcompound1);
        final CompoundTag nbttagcompound2 = new CompoundTag();
        final Iterator iterator1 = references.entrySet().iterator();

        while (iterator1.hasNext()) {
            final Map.Entry<Structure, LongSet> entry1 = (Map.Entry) iterator1.next();
            if (!((LongSet) entry1.getValue()).isEmpty()) {
                final ResourceLocation minecraftkey1 = iregistry.getKey((Structure) entry1.getKey());
                nbttagcompound2.put(minecraftkey1.toString(), new LongArrayTag((LongSet) entry1.getValue()));
            }
        }

        nbttagcompound.put("References", nbttagcompound2);
        return nbttagcompound;
    }

    private static Map<Structure, StructureStart> unpackStructureStart(final StructurePieceSerializationContext context,
            final CompoundTag nbt, final long worldSeed) {
        final Map<Structure, StructureStart> map = Maps.newHashMap();
        final Registry<Structure> iregistry = context.registryAccess().registryOrThrow(Registries.STRUCTURE);
        final CompoundTag nbttagcompound1 = nbt.getCompound("starts");
        final Iterator iterator = nbttagcompound1.getAllKeys().iterator();

        while (iterator.hasNext()) {
            final String s = (String) iterator.next();
            final ResourceLocation minecraftkey = ResourceLocation.tryParse(s);
            final Structure structure = (Structure) iregistry.get(minecraftkey);
            if (structure == null) {
            } else {
                final StructureStart structurestart = StructureStart.loadStaticStart(context, nbttagcompound1.getCompound(s), worldSeed);
                if (structurestart != null) {
                    final Tag persistentBase = nbttagcompound1.getCompound(s).get("StructureBukkitValues");
                    if (persistentBase instanceof CompoundTag) {
                        structurestart.persistentDataContainer.putAll((CompoundTag) persistentBase);
                    }

                    map.put(structure, structurestart);
                }
            }
        }

        return map;
    }

    private static Map<Structure, LongSet> unpackStructureReferences(final RegistryAccess registryManager, final ChunkPos pos,
            final CompoundTag nbt) {
        final Map<Structure, LongSet> map = Maps.newHashMap();
        final Registry<Structure> iregistry = registryManager.registryOrThrow(Registries.STRUCTURE);
        final CompoundTag nbttagcompound1 = nbt.getCompound("References");
        final Iterator iterator = nbttagcompound1.getAllKeys().iterator();

        while (iterator.hasNext()) {
            final String s = (String) iterator.next();
            final ResourceLocation minecraftkey = ResourceLocation.tryParse(s);
            final Structure structure = (Structure) iregistry.get(minecraftkey);
            if (structure == null) {

            } else {
                final long[] along = nbttagcompound1.getLongArray(s);
                if (along.length != 0) {
                    map.put(structure, new LongOpenHashSet(Arrays.stream(along).filter(i -> {
                        final ChunkPos chunkcoordintpair1 = new ChunkPos(i);
                        if (chunkcoordintpair1.getChessboardDistance(pos) > 8) {

                            return false;
                        } else {
                            return true;
                        }
                    }).toArray()));
                }
            }
        }

        return map;
    }

    private static void saveTicks(final ServerLevel world, final CompoundTag nbt, final ChunkAccess.TicksToSave tickSchedulers) {
        final long i = world.getLevelData().getGameTime();
        nbt.put("block_ticks", tickSchedulers.blocks().save(i, block -> BuiltInRegistries.BLOCK.getKey(block).toString()));
        nbt.put("fluid_ticks", tickSchedulers.fluids().save(i, fluidtype -> BuiltInRegistries.FLUID.getKey(fluidtype).toString()));
    }

    public static CompoundTag saveChunk(final ServerLevel world, final ChunkAccess chunk, final Branch_120_6_ChunkLight light,
            final List<Runnable> asyncRunnable) {

        final CompoundTag finalnbt = Branch_120_6_ChunkRegionLoader.saveChunk(world, chunk, null);
        SaveUtil.saveLightHook(world, (ChunkAccess) chunk, finalnbt);

        return finalnbt;
    }

    public static CompoundTag saveChunkOld(final ServerLevel world, final ChunkAccess chunk, final Branch_120_6_ChunkLight light,
            final List<Runnable> asyncRunnable) {
        final int minSection = world.getMinSection() - 1; // WorldUtil.getMinLightSection();
        final SWMRNibbleArray[] blockNibbles = chunk.getBlockNibbles();
        final SWMRNibbleArray[] skyNibbles = chunk.getSkyNibbles();
        final ChunkPos chunkPos = chunk.getPos();
        final CompoundTag nbt = NbtUtils.addCurrentDataVersion(new CompoundTag());

        nbt.putInt("xPos", chunkPos.x);
        nbt.putInt("yPos", chunk.getMinSection());
        nbt.putInt("zPos", chunkPos.z);
        nbt.putLong("LastUpdate", world.getGameTime());
        nbt.putLong("InhabitedTime", chunk.getInhabitedTime());
        nbt.putString("Status", chunk.getStatus().toString());
        final BlendingData blendingData = chunk.getBlendingData();
        if (blendingData != null) {
            BlendingData.CODEC.encodeStart(NbtOps.INSTANCE, blendingData).resultOrPartial(sx -> {
            }).ifPresent(nbtData -> nbt.put("blending_data", nbtData));
        }

        final BelowZeroRetrogen belowZeroRetrogen = chunk.getBelowZeroRetrogen();
        if (belowZeroRetrogen != null) {
            BelowZeroRetrogen.CODEC.encodeStart(NbtOps.INSTANCE, belowZeroRetrogen).resultOrPartial(sx -> {
            }).ifPresent(nbtData -> nbt.put("below_zero_retrogen", nbtData));
        }

        final LevelChunkSection[] chunkSections = chunk.getSections();
        final ListTag sectionArrayNBT = new ListTag();
        final ThreadedLevelLightEngine lightEngine = world.getChunkSource().getLightEngine();

        // 生態解析器
        final Registry<Biome> biomeRegistry = world.registryAccess().registryOrThrow(Registries.BIOME);
        final Codec<PalettedContainerRO<Holder<Biome>>> paletteCodec = Branch_120_6_ChunkRegionLoader.makeBiomeCodec(biomeRegistry);
        boolean lightCorrect = false;

        for (int locationY = lightEngine.getMinLightSection(); locationY < lightEngine.getMaxLightSection(); ++locationY) {
            final int sectionY = chunk.getSectionIndexFromSectionY(locationY);
            final boolean inSections = sectionY >= 0 && sectionY < chunkSections.length;
            final ThreadedLevelLightEngine lightEngineThreaded = world.getChunkSource().getLightEngine();
            Object blockNibble;
            Object skyNibble;
            try {
                /*
                 * // 適用於 paper blockNibble = chunk.getBlockNibbles()[locationY -
                 * minSection].toVanillaNibble(); skyNibble = chunk.getSkyNibbles()[locationY -
                 * minSection].toVanillaNibble();
                 */
                blockNibble = blockNibbles[locationY - minSection].getSaveState();
                skyNibble = skyNibbles[locationY - minSection].getSaveState();

            } catch (final NoSuchMethodError noSuchMethodError) {
                // 適用於 spigot (不推薦)
                blockNibble = lightEngineThreaded.getLayerListener(LightLayer.BLOCK).getDataLayerData(SectionPos.of(chunkPos, locationY));
                skyNibble = lightEngineThreaded.getLayerListener(LightLayer.SKY).getDataLayerData(SectionPos.of(chunkPos, locationY));
            }

            if (inSections || blockNibble != null || skyNibble != null) {
                final CompoundTag sectionNBT = new CompoundTag();
                if (inSections) {
                    final LevelChunkSection chunkSection = chunkSections[sectionY];
                    asyncRunnable.add(() -> {
                        sectionNBT.put("block_states", ChunkSerializer.BLOCK_STATE_CODEC
                                .encodeStart(NbtOps.INSTANCE, chunkSection.getStates()).getOrThrow(Branch_120_6_NothingException::new));
                        sectionNBT.put("biomes", paletteCodec.encodeStart(NbtOps.INSTANCE, chunkSection.getBiomes())
                                .getOrThrow(Branch_120_6_NothingException::new));
                    });
                }

                if (blockNibble != null && blockNibble instanceof final SWMRNibbleArray.SaveState bnib) {
                    if (bnib.data != null) {
                        if (light != null) {
                            light.setBlockLight(locationY, bnib.data);
                        } else {
                            sectionNBT.putByteArray("BlockLight", bnib.data);
                            lightCorrect = true;
                        }
                    }
                    sectionNBT.putInt("starlight.blocklight_state", bnib.state);

                }

                if (skyNibble != null && skyNibble instanceof final SWMRNibbleArray.SaveState skinb) {
                    if (light != null) {
                        light.setSkyLight(locationY, skinb.data);
                    } else {
                        sectionNBT.putByteArray("SkyLight", skinb.data);
                        lightCorrect = true;
                    }
                    sectionNBT.putInt("starlight.skylight_state", skinb.state);

                }

                // 增加 inSections 確保 asyncRunnable 不會出資料錯誤
                if (!sectionNBT.isEmpty() || inSections) {
                    sectionNBT.putByte("Y", (byte) locationY);
                    sectionArrayNBT.add(sectionNBT);
                }
            }
        }
        nbt.put("sections", sectionArrayNBT);

        if (lightCorrect) {
            nbt.putInt("starlight.light_version", 9);
            nbt.putBoolean("isLightOn", true);
        }

        // 實體方塊
        final ListTag blockEntitiesNBT = new ListTag();
        for (final BlockPos blockPos : chunk.getBlockEntitiesPos()) {
            final CompoundTag blockEntity = chunk.getBlockEntityNbtForSaving(blockPos, null);
            if (blockEntity != null) {
                blockEntitiesNBT.add(blockEntity);
            }
        }
        nbt.put("block_entities", blockEntitiesNBT);

        if (chunk.getStatus().getChunkType() == ChunkType.PROTOCHUNK) {
        }

        final ChunkAccess.TicksToSave tickSchedulers = chunk.getTicksForSerialization();
        final long gameTime = world.getLevelData().getGameTime();
        nbt.put("block_ticks", tickSchedulers.blocks().save(gameTime, block -> BuiltInRegistries.BLOCK.getKey(block).toString()));
        nbt.put("fluid_ticks", tickSchedulers.fluids().save(gameTime, fluid -> BuiltInRegistries.FLUID.getKey(fluid).toString()));

        final ShortList[] packOffsetList = chunk.getPostProcessing();
        final ListTag packOffsetsNBT = new ListTag();
        for (final ShortList shortlist : packOffsetList) {
            final ListTag packsNBT = new ListTag();
            if (shortlist != null) {
                for (final Short shortData : shortlist) {
                    packsNBT.add(ShortTag.valueOf(shortData));
                }
            }
            packOffsetsNBT.add(packsNBT);
        }
        nbt.put("PostProcessing", packOffsetsNBT);

        // 高度圖
        final CompoundTag heightmapsNBT = new CompoundTag();
        for (final Map.Entry<Heightmap.Types, Heightmap> entry : chunk.getHeightmaps()) {
            if (chunk.getStatus().heightmapsAfter().contains(entry.getKey())) {
                heightmapsNBT.put(entry.getKey().getSerializationKey(), new LongArrayTag(entry.getValue().getRawData()));
            }
        }
        nbt.put("Heightmaps", heightmapsNBT);

        return nbt;
    }

    public static ListTag packOffsets(final ShortList[] lists) {
        final ListTag nbttaglist = new ListTag();
        final ShortList[] ashortlist1 = lists;
        final int i = lists.length;

        for (int j = 0; j < i; ++j) {
            final ShortList shortlist = ashortlist1[j];
            final ListTag nbttaglist1 = new ListTag();
            if (shortlist != null) {
                final ShortListIterator shortlistiterator = shortlist.iterator();

                while (shortlistiterator.hasNext()) {
                    final Short oshort = shortlistiterator.next();
                    nbttaglist1.add(ShortTag.valueOf(oshort));
                }
            }

            nbttaglist.add(nbttaglist1);
        }

        return nbttaglist;
    }

}
