package com.lycanitesmobs.core.entity.creature.dragon;

import com.lycanitesmobs.core.entity.base.TameableCreatureEntity;
import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;

/**
 * Trimmed - original extends TameableCreatureEntity (tame/master/pet-control/bag, not ported).
 * Original's only attack is AttackRangedGoal + attackRanged()/fireProjectile("venomshot", ...)
 * (ProjectileManager not ported) - substituted a plain AttackMeleeGoal so this isn't a
 * completely defenseless flyer, same pattern as other ranged-only creatures this batch.
 * PHASE 5e UPDATE (2026-09-26): re-parented to its official superclass now that TameableCreatureEntity is
 * ported (taming/ownership/pet behaviour work; RideableCreatureEntity is still a stub). Any wording above
 * about extending Base/AgeableCreatureEntity or taming being unported is outdated.
 */
public class EntityRemobra extends TameableCreatureEntity implements Enemy {

    public EntityRemobra(EntityType<? extends EntityRemobra> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = false;
        this.flySoundSpeed = 20;
        this.setupMob();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setLongMemory(false));
    }

    @Override
    public boolean isFlying() {
        return true;
    }
}
