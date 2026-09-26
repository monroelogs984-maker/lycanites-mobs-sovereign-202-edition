package com.lycanitesmobs.core.entity.creature.aberration;

import com.lycanitesmobs.core.entity.base.TameableCreatureEntity;
import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;

/**
 * Trimmed - original extends TameableCreatureEntity (tame/bag, not ported); rebased onto
 * BaseCreatureEntity. Dropped the random leap-at-target in aiStep() (leap(float,double,Entity)
 * doesn't exist on this port's BaseCreatureEntity) and webProof()/getFallResistance() (not real
 * hooks here). canBeAffected() is a vanilla LivingEntity method, kept as-is.
 * PHASE 5e UPDATE (2026-09-26): re-parented to its official superclass now that TameableCreatureEntity is
 * ported (taming/ownership/pet behaviour work; RideableCreatureEntity is still a stub). Any wording above
 * about extending Base/AgeableCreatureEntity or taming being unported is outdated.
 */
public class EntityTrite extends TameableCreatureEntity implements Enemy {

    public EntityTrite(EntityType<? extends EntityTrite> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = true;
        this.setupMob();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this));
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        // TODO(port): official also excludes EntityAsmodeus - restore once it's ported.
        if (target instanceof EntityAstaroth)
            return false;
        return super.canAttack(target);
    }

    @Override
    public boolean canClimb() {
        return true;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance potionEffect) {
        if (potionEffect.getEffect() == MobEffects.WITHER)
            return false;
        return super.canBeAffected(potionEffect);
    }

    @Override
    public boolean canBurn() {
        return false;
    }
}
