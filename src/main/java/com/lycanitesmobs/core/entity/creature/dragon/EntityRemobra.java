package com.lycanitesmobs.core.entity.creature.dragon;

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
 */
public class EntityRemobra extends BaseCreatureEntity implements Enemy {

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
