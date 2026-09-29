package com.lycanitesmobs.core.entity.creature.aberration;

import com.lycanitesmobs.core.manager.ObjectManager;
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
        if (target instanceof EntityAstaroth || target instanceof EntityAsmodeus)
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


    // ==================================================
    //   Restored from official 2026-09-28 (method audit)
    // ==================================================
    // ==================================================
    //                      Updates
    // ==================================================
	// ========== Living Update ==========
	@Override
    public void aiStep() {
        super.aiStep();

        // Leap:
        if(this.hasAttackTarget() && this.onGround() && !this.getCommandSenderWorld().isClientSide && this.random.nextInt(10) == 0)
        	this.leap(6.0F, 0.6D, this.getTarget());
    }

    @Override
    public int getBagSize() { return this.creatureInfo.getBagSize(); }

    @Override
    public float getFallResistance() {
        return 10;
    }

    // ==================================================
    //                     Equipment
    // ==================================================
    @Override
    public int getNoBagSize() { return 0; }

    // ========== Environmental ==========
    @Override
    public boolean webProof() { return true; }
}
