package com.lycanitesmobs.core.entity.creature.insect;

import com.lycanitesmobs.core.entity.base.TameableCreatureEntity;
import com.lycanitesmobs.core.entity.creature.beast.EntityBalayang;
import com.lycanitesmobs.core.entity.goals.targeting.FindAvoidTargetGoal;
import net.minecraft.world.entity.player.Player;
import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import com.lycanitesmobs.core.entity.goals.targeting.FindAttackTargetGoal;
import com.lycanitesmobs.core.manager.DeferredLevelActionManager;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;

/**
 * Trimmed: dropped the FindAvoidTargetGoal(EntityBalayang) (balayang is a different fork's
 * batch - avoid a cross-batch class-load-order dependency), the `attribute` field assignment
 * (MobType.ARTHROPOD - field was dropped in the Phase 5 trim), setMinion() (marker not ported),
 * and the bag/equipment overrides. The minion-swarm-on-hit-target mechanic is kept since it's
 * self-contained (just spawns more gorgomites via CreatureManager, same pattern as
 * EntityConcapedeHead's segment spawning).
 */
public class EntityGorgomite extends TameableCreatureEntity implements Enemy {
    private int swarmLimit = 5;

    public EntityGorgomite(EntityType<? extends EntityGorgomite> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = true;
        this.setupMob();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setLongMemory(true));
        this.targetSelector.addGoal(this.claimSpecialTargetGoalIndex(), new FindAvoidTargetGoal(this).setTargetClass(EntityBalayang.class));
    }

    @Override
    public void loadCreatureFlags() {
        this.swarmLimit = this.creatureInfo.getFlag("swarmLimit", this.swarmLimit);
    }

    @Override
    public void aiStep() {
        if (!this.getCommandSenderWorld().isClientSide && !this.isTamed() && this.hasAttackTarget() && this.getTarget() instanceof net.minecraft.world.entity.player.Player && this.updateTick % 60 == 0) {
            this.allyUpdate();
        }
        super.aiStep();
    }

    public void allyUpdate() {
        if (this.getCommandSenderWorld().isClientSide)
            return;
        if (this.swarmLimit > 0 && this.countAllies(64D) < this.swarmLimit) {
            float random = this.random.nextFloat();
            if (random <= 0.25F)
                this.spawnAlly(this.position().x() - 2 + (random * 4), this.position().y(), this.position().z() - 2 + (random * 4));
        }
    }

    public void spawnAlly(double x, double y, double z) {
        BaseCreatureEntity minion = (BaseCreatureEntity) this.creatureInfo.createEntity(this.getCommandSenderWorld());
        minion.moveTo(x, y, z, this.random.nextFloat() * 360.0F, 0.0F);
        minion.applyVariant(this.getVariantIndex());
        DeferredLevelActionManager.spawnEntity(this.getCommandSenderWorld(), this.blockPosition(), null, minion);
        if (this.getTarget() != null)
            minion.setLastHurtByMob(this.getTarget());
    }

    @Override
    public void die(DamageSource damageSource) {
        boolean wasDead = this.dead;
        super.die(damageSource);
        if (wasDead || !this.dead) {
            return;
        }
        allyUpdate();
    }

    @Override
    public boolean canClimb() {
        return true;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (source.is(DamageTypes.CACTUS)) return true;
        return super.isInvulnerableTo(source);
    }


    // ==================================================
    //   Restored from official 2026-09-28 (method audit)
    // ==================================================
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
