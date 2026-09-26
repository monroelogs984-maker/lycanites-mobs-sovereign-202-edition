package com.lycanitesmobs.core.entity.creature.demon;

import net.minecraft.world.entity.Entity;
import org.joml.Vector3d;
import com.lycanitesmobs.core.entity.goals.actions.AttackRangedGoal;
import com.lycanitesmobs.core.entity.base.TameableCreatureEntity;
import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;

/**
 * Trimmed - original extends TameableCreatureEntity (tame/pet-control/bag, not ported) and is
 * entirely a ranged fireball caster (AttackRangedGoal/fireProjectile need ProjectileManager,
 * not ported) plus door-breaking (BreakDoorGoal not ported) and a client-side hellfire-orb
 * visual sync (EntityRahovart.updateHellfireOrbs, rendering helper, not ported). Substituted a
 * plain melee goal so it isn't left with no attack at all after the ranged trim.
 * PHASE 5e UPDATE (2026-09-26): re-parented to its official superclass now that TameableCreatureEntity is
 * ported (taming/ownership/pet behaviour work; RideableCreatureEntity is still a stub). Any wording above
 * about extending Base/AgeableCreatureEntity or taming being unported is outdated.
 * PHASE 6a UPDATE (2026-09-26): ranged attack restored (projectiles ported) - any wording above about a
 * substituted melee attack or ProjectileManager being unported is outdated.
 */
public class EntityBelphegor extends TameableCreatureEntity implements Enemy {

    public EntityBelphegor(EntityType<? extends EntityBelphegor> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = false;
        this.setupMob();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackRangedGoal(this).setSpeed(1.0D).setRange(16.0F).setMinChaseDistance(8.0F).setChaseTime(-1));
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (target instanceof EntityBehemophet)
            return false;
        return super.canAttack(target);
    }

    @Override
    public boolean canBurn() {
        return false;
    }

    @Override
    public void attackRanged(Entity target, float range) {
        this.fireProjectile("doomfireball", target, range, 0, new Vector3d(0, 0, 0), 1.2f, 2f, 1F);
        super.attackRanged(target, range);
    }
}
