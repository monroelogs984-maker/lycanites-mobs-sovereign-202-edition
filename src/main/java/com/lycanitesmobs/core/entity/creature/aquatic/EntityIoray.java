package com.lycanitesmobs.core.entity.creature.aquatic;

import com.lycanitesmobs.core.entity.base.RideableCreatureEntity;
import com.lycanitesmobs.core.entity.base.AgeableCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Trimmed - originally extended RideableCreatureEntity (mount system not ported) with a
 * ProjectileManager-driven "waterjet" ranged attack and mount ability (ProjectileManager isn't
 * ported at all in this port yet - see PORT_PLAN.md). Dropped entirely: AttackRangedGoal/
 * attackRanged()/mountAbility()/riderEffects()/onDismounted()/getPassengersRidingOffset() (all
 * projectile- or mount-specific). Kept the melee attack goal and all aquatic stat/immunity
 * config - ends up melee-only rather than the original's ranged water-jet attacker.
 * PHASE 5e UPDATE (2026-09-26): re-parented to its official superclass now that TameableCreatureEntity is
 * ported (taming/ownership/pet behaviour work; RideableCreatureEntity is still a stub). Any wording above
 * about extending Base/AgeableCreatureEntity or taming being unported is outdated.
 */
public class EntityIoray extends RideableCreatureEntity implements Enemy {

    public EntityIoray(EntityType<? extends EntityIoray> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = true;
        this.babySpawnChance = 0D;
        this.canGrow = true;
        this.setupMob();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setLongMemory(false).setMaxChaseDistanceSq(4.0F));
    }

    @Override
    public float getBlockPathWeight(int x, int y, int z) {
        int waterWeight = 10;
        Block block = this.getCommandSenderWorld().getBlockState(new BlockPos(x, y, z)).getBlock();
        if (block == Blocks.WATER)
            return (super.getBlockPathWeight(x, y, z) + 1) * (waterWeight + 1);
        if (this.getCommandSenderWorld().isRaining() && this.getCommandSenderWorld().canSeeSkyFromBelowWater(new BlockPos(x, y, z)))
            return (super.getBlockPathWeight(x, y, z) + 1) * (waterWeight + 1);
        if (this.getTarget() != null)
            return super.getBlockPathWeight(x, y, z);
        if (this.waterContact())
            return -999999.0F;
        return super.getBlockPathWeight(x, y, z);
    }

    @Override
    public boolean isStrongSwimmer() {
        return true;
    }

    @Override
    public boolean canWalk() {
        return false;
    }

    @Override
    public boolean creatureCanBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean canBreatheAir() {
        return false;
    }

    @Override
    public boolean canBurn() {
        return false;
    }
}
