package com.lycanitesmobs.core.entity.creature.aquatic;

import com.lycanitesmobs.core.entity.base.TameableCreatureEntity;
import com.lycanitesmobs.core.entity.base.AgeableCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/**
 * Trimmed - TameableCreatureEntity -> AgeableCreatureEntity (see EntityAglebemu), bag/
 * attribute/spawnsOnLand/spawnsInWater dropped. The original's "static aura AOE attack" tick
 * block did nothing but increment a counter (its actual damage line was already commented out
 * upstream) - dropped entirely; kept the particle-effect half of aiStep(), replacing the
 * tame-only isPetType("familiar") tick-rate check with a fixed 40.
 * PHASE 5e UPDATE (2026-09-26): re-parented to its official superclass now that TameableCreatureEntity is
 * ported (taming/ownership/pet behaviour work; RideableCreatureEntity is still a stub). Any wording above
 * about extending Base/AgeableCreatureEntity or taming being unported is outdated.
 */
public class EntityAbaia extends TameableCreatureEntity implements Enemy {

    public EntityAbaia(EntityType<? extends EntityAbaia> entityType, Level world) {
        super(entityType, world);
        this.spawnsOnLand = false;
        this.spawnsInWater = true;
        this.hasAttackSound = true;
        this.babySpawnChance = 0.05D;
        this.canGrow = true;
        this.setupMob();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setLongMemory(false));
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (this.getCommandSenderWorld().isClientSide && this.hasAttackTarget()) {
            this.getCommandSenderWorld().addParticle(ParticleTypes.CRIT, this.position().x() + (this.random.nextDouble() - 0.5D) * (double) this.getDimensions(Pose.STANDING).width(), this.position().y() + this.random.nextDouble() * (double) this.getDimensions(Pose.STANDING).height(), this.position().z() + (this.random.nextDouble() - 0.5D) * (double) this.getDimensions(Pose.STANDING).width(), 0.0D, 0.0D, 0.0D);

            List<LivingEntity> aoeTargets = this.getNearbyEntities(LivingEntity.class, (e) -> true, 4);
            for (LivingEntity target : aoeTargets) {
                if (this.canAttackType(target.getType()) && this.canAttack(target) && this.getSensing().hasLineOfSight(target)) {
                    this.getCommandSenderWorld().addParticle(ParticleTypes.CRIT, target.position().x() + (this.random.nextDouble() - 0.5D) * (double) target.getDimensions(Pose.STANDING).width(), target.position().y() + this.random.nextDouble() * (double) target.getDimensions(Pose.STANDING).height(), target.position().z() + (this.random.nextDouble() - 0.5D) * (double) target.getDimensions(Pose.STANDING).width(), 0.0D, 0.0D, 0.0D);
                }
            }
        }
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
    public boolean isInvulnerableTo(DamageSource source) {
        if (source.is(DamageTypes.LIGHTNING_BOLT)) return true;
        return super.isInvulnerableTo(source);
    }

    @Override
    public boolean creatureCanBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean canBreatheAir() {
        return false;
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
