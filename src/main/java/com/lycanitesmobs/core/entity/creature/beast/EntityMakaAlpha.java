package com.lycanitesmobs.core.entity.creature.beast;

import com.lycanitesmobs.core.entity.base.AgeableCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import com.lycanitesmobs.core.entity.goals.targeting.FindAttackTargetGoal;
import com.lycanitesmobs.core.manager.CreatureManager;
import com.lycanitesmobs.core.data.tag.LycanitesBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Trimmed - dropped DefendEntitiesGoal (not ported - the pack-defense AI goal); isProtective()
 * still returns true for EntityMaka so the "alpha protects its pack" relationship still applies
 * to targeting/AI checks that consult it, just not as its own dedicated goal. Dropped
 * getDamageModifier(DamageSource) (no such hook in this port's CreatureModel/BaseCreatureEntity),
 * getNoBagSize/getBagSize (bag subsystem not ported), and the MobType.UNDEFINED attribute
 * assignment. canBeLeashed() is the 1.21.1 no-arg signature (was canBeLeashed(Player)).
 */
public class EntityMakaAlpha extends AgeableCreatureEntity {

    public EntityMakaAlpha(EntityType<? extends EntityMakaAlpha> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = true;
        this.attackCooldownMax = 10;
        this.setupMob();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.targetSelector.addGoal(this.claimFindTargetGoalIndex(), new FindAttackTargetGoal(this).addTargets(this.getClass()));
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setTargetClass(Player.class).setLongMemory(false));
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();

        // Alpha Sparring Cooldown:
        if (this.hasAttackTarget() && this.getTarget() instanceof EntityMakaAlpha) {
            if (this.getHealth() / this.getMaxHealth() <= 0.25F || this.getTarget().getHealth() / this.getTarget().getMaxHealth() <= 0.25F) {
                this.setTarget(null);
            }
        }
    }

    @Override
    public boolean isProtective(Entity entity) {
        if (entity instanceof EntityMaka) {
            return true;
        }
        return super.isProtective(entity);
    }

    @Override
    public float getBlockPathWeight(int x, int y, int z) {
        BlockState blockState = this.getCommandSenderWorld().getBlockState(new BlockPos(x, y - 1, z));
        if (blockState.is(LycanitesBlockTags.CREATURE_PATH_GRASS_PREFERRED))
            return 10F;
        if (blockState.is(LycanitesBlockTags.CREATURE_PATH_DIRT_PREFERRED))
            return 7F;
        return super.getBlockPathWeight(x, y, z);
    }

    @Override
    public boolean canBeLeashed() {
        return true;
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (target instanceof EntityMaka)
            return false;
        if (target instanceof EntityMakaAlpha && (this.getHealth() / this.getMaxHealth() <= 0.25F || target.getHealth() / target.getMaxHealth() <= 0.25F))
            return false;
        return super.canAttack(target);
    }

    @Override
    public boolean canAttackOwnSpecies() {
        return true;
    }

    @Override
    public void setTarget(LivingEntity entity) {
        LivingEntity previousTarget = this.getTarget();
        super.setTarget(entity);
        if (entity == null && previousTarget instanceof EntityMakaAlpha && this.getTarget() == null) {
            this.heal((this.getMaxHealth() - this.getHealth()) / 2);
            this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 20, 2, false, false));
            previousTarget.heal((this.getMaxHealth() - this.getHealth()) / 2);
            previousTarget.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 20, 2, false, false));
        }
    }

    @Override
    public boolean rollAttackTargetChance(LivingEntity target) {
        if (target instanceof Player || target.getType() == this.getType())
            return this.getRandom().nextDouble() <= 0.01D;
        return true;
    }

    @Override
    public AgeableCreatureEntity createChild(AgeableCreatureEntity partner) {
        return (AgeableCreatureEntity) CreatureManager.getInstance().getCreature("maka").createEntity(this.getCommandSenderWorld());
    }
}
