package com.lycanitesmobs.core.entity.creature.reptile;

import com.lycanitesmobs.core.entity.base.AgeableCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;

/**
 * Trimmed - was TameableCreatureEntity (not ported, now AgeableCreatureEntity), dropped the
 * random-leaping aiStep behaviour (leap() isn't a ported method) and getRangedCooldown()/
 * getFallResistance() (no ranged system/hook to override).
 */
public class EntityGeken extends AgeableCreatureEntity implements Enemy {

    public EntityGeken(EntityType<? extends EntityGeken> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = true;
        this.canGrow = true;
        this.babySpawnChance = 0.1D;
        this.setupMob();
        this.attackPhaseMax = 3;
        this.setAttackCooldownMax(10);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this));
    }

    @Override
    public int getMeleeCooldown() {
        if (this.getAttackPhase() == 2)
            return super.getMeleeCooldown() * 3;
        return super.getMeleeCooldown();
    }

    @Override
    public boolean canClimb() {
        return true;
    }
}
