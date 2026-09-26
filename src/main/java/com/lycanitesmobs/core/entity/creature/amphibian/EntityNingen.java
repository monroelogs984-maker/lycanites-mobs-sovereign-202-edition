package com.lycanitesmobs.core.entity.creature.amphibian;

import com.lycanitesmobs.core.entity.base.AgeableCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;

/**
 * Trimmed - see EntityAglebemu's class doc, same reasoning (TameableCreatureEntity ->
 * AgeableCreatureEntity, attribute/spawnsOnLand/spawnsInWater/bag/petControlsEnabled dropped,
 * getAISpeedModifier()/waterContact() water-speed-boost hooks aren't on the trimmed
 * BaseCreatureEntity so dropped rather than restored just for this).
 */
public class EntityNingen extends AgeableCreatureEntity implements Enemy {

    public EntityNingen(EntityType<? extends EntityNingen> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = true;
        this.babySpawnChance = 0.01D;
        this.canGrow = false;
        this.setupMob();
        this.setPathfindingMalus(PathType.WATER, 0F);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setLongMemory(false).setRange(1));
    }

    @Override
    public float getBlockPathWeight(int x, int y, int z) {
        int waterWeight = 10;
        BlockPos pos = new BlockPos(x, y, z);
        BlockState blockState = this.getCommandSenderWorld().getBlockState(pos);
        if (blockState.getBlock() == Blocks.WATER)
            return (super.getBlockPathWeight(x, y, z) + 1) * (waterWeight + 1);
        if (this.getCommandSenderWorld().isRaining() && this.getCommandSenderWorld().canSeeSkyFromBelowWater(pos))
            return (super.getBlockPathWeight(x, y, z) + 1) * (waterWeight + 1);
        return super.getBlockPathWeight(x, y, z);
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean isAggressive() {
        if (this.getAirSupply() <= -100)
            return false;
        return super.isAggressive();
    }

    @Override
    public boolean creatureCanBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean canBreatheAir() {
        return false;
    }
}
