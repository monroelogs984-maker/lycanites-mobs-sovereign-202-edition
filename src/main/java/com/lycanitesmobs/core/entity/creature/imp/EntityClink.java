package com.lycanitesmobs.core.entity.creature.imp;

import com.lycanitesmobs.core.entity.base.TameableCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;

/**
 * Trimmed - original is a purely ranged attacker (thrown scythe via AttackRangedGoal/
 * fireProjectile, phase-cycling melee/ranged cooldowns via nextAttackPhase/getRangedCooldown -
 * neither exists on this port's BaseCreatureEntity) - ProjectileManager isn't ported, so
 * substituted a plain AttackMeleeGoal and dropped the phase-cooldown cycling entirely. Dropped
 * petControlsEnabled/bag getters (tame/equipment, not ported).
 */
public class EntityClink extends TameableCreatureEntity implements Enemy {

    public EntityClink(EntityType<? extends EntityClink> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = false;
        this.setupMob();
        this.setAttackCooldownMax(10);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this));
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (source.is(DamageTypes.CACTUS)) return true;
        return super.isInvulnerableTo(source);
    }
}
