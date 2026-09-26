package com.lycanitesmobs.core.entity.creature.insect;

import net.minecraft.world.entity.Entity;
import org.joml.Vector3d;
import com.lycanitesmobs.core.entity.goals.actions.AttackRangedGoal;
import com.lycanitesmobs.core.entity.base.RideableCreatureEntity;
import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import com.lycanitesmobs.core.entity.goals.targeting.FindAttackTargetGoal;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Heavily trimmed: the original extends RideableCreatureEntity and its whole identity is a
 * mountable ranged mud-shooter (AttackRangedGoal/ProjectileManager "mudshot" projectile, a
 * stamina-gated mount ability, speed-boost terrain tags, passenger riding offset). None of the
 * ride/mount/stamina/projectile subsystems are ported, so this is reduced to a plain ground
 * melee attacker - still walks around and fights, just can't be ridden or shoot mud.
 * PHASE 5e UPDATE (2026-09-26): re-parented to its official superclass now that TameableCreatureEntity is
 * ported (taming/ownership/pet behaviour work; RideableCreatureEntity is still a stub). Any wording above
 * about extending Base/AgeableCreatureEntity or taming being unported is outdated.
 * PHASE 6a UPDATE (2026-09-26): ranged attack restored (projectiles ported) - any wording above about a
 * substituted melee attack or ProjectileManager being unported is outdated.
 */
public class EntityErepede extends RideableCreatureEntity {

    public EntityErepede(EntityType<? extends EntityErepede> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = false;
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
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackRangedGoal(this).setSpeed(0.75D).setRange(14.0F).setMinChaseDistance(6.0F));
        this.targetSelector.addGoal(this.claimFindTargetGoalIndex(), new FindAttackTargetGoal(this).addTargets(EntityType.PLAYER));
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (source.is(DamageTypes.CACTUS)) return true;
        return super.isInvulnerableTo(source);
    }

    @Override
    public void attackRanged(Entity target, float range) {
        this.fireProjectile("mudshot", target, range, 0, new Vector3d(0, 0, 0), 1.2f, 2f, 1F);
        super.attackRanged(target, range);
    }
}
