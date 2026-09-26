package com.lycanitesmobs.core.entity.navigation;

import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import com.lycanitesmobs.core.manager.ObjectManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.Target;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * Phase 5g: ported from the official source. Walking uses vanilla WalkNodeEvaluator; flying and strong swimming
 * use 6-direction open-space/water node searches.
 *
 * <p>1.21.1 API changes: getGoal() -> getTarget(); getBlockPathType(BlockGetter, x, y, z, Mob) ->
 * getPathTypeOfMob(PathfindingContext, x, y, z, Mob); BlockPathTypes -> PathType; the evaluator's
 * {@code level} field is gone - block states are read through {@code currentContext} (set in prepare());
 * BlockState.isPathfindable() lost its level/pos parameters.
 */
public class CreatureNodeProcessor extends WalkNodeEvaluator implements ICreatureNodeProcessor {

    private BaseCreatureEntity entityCreature;

    public static double getGroundY(BlockGetter blockReader, BlockPos pos) {
        BlockPos blockpos = pos.below();
        VoxelShape voxelshape = blockReader.getBlockState(blockpos).getCollisionShape(blockReader, blockpos);
        return (double) blockpos.getY() + (voxelshape.isEmpty() ? 0.0D : voxelshape.max(Direction.Axis.Y));
    }

    @Override
    public void prepare(PathNavigationRegion region, Mob mob) {
        this.updateEntitySize(mob);
        super.prepare(region, mob);
        if (mob instanceof BaseCreatureEntity creature) {
            this.entityCreature = creature;
        }
    }

    @Override
    public void updateEntitySize(Entity updateEntity) {
        this.entityWidth = Math.min(Mth.floor(this.getWidth(true, updateEntity) + 1.0F), 3);
        this.entityHeight = Math.min(Mth.floor(updateEntity.getBbHeight() + 1.0F), 3);
        this.entityDepth = Math.min(Mth.floor(this.getWidth(true, updateEntity) + 1.0F), 3);
    }

    /** Returns the starting position to create a new path from. **/
    @Override
    public Node getStart() {
        // Flying/Strong Swimming:
        if (this.flying() || (this.entityCreature != null && this.entityCreature.isStrongSwimmer() && this.entityCreature.isUnderWater())) {
            return this.getNode(Mth.floor(this.mob.getBoundingBox().minX), Mth.floor(this.mob.getBoundingBox().minY + 0.5D), Mth.floor(this.mob.getBoundingBox().minZ));
        }
        return super.getStart();
    }

    /** Returns true if the entity is capable of pathing/moving in water at all. **/
    @Override
    public boolean canFloat() {
        if (this.entityCreature != null)
            return this.entityCreature.canWade() || this.entityCreature.isStrongSwimmer();
        return super.canFloat();
    }

    /** Returns a target node at the given coordinates. **/
    @Override
    public Target getTarget(double x, double y, double z) {
        // Flying/Strong Swimming:
        if (this.flying() || this.swimming()) {
            return new Target(this.getNode(Mth.floor(x - this.getWidth(false)), Mth.floor(y + 0.5D), Mth.floor(z - this.getWidth(false))));
        }
        return super.getTarget(x, y, z);
    }

    /** Checks points around the provided fromPoint and adds it to path options if it is a valid point to travel to. **/
    @Override
    public int getNeighbors(Node[] pathOptions, Node fromPoint) {
        this.updateEntitySize(this.mob);

        // Flying/Strong Swimming/Diving:
        if (this.flying() || this.swimming()) {
            int i = 0;
            for (Direction direction : Direction.values()) {
                Node pathPoint = null;
                if (this.swimming()) {
                    pathPoint = this.getWaterNode(fromPoint.x + direction.getStepX(), fromPoint.y + direction.getStepY(), fromPoint.z + direction.getStepZ());
                }
                if (pathPoint == null) {
                    pathPoint = this.getFlightNode(fromPoint.x + direction.getStepX(), fromPoint.y + direction.getStepY(), fromPoint.z + direction.getStepZ());
                }
                if (pathPoint != null && !pathPoint.closed) {
                    pathOptions[i++] = pathPoint;
                }
            }
            return i;
        }

        return super.getNeighbors(pathOptions, fromPoint);
    }

    @Override
    public PathType getPathTypeOfMob(PathfindingContext context, int x, int y, int z, Mob mob) {
        if (this.swimming()) {
            return PathType.WATER;
        }
        return super.getPathTypeOfMob(context, x, y, z, mob);
    }

    /** Returns true if the entity should use swimming focused pathing. **/
    public boolean swimming() {
        if (this.entityCreature == null) {
            return false;
        }
        if (this.entityCreature.isInSwimmableFluid()) { // S202: official checked isInWater() only.
            return this.entityCreature.isStrongSwimmer() || (this.entityCreature.canWade() && this.entityCreature.shouldDive());
        }
        return false;
    }

    /** Returns true if the entity should use flight focused pathing. **/
    public boolean flying() {
        return this.entityCreature != null && this.entityCreature.isFlying() && !this.entityCreature.isUnderWater();
    }

