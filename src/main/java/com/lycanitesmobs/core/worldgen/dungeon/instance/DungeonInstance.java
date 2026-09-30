package com.lycanitesmobs.core.worldgen.dungeon.instance;

import com.lycanitesmobs.core.worldgen.dungeon.definition.DungeonSchematic;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

/**
 * A Dungeon Instance is a dungeon that is placed in the world.
 *
 * Port: only the parts the structure path (LMDungeonStructure/LMDungeonPiece) uses. The official class also kept the
 * legacy ExtendedWorld-tracked generation (init, async build plans, per-chunk build progress, NBT); its only entry
 * point, DungeonFeature, is commented out upstream, so none of that is ported.
 */
public class DungeonInstance {
    /**
     * The Schematic this instance builds from.
     **/
    protected DungeonSchematic schematic;

    /**
     * The origin block position of this dungeon where it begins building from.
     **/
    protected BlockPos originPos;

    /**
     * The minimum xz chunk that sectors that this layout is in.
     **/
    protected ChunkPos chunkMin;

    /**
     * The maximum xz chunk that sectors that this layout is in.
     **/
    protected ChunkPos chunkMax;

    /**
     * The world that the dungeon builds in. Null while the layout is generated during structure placement.
     **/
    protected Level world;

    /**
     * The seed for generating this dungeon, so that the layout generates the same every time it is rebuilt.
     **/
    protected long seed = 0;

    /**
     * The highest y a sector may occupy. Port: the official hardcoded 255, which put Nether towers above the bedrock
     * roof; this is set from the chunk generator's height instead (see LMDungeonStructure.getDungeonMaxY).
     **/
    protected int maxY = 255;

    public DungeonSchematic getSchematic() {
        return this.schematic;
    }

    public void setSchematic(DungeonSchematic schematic) {
        this.schematic = schematic;
    }

    public ChunkPos getChunkMin() {
        return this.chunkMin;
    }

    public ChunkPos getChunkMax() {
        return this.chunkMax;
    }

    public Level getWorld() {
        return this.world;
    }

    public void setWorldIfMissing(Level world) {
        if (this.world == null) {
            this.world = world;
        }
    }

    public long getSeed() {
        return this.seed;
    }

    public void setSeed(long seed) {
        this.seed = seed;
    }

    public int getMaxY() {
        return this.maxY;
    }

    public void setMaxY(int maxY) {
        this.maxY = maxY;
    }

    public BlockPos getOrigin() {
        return this.originPos;
    }

    /**
     * Sets the origin position. This must be set before generating a layout.
     *
     * @param blockPos The exact block position that this dungeon builds from.
     */
    public void setOrigin(BlockPos blockPos) {
        this.originPos = blockPos;
        if (this.chunkMin == null) {
            this.chunkMin = new ChunkPos(blockPos);
        }
        if (this.chunkMax == null) {
            this.chunkMax = new ChunkPos(blockPos);
        }
    }

    protected void expandChunkBounds(SectorInstance sectorInstance) {
        ChunkPos minChunkPos = new ChunkPos(sectorInstance.getOccupiedBoundsMin());
        this.chunkMin = new ChunkPos(Math.min(minChunkPos.x, this.chunkMin.x), Math.min(minChunkPos.z, this.chunkMin.z));
        ChunkPos maxChunkPos = new ChunkPos(sectorInstance.getOccupiedBoundsMax());
        this.chunkMax = new ChunkPos(Math.max(maxChunkPos.x, this.chunkMax.x), Math.max(maxChunkPos.z, this.chunkMax.z));
    }

    /**
     * Returns a descriptive string of this Dungeon Instance.
     *
     * @return A formatted string.
     */
    @Override
    public String toString() {
        String schematic = "";
        if (this.schematic != null)
            schematic = " - Schematic: " + this.schematic.getName();
        String tpCommand = "/tp " + this.originPos.getX() + " " + (this.originPos.getY() + 2) + " " + this.originPos.getZ() + " ";
        return "Dungeon Instance" + schematic + " - TP Command: " + tpCommand + " - Origin: " + this.originPos + " - Seed: " + this.seed;
    }
}
