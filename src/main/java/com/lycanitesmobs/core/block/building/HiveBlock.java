package com.lycanitesmobs.core.block.building;

import com.lycanitesmobs.core.block.base.BlockBase;
import com.lycanitesmobs.core.entity.creature.insect.EntityVespidQueen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;

/**
 * Vespid hive blocks (propolis, veswax). Hive-built blocks (AGE below 8) decay when no Vespid Queen is within 32
 * blocks, player-placed blocks (AGE 8+) are permanent.
 */
public class HiveBlock extends BlockBase {

    public HiveBlock(Block.Properties properties, String name) {
        super(properties, name);
        this.tickRate = 100;
        this.removeOnTick = true;
        this.registerDefaultState(this.getStateDefinition().any().setValue(AGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        if (placer == null) {
            return;
        }
        int orientationMeta = placer.getDirection().getOpposite().get3DDataValue() + 8;
        world.setBlock(pos, state.setValue(AGE, orientationMeta), 1);
    }

    @Override
    public int tickRate(LevelAccessor world) {
        return this.tickRate;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(AGE) < 8;
    }

    @Override
    public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        if (state.getValue(AGE) >= 8)
            return;
        double range = 32D;
        if (!world.getEntitiesOfClass(EntityVespidQueen.class, new AABB(pos).inflate(range)).isEmpty())
            return;
        super.tick(state, world, pos, random);
    }
}
