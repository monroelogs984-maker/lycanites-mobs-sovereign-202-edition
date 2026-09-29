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
        this.spawnsUnderground = true;
        this.hasAttackSound = true;
        this.canGrow = true;
        this.babySpawnChance = 0.01D;
        // Restored from official (2026-09-28 constructor audit):
        this.hasJumpSound = true;
        this.spreadFire = false;
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


    // ==================================================
    //   Restored from official 2026-09-28 (method audit)
    // ==================================================
	@Override
    public void aiStep() {
        super.aiStep();

		// Random Leaping:
        if(!this.isTamed() && this.onGround() && !this.getCommandSenderWorld().isClientSide) {
        	if(this.hasAttackTarget()) {
        		if(this.random.nextInt(10) == 0)
        			this.leap(6.0F, 0.5D, this.getTarget());
        	}
        	else {
        		if(this.random.nextInt(50) == 0 && this.isMoving())
        			this.leap(2.0D, 0.5D);
        	}
        }
    }

    @Override
    public int getBagSize() { return this.creatureInfo.getBagSize(); }

    @Override
    public float getFallResistance() {
    	return 100;
    }

    // ==================================================
    //                     Equipment
    // ==================================================
    @Override
    public int getNoBagSize() { return 0; }

	public boolean petControlsEnabled() { return true; }
}