    /**
     * Returns a width to path with.
     * @param blockChecks If true, this width is used for checking blocks, a reduced width can be returned for better performance here.
     */
    public double getWidth(boolean blockChecks) {
        return this.getWidth(blockChecks, this.mob);
    }

    public double getWidth(boolean blockChecks, Entity entity) {
        return Math.min(3, (double) entity.getBbWidth());
    }

    private BlockState getBlockState(BlockPos pos) {
        return this.currentContext.getBlockState(pos);
    }

    /** Flight Pathing **/
    @Nullable
    protected Node getFlightNode(int x, int y, int z) {
        PathType pathnodetype = this.isFlyablePathNode(x, y, z);
        if (this.entityCreature != null && this.entityCreature.isStrongSwimmer()) {
            if (pathnodetype == PathType.WATER)
                return this.getNode(x, y, z);
        }
        return pathnodetype == PathType.OPEN ? this.getNode(x, y, z) : null;
    }

    protected PathType isFlyablePathNode(int x, int y, int z) {
        BlockPos centerPos = new BlockPos(x, y, z);
        for (int i = 0; i <= this.entityWidth; ++i) {
            for (int j = 0; j <= Math.min(this.entityHeight, 2); ++j) {
                for (int k = 0; k <= this.entityDepth; ++k) {
                    BlockState blockState = this.getBlockState(centerPos.offset(i, k, j));

                    // Non-Solid:
                    if (!blockState.isSolid() && !(blockState.getBlock() instanceof LiquidBlock)) {
                        return PathType.OPEN;
                    }

                    // Check For Open Air:
                    if (!blockState.isAir()) {
                        // If Can Swim Check For Swimmable Node:
                        if (this.entityCreature != null && this.entityCreature.isStrongSwimmer()) {
                            return this.isSwimmablePathNode(x, y, z);
                        }
                        return PathType.BLOCKED;
                    }
                }
            }
        }
        return PathType.OPEN;
    }

    /** Power Swim Pathing **/
    @Nullable
    protected Node getWaterNode(int x, int y, int z) {
        PathType pathnodetype;
        if (this.entityCreature != null && this.entityCreature.isFlying()) {
            pathnodetype = this.isFlyablePathNode(x, y, z);
            if (pathnodetype == PathType.OPEN)
                return this.getNode(x, y, z);
        } else {
            pathnodetype = this.isSwimmablePathNode(x, y, z);
        }
        return pathnodetype == PathType.WATER ? this.getNode(x, y, z) : null;
    }

    protected PathType isSwimmablePathNode(int x, int y, int z) {
        if (this.entityCreature == null) {
            return PathType.BLOCKED;
        }

        BlockPos centerPos = new BlockPos(x, y, z);
        boolean waterDamages = this.entityCreature.waterDamage();
        boolean lavaDamages = this.entityCreature.canBurn();
        boolean canFreeze = this.entityCreature.canFreeze();
        Block oozeBlock = ObjectManager.getBlock("ooze");
        for (int i = 0; i <= this.entityWidth; ++i) {
            for (int j = 0; j <= Math.min(this.entityHeight, 2); ++j) {
                for (int k = 0; k <= this.entityDepth; ++k) {
                    BlockPos blockPos = centerPos.offset(i, k, j);
                    BlockState blockState = this.getBlockState(blockPos);

                    // S202 fix (not in the official source): vanilla LiquidBlock.isPathfindable() is always false for
                    // lava, so lava was never a swimmable node. Lava is swimmable when it doesn't burn this creature.
                    // Necessary but NOT sufficient: lava fish (Cephignis) still don't move in lava - open issue,
                    // see PORT_PLAN.md "Phase 5g".
                    if (!lavaDamages && blockState.getFluidState().is(FluidTags.LAVA)) {
                        continue;
                    }

                    if (!blockState.isPathfindable(PathComputationType.WATER)) {
                        if (j == y) { // Y must be water. (Official quirk kept: compares the loop index to world Y.)
                            return PathType.BLOCKED;
                        }
                        if (!blockState.isPathfindable(PathComputationType.AIR) && !blockState.isPathfindable(PathComputationType.WATER)) { // Blocked above water.
                            return PathType.BLOCKED;
                        }
                    }

                    // Water Damages:
                    Block block = blockState.getBlock();
                    if (waterDamages && block == Blocks.WATER) {
                        return PathType.BLOCKED;
                    }

                    // Lava Damages:
                    if (lavaDamages && block == Blocks.LAVA) {
                        return PathType.BLOCKED;
                    }

                    // Ooze Swimming (With Water Damage):
                    if (!canFreeze && oozeBlock != null && block == oozeBlock) {
                        return PathType.WATER;
                    }

                    // Custom Fluid State Checks:
                    FluidState fluidState = blockState.getFluidState();
                    if (!fluidState.isSource()) {
                        if (waterDamages && fluidState.is(FluidTags.WATER)) {
                            return PathType.BLOCKED;
                        }
                        if (lavaDamages && fluidState.is(FluidTags.LAVA)) {
                            return PathType.BLOCKED;
                        }
                    }
                }
            }
        }
        return PathType.WATER;
    }
}
