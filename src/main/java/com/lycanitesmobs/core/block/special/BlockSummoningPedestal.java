package com.lycanitesmobs.core.block.special;

import com.lycanitesmobs.core.block.base.BlockBase;
import com.lycanitesmobs.core.block.blockentity.TileEntitySummoningPedestal;
import com.lycanitesmobs.core.capabilities.entity.ExtendedPlayer;
import com.lycanitesmobs.core.container.provider.SummoningPedestalContainerProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

/**
 * The Summoning Pedestal block: placing it binds it to the placer and copies their selected summon set; right-clicking
 * opens its screen (summon set, behaviour, fuel slot). The "owner" state picks the model (0 none, 1 client, 2 player).
 * Port: Player.openMenu replaces NetworkHooks.openScreen; the block entity ticks through getTicker.
 */
public class BlockSummoningPedestal extends BlockBase implements EntityBlock {
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

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntitySummoningPedestal(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == TileEntitySummoningPedestal.TYPE.get() ? (BlockEntityTicker<T>) (BlockEntityTicker<TileEntitySummoningPedestal>) TileEntitySummoningPedestal::tick : null;
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (world.getBlockEntity(pos) instanceof TileEntitySummoningPedestal pedestal) {
            pedestal.setOwner(placer);
            if (placer instanceof Player player) {
                ExtendedPlayer playerExt = ExtendedPlayer.getForPlayer(player);
                if (playerExt != null) {
                    pedestal.setSummonSet(playerExt.getSelectedSummonSet());
                }
            }
        }
    }

    @Override
    protected void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && world.getBlockEntity(pos) instanceof TileEntitySummoningPedestal pedestal) {
            pedestal.onBlockRemoved();
        }
        super.onRemove(state, world, pos, newState, isMoving);
    }

    /** Sets the owner model state. The block entity stays (same block, only the property changes). **/
    public static void setState(EnumSummoningPedestal owner, Level world, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos);
        if (blockState.hasProperty(PROPERTY_OWNER) && blockState.getValue(PROPERTY_OWNER) != owner.getOwnerId()) {
            world.setBlock(pos, blockState.setValue(PROPERTY_OWNER, owner.getOwnerId()), 3);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!world.isClientSide() && player instanceof ServerPlayer serverPlayer && world.getBlockEntity(pos) instanceof TileEntitySummoningPedestal pedestal) {
            pedestal.syncToClients();
            serverPlayer.openMenu(new SummoningPedestalContainerProvider(pedestal), buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(world.isClientSide);
    }

    @Override
    protected boolean triggerEvent(BlockState state, Level world, BlockPos pos, int eventID, int eventParam) {
        BlockEntity tileEntity = world.getBlockEntity(pos);
        return tileEntity != null && tileEntity.triggerEvent(eventID, eventParam);
    }
}
