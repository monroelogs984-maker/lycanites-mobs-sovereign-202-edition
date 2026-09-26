package com.lycanitesmobs.core.entity.creature.aberration;

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
 */
public class EntityAstaroth extends TameableCreatureEntity implements Enemy {

    public EntityAstaroth(EntityType<? extends EntityAstaroth> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = false;
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
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        // TODO(port): official also excludes EntityMalwrath and EntityAsmodeus - restore once those are ported.
        if (target instanceof EntityTrite)
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
}
