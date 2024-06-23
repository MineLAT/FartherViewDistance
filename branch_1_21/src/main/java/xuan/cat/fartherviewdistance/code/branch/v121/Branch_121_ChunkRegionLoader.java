package xuan.cat.fartherviewdistance.code.branch.v121;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;

import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;

import ca.spottedleaf.moonrise.patches.starlight.light.SWMRNibbleArray;
import ca.spottedleaf.moonrise.patches.starlight.light.StarLightEngine;
import ca.spottedleaf.moonrise.patches.starlight.util.SaveUtil;
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
import net.minecraft.world.level.chunk.DataLayer;
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
public final class Branch_121_ChunkRegionLoader {

    private static final int CURRENT_DATA_VERSION = SharedConstants.getCurrentVersion().getDataVersion().getVersion();
    private static final boolean JUST_CORRUPT_IT = Boolean.getBoolean("Paper.ignoreWorldDataVersion");

    public static BranchChunk.Status loadStatus(final CompoundTag nbt) {
        return Branch_121_Chunk.ofStatus(ChunkStatus.byName(nbt.getString("Status")));
    }

    private static Codec<PalettedContainerRO<Holder<Biome>>> makeBiomeCodec(final Registry<Biome> biomeRegistry) {
        return PalettedContainer.codecRO(biomeRegistry.asHolderIdMap(), biomeRegistry.holderByNameCodec(),
                PalettedContainer.Strategy.SECTION_BIOMES, biomeRegistry.getHolderOrThrow(Biomes.PLAINS));
    }

    private static Method method_ChunkSerializer_makeBiomeCodecRW;

    static {
        try {
            Branch_121_ChunkRegionLoader.method_ChunkSerializer_makeBiomeCodecRW = ChunkSerializer.class
                    .getDeclaredMethod("makeBiomeCodecRW", Registry.class);
            Branch_121_ChunkRegionLoader.method_ChunkSerializer_makeBiomeCodecRW.setAccessible(true);
        } catch (final NoSuchMethodException ex) {
            ex.printStackTrace();
        }
    }
    private static Field DimensionDataStorage_Provider;

    static {
        try {
            Branch_121_ChunkRegionLoader.DimensionDataStorage_Provider = DimensionDataStorage.class.getDeclaredField("registries");
            Branch_121_ChunkRegionLoader.DimensionDataStorage_Provider.setAccessible(true);
        } catch (final NoSuchFieldException ex) {
            ex.printStackTrace();
        }
    }

    private static Codec<PalettedContainer<Holder<Biome>>> makeBiomeCodecRW(final Registry<Biome> biomeRegistry) {
        try {
            return (Codec<PalettedContainer<Holder<Biome>>>) Branch_121_ChunkRegionLoader.method_ChunkSerializer_makeBiomeCodecRW
                    .invoke(null, biomeRegistry);
        } catch (InvocationTargetException | IllegalAccessException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static BranchChunk loadChunk(final ServerLevel world, final int chunkX, final int chunkZ, final CompoundTag nbt,
            final boolean integralHeightmap) {
        if (nbt.contains("DataVersion", 99)) {
            final int dataVersion = nbt.getInt("DataVersion");
            if (!Branch_121_ChunkRegionLoader.JUST_CORRUPT_IT && dataVersion > Branch_121_ChunkRegionLoader.CURRENT_DATA_VERSION) {
                (new RuntimeException("Server attempted to load chunk saved with newer version of minecraft! " + dataVersion + " > "
                        + Branch_121_ChunkRegionLoader.CURRENT_DATA_VERSION)).printStackTrace();
                System.exit(1);
            }
        }
        final ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
        final UpgradeData upgradeData = nbt.contains("UpgradeData", 10) ? new UpgradeData(nbt.getCompound("UpgradeData"), world)
                : UpgradeData.EMPTY;
        /*
         * final boolean isLightOn =
         * Objects.requireNonNullElse(ChunkStatus.byName(nbt.getString("Status")),
         * ChunkStatus.EMPTY) .isOrAfter(ChunkStatus.LIGHT) && (nbt.get("isLightOn") !=
         * null || nbt.getInt("starlight.light_version") == 6);
         */
        final boolean isLightOn = nbt.getBoolean("isLightOn");
        final ListTag sectionArrayNBT = nbt.getList("sections", 10);
        final int sectionsCount = world.getSectionsCount();
        final LevelChunkSection[] sections = new LevelChunkSection[sectionsCount];
        final ServerChunkCache chunkSource = world.getChunkSource();
        final LevelLightEngine lightEngine = chunkSource.getLightEngine();

        final SWMRNibbleArray[] blockNibbles = StarLightEngine.getFilledEmptyLight(world);
        final SWMRNibbleArray[] skyNibbles = StarLightEngine.getFilledEmptyLight(world);
        final int minSection = WorldUtil.getMinLightSection(world);

        final Registry<Biome> biomeRegistry = world.registryAccess().registryOrThrow(Registries.BIOME);
        final Codec<PalettedContainer<Holder<Biome>>> paletteCodec = Branch_121_ChunkRegionLoader.makeBiomeCodecRW(biomeRegistry);

        boolean flag2 = false;

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
                            }).getOrThrow(Branch_121_NothingException::new);
                } else {
                    paletteBlock = new PalettedContainer<>(Block.BLOCK_STATE_REGISTRY, Blocks.AIR.defaultBlockState(),
                            PalettedContainer.Strategy.SECTION_STATES,
                            world.chunkPacketBlockController.getPresetBlockStates(world, chunkPos, locationY));
                }

