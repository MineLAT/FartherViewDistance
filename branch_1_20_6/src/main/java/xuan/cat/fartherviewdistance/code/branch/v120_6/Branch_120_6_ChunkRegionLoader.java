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
import com.mojang.serialization.Dynamic;

import ca.spottedleaf.starlight.common.light.SWMRNibbleArray;
import ca.spottedleaf.starlight.common.light.StarLightEngine;
import io.papermc.paper.util.WorldUtil;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.shorts.ShortList;
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
import net.minecraft.world.level.levelgen.BelowZeroRetrogen;
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
        final int minSection = WorldUtil.getMinLightSection(world);
        final int maxSection = WorldUtil.getMaxLightSection(world);
        final boolean canReadSky = world.dimensionType().hasSkyLight();
        final Registry<Biome> biomeRegistry = world.registryAccess().registryOrThrow(Registries.BIOME);
        final Codec<PalettedContainer<Holder<Biome>>> paletteCodec = Branch_120_6_ChunkRegionLoader.makeBiomeCodecRW(biomeRegistry);

        final boolean flag2 = false;

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
        final ChunkType chunkType = ChunkSerializer.getChunkTypeFromTag(nbt);
        final BlendingData blendingData;
        if (nbt.contains("blending_data", 10)) {
            blendingData = BlendingData.CODEC.parse(new Dynamic<>(NbtOps.INSTANCE, nbt.getCompound("blending_data")))
                    .resultOrPartial(sx -> {
                    }).orElse(null);
        } else {
            blendingData = null;
        }

        final ChunkAccess chunk;
        if (chunkType == ChunkType.LEVELCHUNK) {
            final LevelChunkTicks<Block> ticksBlock = LevelChunkTicks.load(nbt.getList("block_ticks", 10),
                    sx -> BuiltInRegistries.BLOCK.getOptional(ResourceLocation.tryParse(sx)), chunkPos);
            final LevelChunkTicks<Fluid> ticksFluid = LevelChunkTicks.load(nbt.getList("fluid_ticks", 10),
                    sx -> BuiltInRegistries.FLUID.getOptional(ResourceLocation.tryParse(sx)), chunkPos);
            final LevelChunk levelChunk = new LevelChunk(world.getLevel(), chunkPos, upgradeData, ticksBlock, ticksFluid, inhabitedTime,
                    sections, Branch_120_6_ChunkRegionLoader.postLoadChunk(world, nbt), blendingData);
            chunk = levelChunk;
            chunk.setBlockNibbles(blockNibbles);
            chunk.setSkyNibbles(skyNibbles);

        } else {
            final ProtoChunkTicks<Block> ticksBlock = ProtoChunkTicks.load(nbt.getList("block_ticks", 10),
                    sx -> BuiltInRegistries.BLOCK.getOptional(ResourceLocation.tryParse(sx)), chunkPos);
            final ProtoChunkTicks<Fluid> ticksFluid = ProtoChunkTicks.load(nbt.getList("fluid_ticks", 10),
                    sx -> BuiltInRegistries.FLUID.getOptional(ResourceLocation.tryParse(sx)), chunkPos);
            final ProtoChunk protochunk = new ProtoChunk(chunkPos, upgradeData, sections, ticksBlock, ticksFluid, world, biomeRegistry,
                    blendingData);
            chunk = protochunk;
            chunk.setBlockNibbles(blockNibbles);
            chunk.setSkyNibbles(skyNibbles);
            chunk.setInhabitedTime(inhabitedTime);
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

        chunk.setLightCorrect(isLightOn);

        // 高度圖
        final CompoundTag heightmapsNBT = nbt.getCompound("Heightmaps");
        final EnumSet<Heightmap.Types> enumHeightmapType = EnumSet.noneOf(Heightmap.Types.class);
        for (final Heightmap.Types heightmapTypes : chunk.getStatus().heightmapsAfter()) {
            final String serializationKey = heightmapTypes.getSerializationKey();
            if (heightmapsNBT.contains(serializationKey, 12)) {
                chunk.setHeightmap(heightmapTypes, heightmapsNBT.getLongArray(serializationKey));
            } else {
                enumHeightmapType.add(heightmapTypes);
            }
        }
        Heightmap.primeHeightmaps(chunk, enumHeightmapType);

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
                chunk.addPackedPostProcess(processNBT.getShort(index), indexList);
            }
        }

        if (chunkType == ChunkType.LEVELCHUNK) {
            return new Branch_120_6_Chunk(world, (LevelChunk) chunk);
        } else {
            final ProtoChunk protoChunk = (ProtoChunk) chunk;
            return new Branch_120_6_Chunk(world, new LevelChunk(world, protoChunk, v -> {
            }));
        }
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
            if (structure != null) {
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
            if (structure != null) {
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

    public static CompoundTag saveChunk(final ServerLevel world, final ChunkAccess chunk, final Branch_120_6_ChunkLight light,
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
            nbt.putInt("starlight.light_version", 6);
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
}
