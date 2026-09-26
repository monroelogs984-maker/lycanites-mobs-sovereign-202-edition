package com.lycanitesmobs.core.entity.creature.beast;

import com.lycanitesmobs.core.entity.base.AgeableCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Heavily trimmed - the original extends RideableCreatureEntity (the mount subsystem isn't
 * ported at all); this extends AgeableCreatureEntity instead (for canGrow/babySpawnChance, which
 * the original also used). Dropped entirely: the whole leap/mount-ability/rider-effects/stamina
 * system (mountAbility/riderEffects/getStaminaCost/getPassengersRidingOffset/getAISpeedModifier
 * and the leap-landing-paralysis aiStep logic - all mount-specific, and leap() itself isn't on
 * BaseCreatureEntity either, not added here since this batch must not touch shared base-entity
 * files while other creatures are being ported in parallel), getNoBagSize/getBagSize/
 * petControlsEnabled (bag/tame, same as EntityBalayang), and getFallResistance().
 */
public class EntityBarghest extends AgeableCreatureEntity {

    public EntityBarghest(EntityType<? extends EntityBarghest> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = true;
        this.spreadFire = false;
        this.canGrow = true;
        this.babySpawnChance = 0.1D;
        this.setupMob();
    }

    // NOTE: 1.21.1 replaced the old setMaxUpStep(float) setter with an overridable
    // maxUpStep() getter (default 0.0F on Entity) - override it directly instead.
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
    public boolean canClimb() {
        return true;
    }
}
