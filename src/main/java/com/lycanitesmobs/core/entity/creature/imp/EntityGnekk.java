package com.lycanitesmobs.core.entity.creature.imp;

import com.lycanitesmobs.core.entity.base.TameableCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Trimmed - dropped the random-leap-at-target aiStep behavior (leap(float,double,Entity) isn't
 * on this port's BaseCreatureEntity) and petControlsEnabled/bag getters/getFallResistance
 * (tame/equipment, not ported).
 */
public class EntityGnekk extends TameableCreatureEntity {

    public EntityGnekk(EntityType<? extends EntityGnekk> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = true;
        this.canGrow = true;
        this.babySpawnChance = 0.01D;
        this.setupMob();
    }

    @Override
    public float maxUpStep() {
        return 1.0F;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setTargetClass(Player.class).setLongMemory(false));
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this));
    }

    @Override
    public float getAISpeedModifier() {
        if (!this.onGround())
            return 5.0F;
        return 1.0F;
    }

    @Override
    public double getFallingMod() {
        return 0.98D;
    }

    @Override
    public boolean canClimb() {
        return true;
    }
}
