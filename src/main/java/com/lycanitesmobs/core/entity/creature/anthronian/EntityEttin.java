package com.lycanitesmobs.core.entity.creature.anthronian;

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

    public EntityEttin(EntityType<? extends EntityEttin> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = true;
        this.setupMob();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setLongMemory(false));

        if (this.getNavigation() instanceof GroundPathNavigation) {
            GroundPathNavigation pathNavigateGround = (GroundPathNavigation) this.getNavigation();
            pathNavigateGround.setCanOpenDoors(true);
        }
    }
}
