package com.lycanitesmobs.core.entity.creature.elemental;

import net.minecraft.world.entity.ai.attributes.Attributes;
import com.lycanitesmobs.core.entity.base.TameableCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackRangedGoal;
import com.lycanitesmobs.core.manager.ObjectManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import org.joml.Vector3d;

public class EntityReiver extends TameableCreatureEntity implements Enemy {

    // ==================================================
    //                    Constructor
    // ==================================================
    public EntityReiver(EntityType<? extends EntityReiver> entityType, Level world) {
        super(entityType, world);

        // Setup:
        this.hasAttackSound = false;
        this.setupMob();

        this.getAttribute(Attributes.STEP_HEIGHT).setBaseValue(1.0F);
    }

    // ========== Init AI ==========
    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackRangedGoal(this).setSpeed(0.75D).setRange(16.0F).setMinChaseDistance(8.0F));
    }


    // ==================================================
    //                      Updates
    // ==================================================
    // ========== Living Update ==========
    @Override
    public void aiStep() {
        super.aiStep();

        // Particles:
        if (this.getCommandSenderWorld().isClientSide)
            for (int i = 0; i < 2; ++i) {
                this.getCommandSenderWorld().addParticle(ParticleTypes.ITEM_SNOWBALL, this.position().x() + (this.random.nextDouble() - 0.5D) * (double) this.getBbWidth(), this.position().y() + this.random.nextDouble() * (double) this.getBbHeight(), this.position().z() + (this.random.nextDouble() - 0.5D) * (double) this.getBbWidth(), 0.0D, 0.0D, 0.0D);
            }
    }


    // ==================================================
    //                      Attacks
    // ==================================================
    // ========== Ranged Attack ==========
    @Override
    public void attackRanged(Entity target, float range) {
        this.fireProjectile("frostbolt", target, range, 0, new Vector3d(0, 0, 0), 1.2f, 2f, 1F);
        super.attackRanged(target, range);
    }


    // ==================================================
    //                     Abilities
    // ==================================================
    @Override
    public boolean isFlying() {
        return true;
    }


    // ==================================================
    //                     Pet Control
    // ==================================================
    public boolean petControlsEnabled() {
        return true;
    }

    // ==================================================
    //                     Equipment
    // ==================================================
    // TODO(port): restore @Override once creature inventories are ported
    public int getNoBagSize() {
        return 0;
    }

    // TODO(port): restore @Override once creature inventories are ported
    public int getBagSize() {
        return this.creatureInfo.getBagSize();
    }

    // ==================================================
    //                     Immunities
    // ==================================================
    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (source.type().equals(ObjectManager.getDamageSource(this.level(), "ooze").type())) return true;
        return super.isInvulnerableTo(source);
    }

    @Override
    public boolean creatureCanBreatheUnderwater() {
        return true;
    }
}
