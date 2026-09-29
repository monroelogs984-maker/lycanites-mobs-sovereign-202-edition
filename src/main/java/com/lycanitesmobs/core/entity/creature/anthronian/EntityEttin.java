package com.lycanitesmobs.core.entity.creature.anthronian;

import com.lycanitesmobs.core.entity.base.AgeableCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.BreakDoorGoal;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.GameRules;
import com.lycanitesmobs.core.entity.base.TameableCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;

/**
 * Trimmed - block-destroying AoE (destroyArea/griefing/GameRules.RULE_MOBGRIEFING check) not
 * ported, no such hook on BaseCreatureEntity. BreakDoorGoal dropped too (a whole new goal class
 * wrapping vanilla's DoorInteractGoal, not worth porting just for this one creature's door-
 * breaking flavor - it can still open doors via setCanOpenDoors below, just won't smash locked
 * ones). attackPhase-cycling on hit dropped (nextAttackPhase()/attackPhaseMax flavor only, no
 * real behavioral difference gated on it in this port). solidCollision field and
 * petControlsEnabled/getNoBagSize/getBagSize dropped - equipment/pet-control not ported.
 * MobType.UNDEFINED attribute assignment dropped.
 */
public class EntityEttin extends TameableCreatureEntity implements Enemy {
    // Fields restored from official (2026-09-28 method audit):
    protected boolean griefing = true;


    public EntityEttin(EntityType<? extends EntityEttin> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = true;
        // Restored from official (2026-09-28 constructor audit):
        this.attackPhaseMax = 2;
        this.solidCollision = true;
        this.setupMob();
    }

    @Override
    protected void registerGoals() {
		super.registerGoals();
		this.goalSelector.addGoal(this.claimDistractionGoalIndex(), new BreakDoorGoal(this));
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setLongMemory(false));

		if(this.getNavigation() instanceof GroundPathNavigation) {
            GroundPathNavigation pathNavigateGround = (GroundPathNavigation)this.getNavigation();
			pathNavigateGround.setCanOpenDoors(true);
		}
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
    	// Destroy Blocks:
		if(!this.getCommandSenderWorld().isClientSide)
	        if(this.getTarget() != null && this.getCommandSenderWorld().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING) && this.griefing) {
		    	float distance = this.getTarget().distanceTo(this);
		    		if(distance <= this.getDimensions(Pose.STANDING).width() + 4.0F)
		    			this.destroyArea((int)this.position().x(), (int)this.position().y(), (int)this.position().z(), 0.5F, true);
	        }
        
        super.aiStep();
    }

    // ==================================================
    //                      Attacks
    // ==================================================
    // ========== Ranged Attack ==========
    @Override
    public boolean attackMelee(Entity target, double damageScale) {
    	boolean success = super.attackMelee(target, damageScale);
    	if(success)
    		this.nextAttackPhase();
    	return success;
    }

    @Override
    public int getBagSize() { return this.creatureInfo.getBagSize(); }

    // ==================================================
    //                     Equipment
    // ==================================================
    @Override
    public int getNoBagSize() { return 10; }

	@Override
	public void loadCreatureFlags() {
		this.griefing = this.creatureInfo.getFlag("griefing", this.griefing);
	}

    // ==================================================
    //                     Pet Control
    // ==================================================
    public boolean petControlsEnabled() { return true; }
}
