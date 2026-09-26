package com.lycanitesmobs.core.entity.creature.aquatic;

import com.lycanitesmobs.core.entity.base.AgeableCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Trimmed - original extends TameableCreatureEntity (tame/master/pet-control system, not
 * ported); rebased onto AgeableCreatureEntity, dropping bag-size overrides and
 * petControlsEnabled(). MobType.UNDEFINED attribute assignment dropped - no such field on
 * BaseCreatureEntity in this port.
 */
public class EntityLacedon extends AgeableCreatureEntity implements Enemy {

    public EntityLacedon(EntityType<? extends EntityLacedon> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = true;
        this.babySpawnChance = 0.1D;
        this.canGrow = true;
        this.setupMob();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setLongMemory(false).setRange(1));
    }

    @Override
    public float getAISpeedModifier() {
        if (this.isInWater())
            return 1.5F;
        else if (this.waterContact())
            return 1.25F;
        return super.getAISpeedModifier();
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
        if (this.getTarget() != null)
            return super.getBlockPathWeight(x, y, z);
        if (this.waterContact())
            return -999999.0F;
        return super.getBlockPathWeight(x, y, z);
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    public BlockPos getWanderPosition(BlockPos wanderPosition) {
        BlockPos groundPos;
        for (groundPos = wanderPosition.below(); groundPos.getY() > 0 && !this.getCommandSenderWorld().getBlockState(groundPos).isSolid(); groundPos = groundPos.below()) {
        }
        return groundPos.above();
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
