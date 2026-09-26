package com.lycanitesmobs.core.entity.creature.insect;

import com.lycanitesmobs.core.entity.base.BaseProjectileEntity;
import com.lycanitesmobs.core.manager.DeferredLevelActionManager;
import com.lycanitesmobs.core.manager.ProjectileManager;
import com.lycanitesmobs.core.data.info.projectile.ProjectileInfo;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3d;
import com.lycanitesmobs.core.entity.goals.actions.AttackRangedGoal;
import com.lycanitesmobs.core.entity.base.RideableCreatureEntity;
import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import com.lycanitesmobs.core.entity.goals.targeting.FindAttackTargetGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Heavily trimmed: the original extends RideableCreatureEntity and its whole identity is a
 * mountable ranged poison-laser shooter (AttackRangedGoal/mount-ability both using
 * ProjectileManager's "poisonray", none of which is ported). Reduced to a plain melee attacker
 * with a short leash range (matching the original's melee goal's 4-block max chase). Kept
 * isStrongSwimmer/isPushedByFluid (real swimming behavior, no unported dependency) and the
 * daytime-lurking isAggressive check (uses testLightLevel()/isDaytime(), already available).
 * Dropped canBreatheUnderwater (LivingEntity.canBreatheUnderwater() is final/tag-driven in
 * 1.21.1, can't be overridden - see BaseCreatureEntity's own note on this), getFallResistance
 * and petControlsEnabled (not on BaseCreatureEntity), and the tame-gated isAggressive branch.
 * PHASE 5e UPDATE (2026-09-26): re-parented to its official superclass now that TameableCreatureEntity is
 * ported (taming/ownership/pet behaviour work; RideableCreatureEntity is still a stub). Any wording above
 * about extending Base/AgeableCreatureEntity or taming being unported is outdated.
 * PHASE 6a UPDATE (2026-09-26): ranged attack restored (projectiles ported) - any wording above about a
 * substituted melee attack or ProjectileManager being unported is outdated.
 */
public class EntityEyewig extends RideableCreatureEntity {

    /** The active laser projectile (poison ray / water jet), refreshed while attacking. */
    protected BaseProjectileEntity projectile;

    public EntityEyewig(EntityType<? extends EntityEyewig> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = true;
        this.setupMob();
    }

    @Override
    public float maxUpStep() {
        return 1.0F;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setLongMemory(false).setMaxChaseDistanceSq(4.0F));
        this.targetSelector.addGoal(this.claimFindTargetGoalIndex(), new FindAttackTargetGoal(this).addTargets(EntityType.PLAYER));
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackRangedGoal(this).setSpeed(0.75D).setStaminaTime(100).setRange(8.0F).setMinChaseDistance(4.0F).setMountedAttacking(false));
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean isAggressive() {
        if (this.getCommandSenderWorld() != null && this.getCommandSenderWorld().isDay())
            return this.testLightLevel() < 2;
        return super.isAggressive();
    }

    @Override
    public boolean canClimb() {
        return true;
    }

    @Override
    public boolean isStrongSwimmer() {
        return true;
    }

    @Override
    public void attackRanged(Entity target, float range) {
        ProjectileInfo projectileInfo = ProjectileManager.getInstance().getProjectile("poisonray");
        if (projectileInfo == null) {
            return;
        }

        // Update Laser:
        if (this.projectile != null && this.projectile.isAlive()) {
            this.projectile.setProjectileLife(20);
        } else {
            this.projectile = null;
        }

        // Create New Laser:
        if (this.projectile == null) {
            this.projectile = projectileInfo.createProjectile(this.getCommandSenderWorld(), this);
            if (this.projectile.getLaunchSound() != null) {
                this.playSound(this.projectile.getLaunchSound(), 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
            }
            DeferredLevelActionManager.spawnEntity(this.getCommandSenderWorld(), this.blockPosition(), null, this.projectile);
        }

        super.attackRanged(target, range);
    }
}
