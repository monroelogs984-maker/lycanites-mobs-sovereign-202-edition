package com.lycanitesmobs.core.entity.creature.beast;

import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;

/**
 * Trimmed - the original extends TameableCreatureEntity (not ported), dropping
 * MobType.UNDEFINED/getNoBagSize/getBagSize/petControlsEnabled the same as EntityBalayang. The
 * original's only real attack is ranged (AttackRangedGoal/attackRanged firing "quill"
 * projectiles), with melee explicitly disabled as a fallback - ProjectileManager isn't ported at
 * all yet, so the ranged goal is dropped, and the melee goal is left ENABLED instead of disabled
 * (unlike the original) so this creature isn't completely toothless with no attack at all.
 */
public class EntityBrucha extends BaseCreatureEntity implements Enemy {

    public EntityBrucha(EntityType<? extends EntityBrucha> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = true;
        this.setupMob();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setLongMemory(true));
    }
}
