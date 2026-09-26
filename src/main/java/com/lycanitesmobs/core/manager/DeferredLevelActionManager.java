package com.lycanitesmobs.core.manager;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Trimmed - the original queues entity spawns to run on a later server tick once their target
 * chunk is confirmed loaded (via a TickEvent/LevelEvent.Unload listener, ~150 lines of queue
 * plumbing), to avoid spawning into an unloaded chunk mid-tick. Every current caller (the
 * concapede segment-chain spawning) always spawns at/near an already-loaded, actively-ticking
 * parent entity's position, so that safety net isn't needed yet - this just spawns immediately.
 * Port the real deferred-queue version if/when something spawns at a position that might not
 * be loaded (e.g. cross-chunk or cross-dimension spawns).
 */
public class DeferredLevelActionManager {
    public static boolean spawnEntity(Level level, BlockPos pos, @Nullable String key, Entity entity) {
        return spawnEntity(level, pos, key, entity, null);
    }

    public static boolean spawnEntity(Level level, BlockPos pos, @Nullable String key, Entity entity, @Nullable Runnable afterSpawn) {
        level.addFreshEntity(entity);
        if (afterSpawn != null) {
            afterSpawn.run();
        }
        return true;
    }
}
