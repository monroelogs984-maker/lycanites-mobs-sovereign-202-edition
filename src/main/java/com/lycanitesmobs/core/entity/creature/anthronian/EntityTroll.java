package com.lycanitesmobs.core.entity.creature.anthronian;

import net.minecraft.world.entity.LivingEntity;
import com.lycanitesmobs.core.data.info.ObjectLists;
import com.lycanitesmobs.core.entity.goals.actions.BreakDoorGoal;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
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
 * note). Block-destroying griefing AoE dropped (no destroyArea hook). getFallResistance
 * dropped (not a real hook in this port); pickaxe-extra-damage getDamageModifier restored 2026-09-29. Equipment/
 * pet-control dropped. Daylight stone-form (petrification when it can see sky during the day)
 * kept - self-contained, uses already-ported isDaytime().
 * PHASE 6a UPDATE (2026-09-26): ranged attack restored (projectiles ported) - any wording above about a
 * substituted melee attack or ProjectileManager being unported is outdated.
 */
public class EntityTroll extends TameableCreatureEntity implements Enemy {
    // Fields restored from official (2026-09-28 method audit):
    protected boolean griefing = true;


    protected boolean stoneForm = false;

    public EntityTroll(EntityType<? extends EntityTroll> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = false;
        // Restored from official (2026-09-28 constructor audit):
        this.babySpawnChance = 0.01D;
        this.canGrow = false;
        this.solidCollision = true;
        this.setupMob();
    }

    @Override
    protected void registerGoals() {
		super.registerGoals();
		this.goalSelector.addGoal(this.claimDistractionGoalIndex(), new BreakDoorGoal(this));
		this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackRangedGoal(this).setSpeed(0.75D).setRange(40.0F).setMinChaseDistance(10.0F).setLongMemory(false));

		if(this.getNavigation() instanceof GroundPathNavigation) {
			GroundPathNavigation pathNavigateGround = (GroundPathNavigation)this.getNavigation();
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


    // ==================================================
    //   Restored from official 2026-09-28 (method audit)
    // ==================================================
	@Override
	public int getBagSize() { return this.creatureInfo.getBagSize(); }

    // ==================================================
   	//                     Immunities
   	// ==================================================
    @Override
    public float getFallResistance() {
    	return 50;
    }

	// ==================================================
	//                     Equipment
	// ==================================================
	@Override
	public int getNoBagSize() { return 0; }

	@Override
	public void loadCreatureFlags() {
		this.griefing = this.creatureInfo.getFlag("griefing", this.griefing);
	}

    // ==================================================
    //                     Pet Control
    // ==================================================
    public boolean petControlsEnabled() { return true; }

    // Restored from official 2026-09-29 (method audit) - hooked in BaseCreatureEntity.hurt().
    @Override
    public float getDamageModifier(DamageSource damageSrc) {
        if (damageSrc.getEntity() instanceof LivingEntity livingEntity
                && ObjectLists.isPickaxe(livingEntity.getItemInHand(InteractionHand.MAIN_HAND))) {
            return 3.0F;
        }
        return super.getDamageModifier(damageSrc);
    }
}
