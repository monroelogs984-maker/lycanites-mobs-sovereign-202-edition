package com.lycanitesmobs.core.worldgen.structure;

import com.lycanitesmobs.core.entity.spawner.condition.WorldSpawnCondition;
import com.lycanitesmobs.core.manager.DungeonManager;
import com.lycanitesmobs.core.util.helpers.LMHelperClass;
import com.lycanitesmobs.core.worldgen.dungeon.definition.DungeonSchematic;
import com.lycanitesmobs.core.worldgen.dungeon.instance.DungeonInstance;
import com.lycanitesmobs.core.worldgen.dungeon.instance.DungeonLayout;
import com.lycanitesmobs.core.worldgen.dungeon.instance.SectorInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public class LMDungeonPiece extends StructurePiece {

    private String schematicName;
    private DungeonLayout layout;
    private long layoutSeed;
    private BlockPos originPos;
    private int maxY;
    private boolean placementChecked;
    private boolean placementAllowed;

    public LMDungeonPiece(BoundingBox boundingBox, String schematicName, DungeonLayout layout) {
        super(ModStructureTypes.LM_DUNGEON_PIECE.get(), 0, boundingBox);
        this.schematicName = schematicName;
        this.layout = layout;
        this.originPos = layout.getDungeonInstance().getOrigin();
        this.layoutSeed = layout.getDungeonInstance().getSeed();
        this.maxY = layout.getDungeonInstance().getMaxY();
    }

    public LMDungeonPiece(BoundingBox boundingBox, String schematicName, BlockPos originPos, long layoutSeed, int maxY) {
        super(ModStructureTypes.LM_DUNGEON_PIECE.get(), 0, boundingBox);
        this.schematicName = schematicName;
        this.originPos = originPos;
        this.layoutSeed = layoutSeed;
        this.maxY = maxY;
    }

    public LMDungeonPiece(CompoundTag tag) {
        super(ModStructureTypes.LM_DUNGEON_PIECE.get(), tag);
        this.schematicName = tag.getString("SchematicName");
        this.layoutSeed = tag.getLong("LayoutSeed");
        int[] origin = tag.getIntArray("Origin");
        this.originPos = new BlockPos(origin[0], origin[1], origin[2]);
        this.maxY = tag.contains("MaxY") ? tag.getInt("MaxY") : 255;
        if (!this.requiresRuntimePlacementCheck()) {
            this.layout = this.regenerateLayout();
        }
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putString("SchematicName", this.schematicName);
        tag.putLong("LayoutSeed", this.layoutSeed);
        tag.putInt("MaxY", this.maxY);
        tag.putIntArray("Origin", new int[] {
                this.originPos.getX(),
                this.originPos.getY(),
                this.originPos.getZ()
        });
    }

    @Override
    public void postProcess(WorldGenLevel worldGenLevel, StructureManager structureManager,
            ChunkGenerator chunkGenerator, RandomSource random,
            BoundingBox chunkBoundingBox, ChunkPos chunkPos, BlockPos blockPos) {
        if (!this.canPlaceInWorld(worldGenLevel)) {
            return;
        }

        if (this.layout == null) {
            this.layout = this.regenerateLayout();
        }

        if (this.layout == null) {
            LMHelperClass.logWarning("Dungeon",
                    "postProcess: layout is null for " + this.schematicName + ", skipping chunk " + chunkPos);
            return;
        }

        if (!this.layout.hasSectorsInChunk(chunkPos)) {
            return;
        }

        this.layout.getDungeonInstance().setWorldIfMissing(worldGenLevel.getLevel());

        try {
            var sectors = this.layout.getSectorsInChunk(chunkPos);
            for (SectorInstance sector : sectors) {
                sector.build(worldGenLevel, worldGenLevel.getLevel(), chunkPos, random);
            }

            LMHelperClass.logDebug("DungeonPerf", () ->
                    "postProcess: built " + sectors.size() +
                            " sectors in chunk " + chunkPos + " for " + this.schematicName);
        } catch (Exception e) {
            LMHelperClass.logErrorMessageOnceCatchable(
                    "postProcess failed for " + this.schematicName + " chunk " + chunkPos + ": ", e);
        }
    }

    private DungeonLayout regenerateLayout() {
        DungeonSchematic schematic = DungeonManager.getInstance().getSchematic(this.schematicName);
        if (schematic == null) {
            LMHelperClass.logWarning("Dungeon",
                    "Cannot regenerate layout: schematic '" + this.schematicName + "' not found");
            return null;
        }

        DungeonInstance tempInstance = new DungeonInstance();
        tempInstance.setSchematic(schematic);
        tempInstance.setOrigin(this.originPos);
        tempInstance.setSeed(this.layoutSeed);
        tempInstance.setMaxY(this.maxY);

        RandomSource layoutRandom = RandomSource.create(this.layoutSeed);
        DungeonLayout newLayout = new DungeonLayout(tempInstance);
        newLayout.generate(layoutRandom);

        if (!newLayout.hasSectors()) {
            LMHelperClass.logWarning("Dungeon",
                    "Regenerated layout has no sectors for " + this.schematicName);
            return null;
        }

        LMHelperClass.logDebug("Dungeon", () ->
                "Regenerated layout from seed for " + this.schematicName +
                        " with " + newLayout.getSectors().size() + " sectors");
        return newLayout;
    }

    private synchronized boolean canPlaceInWorld(WorldGenLevel worldGenLevel) {
        if (this.placementChecked) {
            return this.placementAllowed;
        }

        this.placementAllowed = this.resolvePlacementAllowed(worldGenLevel);
        this.placementChecked = true;
        return this.placementAllowed;
    }

    private boolean resolvePlacementAllowed(WorldGenLevel worldGenLevel) {
        DungeonSchematic schematic = DungeonManager.getInstance().getSchematic(this.schematicName);
        if (schematic == null || !schematic.isEnabled()) {
            return false;
        }

        int footprintRadius = DungeonLayout.getMaximumReferenceRadius();
        if (!schematic.isFootprintFarEnoughFromSpawn(worldGenLevel.getLevel(), this.originPos, footprintRadius)) {
            LMHelperClass.logInfo("Dungeon", () ->
                    "postProcess: skipping " + this.schematicName + " near world spawn at " + this.originPos +
                            " distanceFromSpawn=" +
                            String.format("%.1f", schematic.getHorizontalDistanceFromSpawn(worldGenLevel.getLevel(), this.originPos)) +
                            " footprintDistanceFromSpawn=" +
                            String.format("%.1f", schematic.getHorizontalFootprintDistanceFromSpawn(
                                    worldGenLevel.getLevel(),
                                    this.originPos,
                                    footprintRadius)) +
                            " footprintRadius=" + footprintRadius +
                            " minDistanceFromSpawn=" + schematic.getMinDistanceFromSpawn() +
                            " spawn=" + schematic.getWorldSpawnPosition(worldGenLevel.getLevel()));
            return false;
        }

        WorldSpawnCondition condition = schematic.getWorldSpawnCondition();
        if (condition == null) {
            return true;
        }

        String dimensionId = worldGenLevel.getLevel().dimension().location().toString();
        if (condition.isAllowedDimensionId(dimensionId)) {
            return true;
        }

        LMHelperClass.logDebug("Dungeon", () ->
                "postProcess: skipping " + this.schematicName + " in disallowed dimension " + dimensionId);
        return false;
    }

    private boolean requiresRuntimePlacementCheck() {
        DungeonSchematic schematic = DungeonManager.getInstance().getSchematic(this.schematicName);
        return schematic != null && schematic.requiresRuntimePlacementCheck();
    }

}
