package com.lycanitesmobs.core.entity.creature.insect;

import net.minecraft.world.entity.*;
import com.lycanitesmobs.core.entity.base.AgeableCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import com.lycanitesmobs.core.entity.goals.targeting.FindAttackTargetGoal;
import com.lycanitesmobs.core.data.tag.LycanitesBlockTags;
import com.lycanitesmobs.core.manager.CreatureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Trimmed: dropped the `attribute` field assignment (MobType.UNDEFINED - field was dropped in
 * the Phase 5 trim) and the bag/equipment overrides (getNoBagSize/getBagSize - not ported).
 */
public class EntityJousteAlpha extends AgeableCreatureEntity {

    public EntityJousteAlpha(EntityType<? extends EntityJousteAlpha> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = true;
        this.setAttackCooldownMax(10);
        // Restored from official (2026-09-28 constructor audit):
        this.attackCooldownMax = 10;
        this.setupMob();
    }

    @Override
    protected void registerGoals() {
        this.targetSelector.addGoal(this.claimFindTargetGoalIndex(), new FindAttackTargetGoal(this).addTargets(this.getClass()));

        super.registerGoals();

        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setTargetClass(Player.class).setLongMemory(false));
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this));
    }

    @Override
    public float getBlockPathWeight(int x, int y, int z) {
        BlockState blockState = this.getCommandSenderWorld().getBlockState(new BlockPos(x, y - 1, z));
        if (blockState.getBlock() != Blocks.AIR) {
            if (blockState.is(LycanitesBlockTags.CREATURE_PATH_SAND_PREFERRED))
                return 10F;
            if (blockState.is(LycanitesBlockTags.CREATURE_PATH_CLAY_PREFERRED))
                return 7F;
            if (blockState.is(LycanitesBlockTags.CREATURE_PATH_STONE_PREFERRED))
                return 5F;
        }
        return super.getBlockPathWeight(x, y, z);
    }

    @Override
    public boolean canAttackOwnSpecies() {
        return true;
    }

    @Override
    public void setTarget(LivingEntity entity) {
        LivingEntity previousTarget = this.getTarget();
        super.setTarget(entity);
        if (entity == null && previousTarget instanceof EntityJousteAlpha && this.getTarget() == null && this.getHealth() < this.getMaxHealth()) {
            this.heal((this.getMaxHealth() - this.getHealth()) / 2);
            this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 10 * 20, 2, false, true));
        }
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (source.is(DamageTypes.CACTUS)) return true;
        return super.isInvulnerableTo(source);
    }

    @Override
    public AgeableCreatureEntity createChild(AgeableCreatureEntity partner) {
        return (AgeableCreatureEntity) CreatureManager.getInstance().getCreature("jouste").createEntity(this.getCommandSenderWorld());
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
