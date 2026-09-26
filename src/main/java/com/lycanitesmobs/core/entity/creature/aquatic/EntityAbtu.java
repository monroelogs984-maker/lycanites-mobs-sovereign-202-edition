package com.lycanitesmobs.core.entity.creature.aquatic;

import com.lycanitesmobs.core.entity.base.TameableCreatureEntity;
import com.lycanitesmobs.core.entity.base.AgeableCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import com.lycanitesmobs.core.manager.DeferredLevelActionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Trimmed - TameableCreatureEntity -> AgeableCreatureEntity (see EntityAglebemu). Dropped: the
 * random-leap-at-target behaviour (leap() hook not on the trimmed BaseCreatureEntity), and
 * minion.setMinion(true) when spawning allies (the MINION creature-state flag exists as an enum
 * constant but has no getter/setter wired up yet - purely cosmetic, harmless to skip).
 * nearbyCreatureCount(type, range) replaced with the equivalent countAllies(range) (both just
 * count same-type nearby entities). Ally-spawning (allyUpdate/spawnAlly) is otherwise kept
 * faithfully - DeferredLevelActionManager is real now (added for concapede).
 * PHASE 5e UPDATE (2026-09-26): re-parented to its official superclass now that TameableCreatureEntity is
 * ported (taming/ownership/pet behaviour work; RideableCreatureEntity is still a stub). Any wording above
 * about extending Base/AgeableCreatureEntity or taming being unported is outdated.
 */
public class EntityAbtu extends TameableCreatureEntity implements Enemy {
    int swarmLimit = 5;

    public EntityAbtu(EntityType<? extends EntityAbtu> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = true;
        this.babySpawnChance = 0.9D;
        this.canGrow = true;
        this.setupMob();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setLongMemory(false));
    }

    @Override
    public void loadCreatureFlags() {
        this.swarmLimit = this.creatureInfo.getFlag("swarmLimit", this.swarmLimit);
    }

    @Override
    public void aiStep() {
        if (this.hasAttackTarget() && this.updateTick % 20 == 0) {
            this.allyUpdate();
        }
        super.aiStep();
    }

    public void allyUpdate() {
        if (this.getCommandSenderWorld().isClientSide || this.isBaby())
            return;
        if (this.swarmLimit > 0 && this.countAllies(64D) < this.swarmLimit) {
            float random = this.random.nextFloat();
            float spawnChance = 0.25F;
            if (random <= spawnChance)
                this.spawnAlly(this.position().x() - 2 + (random * 4), this.position().y(), this.position().z() - 2 + (random * 4));
        }
    }

    public void spawnAlly(double x, double y, double z) {
        AgeableCreatureEntity minion = (AgeableCreatureEntity) this.creatureInfo.createEntity(this.getCommandSenderWorld());
        minion.setGrowingAge(minion.getInitialGrowthTime());
        minion.moveTo(x, y, z, this.random.nextFloat() * 360.0F, 0.0F);
        minion.applyVariant(this.getVariantIndex());
        DeferredLevelActionManager.spawnEntity(this.getCommandSenderWorld(), this.blockPosition(), null, minion);
        if (this.getTarget() != null)
            minion.setLastHurtByMob(this.getTarget());
    }

    @Override
    public float getBlockPathWeight(int x, int y, int z) {
        int waterWeight = 10;
        Block block = this.getCommandSenderWorld().getBlockState(new BlockPos(x, y, z)).getBlock();
        if (block == Blocks.WATER)
            return (super.getBlockPathWeight(x, y, z) + 1) * (waterWeight + 1);
        if (this.getCommandSenderWorld().isRaining() && this.getCommandSenderWorld().canSeeSkyFromBelowWater(new BlockPos(x, y, z)))
            return (super.getBlockPathWeight(x, y, z) + 1) * (waterWeight + 1);
        if (this.getTarget() != null)
            return super.getBlockPathWeight(x, y, z);
        if (this.waterContact())
            return -999999.0F;
        return super.getBlockPathWeight(x, y, z);
    }

    @Override
    public boolean isStrongSwimmer() {
        return true;
    }

    @Override
    public boolean canWalk() {
        return false;
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
    public boolean creatureCanBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean canBreatheAir() {
        return false;
    }
}
