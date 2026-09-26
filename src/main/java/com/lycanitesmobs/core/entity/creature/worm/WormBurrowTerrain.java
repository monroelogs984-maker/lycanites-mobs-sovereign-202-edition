package com.lycanitesmobs.core.entity.creature.worm;

import com.lycanitesmobs.core.data.tag.LycanitesBlockTags;
import net.minecraft.world.level.block.state.BlockState;

final class WormBurrowTerrain {
    private WormBurrowTerrain() {
    }

    static boolean isBurrowable(BlockState blockState) {
        return blockState.is(LycanitesBlockTags.WORM_BURROWABLE);
    }
}