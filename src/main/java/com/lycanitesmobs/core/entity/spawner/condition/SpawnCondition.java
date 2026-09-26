package com.lycanitesmobs.core.entity.spawner.condition;

import com.google.gson.JsonObject;
import com.lycanitesmobs.core.util.helpers.LMHelperClass;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public abstract class SpawnCondition {
    /** Spawn Conditions determine if the Spawner is allowed to be triggered. **/


    /**
     * Loads this Spawn Condition from the provided JSON data.
     *
     * TODO Phase 6: WorldSpawnCondition/PlayerSpawnCondition/EventSpawnCondition/
     * DateSpawnCondition/GroupSpawnCondition (the spawner system) aren't ported yet - all
     * types fall through to null for now, so no subspecies/dungeon spawn condition actually
     * restricts anything until those are ported.
     **/
    public static SpawnCondition createFromJSON(JsonObject json) {
        String type = json.get("type").getAsString();
        LMHelperClass.logWarningMessageOnce("[SpawnCondition] Spawn condition type '" + type + "' skipped - the spawner condition system isn't ported yet (Phase 6).");
        return null;
    }


    /** Loads this Spawn Condition from the provided JSON data. **/
    public void loadFromJSON(JsonObject json) {
        // No base properties to load currently.
    }


    /**
     * Returns true if this Spawn Condition is met allowing the Spawner to use its Triggers.
     * @param world The world to check the conditions of or in.
     * @param player The player to check the conditions of, can be null for some types.
     * @param position The positon to check the conditions from, can be null for some types.
     * @return True if conditions are met.
     */
    public boolean isMet(Level world, Player player, BlockPos position) {
        return true;
    }
}
