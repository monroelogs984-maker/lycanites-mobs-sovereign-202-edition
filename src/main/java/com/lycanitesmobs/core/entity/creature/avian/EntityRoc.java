package com.lycanitesmobs.core.entity.creature.avian;

import com.lycanitesmobs.core.entity.base.RideableCreatureEntity;
import com.lycanitesmobs.core.entity.base.AgeableCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import com.lycanitesmobs.core.entity.goals.targeting.FindAttackTargetGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;

/**
 * Trimmed - was RideableCreatureEntity implements Enemy (now AgeableCreatureEntity). Roc's whole
 * identity in the original is picking up creepers and dropping them on targets - that entire
 * mechanic depends on the unported entity-pickup system/ExtendedEntity capability (same gap as
 * EntityRaiko), so it's dropped entirely here; kept the creeper-hunting target goal since that's
 * cheap and still flavorful even without the carry-and-drop payoff.
 * PHASE 5e UPDATE (2026-09-26): re-parented to its official superclass now that TameableCreatureEntity is
 * ported (taming/ownership/pet behaviour work; RideableCreatureEntity is still a stub). Any wording above
 * about extending Base/AgeableCreatureEntity or taming being unported is outdated.
 */
public class EntityRoc extends RideableCreatureEntity implements Enemy {

    protected boolean creeperDropping = true;

    public EntityRoc(EntityType<? extends EntityRoc> entityType, Level world) {
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
        if (this.creeperDropping) {
            this.targetSelector.addGoal(this.claimFindTargetGoalIndex(), new FindAttackTargetGoal(this).addTargets(EntityType.CREEPER));
        }
        super.registerGoals();
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setLongMemory(false));
    }

    @Override
    public void loadCreatureFlags() {
        this.creeperDropping = this.creatureInfo.getFlag("creeperDropping", this.creeperDropping);
    }
}
