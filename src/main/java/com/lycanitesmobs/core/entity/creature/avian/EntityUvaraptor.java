package com.lycanitesmobs.core.entity.creature.avian;

import com.lycanitesmobs.core.entity.base.RideableCreatureEntity;
import com.lycanitesmobs.core.entity.base.AgeableCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Trimmed - was RideableCreatureEntity (not ported, now AgeableCreatureEntity). Dropped the
 * mount-ability/stamina system and the random-leaping aiStep behaviour (leap() isn't a ported
 * method), and getFallResistance() (no hook to override).
 * PHASE 5e UPDATE (2026-09-26): re-parented to its official superclass now that TameableCreatureEntity is
 * ported (taming/ownership/pet behaviour work; RideableCreatureEntity is still a stub). Any wording above
 * about extending Base/AgeableCreatureEntity or taming being unported is outdated.
 */
public class EntityUvaraptor extends RideableCreatureEntity {

    public EntityUvaraptor(EntityType<? extends EntityUvaraptor> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = true;
        this.hasJumpSound = true;
        this.canGrow = true;
        this.babySpawnChance = 0.01D;
        this.setupMob();
    }

    // 1.21.1 replaced the old setMaxUpStep(float) with an overridable maxUpStep() getter.
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
    public double getFallingMod() {
        return 0.9D;
    }
}
