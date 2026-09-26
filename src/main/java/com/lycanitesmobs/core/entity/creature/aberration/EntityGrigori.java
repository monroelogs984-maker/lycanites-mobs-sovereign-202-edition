package com.lycanitesmobs.core.entity.creature.aberration;

import com.lycanitesmobs.core.entity.base.TameableCreatureEntity;
import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;

/**
 * Trimmed - original extends TameableCreatureEntity (tame/master/bag, not ported); rebased onto
 * BaseCreatureEntity. Dropped FindMasterGoal/CopyMasterAttackTargetGoal (master/tame system) and
 * the canAttack() override checking for its master EntityGrell's vehicle (mount system, not
 * ported). setMaxUpStep() fixed to the 1.21.1 maxUpStep() getter override.
 * PHASE 5e UPDATE (2026-09-26): re-parented to its official superclass now that TameableCreatureEntity is
 * ported (taming/ownership/pet behaviour work; RideableCreatureEntity is still a stub). Any wording above
 * about extending Base/AgeableCreatureEntity or taming being unported is outdated.
 */
public class EntityGrigori extends TameableCreatureEntity implements Enemy {
    public EntityGrigori(EntityType<? extends EntityGrigori> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = true;
        this.setupMob();
    }

    @Override
    public float maxUpStep() {
        return 1.0F;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setSpeed(2.0D).setLongMemory(false));
    }

    @Override
    public boolean rollWanderChance() {
        return this.getRandom().nextDouble() <= 0.25D;
    }

    @Override
    public boolean isFlying() {
        return true;
    }

    @Override
    public boolean canBurn() {
        return false;
    }
}
