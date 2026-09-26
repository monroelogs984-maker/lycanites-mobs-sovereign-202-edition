package com.lycanitesmobs.core.entity.creature.insect;

import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import com.lycanitesmobs.core.entity.goals.targeting.FindAttackTargetGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Heavily trimmed: the original extends TameableCreatureEntity and its whole identity is a
 * stealth ambush predator (StealthGoal, canStealth/startStealth invisibility tied to
 * plague/instability effects via ObjectManager.getEffect, a leap-in-to-attack preamble, and
 * tame-gated pet-invisibility). None of the stealth system, those specific effects, the tame
 * system, or leap() (not on BaseCreatureEntity in this trimmed port) exist here, so this is
 * reduced to a plain melee attacker - still climbs and fights, just visible and without the
 * ambush mechanic.
 */
public class EntityOstimien extends BaseCreatureEntity {

    public EntityOstimien(EntityType<? extends EntityOstimien> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = true;
        this.setupMob();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setLongMemory(false));
        this.targetSelector.addGoal(this.claimFindTargetGoalIndex(), new FindAttackTargetGoal(this).addTargets(EntityType.PLAYER));
    }

    @Override
    public boolean canClimb() {
        return true;
    }
}
