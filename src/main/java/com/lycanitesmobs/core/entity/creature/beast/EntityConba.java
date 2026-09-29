package com.lycanitesmobs.core.entity.creature.beast;

import com.lycanitesmobs.core.manager.CreatureManager;
import com.lycanitesmobs.core.manager.DeferredLevelActionManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import com.lycanitesmobs.core.entity.goals.actions.abilities.FireProjectilesGoal;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3d;
import com.lycanitesmobs.core.entity.goals.actions.AttackRangedGoal;
import com.lycanitesmobs.core.entity.base.TameableCreatureEntity;
import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import com.lycanitesmobs.core.entity.creature.insect.EntityVespid;
import com.lycanitesmobs.core.entity.creature.insect.EntityVespidQueen;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import com.lycanitesmobs.core.entity.goals.targeting.FindAvoidTargetGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Heavily trimmed - the original extends TameableCreatureEntity (not ported); this extends
 * BaseCreatureEntity directly, dropping MobType.UNDEFINED/getNoBagSize/getBagSize/
 * petControlsEnabled the same as EntityBalayang. Also dropped entirely: the vespid-infection
 * mechanic (infectWithVespids/spawnVespidSwarm/getSpeciesName/getTextureName overrides/
 * extraAnimation01/NBT fields) - it's gated on hasSpawnEventType() and an extraAnimation01 sync
 * field, neither of which exist on BaseCreatureEntity (not added here - this batch must not
 * touch shared base-entity files while other creatures are being ported in parallel), the
 * ranged "poop" projectile attack (ProjectileManager isn't ported), the random-leap aiStep
 * behavior (leap() not on BaseCreatureEntity, same reason), shouldCreatureGroupFlee() (not a
 * hook here), and getFallResistance(). claimSpecialTargetGoalIndex() doesn't exist either -
 * substituted claimFindTargetGoalIndex() for the avoid-target goals below, which claims from the
 * same targetSelector priority counter family.
 * <p>
 * NOTE: depends on EntityVespid/EntityVespidQueen (ported by a parallel fork in this same batch
 * of work) for the canAttack() immunity check - if those aren't compiled yet this won't build
 * standalone, but should resolve once all batches land.
 * PHASE 5e UPDATE (2026-09-26): re-parented to its official superclass now that TameableCreatureEntity is
 * ported (taming/ownership/pet behaviour work; RideableCreatureEntity is still a stub). Any wording above
 * about extending Base/AgeableCreatureEntity or taming being unported is outdated.
 * PHASE 6a UPDATE (2026-09-26): ranged attack restored (projectiles ported) - any wording above about a
 * substituted melee attack or ProjectileManager being unported is outdated.
 */
public class EntityConba extends TameableCreatureEntity implements Enemy {
    AttackMeleeGoal aiAttackMelee;
    // Fields restored from official (2026-09-28 method audit):
    private boolean vespidInfection = false;
    protected int vespidInfectionTime = 0;


    public EntityConba(EntityType<? extends EntityConba> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = true;
        this.setupMob();
    }

    @Override
    protected void registerGoals() {
        this.aiAttackMelee = new AttackMeleeGoal(this).setLongMemory(true).setEnabled(false);
        this.goalSelector.addGoal(this.claimPriorityGoalIndex(), this.aiAttackMelee); // Melee is a priority as it is used when infected.

        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new FireProjectilesGoal(this).setProjectile("poop").setFireRate(20).setVelocity(1.2F));

        super.registerGoals();

        this.targetSelector.addGoal(this.claimSpecialTargetGoalIndex(), new FindAvoidTargetGoal(this).setTargetClass(Player.class).setTameTargetting(false));
        this.targetSelector.addGoal(this.claimSpecialTargetGoalIndex(), new FindAvoidTargetGoal(this).setTargetClass(Villager.class));
        this.targetSelector.addGoal(this.claimSpecialTargetGoalIndex(), new FindAvoidTargetGoal(this).setTargetClass(Pillager.class));
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (target instanceof EntityVespid || target instanceof EntityVespidQueen)
            return false;
        return super.canAttack(target);
    }

    @Override
    public void attackRanged(Entity target, float range) {
        this.fireProjectile("poop", target, range, 0, new Vector3d(0, 0, 0), 1.2f, 2f, 1F);
        super.attackRanged(target, range);
    }


    // ==================================================
    //   Restored from official 2026-09-28 (method audit)
    // ==================================================
    /**
     * Used when saving this mob to a chunk.
     **/
    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putBoolean("VespidInfection", this.vespidInfection);
        if (this.vespidInfection)
            nbt.putInt("VespidInfectionTime", this.vespidInfectionTime);
    }

    // ==================================================
    //                      Updates
    // ==================================================
    // ========== Living Update ==========
    @Override
    public void aiStep() {
        super.aiStep();

        // Random Leaping:
        if (this.onGround() && !this.getCommandSenderWorld().isClientSide) {
            if (this.hasAvoidTarget()) {
                if (this.random.nextInt(10) == 0)
                    this.leap(1.0F, 0.6D, this.getTarget());
            } else {
                if (this.random.nextInt(50) == 0 && this.isMoving())
                    this.leap(1.0D, 0.6D);
            }
        }

        // Infected AI:
        if (!this.getCommandSenderWorld().isClientSide) {
            // The Swarm:
            if (!this.vespidInfection && this.hasSpawnEventType("theswarm")) {
                this.infectWithVespids();
            }

            if (this.vespidInfection && !this.getCommandSenderWorld().isClientSide) {
                this.aiAttackMelee.setEnabled(true);
                if (this.vespidInfectionTime++ >= 60 * 20) {
                    this.spawnVespidSwarm();
                    this.remove(RemovalReason.DISCARDED);
                }
            } else {
                this.aiAttackMelee.setEnabled(false);
            }
        }

        // Infected Visuals
        if (this.getCommandSenderWorld().isClientSide) {
            this.vespidInfection = this.extraAnimation01();
            if (this.vespidInfection) {
                for (int i = 0; i < 2; ++i) {
                    this.getCommandSenderWorld().addParticle(ParticleTypes.WITCH, this.position().x() + (this.random.nextDouble() - 0.5D) * (double) this.getDimensions(Pose.STANDING).width(), this.position().y() + this.random.nextDouble() * (double) this.getDimensions(Pose.STANDING).height(), this.position().z() + (this.random.nextDouble() - 0.5D) * (double) this.getDimensions(Pose.STANDING).width(), 0.0D, 0.0D, 0.0D);
                }
            }
        }
    }

    // ==================================================
    //                      Death
    // ==================================================
    @Override
    public void die(DamageSource damageSource) {
        boolean wasDead = this.dead;
        super.die(damageSource);
        if (wasDead || !this.dead) {
            return;
        }

        if (!this.getCommandSenderWorld().isClientSide && this.vespidInfection)
            this.spawnVespidSwarm();
    }

    /**
     * An additional animation boolean that is passed to all clients through the animation mask.
     **/
    public boolean extraAnimation01() {
        if (!this.getCommandSenderWorld().isClientSide)
            return this.vespidInfection;
        else
            return this.extraAnimation01;
    }

    @Override
    public int getBagSize() {
        return this.creatureInfo.getBagSize();
    }

    // ==================================================
    //                     Immunities
    // ==================================================
    @Override
    public float getFallResistance() {
        return 100;
    }

    // ==================================================
    //                     Equipment
    // ==================================================
    @Override
    public int getNoBagSize() {
        return 0;
    }

    /**
     * Returns the species name of this entity.
     **/
    @Override
    public MutableComponent getSpeciesName() {
        MutableComponent infection = Component.literal("");
        if (this.vespidInfection) {
            String entityName = this.creatureInfo.getName();
            if (entityName != null) {
                infection = Component.translatable("entity." + this.creatureInfo.getModInfo().modid + "." + entityName + ".infected");
                infection.append(" ");
            }
        }
        return infection.append(super.getSpeciesName());
    }

    public String getTextureName() {
        if (this.vespidInfection)
            return super.getTextureName() + "_infected";
        return super.getTextureName();
    }

    public void infectWithVespids() {
        this.vespidInfection = true;
    }

    public boolean isVespidInfected() {
        return this.vespidInfection;
    }

    // ==================================================
    //                     Pet Control
    // ==================================================
    public boolean petControlsEnabled() {
        return true;
    }

    /**
     * Used when loading this mob from a saved chunk.
     **/
    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);

        if (nbt.contains("VespidInfection")) {
            this.vespidInfection = nbt.getBoolean("VespidInfection");
        }
        if (nbt.contains("VespidInfectionTime")) {
            this.vespidInfectionTime = nbt.getInt("VespidInfectionTime");
        }
    }

    // ========== AI Update ==========
    @Override
    public boolean shouldCreatureGroupFlee(LivingEntity target) {
        if (this.isTamed())
            return false;
        return super.shouldCreatureGroupFlee(target);
    }

    public void spawnVespidSwarm() {
        int j = 2 + this.random.nextInt(5) + getCommandSenderWorld().getDifficulty().getId() - 1;
        for (int k = 0; k < j; ++k) {
            float f = ((float) (k % 2) - 0.5F) * this.getDimensions(Pose.STANDING).width() / 4.0F;
            float f1 = ((float) (k / 2) - 0.5F) * this.getDimensions(Pose.STANDING).width() / 4.0F;
            EntityVespid vespid = (EntityVespid) CreatureManager.getInstance().getCreature("vespid").createEntity(this.getCommandSenderWorld());
            vespid.moveTo(this.position().x() + (double) f, this.position().y() + 0.5D, this.position().z() + (double) f1, this.random.nextFloat() * 360.0F, 0.0F);
            vespid.applyVariant(this.getVariantIndex());
            vespid.setGrowingAge(vespid.getInitialGrowthTime());
            vespid.inheritSpawnEventFrom(this);
            DeferredLevelActionManager.spawnEntity(this.getCommandSenderWorld(), this.blockPosition(), null, vespid);
            if (this.getTarget() != null)
                vespid.setLastHurtByMob(this.getTarget());
        }
    }
}
