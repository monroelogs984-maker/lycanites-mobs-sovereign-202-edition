package com.lycanitesmobs.core.entity.creature.demon;

import com.lycanitesmobs.core.entity.base.BaseProjectileEntity;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3d;
import com.lycanitesmobs.core.entity.goals.actions.AttackRangedGoal;
import com.lycanitesmobs.core.entity.base.TameableCreatureEntity;
import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Trimmed - original extends TameableCreatureEntity (tame/pet-control/bag, not ported) and its
 * whole kit is ranged fireballs (AttackRangedGoal/fireProjectile - needs ProjectileManager,
 * not ported), ally buff auras (EffectAuraGoal - not ported), and minion summoning
 * (SummonMinionsGoal targeting belphegor - not ported). Reduced to a plain melee attacker with
 * base stats; kept the cosmetic smoke/flame particle trail since it's self-contained.
 * PHASE 5e UPDATE (2026-09-26): re-parented to its official superclass now that TameableCreatureEntity is
 * ported (taming/ownership/pet behaviour work; RideableCreatureEntity is still a stub). Any wording above
 * about extending Base/AgeableCreatureEntity or taming being unported is outdated.
 * PHASE 6a UPDATE (2026-09-26): ranged attack restored (projectiles ported) - any wording above about a
 * substituted melee attack or ProjectileManager being unported is outdated.
 */
public class EntityApollyon extends TameableCreatureEntity implements Enemy {

    public EntityApollyon(EntityType<? extends EntityApollyon> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = false;
        this.setupMob();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackRangedGoal(this).setSpeed(1.0D).setRange(32.0F).setMinChaseDistance(16.0F).setChaseTime(-1));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getCommandSenderWorld().isClientSide) {
            for (int i = 0; i < 2; ++i) {
                this.getCommandSenderWorld().addParticle(ParticleTypes.SMOKE,
                        this.position().x() + (this.random.nextDouble() - 0.5D) * (double) this.getDimensions(Pose.STANDING).width(),
                        this.position().y() + this.random.nextDouble() * (double) this.getDimensions(Pose.STANDING).height(),
                        this.position().z() + (this.random.nextDouble() - 0.5D) * (double) this.getDimensions(Pose.STANDING).width(),
                        0.0D, 0.0D, 0.0D);
                this.getCommandSenderWorld().addParticle(ParticleTypes.FLAME,
                        this.position().x() + (this.random.nextDouble() - 0.5D) * (double) this.getDimensions(Pose.STANDING).width(),
                        this.position().y() + this.random.nextDouble() * (double) this.getDimensions(Pose.STANDING).height(),
                        this.position().z() + (this.random.nextDouble() - 0.5D) * (double) this.getDimensions(Pose.STANDING).width(),
                        0.0D, 0.0D, 0.0D);
            }
        }
    }

    @Override
    public boolean canBurn() {
        return false;
    }

    @Override
    public void attackRanged(Entity target, float range) {
        BaseProjectileEntity projectile = this.fireProjectile("doomfireball", target, range, 0, new Vector3d(0, 0, 0), 0.6f, 3f, 4F);
        if (projectile != null) {
            projectile.setPos(target.getX(), target.getY() + 4 + this.getRandom().nextDouble() * 3, target.getZ());
            projectile.shoot(0, -1, 0, 0.5F, 4);
        }
        super.attackRanged(target, range);
    }
}
