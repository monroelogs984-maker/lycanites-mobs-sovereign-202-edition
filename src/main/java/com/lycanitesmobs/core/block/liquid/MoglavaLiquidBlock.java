package com.lycanitesmobs.core.block.liquid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

/** Moglava: lava-like, turns water to stone and burns dropped items. **/
public class MoglavaLiquidBlock extends BaseLiquidBlock {
    public MoglavaLiquidBlock(FlowingFluid fluid, BlockBehaviour.Properties properties, String name, String elementName, boolean destroyItems) {
        super(fluid, properties, name, elementName, destroyItems);
    }

    @Override
    public boolean shouldSpreadLiquid(Level world, BlockPos neighborBlockPos, BlockState neighborState) {
        if (this.isWaterLikeFluid(world, neighborBlockPos)) {
            world.setBlock(neighborBlockPos, Blocks.STONE.defaultBlockState(), 3);
            return false;
        }
        return super.shouldSpreadLiquid(world, neighborBlockPos, neighborState);
    }

    @Override
    protected void entityInside(BlockState blockState, Level world, BlockPos pos, Entity entity) {
        if (entity instanceof ItemEntity)
            entity.hurt(world.damageSources().lava(), 10F);
        super.entityInside(blockState, world, pos, entity);
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
        this.spawnParticle(world, pos, random, ParticleTypes.LAVA);
        super.animateTick(state, world, pos, random);
    }
}
