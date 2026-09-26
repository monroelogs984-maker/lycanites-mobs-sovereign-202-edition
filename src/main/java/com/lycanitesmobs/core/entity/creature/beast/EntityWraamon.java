package com.lycanitesmobs.core.entity.creature.beast;

import com.lycanitesmobs.core.entity.base.TameableCreatureEntity;
import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;

/**
 * Trimmed - original extends TameableCreatureEntity (tame/pet-control/bag, not ported);
 * rebased onto BaseCreatureEntity. Dropped: StealthGoal (not ported - no equivalent AI goal
 * class exists yet in this port), and the random leap-at-target in aiStep() (leap(float,
 * double, Entity) doesn't exist on this port's BaseCreatureEntity). getFallResistance() also
 * dropped - not a real hook in this port's damage pipeline. The knockback-disable melee attack
 * trick and isInvulnerableTo(IN_WALL) are both self-contained vanilla API and kept as-is.
 * PHASE 5e UPDATE (2026-09-26): re-parented to its official superclass now that TameableCreatureEntity is
 * ported (taming/ownership/pet behaviour work; RideableCreatureEntity is still a stub). Any wording above
 * about extending Base/AgeableCreatureEntity or taming being unported is outdated.
 */
public class EntityWraamon extends TameableCreatureEntity implements Enemy {

    public EntityWraamon(EntityType<? extends EntityWraamon> entityType, Level world) {
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
    public boolean attackMelee(Entity target, double damageScale) {
        double targetKnockbackResistance = 0;
        if (target instanceof LivingEntity) {
            targetKnockbackResistance = ((LivingEntity) target).getAttribute(Attributes.KNOCKBACK_RESISTANCE).getValue();
            ((LivingEntity) target).getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
        }

        if (!super.attackMelee(target, damageScale))
            return false;

        if (target instanceof LivingEntity)
            ((LivingEntity) target).getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(targetKnockbackResistance);

        return true;
    }

    @Override
    public boolean canClimb() {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (source.is(DamageTypes.IN_WALL)) return true;
        return super.isInvulnerableTo(source);
    }
}
