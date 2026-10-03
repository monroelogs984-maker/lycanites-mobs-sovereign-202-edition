package com.lycanitesmobs.core.entity.creature.insect;

import com.lycanitesmobs.core.entity.base.TameableCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.TemptGoal;
import com.lycanitesmobs.core.entity.goals.targeting.CopyMasterAttackTargetGoal;
import com.lycanitesmobs.core.entity.goals.targeting.FindMasterGoal;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import com.lycanitesmobs.core.entity.base.AgeableCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import com.lycanitesmobs.core.entity.goals.targeting.FindAttackTargetGoal;
import com.lycanitesmobs.core.data.info.creature.CreatureInfo;
import com.lycanitesmobs.core.data.tag.LycanitesBlockTags;
import com.lycanitesmobs.core.manager.CreatureManager;
import com.lycanitesmobs.core.manager.DeferredLevelActionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Trimmed: dropped TemptGoal/FindMasterGoal/CopyMasterAttackTargetGoal (tame/master system not
 * ported) - replaced with a plain FindAttackTargetGoal(Player) so it isn't left with no way to
 * acquire a target at all. Dropped the `attribute` field assignment and hasMaster() check in
 * canBeLeashed (also master-system). canBeLeashed fixed to 1.21.1's no-arg signature. The
 * random-chance-to-become-a-JousteAlpha mechanic is kept verbatim since it's self-contained.
 */
public class EntityJouste extends TameableCreatureEntity {
    public EntityJouste(EntityType<? extends EntityJouste> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = true;
        this.babySpawnChance = 0.1D;
        this.canGrow = true;
        this.setAttackCooldownMax(10);
        // Restored from official (2026-09-28 constructor audit):
        this.attackCooldownMax = 10;
        this.setupMob();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimDistractionGoalIndex(), new TemptGoal(this).setIncludeDiet(true));
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setLongMemory(false));

        this.targetSelector.addGoal(this.claimFindTargetGoalIndex(), new FindMasterGoal(this).setTargetClass(EntityJousteAlpha.class).setSightCheck(false));
        this.targetSelector.addGoal(this.claimFindTargetGoalIndex(), new CopyMasterAttackTargetGoal(this));
    }

    @Override
    public void onFirstSpawn() {
        CreatureInfo alphaInfo = CreatureManager.getInstance().getCreature("joustealpha");
        if (alphaInfo != null && !this.isMinion() && !this.isTamed()) { // S202: never swap a summon/pet for a wild alpha
            float alphaChance = (float) alphaInfo.getCreatureSpawn().getSpawnWeight() / Math.max(this.creatureInfo.getCreatureSpawn().getSpawnWeight(), 1);
            if (this.getRandom().nextFloat() <= alphaChance) {
                EntityJousteAlpha alpha = (EntityJousteAlpha) CreatureManager.getInstance().getCreature("joustealpha").createEntity(this.getCommandSenderWorld());
                alpha.copyPosition(this);
                DeferredLevelActionManager.spawnEntity(this.getCommandSenderWorld(), this.blockPosition(), null, alpha);
                this.remove(RemovalReason.DISCARDED);
            }
        }
        super.onFirstSpawn();
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
    public boolean canBeLeashed() {
        if (!this.hasAttackTarget())
            return true;
        return super.canBeLeashed();
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (target instanceof EntityJousteAlpha)
            return false;
        return super.canAttack(target);
    }

    @Override
    public boolean isProtective(Entity entity) {
        if (entity instanceof EntityJouste) {
            return true;
        }
        return super.isProtective(entity);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (source.is(DamageTypes.CACTUS)) return true;
        return super.isInvulnerableTo(source);
    }

    @Override
    public void setGrowingAge(int age) {
        if (age == 0 && this.getGrowingAge() < 0) {
            CreatureInfo alphaInfo = CreatureManager.getInstance().getCreature("joustealpha");
            if (alphaInfo != null && this.getRandom().nextFloat() >= 0.9F) {
                EntityJousteAlpha alpha = (EntityJousteAlpha) CreatureManager.getInstance().getCreature("joustealpha").createEntity(this.getCommandSenderWorld());
                alpha.copyPosition(this);
                DeferredLevelActionManager.spawnEntity(this.getCommandSenderWorld(), this.blockPosition(), null, alpha);
                this.remove(RemovalReason.DISCARDED);
            }
        }
        super.setGrowingAge(age);
    }
}
