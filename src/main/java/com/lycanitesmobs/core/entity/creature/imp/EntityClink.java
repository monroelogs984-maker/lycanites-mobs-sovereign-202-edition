package com.lycanitesmobs.core.entity.creature.imp;

import net.minecraft.world.entity.Entity;
import org.joml.Vector3d;
import com.lycanitesmobs.core.entity.goals.actions.AttackRangedGoal;
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
 * PHASE 6a UPDATE (2026-09-26): ranged attack restored (projectiles ported) - any wording above about a
 * substituted melee attack or ProjectileManager being unported is outdated.
 */
public class EntityClink extends TameableCreatureEntity implements Enemy {

    public EntityClink(EntityType<? extends EntityClink> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = false;
        // Restored from official (2026-09-28 constructor audit):
        this.attackPhaseMax = 3;
        this.setupMob();
        this.setAttackCooldownMax(10);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackRangedGoal(this).setSpeed(0.75D).setRange(14.0F).setMinChaseDistance(4.0F));
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (source.is(DamageTypes.CACTUS)) return true;
        return super.isInvulnerableTo(source);
    }

    @Override
    public void attackRanged(Entity target, float range) {
        this.fireProjectile("throwingscythe", target, range, 0, new Vector3d(0, 0, 0), 1.2f, 2f, 1F);
        this.nextAttackPhase();
        super.attackRanged(target, range);
    }


    // ==================================================
    //   Restored from official 2026-09-28 (method audit)
    // ==================================================
    @Override
    public int getBagSize() {
        return this.creatureInfo.getBagSize();
    }

    @Override
    public int getMeleeCooldown() {
        if (this.getAttackPhase() == 2)
            return super.getMeleeCooldown();
        return Math.round((float) super.getMeleeCooldown() / 6);
    }

    // ==================================================
    //                     Equipment
    // ==================================================
    @Override
    public int getNoBagSize() {
        return 0;
    }

    @Override
    public int getRangedCooldown() {
        if (this.getAttackPhase() == 2)
            return super.getRangedCooldown();
        return Math.round((float) super.getRangedCooldown() / 6);
    }

    // ==================================================
    //                     Pet Control
    // ==================================================
    public boolean petControlsEnabled() {
        return true;
    }
}
