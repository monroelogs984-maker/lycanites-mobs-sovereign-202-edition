package com.lycanitesmobs.core.entity.creature.dragon;

import net.minecraft.world.entity.Entity;
import org.joml.Vector3d;
import com.lycanitesmobs.core.entity.goals.actions.AttackRangedGoal;
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
 * PHASE 6a UPDATE (2026-09-26): ranged attack restored (projectiles ported) - any wording above about a
 * substituted melee attack or ProjectileManager being unported is outdated.
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
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackRangedGoal(this).setSpeed(0.75D).setRange(14.0F).setMinChaseDistance(5.0F));
    }

    @Override
    public boolean isFlying() {
        return true;
    }

    @Override
    public void attackRanged(Entity target, float range) {
        this.fireProjectile("venomshot", target, range, 0, new Vector3d(0, 0, 0), 1.2f, 2f, 1F);
        super.attackRanged(target, range);
    }
}
