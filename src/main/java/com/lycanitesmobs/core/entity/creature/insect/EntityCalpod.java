package com.lycanitesmobs.core.entity.creature.insect;

import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import com.lycanitesmobs.core.entity.goals.targeting.FindAttackTargetGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;

/**
 * First proof-of-concept creature for the port - chosen because it extends BaseCreatureEntity
 * directly (unlike ~69 creatures that go through TameableCreatureEntity/AgeableCreatureEntity,
 * neither ported yet). Trimmed: swarm minion-spawning (allyUpdate/spawnAlly, needs
 * DeferredLevelActionManager), block-griefing on attack (destroyAreaBlock), and bag-size
 * equipment overrides are dropped - see PORT_PLAN.md Phase 5/6.
 */
public class EntityCalpod extends BaseCreatureEntity implements Enemy {

    public EntityCalpod(EntityType<? extends EntityCalpod> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = true;
        this.setupMob();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.targetSelector.addGoal(this.claimFindTargetGoalIndex(), new FindAttackTargetGoal(this).addTargets(EntityType.PLAYER));
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setLongMemory(true));
    }

    @Override
    public boolean canClimb() {
        return true;
    }
}
