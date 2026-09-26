package com.lycanitesmobs.core.entity.creature.anthronian;

import net.minecraft.world.entity.Entity;
import org.joml.Vector3d;
import com.lycanitesmobs.core.entity.goals.actions.AttackRangedGoal;
import com.lycanitesmobs.core.entity.base.TameableCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;

/**
 * Trimmed - ranged boulder-throw attack (fireProjectile/AttackRangedGoal) dropped, needs
 * ProjectileManager (not ported); falls back to melee. BreakDoorGoal dropped (see EntityEttin's
 * note). Block-destroying griefing AoE dropped (no destroyArea hook). Pickaxe-extra-damage
 * getDamageModifier and getFallResistance dropped (not real hooks in this port). Equipment/
 * pet-control dropped. Daylight stone-form (petrification when it can see sky during the day)
 * kept - self-contained, uses already-ported isDaytime().
 * PHASE 6a UPDATE (2026-09-26): ranged attack restored (projectiles ported) - any wording above about a
 * substituted melee attack or ProjectileManager being unported is outdated.
 */
public class EntityTroll extends TameableCreatureEntity implements Enemy {

    protected boolean stoneForm = false;

    public EntityTroll(EntityType<? extends EntityTroll> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = false;
        this.setupMob();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackRangedGoal(this).setSpeed(0.75D).setRange(40.0F).setMinChaseDistance(10.0F).setLongMemory(false));

        if (this.getNavigation() instanceof GroundPathNavigation) {
            GroundPathNavigation pathNavigateGround = (GroundPathNavigation) this.getNavigation();
            pathNavigateGround.setCanOpenDoors(true);
        }
    }

    @Override
    public String getTextureName() {
        if (this.stoneForm)
            return super.getTextureName() + "_stone";
        return super.getTextureName();
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.stoneForm) {
            if (this.isDaytime() && this.getCommandSenderWorld().canSeeSkyFromBelowWater(this.blockPosition())) {
                this.stoneForm = true;
            }
        } else {
            if (!this.isDaytime() || !this.getCommandSenderWorld().canSeeSkyFromBelowWater(this.blockPosition())) {
                this.stoneForm = false;
            }
        }
    }

    @Override
    public float getAISpeedModifier() {
        if (this.stoneForm)
            return 0.125F;
        return 1.0F;
    }

    @Override
    public boolean canBurn() {
        return !this.stoneForm;
    }

    @Override
    public void attackRanged(Entity target, float range) {
        this.fireProjectile("boulderblast", target, range, 0, new Vector3d(0, 0, 0), 1.2f, 2f, 1F);
        super.attackRanged(target, range);
    }
}
