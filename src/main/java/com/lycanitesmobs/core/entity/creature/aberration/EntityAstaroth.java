package com.lycanitesmobs.core.entity.creature.aberration;

import com.lycanitesmobs.core.entity.projectile.hellfire.EntityHellShield;
import com.lycanitesmobs.core.manager.DeferredLevelActionManager;
import com.lycanitesmobs.core.manager.ProjectileManager;
import com.lycanitesmobs.core.util.helpers.LMHelperClass;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import com.lycanitesmobs.core.entity.creature.demon.EntityMalwrath;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3d;
import com.lycanitesmobs.core.entity.goals.actions.AttackRangedGoal;
import com.lycanitesmobs.core.entity.base.TameableCreatureEntity;
import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import com.lycanitesmobs.core.manager.CreatureManager;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;

/**
 * Trimmed - original extends TameableCreatureEntity (tame/bag, not ported); rebased onto
 * BaseCreatureEntity. Dropped: AttackRangedGoal + attackRanged()/fireProjectile() (needs the
 * unported ProjectileManager - the "devilstar" ranged attack), the Asmodeus-master hellfire-
 * shield support mechanic in aiStep() (EntityHellShield/ProjectileManager, not ported), and
 * solidCollision (no such field here). The on-death trite-swarm mechanic is kept but simplified:
 * summonMinion()/setTemporary() don't exist on this port's BaseCreatureEntity, so spawned trites
 * are plain permanent entities added directly via addFreshEntity() instead of temporary minions.
 * setMaxUpStep() fixed to the 1.21.1 maxUpStep() getter override.
 * PHASE 5e UPDATE (2026-09-26): re-parented to its official superclass now that TameableCreatureEntity is
 * ported (taming/ownership/pet behaviour work; RideableCreatureEntity is still a stub). Any wording above
 * about extending Base/AgeableCreatureEntity or taming being unported is outdated.
 * PHASE 6a UPDATE (2026-09-26): ranged attack restored (projectiles ported) - any wording above about a
 * substituted melee attack or ProjectileManager being unported is outdated.
 */
public class EntityAstaroth extends TameableCreatureEntity implements Enemy {

    public EntityAstaroth(EntityType<? extends EntityAstaroth> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = false;
        // Restored from official (2026-09-28 constructor audit):
        this.solidCollision = false;
        this.setupMob();
        this.hitAreaWidthScale = 1.5F;
    }

    @Override
    public float maxUpStep() {
        return 1.0F;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setLongMemory(false).setRange(1D).setMaxChaseDistance(8.0F));
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackRangedGoal(this).setSpeed(1.0D).setRange(40.0F).setMinChaseDistance(16.0F).setChaseTime(-1));
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (target instanceof EntityTrite || target instanceof EntityMalwrath || target instanceof EntityAsmodeus)
            return false;
        return super.canAttack(target);
    }

    @Override
    public void die(DamageSource damageSource) {
        boolean wasDead = this.dead;
        super.die(damageSource);
        if (wasDead || !this.dead) {
            return;
        }

        if (!this.getCommandSenderWorld().isClientSide && CreatureManager.getInstance().getCreature("trite").isEnabled()) {
            int j = 2 + this.random.nextInt(5) + getCommandSenderWorld().getDifficulty().getId() - 1;
            for (int k = 0; k < j; ++k) {
                LivingEntity trite = CreatureManager.getInstance().getCreature("trite").createEntity(this.getCommandSenderWorld());
                if (trite != null) {
                    trite.setPos(this.position().x(), this.position().y(), this.position().z());
                    this.getCommandSenderWorld().addFreshEntity(trite);
                }
            }
        }
    }

    @Override
    public void attackRanged(Entity target, float range) {
        this.fireProjectile("devilstar", target, range, 0, new Vector3d(0, 0, 0), 1.2f, 1f, 1F);
        super.attackRanged(target, range);
    }


    // ==================================================
    //   Restored from official 2026-09-28 (method audit)
    // ==================================================
    // ==================================================
    //                      Update
    // ==================================================
    // ========== Living Update ==========
    @Override
    public void aiStep() {
        super.aiStep();

        // Asmodeus Master:
        if (this.updateTick % 20 == 0) {
            if (this.getMasterTarget() != null && this.getMasterTarget() instanceof EntityAsmodeus && ((BaseCreatureEntity) this.getMasterTarget()).getBattlePhase() > 0) {
                EntityHellShield projectile = new EntityHellShield(ProjectileManager.getInstance().getOldProjectileType(EntityHellShield.class), this.getCommandSenderWorld(), this);
                projectile.setProjectileScale(3f);
                projectile.setPos(
                        projectile.position().x(),
                        projectile.position().y() - this.getDimensions(Pose.STANDING).height() * 0.35D,
                        projectile.position().z()
                );
                double dX = this.getMasterTarget().position().x() - this.position().x();
                double dY = this.getMasterTarget().position().y() + (this.getMasterTarget().getDimensions(Pose.STANDING).height() * 0.75D) - projectile.position().y();
                double dZ = this.getMasterTarget().position().z() - this.position().z();
                double distance = Mth.sqrt(LMHelperClass.convertToFloat(dX * dX + dZ * dZ)) * 0.1F;
                float velocity = 0.8F;
                projectile.shoot(dX, dY + distance, dZ, velocity, 0.0F);
                DeferredLevelActionManager.spawnEntity(this.getCommandSenderWorld(), this.blockPosition(), null, projectile);
            }
        }
    }

    @Override
    public int getBagSize() {
        return this.creatureInfo.getBagSize();
    }

    // ==================================================
    //                     Equipment
    // ==================================================
    @Override
    public int getNoBagSize() {
        return 0;
    }
}
