package com.lycanitesmobs.core.entity.creature.elemental;

import com.lycanitesmobs.core.entity.IFusable;
import com.lycanitesmobs.core.entity.goals.actions.abilities.StealthGoal;
import com.lycanitesmobs.core.manager.CreatureManager;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3d;
import com.lycanitesmobs.core.entity.goals.actions.AttackRangedGoal;
import com.lycanitesmobs.core.entity.base.TameableCreatureEntity;
import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;

/**
 * Trimmed - original extends TameableCreatureEntity implements IFusable (tame/bag/fusion, not
 * ported - see EntityAegis). StealthGoal dropped (not ported, same as EntityWraamon). Ranged
 * chaosorb attack (AttackRangedGoal/attackRanged/fireProjectile) dropped - needs
 * ProjectileManager, not ported at all yet; Argus ends up melee-only.
 * PHASE 5e UPDATE (2026-09-26): re-parented to its official superclass now that TameableCreatureEntity is
 * ported (taming/ownership/pet behaviour work; RideableCreatureEntity is still a stub). Any wording above
 * about extending Base/AgeableCreatureEntity or taming being unported is outdated.
 * PHASE 6a UPDATE (2026-09-26): ranged attack restored (projectiles ported) - any wording above about a
 * substituted melee attack or ProjectileManager being unported is outdated.
 */
public class EntityArgus extends TameableCreatureEntity implements Enemy, IFusable {
    // Fields restored from official (2026-09-28 method audit):
    protected IFusable fusionTarget;


    public EntityArgus(EntityType<? extends EntityArgus> entityType, Level world) {
        super(entityType, world);
        this.spawnsInWater = true;
        this.hasAttackSound = true;
        this.setupMob();
        this.setAttackCooldownMax(40);
    }

    @Override
    public float maxUpStep() {
        return 1.0F;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimPriorityGoalIndex(), new StealthGoal(this).setStealthTime(20).setStealthAttack(true).setStealthMove(true));
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setLongMemory(true).setMaxChaseDistanceSq(5.0F));
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackRangedGoal(this).setSpeed(0.75D).setRange(16.0F).setMinChaseDistance(14.0F));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.getCommandSenderWorld().isClientSide)
            for (int i = 0; i < 2; ++i) {
                this.getCommandSenderWorld().addParticle(ParticleTypes.PORTAL, this.position().x() + (this.random.nextDouble() - 0.5D) * (double) this.getDimensions(Pose.STANDING).width(), this.position().y() + this.random.nextDouble() * (double) this.getDimensions(Pose.STANDING).height(), this.position().z() + (this.random.nextDouble() - 0.5D) * (double) this.getDimensions(Pose.STANDING).width(), 0.0D, 0.0D, 0.0D);
            }
    }

    @Override
    public boolean isFlying() {
        return true;
    }

    @Override
    public boolean isStrongSwimmer() {
        return true;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (source.is(DamageTypes.IN_WALL)) return true;
        return super.isInvulnerableTo(source);
    }

    @Override
    public boolean creatureCanBreatheUnderwater() {
        return true;
    }

    @Override
    public void attackRanged(Entity target, float range) {
        this.fireProjectile("chaosorb", target, range, 0, new Vector3d(0, 0, 0), 0.6f, 1f, 1F);
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
    public IFusable getFusionTarget() {
        return this.fusionTarget;
    }

    @Override
    public EntityType<? extends LivingEntity> getFusionType(IFusable fusable) {
        if (fusable instanceof EntityCinder) {
            return CreatureManager.getInstance().getEntityType("grue");
        }
        if (fusable instanceof EntityJengu) {
            return CreatureManager.getInstance().getEntityType("eechetik");
        }
        if (fusable instanceof EntityGeonach) {
            return CreatureManager.getInstance().getEntityType("tremor");
        }
        if (fusable instanceof EntityZephyr) {
            return CreatureManager.getInstance().getEntityType("wraith");
        }
        if (fusable instanceof EntityAegis) {
            return CreatureManager.getInstance().getEntityType("spectre");
        }
        return null;
    }

    // ==================================================
    //                     Equipment
    // ==================================================
    @Override
    public int getNoBagSize() {
        return 0;
    }

    // ==================================================
    //                     Pet Control
    // ==================================================
    public boolean petControlsEnabled() {
        return true;
    }

    @Override
    public void setFusionTarget(IFusable fusionTarget) {
        this.fusionTarget = fusionTarget;
    }
}
