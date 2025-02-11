package xuan.cat.fartherviewdistance.module.server;

import ca.spottedleaf.moonrise.common.util.MixinWorkarounds;
import ca.spottedleaf.moonrise.common.util.WorldUtil;
import ca.spottedleaf.moonrise.patches.starlight.light.SWMRNibbleArray;
import ca.spottedleaf.moonrise.patches.starlight.light.StarLightEngine;
import ca.spottedleaf.moonrise.patches.starlight.util.SaveUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
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
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkAccess.PackedTicks;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunk.PostLoadProcessor;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainer.Strategy;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.status.ChunkType;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import net.minecraft.world.level.levelgen.BelowZeroRetrogen;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.levelgen.blending.BlendingData.Packed;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.ProtoChunkTicks;
import net.minecraft.world.ticks.SavedTick;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Copy of {@link SerializableChunkData} without error logging, since we don't need console spam on try-catch operations.
 */
public record MinecraftChunkData(
        Registry<Biome> biomeRegistry,
        ChunkPos chunkPos,
        int minSectionY,
        long lastUpdateTime,
        long inhabitedTime,
        ChunkStatus chunkStatus,
        @Nullable Packed blendingData,
        @Nullable BelowZeroRetrogen belowZeroRetrogen,
        UpgradeData upgradeData,
        @Nullable long[] carvingMask,
        Map<Types, long[]> heightmaps,
        PackedTicks packedTicks,
        ShortList[] postProcessingSections,
        boolean lightCorrect,
        List<SerializableChunkData.SectionData> sectionData,
        List<CompoundTag> entities,
        List<CompoundTag> blockEntities,
        CompoundTag structureData,
        @Nullable Tag persistentDataContainer
) {
    public static final Codec<PalettedContainer<BlockState>> BLOCK_STATE_CODEC = PalettedContainer.codecRW(
            Block.BLOCK_STATE_REGISTRY, BlockState.CODEC, Strategy.SECTION_STATES, Blocks.AIR.defaultBlockState(), null
    );
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TAG_UPGRADE_DATA = "UpgradeData";
    private static final String BLOCK_TICKS_TAG = "block_ticks";
    private static final String FLUID_TICKS_TAG = "fluid_ticks";
    public static final String X_POS_TAG = "xPos";
    public static final String Z_POS_TAG = "zPos";
    public static final String HEIGHTMAPS_TAG = "Heightmaps";
    public static final String IS_LIGHT_ON_TAG = "isLightOn";
    public static final String SECTIONS_TAG = "sections";
    public static final String BLOCK_LIGHT_TAG = "BlockLight";
    public static final String SKY_LIGHT_TAG = "SkyLight";
    private static final int CURRENT_DATA_VERSION = SharedConstants.getCurrentVersion().getDataVersion().getVersion();
    private static final boolean JUST_CORRUPT_IT = Boolean.getBoolean("Paper.ignoreWorldDataVersion");

    @Nullable
    public static MinecraftChunkData parse(LevelHeightAccessor levelHeightAccessor, RegistryAccess registries, CompoundTag tag) {
        ServerLevel serverLevel = (ServerLevel)levelHeightAccessor;
        if (!tag.contains("Status", Tag.TAG_STRING)) {
            return null;
        } else {
            if (tag.contains("DataVersion", Tag.TAG_ANY_NUMERIC)) {
                int dataVersion = tag.getInt("DataVersion");
                if (!JUST_CORRUPT_IT && dataVersion > CURRENT_DATA_VERSION) {
                    new RuntimeException("Server attempted to load chunk saved with newer version of minecraft! " + dataVersion + " > " + CURRENT_DATA_VERSION)
                            .printStackTrace();
                    System.exit(1);
                }
            }

            ChunkPos chunkPos = new ChunkPos(tag.getInt(X_POS_TAG), tag.getInt(Z_POS_TAG));
            long _long = tag.getLong("LastUpdate");
            long _long1 = tag.getLong("InhabitedTime");
            ChunkStatus chunkStatus = ChunkStatus.byName(tag.getString("Status"));
            UpgradeData upgradeData = tag.contains(TAG_UPGRADE_DATA, Tag.TAG_COMPOUND)
                    ? new UpgradeData(tag.getCompound(TAG_UPGRADE_DATA), levelHeightAccessor)
                    : UpgradeData.EMPTY;
            boolean _boolean = chunkStatus.isOrAfter(ChunkStatus.LIGHT) && tag.get(IS_LIGHT_ON_TAG) != null && tag.getInt(SaveUtil.STARLIGHT_VERSION_TAG) == SaveUtil.getLightVersion();
            Packed packed;
            if (tag.contains("blending_data", Tag.TAG_COMPOUND)) {
                packed = Packed.CODEC.parse(NbtOps.INSTANCE, tag.getCompound("blending_data")).resultOrPartial(LOGGER::error).orElse(null);
            } else {
                packed = null;
            }

            BelowZeroRetrogen belowZeroRetrogen;
            if (tag.contains("below_zero_retrogen", Tag.TAG_COMPOUND)) {
                belowZeroRetrogen = BelowZeroRetrogen.CODEC
                        .parse(NbtOps.INSTANCE, tag.getCompound("below_zero_retrogen"))
                        .resultOrPartial(LOGGER::error)
                        .orElse(null);
            } else {
                belowZeroRetrogen = null;
            }

            long[] longArray;
            if (tag.contains("carving_mask", Tag.TAG_LONG_ARRAY)) {
                longArray = tag.getLongArray("carving_mask");
            } else {
                longArray = null;
            }

            CompoundTag compound = tag.getCompound(HEIGHTMAPS_TAG);
            Map<Types, long[]> map = new EnumMap<>(Types.class);

            for (Types types : chunkStatus.heightmapsAfter()) {
                String serializationKey = types.getSerializationKey();
                if (compound.contains(serializationKey, Tag.TAG_LONG_ARRAY)) {
                    map.put(types, compound.getLongArray(serializationKey));
                }
            }

            List<SavedTick<Block>> list = SavedTick.loadTickList(
                    tag.getList(BLOCK_TICKS_TAG, Tag.TAG_COMPOUND), string -> BuiltInRegistries.BLOCK.getOptional(ResourceLocation.tryParse(string)), chunkPos
            );
            List<SavedTick<Fluid>> list1 = SavedTick.loadTickList(
                    tag.getList(FLUID_TICKS_TAG, Tag.TAG_COMPOUND), string -> BuiltInRegistries.FLUID.getOptional(ResourceLocation.tryParse(string)), chunkPos
            );
            PackedTicks packedTicks = new PackedTicks(list, list1);
            ListTag list2 = tag.getList("PostProcessing", Tag.TAG_LIST);
            ShortList[] lists = new ShortList[list2.size()];

            for (int i = 0; i < list2.size(); i++) {
                ListTag list3 = list2.getList(i);
                ShortList list4 = new ShortArrayList(list3.size());

                for (int i1 = 0; i1 < list3.size(); i1++) {
                    list4.add(list3.getShort(i1));
                }

                lists[i] = list4;
            }

            List<CompoundTag> list5 = Lists.transform(tag.getList("entities", Tag.TAG_COMPOUND), tag1 -> (CompoundTag)tag1);
            List<CompoundTag> list6 = Lists.transform(tag.getList("block_entities", Tag.TAG_COMPOUND), tag1 -> (CompoundTag)tag1);
            CompoundTag compound1 = tag.getCompound("structures");
            ListTag list7 = tag.getList(SECTIONS_TAG, Tag.TAG_COMPOUND);
            List<SerializableChunkData.SectionData> list8 = new ArrayList<>(list7.size());
            Registry<Biome> registry = registries.lookupOrThrow(Registries.BIOME);
            Codec<PalettedContainer<Holder<Biome>>> codec = makeBiomeCodecRW(registry);

            for (int i2 = 0; i2 < list7.size(); i2++) {
                CompoundTag compound2 = list7.getCompound(i2);
                int _byte = compound2.getByte("Y");
                LevelChunkSection levelChunkSection;
                if (_byte >= levelHeightAccessor.getMinSectionY() && _byte <= levelHeightAccessor.getMaxSectionY()) {
                    BlockState[] presetBlockStates = serverLevel.chunkPacketBlockController.getPresetBlockStates(serverLevel, chunkPos, _byte);
                    PalettedContainer<BlockState> palettedContainer;
                    if (compound2.contains("block_states", Tag.TAG_COMPOUND)) {
                        Codec<PalettedContainer<BlockState>> blockStateCodec = presetBlockStates == null
                                ? BLOCK_STATE_CODEC
                                : PalettedContainer.codecRW(
                                Block.BLOCK_STATE_REGISTRY, BlockState.CODEC, Strategy.SECTION_STATES, Blocks.AIR.defaultBlockState(), presetBlockStates
                        );
                        palettedContainer = blockStateCodec.parse(NbtOps.INSTANCE, compound2.getCompound("block_states"))
                                .promotePartial(string -> {})
                                .getOrThrow(NothingException::new);
                    } else {
                        palettedContainer = new PalettedContainer<>(
                                Block.BLOCK_STATE_REGISTRY, Blocks.AIR.defaultBlockState(), Strategy.SECTION_STATES, presetBlockStates
                        );
                    }

                    PalettedContainer<Holder<Biome>> palettedContainerRo;
                    if (compound2.contains("biomes", Tag.TAG_COMPOUND)) {
                        palettedContainerRo = codec.parse(NbtOps.INSTANCE, compound2.getCompound("biomes"))
                                .promotePartial(string -> {})
                                .getOrThrow(NothingException::new);
                    } else {
                        palettedContainerRo = new PalettedContainer<>(
                                registry.asHolderIdMap(), registry.getOrThrow(Biomes.PLAINS), Strategy.SECTION_BIOMES, null
                        );
                    }

                    levelChunkSection = new LevelChunkSection(palettedContainer, palettedContainerRo);
                } else {
                    levelChunkSection = null;
                }

                DataLayer dataLayer = compound2.contains(BLOCK_LIGHT_TAG, Tag.TAG_BYTE_ARRAY) ? new DataLayer(compound2.getByteArray(BLOCK_LIGHT_TAG)) : null;
                DataLayer dataLayer1 = compound2.contains(SKY_LIGHT_TAG, Tag.TAG_BYTE_ARRAY) ? new DataLayer(compound2.getByteArray(SKY_LIGHT_TAG)) : null;
                SerializableChunkData.SectionData MinecraftChunkData = new SerializableChunkData.SectionData(_byte, levelChunkSection, dataLayer, dataLayer1);
                if (compound2.contains(SaveUtil.BLOCKLIGHT_STATE_TAG, Tag.TAG_ANY_NUMERIC)) {
                    MinecraftChunkData.starlight$setBlockLightState(compound2.getInt(SaveUtil.BLOCKLIGHT_STATE_TAG));
                }

                if (compound2.contains(SaveUtil.SKYLIGHT_STATE_TAG, Tag.TAG_ANY_NUMERIC)) {
                    MinecraftChunkData.starlight$setSkyLightState(compound2.getInt(SaveUtil.SKYLIGHT_STATE_TAG));
                }

                list8.add(MinecraftChunkData);
            }

            return new MinecraftChunkData(
                    registry,
                    chunkPos,
                    levelHeightAccessor.getMinSectionY(),
                    _long,
                    _long1,
                    chunkStatus,
                    packed,
                    belowZeroRetrogen,
                    upgradeData,
                    longArray,
                    map,
                    packedTicks,
                    lists,
                    _boolean,
                    list8,
                    list5,
                    list6,
                    compound1,
                    tag.get("ChunkBukkitValues")
            );
        }
    }

    private ProtoChunk loadStarlightLightData(ServerLevel world, ProtoChunk ret) {
        boolean hasSkyLight = world.dimensionType().hasSkyLight();
        int minSection = WorldUtil.getMinLightSection(world);
        SWMRNibbleArray[] blockNibbles = StarLightEngine.getFilledEmptyLight(world);
        SWMRNibbleArray[] skyNibbles = StarLightEngine.getFilledEmptyLight(world);
        if (!this.lightCorrect) {
            ret.starlight$setBlockNibbles(blockNibbles);
            ret.starlight$setSkyNibbles(skyNibbles);
            return ret;
        } else {
            try {
                for (SerializableChunkData.SectionData sectionData : this.sectionData) {
                    int y = sectionData.y();
                    DataLayer blockLight = sectionData.blockLight();
                    DataLayer skyLight = sectionData.skyLight();
                    int blockState = sectionData.starlight$getBlockLightState();
                    int skyState = sectionData.starlight$getSkyLightState();
                    if (blockState >= 0) {
                        if (blockLight != null) {
                            blockNibbles[y - minSection] = new SWMRNibbleArray(MixinWorkarounds.clone(blockLight.getData()), blockState);
                        } else {
                            blockNibbles[y - minSection] = new SWMRNibbleArray(null, blockState);
                        }
                    }

                    if (skyState >= 0 && hasSkyLight) {
                        if (skyLight != null) {
                            skyNibbles[y - minSection] = new SWMRNibbleArray(MixinWorkarounds.clone(skyLight.getData()), skyState);
                        } else {
                            skyNibbles[y - minSection] = new SWMRNibbleArray(null, skyState);
                        }
                    }
                }

                ret.starlight$setBlockNibbles(blockNibbles);
                ret.starlight$setSkyNibbles(skyNibbles);
            } catch (Throwable var14) {
                ret.setLightCorrect(false);
                LOGGER.error("Failed to parse light data for chunk " + ret.getPos() + " in world '" + WorldUtil.getWorldName(world) + "'", var14);
            }

            return ret;
        }
    }

    public ProtoChunk read(ServerLevel level, PoiManager poiManager, RegionStorageInfo regionStorageInfo, ChunkPos pos) {
        if (!Objects.equals(pos, this.chunkPos)) {
            LOGGER.error("Chunk file at {} is in the wrong location; relocating. (Expected {}, got {})", pos, pos, this.chunkPos);
            level.getServer().reportMisplacedChunk(this.chunkPos, pos, regionStorageInfo);
        }

        int sectionsCount = level.getSectionsCount();
        LevelChunkSection[] levelChunkSections = new LevelChunkSection[sectionsCount];
        boolean hasSkyLight = level.dimensionType().hasSkyLight();
        ChunkSource chunkSource = level.getChunkSource();
        LevelLightEngine lightEngine = chunkSource.getLightEngine();
        Registry<Biome> registry = level.registryAccess().lookupOrThrow(Registries.BIOME);
        boolean flag = false;

        for (SerializableChunkData.SectionData sectionData : this.sectionData) {
            SectionPos sectionPos = SectionPos.of(pos, sectionData.y());
            if (sectionData.chunkSection() != null) {
                levelChunkSections[level.getSectionIndexFromSectionY(sectionData.y())] = sectionData.chunkSection();
            }

            boolean flag1 = sectionData.blockLight() != null;
            boolean flag2 = hasSkyLight && sectionData.skyLight() != null;
            if (flag1 || flag2) {
                if (!flag) {
                    lightEngine.retainData(pos, true);
                    flag = true;
                }

                if (flag1) {
                    lightEngine.queueSectionData(LightLayer.BLOCK, sectionPos, sectionData.blockLight());
                }

                if (flag2) {
                    lightEngine.queueSectionData(LightLayer.SKY, sectionPos, sectionData.skyLight());
                }
            }
        }

        ChunkType chunkType = this.chunkStatus.getChunkType();
        ChunkAccess chunkAccess;
        if (chunkType == ChunkType.LEVELCHUNK) {
            LevelChunkTicks<Block> levelChunkTicks = new LevelChunkTicks<>(this.packedTicks.blocks());
            LevelChunkTicks<Fluid> levelChunkTicks1 = new LevelChunkTicks<>(this.packedTicks.fluids());
            chunkAccess = new LevelChunk(
                    level.getLevel(),
                    pos,
                    this.upgradeData,
                    levelChunkTicks,
                    levelChunkTicks1,
                    this.inhabitedTime,
                    levelChunkSections,
                    postLoadChunk(level, this.entities, this.blockEntities),
                    BlendingData.unpack(this.blendingData)
            );
        } else {
            ProtoChunkTicks<Block> protoChunkTicks = ProtoChunkTicks.load(this.packedTicks.blocks());
            ProtoChunkTicks<Fluid> protoChunkTicks1 = ProtoChunkTicks.load(this.packedTicks.fluids());
            ProtoChunk protoChunk = new ProtoChunk(
                    pos, this.upgradeData, levelChunkSections, protoChunkTicks, protoChunkTicks1, level, registry, BlendingData.unpack(this.blendingData)
            );
            chunkAccess = protoChunk;
            protoChunk.setInhabitedTime(this.inhabitedTime);
            if (this.belowZeroRetrogen != null) {
                protoChunk.setBelowZeroRetrogen(this.belowZeroRetrogen);
            }

            protoChunk.setPersistedStatus(this.chunkStatus);
            if (this.chunkStatus.isOrAfter(ChunkStatus.INITIALIZE_LIGHT)) {
                protoChunk.setLightEngine(lightEngine);
            }
        }

        if (this.persistentDataContainer instanceof CompoundTag compoundTag) {
            chunkAccess.persistentDataContainer.putAll(compoundTag);
        }

        chunkAccess.setLightCorrect(this.lightCorrect);
        EnumSet<Types> set = EnumSet.noneOf(Types.class);

        for (Types types : chunkAccess.getPersistedStatus().heightmapsAfter()) {
            long[] longs = this.heightmaps.get(types);
            if (longs != null) {
                chunkAccess.setHeightmap(types, longs);
            } else {
                set.add(types);
            }
        }

        Heightmap.primeHeightmaps(chunkAccess, set);
        chunkAccess.setAllStarts(unpackStructureStart(StructurePieceSerializationContext.fromLevel(level), this.structureData, level.getSeed()));
        chunkAccess.setAllReferences(unpackStructureReferences(level.registryAccess(), pos, this.structureData));

        for (int i = 0; i < this.postProcessingSections.length; i++) {
            chunkAccess.addPackedPostProcess(this.postProcessingSections[i], i);
        }

        if (chunkType == ChunkType.LEVELCHUNK) {
            return this.loadStarlightLightData(level, new ImposterProtoChunk((LevelChunk)chunkAccess, false));
        } else {
            ProtoChunk protoChunk1 = (ProtoChunk)chunkAccess;

            for (CompoundTag compoundTag : this.entities) {
                protoChunk1.addEntity(compoundTag);
            }

            for (CompoundTag compoundTag : this.blockEntities) {
                BlockPos blockposition = BlockEntity.getPosFromTag(compoundTag);
                if (blockposition.getX() >> 4 == this.chunkPos.x && blockposition.getZ() >> 4 == this.chunkPos.z) {
                    protoChunk1.setBlockEntityNbt(compoundTag);
                } else {
                    LOGGER.warn(
                            "Tile entity serialized in chunk {} in world '{}' positioned at {} is located outside of the chunk",
                            this.chunkPos,
                            level.getWorld().getName(),
                            blockposition
                    );
                }
            }

            if (this.carvingMask != null) {
                protoChunk1.setCarvingMask(new CarvingMask(this.carvingMask, chunkAccess.getMinY()));
            }

            return this.loadStarlightLightData(level, protoChunk1);
        }
    }

    private static Codec<PalettedContainer<Holder<Biome>>> makeBiomeCodecRW(Registry<Biome> biomeRegistry) {
        return PalettedContainer.codecRW(
                biomeRegistry.asHolderIdMap(), biomeRegistry.holderByNameCodec(), Strategy.SECTION_BIOMES, biomeRegistry.getOrThrow(Biomes.PLAINS), null
        );
    }

    @Nullable
    private static PostLoadProcessor postLoadChunk(ServerLevel level, List<CompoundTag> entities, List<CompoundTag> blockEntities) {
        return entities.isEmpty() && blockEntities.isEmpty()
                ? null
                : chunk -> {
            if (!entities.isEmpty()) {
                level.addLegacyChunkEntities(EntityType.loadEntitiesRecursive(entities, level, EntitySpawnReason.LOAD));
            }

            for (CompoundTag compoundTag : blockEntities) {
                boolean _boolean = compoundTag.getBoolean("keepPacked");
                if (_boolean) {
                    chunk.setBlockEntityNbt(compoundTag);
                } else {
                    BlockPos posFromTag = BlockEntity.getPosFromTag(compoundTag);
                    ChunkPos chunkPos = chunk.getPos();
                    if (posFromTag.getX() >> 4 == chunkPos.x && posFromTag.getZ() >> 4 == chunkPos.z) {
                        BlockEntity blockEntity = BlockEntity.loadStatic(posFromTag, chunk.getBlockState(posFromTag), compoundTag, level.registryAccess());
                        if (blockEntity != null) {
                            chunk.setBlockEntity(blockEntity);
                        }
                    } else {
                        LOGGER.warn(
                                "Tile entity serialized in chunk "
                                        + chunkPos
                                        + " in world '"
                                        + level.getWorld().getName()
                                        + "' positioned at "
                                        + posFromTag
                                        + " is located outside of the chunk"
                        );
                    }
                }
            }
        };
    }

    private static Map<Structure, StructureStart> unpackStructureStart(StructurePieceSerializationContext context, CompoundTag tag, long seed) {
        Map<Structure, StructureStart> map = Maps.newHashMap();
        Registry<Structure> registry = context.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        CompoundTag compound = tag.getCompound("starts");

        for (String string : compound.getAllKeys()) {
            ResourceLocation resourceLocation = ResourceLocation.tryParse(string);
            Structure structure = registry.getValue(resourceLocation);
            if (structure == null) {
                LOGGER.error("Unknown structure start: {}", resourceLocation);
            } else {
                StructureStart structureStart = StructureStart.loadStaticStart(context, compound.getCompound(string), seed);
                if (structureStart != null) {
                    if (compound.getCompound(string).get("StructureBukkitValues") instanceof CompoundTag compoundTag) {
                        structureStart.persistentDataContainer.putAll(compoundTag);
                    }

                    map.put(structure, structureStart);
                }
            }
        }

        return map;
    }

    private static Map<Structure, LongSet> unpackStructureReferences(RegistryAccess registries, ChunkPos pos, CompoundTag tag) {
        Map<Structure, LongSet> map = Maps.newHashMap();
        Registry<Structure> registry = registries.lookupOrThrow(Registries.STRUCTURE);
        CompoundTag compound = tag.getCompound("References");

        for (String string : compound.getAllKeys()) {
            ResourceLocation resourceLocation = ResourceLocation.tryParse(string);
            Structure structure = registry.getValue(resourceLocation);
            if (structure == null) {
                LOGGER.warn("Found reference to unknown structure '{}' in chunk {}, discarding", resourceLocation, pos);
            } else {
                long[] longArray = compound.getLongArray(string);
                if (longArray.length != 0) {
                    map.put(structure, new LongOpenHashSet(Arrays.stream(longArray).filter(l -> {
                        ChunkPos chunkPos = new ChunkPos(l);
                        if (chunkPos.getChessboardDistance(pos) > 8) {
                            LOGGER.warn("Found invalid structure reference [ {} @ {} ] for chunk {}.", resourceLocation, chunkPos, pos);
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
}
