package com.lycanitesmobs.core.block.liquid;

import com.lycanitesmobs.core.entity.creature.insect.EntityVespid;
import com.lycanitesmobs.core.entity.creature.insect.EntityVespidQueen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.phys.Vec3;

/** Vespid honey: turns water to dirt and lava to cobblestone, sticks everything except vespids in place. **/
public class VeshoneyLiquidBlock extends BaseLiquidBlock {
    public VeshoneyLiquidBlock(FlowingFluid fluid, BlockBehaviour.Properties properties, String name, String elementName, boolean destroyItems) {
        super(fluid, properties, name, elementName, destroyItems);
    }

    @Override
    public boolean shouldSpreadLiquid(Level world, BlockPos neighborBlockPos, BlockState neighborState) {
        if (this.isWaterLikeFluid(world, neighborBlockPos)) {
            world.setBlock(neighborBlockPos, Blocks.DIRT.defaultBlockState(), 3);
            return false;
        }
        if (this.isLavaLikeFluid(world, neighborBlockPos)) {
            world.setBlock(neighborBlockPos, Blocks.COBBLESTONE.defaultBlockState(), 3);
            return false;
        }
        return super.shouldSpreadLiquid(world, neighborBlockPos, neighborState);
    }

    @Override
    protected void entityInside(BlockState blockState, Level world, BlockPos pos, Entity entity) {
        if (entity.isOnFire())
            entity.clearFire();
        // S202 fix: the official check was "!isCreative() || !isSpectator()", which is always true - creative and
        // spectator players are meant to be exempt.
        if (entity instanceof LivingEntity && !(entity instanceof EntityVespid) && !(entity instanceof EntityVespidQueen)) {
            if (!(entity instanceof Player player) || (!player.isCreative() && !player.isSpectator())) {
                entity.makeStuckInBlock(blockState, new Vec3(0.3D, 0.6D, 0.3D));
                entity.setDeltaMovement(0, -0.02, 0);
            }
        }
        super.entityInside(blockState, world, pos, entity);
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
        this.spawnParticle(world, pos, random, ParticleTypes.RAIN);
        super.animateTick(state, world, pos, random);
    }
}
