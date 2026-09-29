package com.lycanitesmobs.core.block.special;

import com.lycanitesmobs.core.block.base.BlockBase;
import com.lycanitesmobs.core.util.PortPlaceholder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * PLACEHOLDER for the official equipment blocks (BlockEquipmentForge x3, EquipmentStationBlock,
 * EquipmentInfuserBlock). Keeps their names, horizontal FACING and placement so the official blockstates/models
 * work, but has no block entity or menu: right-clicking reports that the equipment system isn't ported.
 */
public class PlaceholderFacingBlock extends BlockBase {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    protected final String system;

    public PlaceholderFacingBlock(Block.Properties properties, String name, String system) {
        super(properties, name);
        this.system = system;
        this.registerDefaultState(this.getStateDefinition().any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation direction) {
        return state.setValue(FACING, direction.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirrorIn) {
        return state.rotate(mirrorIn.getRotation(state.getValue(FACING)));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        PortPlaceholder.notifyNotFunctional(player, this.getName(), this.system);
        return InteractionResult.sidedSuccess(world.isClientSide);
    }
}
