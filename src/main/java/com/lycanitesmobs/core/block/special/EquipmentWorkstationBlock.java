package com.lycanitesmobs.core.block.special;

import com.lycanitesmobs.core.block.base.BlockBase;
import com.lycanitesmobs.core.container.block.EquipmentForgeContainer;
import com.lycanitesmobs.core.container.block.EquipmentInfuserContainer;
import com.lycanitesmobs.core.container.block.EquipmentStationContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
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
 * The official equipment blocks (BlockEquipmentForge x3, EquipmentInfuserBlock, EquipmentStationBlock), reworked for
 * S202 imprints (design/EQUIPMENT_REWORK.md): the Forge imprints/extracts a part (its tier caps the part level), the
 * Infuser levels a part with charges, the Station recharges its mana. Like vanilla workstations they have no block
 * entity: items stay in the open menu and go back to the player when it closes.
 */
public class EquipmentWorkstationBlock extends BlockBase {
    public enum Kind { FORGE, INFUSER, STATION }

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    protected final Kind kind;
    /** Forges only: the highest part level this forge can imprint or extract (lesser 1, greater 2, master 3). **/
    protected final int forgeLevel;

    public EquipmentWorkstationBlock(Block.Properties properties, String name, Kind kind, int forgeLevel) {
        super(properties, name);
        this.kind = kind;
        this.forgeLevel = forgeLevel;
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
        if (!world.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            ContainerLevelAccess access = ContainerLevelAccess.create(world, pos);
            MenuProvider provider = new SimpleMenuProvider((windowId, inventory, menuPlayer) -> switch (this.kind) {
                case FORGE -> new EquipmentForgeContainer(windowId, inventory, access, this.forgeLevel);
                case INFUSER -> new EquipmentInfuserContainer(windowId, inventory, access);
                case STATION -> new EquipmentStationContainer(windowId, inventory, access);
            }, this.getName());
            serverPlayer.openMenu(provider, buf -> buf.writeVarInt(this.forgeLevel));
        }
        return InteractionResult.sidedSuccess(world.isClientSide);
    }
}
