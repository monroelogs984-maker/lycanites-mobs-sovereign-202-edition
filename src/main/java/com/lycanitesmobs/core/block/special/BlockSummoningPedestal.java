package com.lycanitesmobs.core.block.special;

import com.lycanitesmobs.core.block.base.BlockBase;
import com.lycanitesmobs.core.util.PortPlaceholder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * PLACEHOLDER for the official summoning pedestal: keeps the "owner" state property (0 none, 1 client, 2 player) so
 * the official blockstate/models work, but the TileEntitySummoningPedestal (keeps a summon set's minions summoned
 * around it, with its own screen) isn't ported. Right-clicking reports that.
 */
public class BlockSummoningPedestal extends BlockBase {
    public enum EnumSummoningPedestal {
        NONE(0),
        CLIENT(1),
        PLAYER(2);

        private final int ownerId;

        EnumSummoningPedestal(int ownerId) {
            this.ownerId = ownerId;
        }

        public int getOwnerId() {
            return this.ownerId;
        }
    }

    public static final IntegerProperty PROPERTY_OWNER = IntegerProperty.create("owner", 0, 2);

    public BlockSummoningPedestal(Block.Properties properties) {
        super(properties, "summoningpedestal");
        this.registerDefaultState(this.getStateDefinition().any().setValue(PROPERTY_OWNER, EnumSummoningPedestal.NONE.ownerId));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PROPERTY_OWNER);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        PortPlaceholder.notifyNotFunctional(player, this.getName(), PortPlaceholder.SUMMONING_PEDESTAL);
        return InteractionResult.sidedSuccess(world.isClientSide);
    }
}
