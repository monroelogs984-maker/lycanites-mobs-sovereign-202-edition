package com.lycanitesmobs.core.entity.creature.insect;

import com.lycanitesmobs.core.entity.base.TameableCreatureEntity;
import com.lycanitesmobs.core.entity.base.AgeableCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import com.lycanitesmobs.core.entity.goals.targeting.FindAttackTargetGoal;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;

/**
 * Heavily trimmed: the original extends TameableCreatureEntity (not ported) and its whole
 * identity is the hive/dungeon-structure system (DungeonManager/CreatureStructure building a
 * vespid_hive theme, StayByHomeGoal, home-position tracking, ally-spawning worker vespids,
 * cross-reference to EntityConba's infection mechanic, and taming via CreatureTreatItem). None
 * of that (tame system, dungeon structures, ally spawning) is ported, so this is reduced to a
 * bigger, tougher flying melee attacker with no hive/home/taming behavior at all - see
 * EntityVespid.java for the same trim on the worker side.
 * PHASE 5e UPDATE (2026-09-26): re-parented to its official superclass now that TameableCreatureEntity is
 * ported (taming/ownership/pet behaviour work; RideableCreatureEntity is still a stub). Any wording above
 * about extending Base/AgeableCreatureEntity or taming being unported is outdated.
 */
public class EntityVespidQueen extends TameableCreatureEntity implements Enemy {

    public EntityVespidQueen(EntityType<? extends EntityVespidQueen> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = true;
        this.canGrow = true;
        this.babySpawnChance = 0D;
        this.setAttackCooldownMax(10);
        this.setupMob();
    }

    @Override
    public float maxUpStep() {
        return 1.0F;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setLongMemory(true));
        this.targetSelector.addGoal(this.claimFindTargetGoalIndex(), new FindAttackTargetGoal(this).addTargets(EntityType.PLAYER));
    }

    @Override
    public boolean canAttackOwnSpecies() {
        return true;
    }

    @Override
    public boolean isFlying() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (source.is(DamageTypes.IN_WALL)) return true;
        return super.isInvulnerableTo(source);
    }
}
