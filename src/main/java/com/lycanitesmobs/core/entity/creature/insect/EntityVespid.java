package com.lycanitesmobs.core.entity.creature.insect;

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
 * Heavily trimmed: the original's entire identity is the hive-building master/slave system
 * (FindMasterGoal targeting EntityVespidQueen, PlaceBlockGoal building the hive via
 * CreatureStructure/DungeonManager, CreatureRelationshipEntry attack-permission checks, and a
 * cross-reference to EntityConba's infection mechanic). None of that master/hive/dungeon-
 * structure infrastructure is ported, so this is reduced to a plain flying melee attacker -
 * still flies, still stings, just doesn't build anything or answer to a queen. See
 * EntityVespidQueen.java for the same trim on the queen side.
 */
public class EntityVespid extends AgeableCreatureEntity implements Enemy {

    public EntityVespid(EntityType<? extends EntityVespid> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = true;
        this.canGrow = true;
        this.babySpawnChance = 0.1D;
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
    public boolean isFlying() {
        return true;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (source.is(DamageTypes.IN_WALL)) return true;
        return super.isInvulnerableTo(source);
    }
}
