package com.lycanitesmobs.core.entity.creature.beast;

import com.lycanitesmobs.core.entity.base.TameableCreatureEntity;
import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.level.Level;

/**
 * Trimmed - the original extends TameableCreatureEntity (not ported), dropping
 * MobType.UNDEFINED/getNoBagSize/getBagSize/petControlsEnabled the same as EntityBalayang.
 * Also dropped: the random-leap aiStep behavior (leap() isn't on BaseCreatureEntity - not added
 * here since this batch must not touch shared base-entity files while other creatures are being
 * ported in parallel) and the getFallResistance() immunity override (same reason).
 * PHASE 5e UPDATE (2026-09-26): re-parented to its official superclass now that TameableCreatureEntity is
 * ported (taming/ownership/pet behaviour work; RideableCreatureEntity is still a stub). Any wording above
 * about extending Base/AgeableCreatureEntity or taming being unported is outdated.
 */
public class EntityDawon extends TameableCreatureEntity {

    public EntityDawon(EntityType<? extends EntityDawon> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = true;
        this.setupMob();
        this.attackCooldownMax = 15;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setTargetClass(ZombifiedPiglin.class).setSpeed(1.5D).setDamageScale(8.0D).setRange(2.5D));
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setSpeed(1.5D));
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
        super.aiStep();

        // Random Leaping:
        if(this.onGround() && !this.getCommandSenderWorld().isClientSide) {
            if(this.hasAttackTarget()) {
                if(this.random.nextInt(10) == 0)
                    this.leap(16.0F, 0.2D, this.getTarget());
            }
            else {
                if(this.isMoving() && this.random.nextInt(50) == 0)
                    this.leap(2.0D, 0.5D);
            }
        }
    }

    @Override
    public int getBagSize() { return this.creatureInfo.getBagSize(); }

    // ==================================================
    //                     Immunities
    // ==================================================
    @Override
    public float getFallResistance() {
        return 100;
    }

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
