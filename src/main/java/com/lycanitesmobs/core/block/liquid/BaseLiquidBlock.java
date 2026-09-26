package com.lycanitesmobs.core.block.liquid;

import com.lycanitesmobs.LycanitesMobs;
import com.lycanitesmobs.core.block.BlockTypeGetter;
import com.lycanitesmobs.core.manager.ObjectManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;

/**
 * Base for Lycanites' custom fluid blocks. Each fluid reacts with water-like and lava-like neighbours (turning them
 * into a stone), and hurts or buffs entities inside it.
 *
 * <p>1.21.1: LiquidBlock takes the FlowingFluid instance directly (fluids register before blocks), the neighbour
 * reaction hook replaces vanilla's lava/water interaction via neighborChanged/onPlace.
 */
public class BaseLiquidBlock extends LiquidBlock implements BlockTypeGetter {
    private static final String LAVA_ELEMENT = "lava";

    private final String blockName;
    /** The element name of this fluid (lava-like fluids use "lava"). **/
    protected final String elementName;
    protected final boolean destroyItems;
    private ResourceLocation registryName;

    public BaseLiquidBlock(FlowingFluid fluid, BlockBehaviour.Properties properties, String name, String elementName, boolean destroyItems) {
        super(fluid, properties);
        this.blockName = name;
        this.elementName = elementName;
        this.destroyItems = destroyItems;
        this.registryName = ResourceLocation.fromNamespaceAndPath(LycanitesMobs.MODID, name);
    }

    @Override
    public ResourceLocation getRegistryName() {
        return this.registryName;
    }

    @Override
    public void setRegistryName(ResourceLocation registryName) {
        this.registryName = registryName;
    }

    public String getBlockName() {
        return this.blockName;
    }

    public String getElementName() {
        return this.elementName;
    }

    @Override
    protected void onPlace(BlockState blockState, Level world, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (this.reactWithNeighbours(world, pos)) {
            super.onPlace(blockState, world, pos, oldState, isMoving);
        }
    }

    @Override
    protected void neighborChanged(BlockState blockState, Level world, BlockPos blockPos, Block neighborBlock, BlockPos neighborBlockPos, boolean movedByPiston) {
        if (this.reactWithNeighbours(world, blockPos)) {
            super.neighborChanged(blockState, world, blockPos, neighborBlock, neighborBlockPos, movedByPiston);
        }
    }

    /**
     * Checks every neighbour of this fluid for reactions. Returns false if this fluid should stop updating.
     **/
    protected boolean reactWithNeighbours(Level world, BlockPos pos) {
        for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            BlockState neighborState = world.getBlockState(neighborPos);
            if (neighborState.getBlock() == this || neighborState.getFluidState().isEmpty()) {
                continue;
            }
            if (!this.shouldSpreadLiquid(world, neighborPos, neighborState)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Called with a neighbouring fluid, reacting fluids replace the neighbour and return false.
     **/
    public boolean shouldSpreadLiquid(Level world, BlockPos neighborBlockPos, BlockState neighborState) {
        return true;
    }

    protected boolean isWaterLikeFluid(Level world, BlockPos pos) {
        FluidState fluidState = world.getFluidState(pos);
        if (fluidState.is(FluidTags.WATER)) return true;
        if (world.getBlockState(pos).getBlock() instanceof BaseLiquidBlock liquidBlock) {
            return liquidBlock != this && !LAVA_ELEMENT.equals(liquidBlock.getElementName());
        }
        return false;
    }

    protected boolean isLavaLikeFluid(Level world, BlockPos pos) {
        FluidState fluidState = world.getFluidState(pos);
        if (fluidState.is(FluidTags.LAVA)) return true;
        if (world.getBlockState(pos).getBlock() instanceof BaseLiquidBlock liquidBlock) {
            return liquidBlock != this && LAVA_ELEMENT.equals(liquidBlock.getElementName());
        }
        return false;
    }

    @Override
    protected void entityInside(BlockState blockState, Level world, BlockPos pos, Entity entity) {
        if (this.destroyItems && (entity instanceof ItemEntity || entity instanceof ExperienceOrb)) {
            entity.kill();
        }
        super.entityInside(blockState, world, pos, entity);
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
        if (random.nextInt(52) == 0) {
            SoundEvent sound = ObjectManager.getSound(this.blockName);
            if (sound != null) {
                world.playLocalSound(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, sound, SoundSource.BLOCKS, 0.5F + random.nextFloat(), random.nextFloat() * 0.7F + 0.3F, false);
            }
        }
        super.animateTick(state, world, pos, random);
    }

    /**
     * Spawns an occasional particle, used by the fluid subclasses.
     **/
    protected void spawnParticle(Level world, BlockPos pos, RandomSource random, net.minecraft.core.particles.SimpleParticleType particle) {
        if (random.nextInt(100) == 0) {
            world.addParticle(particle, pos.getX() + random.nextFloat(), pos.getY() + random.nextFloat() * 0.5F, pos.getZ() + random.nextFloat(), 0.0D, 0.0D, 0.0D);
        }
    }
}
