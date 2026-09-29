package com.lycanitesmobs.core.entity.creature.elemental;

import com.lycanitesmobs.core.entity.IFusable;
import com.lycanitesmobs.core.entity.goals.targeting.DefendEntitiesGoal;
import com.lycanitesmobs.core.entity.goals.targeting.DefendVillageGoal;
import com.lycanitesmobs.core.manager.CreatureManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import com.lycanitesmobs.core.entity.base.TameableCreatureEntity;
import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Trimmed - original extends TameableCreatureEntity implements IFusable (tame/master/bag and
 * the elemental-fusion system, e.g. Aegis+Cinder->Wisp - not ported, needs cross-referencing
 * several sibling elemental classes and a fusion-trigger system that doesn't exist here).
 * Rebased onto BaseCreatureEntity. DefendVillageGoal/DefendEntitiesGoal dropped (not ported).
 * canBeTargetedBy/shouldCreatureGroupHunt/onDamage dropped (getDamageModifier restored 2026-09-29) - not real
 * override points on this port's BaseCreatureEntity. The blocking/shield mechanic
 * (setBlocking/isBlocking/canAttackWhileBlocking) is already a real generic system on
 * BaseCreatureEntity, kept as-is with Aegis's own randomized setBlocking() override.
 * PHASE 5e UPDATE (2026-09-26): re-parented to its official superclass now that TameableCreatureEntity is
 * ported (taming/ownership/pet behaviour work; RideableCreatureEntity is still a stub). Any wording above
 * about extending Base/AgeableCreatureEntity or taming being unported is outdated.
 */
public class EntityAegis extends TameableCreatureEntity implements IFusable {
    // Fields restored from official (2026-09-28 method audit):
    protected IFusable fusionTarget;


    public EntityAegis(EntityType<? extends EntityAegis> entityType, Level world) {
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
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setLongMemory(true));

        this.targetSelector.addGoal(this.claimSpecialTargetGoalIndex(), new DefendVillageGoal(this));
        this.targetSelector.addGoal(this.claimSpecialTargetGoalIndex(), new DefendEntitiesGoal(this, Villager.class));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.getCommandSenderWorld().isClientSide) {
            if (!this.hasAttackTarget() && this.currentBlockingTime < 2) {
                this.setBlocking();
            }
        }
    }

    @Override
    public boolean isFlying() {
        return true;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (source.is(DamageTypes.CACTUS) || source.is(DamageTypes.IN_WALL)) return true;
        return super.isInvulnerableTo(source);
    }

    @Override
    public void setBlocking() {
        this.currentBlockingTime = this.blockingTime + this.getRandom().nextInt(this.blockingTime / 2);
    }


    // ==================================================
    //   Restored from official 2026-09-28 (method audit)
    // ==================================================
    @Override
    public boolean canAttackWhileBlocking() {
        return false;
    }

    @Override
    public boolean canBeTargetedBy(LivingEntity entity) {
        if (entity instanceof IronGolem || entity instanceof Villager) {
            return false;
        }
        return super.canBeTargetedBy(entity);
    }

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
            return CreatureManager.getInstance().getEntityType("wisp");
        }
        if (fusable instanceof EntityJengu) {
            return CreatureManager.getInstance().getEntityType("nymph");
        }
        if (fusable instanceof EntityGeonach) {
            return CreatureManager.getInstance().getEntityType("vapula");
        }
        if (fusable instanceof EntityZephyr) {
            return CreatureManager.getInstance().getEntityType("sylph");
        }
        if (fusable instanceof EntityArgus) {
            return CreatureManager.getInstance().getEntityType("spectre");
        }
        return null;
    }

    @Override
    public int getNoBagSize() {
        return 0;
    }

    /**
     * Called when this mob has received damage. Here a random blocking chance is applied.
     **/
    @Override
    public void onDamage(DamageSource damageSrc, float damage) {
        if (this.getRandom().nextDouble() > 0.75D && this.getHealth() / this.getMaxHealth() > 0.25F)
            this.setBlocking();
        if (damageSrc.getEntity() != null) {
            if (damageSrc.getEntity() instanceof Monster)
                damage *= 0.5F;
        }
        super.onDamage(damageSrc, damage);
    }

    public boolean petControlsEnabled() {
        return true;
    }

    @Override
    public void setFusionTarget(IFusable fusionTarget) {
        this.fusionTarget = fusionTarget;
    }

    @Override
    public boolean shouldCreatureGroupHunt(LivingEntity target) {
        if (target instanceof TameableCreatureEntity && ((TameableCreatureEntity) target).isTamed()) {
            return false;
        }
        return super.shouldCreatureGroupHunt(target);
    }

    // Restored from official 2026-09-29 (method audit) - hooked in BaseCreatureEntity.hurt().
    @Override
    public float getDamageModifier(DamageSource damageSrc) {
        if (!this.isBlocking()) {
            return 2.0F;
        }
        return super.getDamageModifier(damageSrc);
    }
}
