package com.lycanitesmobs.core.block.liquid;

import com.lycanitesmobs.core.manager.ObjectManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

/** Freezing ooze: turns lava to obsidian and water to packed ice, damages and slows, extinguishes. **/
public class OozeLiquidBlock extends BaseLiquidBlock {
    public OozeLiquidBlock(FlowingFluid fluid, BlockBehaviour.Properties properties, String name, String elementName, boolean destroyItems) {
        super(fluid, properties, name, elementName, destroyItems);
    }

    @Override
    public boolean shouldSpreadLiquid(Level world, BlockPos neighborBlockPos, BlockState neighborState) {
        if (this.isLavaLikeFluid(world, neighborBlockPos)) {
            world.setBlock(neighborBlockPos, Blocks.OBSIDIAN.defaultBlockState(), 3);
            return false;
        }
        if (this.isWaterLikeFluid(world, neighborBlockPos)) {
            world.setBlock(neighborBlockPos, Blocks.PACKED_ICE.defaultBlockState(), 3);
            return false;
        }
        return super.shouldSpreadLiquid(world, neighborBlockPos, neighborState);
    }

    @Override
    protected void entityInside(BlockState blockState, Level world, BlockPos pos, Entity entity) {
        if (!(entity instanceof ItemEntity) && !(entity instanceof ExperienceOrb)) {
            entity.hurt(ObjectManager.getDamageSource(world, "ooze"), 1F);
        }
        if (entity.isOnFire())
            entity.clearFire();
        if (entity instanceof LivingEntity livingEntity) {
            livingEntity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5 * 20, 0));
        }
        super.entityInside(blockState, world, pos, entity);
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
        this.spawnParticle(world, pos, random, ParticleTypes.RAIN);
        super.animateTick(state, world, pos, random);
    }
}
