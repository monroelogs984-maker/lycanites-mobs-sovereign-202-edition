package com.lycanitesmobs.core.entity.creature.reptile;

import com.lycanitesmobs.core.entity.base.TameableCreatureEntity;
import com.lycanitesmobs.core.entity.IGroupHeavy;
import com.lycanitesmobs.core.entity.goals.actions.TemptGoal;
import net.minecraft.world.entity.player.Player;
import com.lycanitesmobs.core.data.tag.LycanitesBlockTags;
import com.lycanitesmobs.core.entity.base.AgeableCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import com.lycanitesmobs.core.util.helpers.AssetHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Trimmed - dropped TemptGoal (not ported), IGroupHeavy marker interface (not ported), and the
 * fleeHealthPercent/spawnsOnLand-style config fields that were dropped in the Phase 5 base trim.
 * canBeLeashed() uses the 1.21.1 no-arg Leashable signature (was canBeLeashed(Player)).
 */
public class EntityArisaur extends TameableCreatureEntity implements IGroupHeavy {

    public EntityArisaur(EntityType<? extends EntityArisaur> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = false;
        this.canGrow = true;
        this.babySpawnChance = 0.1D;
        this.isAggressiveByDefault = false;
        // Restored from official (2026-09-28 constructor audit):
        this.fleeHealthPercent = 1.0F;
        this.solidCollision = true;
        this.setupMob();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimDistractionGoalIndex(), new TemptGoal(this).setIncludeDiet(true));
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setLongMemory(false));
    }

    @Override
    public float getBlockPathWeight(int x, int y, int z) {
        BlockState blockState = this.getCommandSenderWorld().getBlockState(new BlockPos(x, y - 1, z));
        if (blockState.getBlock() != Blocks.AIR) {
            if (blockState.is(LycanitesBlockTags.CREATURE_PATH_GRASS_PREFERRED))
                return 10F;
            if (blockState.is(LycanitesBlockTags.CREATURE_PATH_DIRT_PREFERRED))
                return 7F;
        }
        return super.getBlockPathWeight(x, y, z);
    }

    @Override
    public boolean canBeLeashed() {
        return true;
    }

    /**
     * Returns this creature's main texture. Also checks for subspecies.
     **/
    @Override
    public ResourceLocation getTexture() {
        if (!this.hasCustomName() || !"Flowersaur".equals(this.getCustomName().getString()))
            return super.getTexture();
        String textureName = this.getTextureName() + "_flowersaur";
        return AssetHelper.entityTexture(textureName);
    }


    // ==================================================
    //   Restored from official 2026-09-28 (method audit)
    // ==================================================
    @Override
    public int getBagSize() {
        return this.creatureInfo.getBagSize();
    }

    @Override
    public int getNoBagSize() {
        return 0;
    }
}
