package com.lycanitesmobs.core.entity.creature.beast;

import com.lycanitesmobs.core.entity.base.RideableCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Trimmed - the original extends RideableCreatureEntity for a full leap-mount ability (queued
 * leap, landing-paralysis AoE, stamina cost, rider effects). RideableCreatureEntity is only a
 * minimal stub here (no mount/stamina system ported), and there is no generic leap() hook on
 * BaseCreatureEntity either, so the entire leap-mount ability is dropped - this is a plain
 * ageable ground melee attacker for now. Also dropped: the MobType.UNDEFINED attribute
 * assignment (field not ported) and getNoBagSize/getBagSize (bag subsystem not ported).
 * setMaxUpStep(1.0F) doesn't exist in 1.21.1 (step height is attribute-driven) - set via
 * Attributes.STEP_HEIGHT directly instead.
 */
public class EntityFeradon extends RideableCreatureEntity {

    public EntityFeradon(EntityType<? extends EntityFeradon> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = true;
        this.spreadFire = false;
        this.canGrow = true;
        this.babySpawnChance = 0.1D;
        this.setupMob();
        this.getAttribute(Attributes.STEP_HEIGHT).setBaseValue(1.0D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setTargetClass(Player.class).setLongMemory(false));
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this));
    }
}
