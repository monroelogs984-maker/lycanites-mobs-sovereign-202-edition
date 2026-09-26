package com.lycanitesmobs.core.entity.creature.demon;

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
 */
public class EntityApollyon extends BaseCreatureEntity implements Enemy {

    public EntityApollyon(EntityType<? extends EntityApollyon> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = false;
        this.setupMob();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this));
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
}
