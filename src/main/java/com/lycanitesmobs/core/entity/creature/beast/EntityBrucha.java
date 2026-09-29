package com.lycanitesmobs.core.entity.creature.beast;

import com.lycanitesmobs.core.entity.goals.actions.AvoidGoal;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3d;
import com.lycanitesmobs.core.entity.goals.actions.AttackRangedGoal;
import com.lycanitesmobs.core.entity.base.TameableCreatureEntity;
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
 * PHASE 5e UPDATE (2026-09-26): re-parented to its official superclass now that TameableCreatureEntity is
 * ported (taming/ownership/pet behaviour work; RideableCreatureEntity is still a stub). Any wording above
 * about extending Base/AgeableCreatureEntity or taming being unported is outdated.
 * PHASE 6a UPDATE (2026-09-26): ranged attack restored (projectiles ported) - any wording above about a
 * substituted melee attack or ProjectileManager being unported is outdated.
 */
public class EntityBrucha extends TameableCreatureEntity implements Enemy {

    public EntityBrucha(EntityType<? extends EntityBrucha> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = true;
        this.setupMob();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setLongMemory(true).setEnabled(false));
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackRangedGoal(this).setSpeed(1.0D).setRange(16.0F).setMinChaseDistance(10.0F).setChaseTime(-1));
    }

    @Override
    public void attackRanged(Entity target, float range) {
        for (int i = -2; i < 12; i++) {
            this.fireProjectile("quill", target, range, 0, new Vector3d(0, 0, 0), 0.75f, 1f, i * 2.0F * (this.getRandom().nextFloat() - 0.5F));
        }
        super.attackRanged(target, range);
    }


    // ==================================================
    //   Restored from official 2026-09-28 (method audit)
    // ==================================================
    @Override
    public int getBagSize() { return this.creatureInfo.getBagSize(); }

    // ==================================================
    //                     Equipment
    // ==================================================
    @Override
    public int getNoBagSize() { return 0; }

    // ==================================================
    //                     Pet Control
    // ==================================================
    public boolean petControlsEnabled() { return true; }
}
