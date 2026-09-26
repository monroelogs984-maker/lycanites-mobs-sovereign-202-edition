package com.lycanitesmobs.core.entity.creature.dragon;

import com.lycanitesmobs.core.entity.IGroupHeavy;
import com.lycanitesmobs.core.entity.base.AgeableCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;

/**
 * Heavily trimmed - original extends RideableCreatureEntity and has a whole land/fly toggle
 * state machine (wantsToLand/isLanded), pickup-and-carry, leap(), mount ability, and rider
 * effect-clearing - none of that (mount/tame/pickup system, leap()) exists on this port's
 * BaseCreatureEntity. Reduced to a plain always-flying melee attacker.
 */
public class EntityMorock extends AgeableCreatureEntity implements Enemy, IGroupHeavy {

    public EntityMorock(EntityType<? extends EntityMorock> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = true;
        this.flySoundSpeed = 20;
        this.setupMob();
    }

    @Override
    public float maxUpStep() {
        return 1.0F;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setLongMemory(false));
    }

    @Override
    public boolean rollWanderChance() {
        if (this.isFlying())
            return this.getRandom().nextDouble() <= 0.25D;
        return this.getRandom().nextDouble() <= 0.008D;
    }

    @Override
    public boolean isFlying() {
        return true;
    }
}