                PalettedContainer<Holder<Biome>> paletteBiome;
                if (sectionNBT.contains("biomes", 10)) {
                    paletteBiome = paletteCodec.parse(NbtOps.INSTANCE, sectionNBT.getCompound("biomes")).promotePartial(sx -> {
                    }).getOrThrow(Branch_121_NothingException::new);
                } else {
                    paletteBiome = new PalettedContainer<>(biomeRegistry.asHolderIdMap(), biomeRegistry.getHolderOrThrow(Biomes.PLAINS),
                            PalettedContainer.Strategy.SECTION_BIOMES, null);

                }

                final LevelChunkSection chunkSection = new LevelChunkSection(paletteBlock, paletteBiome);
                sections[sectionY] = chunkSection;
                SectionPos.of(chunkPos, locationY);
            }

            final boolean isBlockLight = sectionNBT.contains("BlockLight", 7);
            final boolean isSkyLight = isLightOn && sectionNBT.contains("SkyLight", 7);

            if (isBlockLight || isSkyLight) {
                if (!flag2) {
                    lightEngine.retainData(chunkPos, true);
                    flag2 = true;
                }

                if (isBlockLight) {
                    lightEngine.queueSectionData(LightLayer.BLOCK, SectionPos.of(chunkPos, locationY),
                            new DataLayer(sectionNBT.getByteArray("BlockLight")));
                }

                if (isSkyLight) {
                    lightEngine.queueSectionData(LightLayer.SKY, SectionPos.of(chunkPos, locationY),
                            new DataLayer(sectionNBT.getByteArray("SkyLight")));
                }
            }

        }

        final long inhabitedTime = nbt.getLong("InhabitedTime");
        final ChunkType chunkType = ChunkSerializer.getChunkTypeFromTag(nbt);
        BlendingData blendingData;
        if (nbt.contains("blending_data", 10)) {
            blendingData = BlendingData.CODEC.parse(new Dynamic<>(NbtOps.INSTANCE, nbt.getCompound("blending_data")))
                    .resultOrPartial(sx -> {
                    }).orElse(null);
        } else {
            blendingData = null;
        }

        ChunkAccess chunk;
        if (chunkType == ChunkType.LEVELCHUNK) {
            final LevelChunkTicks<Block> ticksBlock = LevelChunkTicks.load(nbt.getList("block_ticks", 10),
                    sx -> BuiltInRegistries.BLOCK.getOptional(ResourceLocation.tryParse(sx)), chunkPos);
            final LevelChunkTicks<Fluid> ticksFluid = LevelChunkTicks.load(nbt.getList("fluid_ticks", 10),
                    sx -> BuiltInRegistries.FLUID.getOptional(ResourceLocation.tryParse(sx)), chunkPos);
            final LevelChunk levelChunk = new LevelChunk(world.getLevel(), chunkPos, upgradeData, ticksBlock, ticksFluid, inhabitedTime,
                    sections, Branch_121_ChunkRegionLoader.postLoadChunk(world, nbt), blendingData);
            chunk = levelChunk;

        } else {
            final ProtoChunkTicks<Block> ticksBlock = ProtoChunkTicks.load(nbt.getList("block_ticks", 10),
                    sx -> BuiltInRegistries.BLOCK.getOptional(ResourceLocation.tryParse(sx)), chunkPos);
            final ProtoChunkTicks<Fluid> ticksFluid = ProtoChunkTicks.load(nbt.getList("fluid_ticks", 10),
                    sx -> BuiltInRegistries.FLUID.getOptional(ResourceLocation.tryParse(sx)), chunkPos);
            final ProtoChunk protochunk = new ProtoChunk(chunkPos, upgradeData, sections, ticksBlock, ticksFluid, world, biomeRegistry,
                    blendingData);

            chunk = protochunk;
            protochunk.setInhabitedTime(inhabitedTime);
            if (nbt.contains("below_zero_retrogen", 10)) {
                BelowZeroRetrogen.CODEC.parse(new Dynamic<>(NbtOps.INSTANCE, nbt.getCompound("below_zero_retrogen")))
                        .resultOrPartial(sx -> {
                        }).ifPresent(protochunk::setBelowZeroRetrogen);
            }

            final ChunkStatus chunkStatus = ChunkStatus.byName(nbt.getString("Status"));
            protochunk.setPersistedStatus(chunkStatus);
            if (chunkStatus.isOrAfter(ChunkStatus.INITIALIZE_LIGHT)) {
                protochunk.setLightEngine(lightEngine);
            }
        }

        final Tag persistentBase = nbt.get("ChunkBukkitValues");
        if (persistentBase instanceof CompoundTag) {
            ((ChunkAccess) chunk).persistentDataContainer.putAll((CompoundTag) persistentBase);
        }

        chunk.setLightCorrect(isLightOn);

        final CompoundTag heightmapsNBT = nbt.getCompound("Heightmaps");
        final EnumSet<Heightmap.Types> enumHeightmapType = EnumSet.noneOf(Heightmap.Types.class);
        final Iterator iterator = ((ChunkAccess) chunk).getPersistedStatus().heightmapsAfter().iterator();

        while (iterator.hasNext()) {
            final Heightmap.Types heightmap_type = (Heightmap.Types) iterator.next();
            final String s = heightmap_type.getSerializationKey();
            if (heightmapsNBT.contains(s, 12)) {
                ((ChunkAccess) chunk).setHeightmap(heightmap_type, heightmapsNBT.getLongArray(s));
            } else {
                enumHeightmapType.add(heightmap_type);
            }
        }

        Heightmap.primeHeightmaps(chunk, enumHeightmapType);

        final CompoundTag nbttagcompound3 = nbt.getCompound("structures");
        ((ChunkAccess) chunk).setAllStarts(Branch_121_ChunkRegionLoader
                .unpackStructureStart(StructurePieceSerializationContext.fromLevel(world), nbttagcompound3, world.getSeed()));
        ((ChunkAccess) chunk).setAllReferences(
                Branch_121_ChunkRegionLoader.unpackStructureReferences(world.registryAccess(), chunkPos, nbttagcompound3));
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

        SaveUtil.loadLightHook(world, chunkPos, nbt, (ChunkAccess) chunk);

        if (chunkType == ChunkType.LEVELCHUNK) {
            return new Branch_121_Chunk(world, (LevelChunk) chunk);
        } else {
            final ProtoChunk protoChunk = (ProtoChunk) chunk;
            return new Branch_121_Chunk(world, new LevelChunk(world, protoChunk, v -> {
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
    private static ListTag getListOfCompoundsOrNull(final CompoundTag nbt, final String key) {
        final ListTag nbttaglist = nbt.getList(key, 10);
        return nbttaglist.isEmpty() ? null : nbttaglist;
    }

    @Nullable
    private static LevelChunk.PostLoadProcessor postLoadChunk(final ServerLevel world, final CompoundTag nbt) {
        final ListTag nbttaglist = Branch_121_ChunkRegionLoader.getListOfCompoundsOrNull(nbt, "entities");
        final ListTag nbttaglist1 = Branch_121_ChunkRegionLoader.getListOfCompoundsOrNull(nbt, "block_entities");
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
                    }
                }
            }

        };
    }

    public static BranchChunkLight loadLight(final ServerLevel world, final CompoundTag nbt) {
        // 檢查資料版本
        if (nbt.contains("DataVersion", 99)) {
            final int dataVersion = nbt.getInt("DataVersion");
            if (!Branch_121_ChunkRegionLoader.JUST_CORRUPT_IT && dataVersion > Branch_121_ChunkRegionLoader.CURRENT_DATA_VERSION) {
                (new RuntimeException("Server attempted to load chunk saved with newer version of minecraft! " + dataVersion + " > "
                        + Branch_121_ChunkRegionLoader.CURRENT_DATA_VERSION)).printStackTrace();
                System.exit(1);
            }
        }

        final boolean isLightOn = Objects.requireNonNullElse(ChunkStatus.byName(nbt.getString("Status")), ChunkStatus.EMPTY)
                .isOrAfter(ChunkStatus.LIGHT) && (nbt.get("isLightOn") != null && nbt.getInt("starlight.light_version") == 9);
        final boolean hasSkyLight = world.dimensionType().hasSkyLight();
        final ListTag sectionArrayNBT = nbt.getList("sections", 10);
        final Branch_121_ChunkLight chunkLight = new Branch_121_ChunkLight(world);
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

    public static CompoundTag saveChunk(final ServerLevel world, final ChunkAccess chunk, final Branch_121_ChunkLight light,
            final List<Runnable> asyncRunnable) {
        final ChunkPos chunkPos = chunk.getPos();
        final CompoundTag nbt = NbtUtils.addCurrentDataVersion(new CompoundTag());

        nbt.putInt("xPos", chunkPos.x);
        nbt.putInt("yPos", chunk.getMinSection());
        nbt.putInt("zPos", chunkPos.z);
        nbt.putLong("LastUpdate", world.getGameTime());
        nbt.putLong("InhabitedTime", chunk.getInhabitedTime());
        nbt.putString("Status", BuiltInRegistries.CHUNK_STATUS.getKey(chunk.getPersistedStatus()).toString());
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

        final UpgradeData chunkconverter = chunk.getUpgradeData();
        if (!chunkconverter.isEmpty()) {
            nbt.put("UpgradeData", chunkconverter.write());
        }

        final LevelChunkSection[] chunkSections = chunk.getSections();
        final ListTag sectionArrayNBT = new ListTag();
        final ThreadedLevelLightEngine lightEngine = world.getChunkSource().getLightEngine();
        final Registry<Biome> biomeRegistry = world.registryAccess().registryOrThrow(Registries.BIOME);
        final Codec<PalettedContainerRO<Holder<Biome>>> paletteCodec = Branch_121_ChunkRegionLoader.makeBiomeCodec(biomeRegistry);
        final boolean lightCorrect = false;

        for (int locationY = lightEngine.getMinLightSection(); locationY < lightEngine.getMaxLightSection(); ++locationY) {
            final int sectionY = chunk.getSectionIndexFromSectionY(locationY);
            final boolean inSections = sectionY >= 0 && sectionY < chunkSections.length;
            final ThreadedLevelLightEngine lightEngineThreaded = world.getChunkSource().getLightEngine();

            final DataLayer nibblearray = lightEngineThreaded.getLayerListener(LightLayer.BLOCK)
                    .getDataLayerData(SectionPos.of(chunkPos, locationY));
            final DataLayer nibblearray1 = lightEngineThreaded.getLayerListener(LightLayer.SKY)
                    .getDataLayerData(SectionPos.of(chunkPos, locationY));

            if (inSections || nibblearray != null || nibblearray1 != null) {
                final CompoundTag nbttagcompound1 = new CompoundTag();
                if (inSections) {
                    final LevelChunkSection chunksection = chunkSections[sectionY];
                    nbttagcompound1.put("block_states",
                            (Tag) ChunkSerializer.BLOCK_STATE_CODEC.encodeStart(NbtOps.INSTANCE, chunksection.getStates()).getOrThrow());
                    nbttagcompound1.put("biomes", (Tag) paletteCodec.encodeStart(NbtOps.INSTANCE, chunksection.getBiomes()).getOrThrow());
                }

                if (nibblearray != null && !nibblearray.isEmpty()) {
                    nbttagcompound1.putByteArray("BlockLight", nibblearray.getData());
                }

                if (nibblearray1 != null && !nibblearray1.isEmpty()) {
                    nbttagcompound1.putByteArray("SkyLight", nibblearray1.getData());
                }

                if (!nbttagcompound1.isEmpty()) {
                    nbttagcompound1.putByte("Y", (byte) locationY);
                    sectionArrayNBT.add(nbttagcompound1);
                }
            }

        }
        nbt.put("sections", sectionArrayNBT);

        if (lightCorrect) {
            nbt.putBoolean("isLightOn", true);
        }

        final ListTag blockEntitiesNBT = new ListTag();
        final Iterator iterator = chunk.getBlockEntitiesPos().iterator();

        CompoundTag blockEntity;
        while (iterator.hasNext()) {
            final BlockPos blockposition = (BlockPos) iterator.next();
            blockEntity = chunk.getBlockEntityNbtForSaving(blockposition, world.registryAccess());
            if (blockEntity != null) {
                blockEntitiesNBT.add(blockEntity);
            }
        }

        nbt.put("block_entities", blockEntitiesNBT);

        if (chunk.getPersistedStatus().getChunkType() == ChunkType.PROTOCHUNK) {
            final ProtoChunk protochunk = (ProtoChunk) chunk;
            final ListTag nbttaglist2 = new ListTag();
            nbttaglist2.addAll(protochunk.getEntities());
            nbt.put("entities", nbttaglist2);
            blockEntity = new CompoundTag();
            final GenerationStep.Carving[] aworldgenstage_features = Carving.values();
            final int k = aworldgenstage_features.length;

            for (int l = 0; l < k; ++l) {
                final GenerationStep.Carving worldgenstage_features = aworldgenstage_features[l];
                final CarvingMask carvingmask = protochunk.getCarvingMask(worldgenstage_features);
                if (carvingmask != null) {
                    blockEntity.putLongArray(worldgenstage_features.toString(), carvingmask.toArray());
                }
            }

            nbt.put("CarvingMasks", blockEntity);
        }

        Branch_121_ChunkRegionLoader.saveTicks(world, nbt, chunk.getTicksForSerialization());

        nbt.put("PostProcessing", Branch_121_ChunkRegionLoader.packOffsets(chunk.getPostProcessing()));
        final CompoundTag nbttagcompound3 = new CompoundTag();
        final Iterator iterator1 = chunk.getHeightmaps().iterator();

        while (iterator1.hasNext()) {
            final Map.Entry<Heightmap.Types, Heightmap> entry = (Map.Entry) iterator1.next();
            if (chunk.getPersistedStatus().heightmapsAfter().contains(entry.getKey())) {
                nbttagcompound3.put(((Heightmap.Types) entry.getKey()).getSerializationKey(),
                        new LongArrayTag(((Heightmap) entry.getValue()).getRawData()));
            }
        }

        nbt.put("Heightmaps", nbttagcompound3);
        nbt.put("structures", Branch_121_ChunkRegionLoader.packStructureData(StructurePieceSerializationContext.fromLevel(world), chunkPos,
                chunk.getAllStarts(), chunk.getAllReferences()));
        if (!chunk.persistentDataContainer.isEmpty()) {
            nbt.put("ChunkBukkitValues", chunk.persistentDataContainer.toTagCompound());
        }

        SaveUtil.saveLightHook(world, chunk, nbt);

        return nbt;
    }

    private static void saveTicks(final ServerLevel world, final CompoundTag nbt, final ChunkAccess.TicksToSave tickSchedulers) {
        final long i = world.getLevelData().getGameTime();
        nbt.put("block_ticks", tickSchedulers.blocks().save(i, block -> BuiltInRegistries.BLOCK.getKey(block).toString()));
        nbt.put("fluid_ticks", tickSchedulers.fluids().save(i, fluidtype -> BuiltInRegistries.FLUID.getKey(fluidtype).toString()));
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
