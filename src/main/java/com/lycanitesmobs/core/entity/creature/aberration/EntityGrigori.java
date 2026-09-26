package com.lycanitesmobs.core.entity.creature.aberration;

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
 */
public class EntityGrigori extends BaseCreatureEntity implements Enemy {
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
