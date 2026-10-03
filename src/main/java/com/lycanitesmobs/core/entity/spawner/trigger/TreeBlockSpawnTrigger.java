package com.lycanitesmobs.core.entity.spawner.trigger;

import com.google.gson.JsonObject;
import com.lycanitesmobs.core.entity.spawner.Spawner;
import com.lycanitesmobs.core.util.helpers.LMHelperClass;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public class TreeBlockSpawnTrigger extends BlockSpawnTrigger {

	/** Constructor **/
	public TreeBlockSpawnTrigger(Spawner spawner) {
		super(spawner);
	}

	@Override
	public void loadFromJSON(JsonObject json) {
		super.loadFromJSON(json);
	}


	@Override
	public boolean isTriggerBlock(BlockState blockState, Level world, BlockPos blockPos, int fortune, @Nullable LivingEntity entity) {
		return this.isTreeLogBlock(blockState.getBlock(), world, blockPos) || this.isTreeLeavesBlock(blockState.getBlock(), world, blockPos);
	}

	public boolean isTreeLogBlock(Block block, Level world, BlockPos pos) {
		if(this.isLog(world.getBlockState(pos))) {
			int x = pos.getX();
			int y = pos.getY();
			int z = pos.getZ();
			for(int searchX = x - 1; searchX <= x + 1; searchX++) {
				for(int searchZ = z - 1; searchZ <= z + 1; searchZ++) {
					// S202 fix (upstream bug): the official checked the centre column for emptiness, which is the broken log
					// itself, so only logs at canopy height ever counted. Each column is now scanned up past trunk logs.
					for(int searchY = y; searchY <= Math.min(world.getMaxBuildHeight(), y + 32); searchY++) {
						BlockState searchState = world.getBlockState(new BlockPos(searchX, searchY, searchZ));
						if(this.isLeaves(searchState))
							return true;
						if(!searchState.isAir() && !this.isLog(searchState))
							break;
					}
				}
			}
		}
		String blockName = LMHelperClass.convertToResourceLocation(block, world.registryAccess()).toString();
		if((blockName.contains("tree") || blockName.contains("traverse")) && blockName.contains("branch")) {
			return true;
		}
		return false;
	}

	public boolean isTreeLeavesBlock(Block block, Level world, BlockPos pos) {
		if(this.isLeaves(world.getBlockState(pos))) {
			int x = pos.getX();
			int y = pos.getY();
			int z = pos.getZ();
			for(int searchX = x - 1; searchX <= x + 1; searchX++) {
				for(int searchZ = z - 1; searchZ <= z + 1; searchZ++) {
					// S202 fix (same upstream bug as isTreeLogBlock, mirrored): scan each column down past leaves.
					for(int searchY = y; searchY >= Math.max(world.getMinBuildHeight(), y - 32); searchY--) {
						BlockState searchState = world.getBlockState(new BlockPos(searchX, searchY, searchZ));
						if(this.isLog(searchState))
							return true;
						if(!searchState.isAir() && !this.isLeaves(searchState))
							break;
					}
				}
			}
		}
		String blockName = LMHelperClass.convertToResourceLocation(block, world.registryAccess()).toString();
		if((blockName.contains("tree") || blockName.contains("traverse")) && blockName.contains("leaves")) {
			return true;
		}
		return false;
	}

	public boolean isLog(BlockState blockState) {
		Block block = blockState.getBlock();
		if(block.defaultBlockState().is(BlockTags.LOGS)) {
			return true;
		}
		return false;
	}

	public boolean isLeaves(BlockState blockState) {
		Block block = blockState.getBlock();
		if(block instanceof LeavesBlock || block.defaultBlockState().is(BlockTags.LEAVES)) {
			return true;
		}
		return false;
	}
}
