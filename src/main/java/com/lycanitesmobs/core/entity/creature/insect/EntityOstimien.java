package com.lycanitesmobs.core.entity.creature.insect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.core.Holder;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import com.lycanitesmobs.core.entity.effect.EffectBase;
import com.lycanitesmobs.core.entity.goals.actions.abilities.StealthGoal;
import com.lycanitesmobs.core.manager.ObjectManager;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import com.lycanitesmobs.core.entity.base.TameableCreatureEntity;
import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import com.lycanitesmobs.core.entity.goals.targeting.FindAttackTargetGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Heavily trimmed: the original extends TameableCreatureEntity and its whole identity is a
 * stealth ambush predator (StealthGoal, canStealth/startStealth invisibility tied to
 * plague/instability effects via ObjectManager.getEffect, a leap-in-to-attack preamble, and
 * tame-gated pet-invisibility). None of the stealth system, those specific effects, the tame
 * system, or leap() (not on BaseCreatureEntity in this trimmed port) exist here, so this is
 * reduced to a plain melee attacker - still climbs and fights, just visible and without the
 * ambush mechanic.
 * PHASE 5e UPDATE (2026-09-26): re-parented to its official superclass now that TameableCreatureEntity is
 * ported (taming/ownership/pet behaviour work; RideableCreatureEntity is still a stub). Any wording above
 * about extending Base/AgeableCreatureEntity or taming being unported is outdated.
 */
public class EntityOstimien extends TameableCreatureEntity {

    public EntityOstimien(EntityType<? extends EntityOstimien> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = true;
        this.setupMob();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimPriorityGoalIndex(), new StealthGoal(this).setStealthTime(20).setStealthAttack(true).setStealthMove(true));
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setLongMemory(false));
    }

    @Override
    public boolean canClimb() {
        return true;
    }


    // ==================================================
    //   Restored from official 2026-09-28 (method audit)
    // ==================================================
    // ==================================================
    //                      Updates
    // ==================================================
	// ========== Living Update ==========
	@Override
    public void aiStep() {
        super.aiStep();
        
        // Lurker Blind Stalking:
        if(this.getTarget() != null) {
        	Holder<MobEffect> stalkingEffect = ObjectManager.getEffectHolder("plague");
        	if(stalkingEffect != null && this.getTarget().hasEffect(stalkingEffect))
        		this.setAvoidTarget(this.getTarget());
        	else
        		this.setAvoidTarget(null);
        }
        else
        	this.setAvoidTarget(null);
        
        // Leap:
        if(this.onGround() && !this.getCommandSenderWorld().isClientSide && this.random.nextInt(10) == 0) {
        	if(this.hasAttackTarget())
        		this.leap(6.0F, 0.4D, this.getTarget());
        	else if(this.hasAvoidTarget())
        		this.leap(4.0F, 0.4D);
        }
    }

    // ==================================================
   	//                     Stealth
   	// ==================================================
    @Override
    public boolean canStealth() {
    	if(this.getCommandSenderWorld().isClientSide) return false;
    	else {
	    	if(this.hasAttackTarget()) {
	    		if(this.getTarget() instanceof Player) {
	    			Player playerTarget = (Player)this.getTarget();
	    			ItemStack itemstack = playerTarget.getInventory().getSelected();
	    			if(this.isTamingItem(itemstack))
	    				return false;
	    		}
				Holder<MobEffect> stalkingEffect = ObjectManager.getEffectHolder("instability");
	    		if(stalkingEffect != null) {
					if(!this.getTarget().hasEffect(stalkingEffect))
						return false;
				}
	    		if(this.distanceTo(this.getTarget()) < (5.0D * 5.0D))
	    			return false;
	    	}
	    	else {
	    		if(this.isMoving())
	    			return false;
	    	}
	        return true;
        }
    }

    @Override
    public int getBagSize() { return this.creatureInfo.getBagSize(); }

    // ==================================================
    //                     Equipment
    // ==================================================
    @Override
    public int getNoBagSize() { return 0; }

    // ==================================================
    //                       Visuals
    // ==================================================
    @OnlyIn(Dist.CLIENT)
    @Override
    public boolean isInvisibleTo(Player player) {
    	if(this.isTamed() && this.getOwner() == player)
    		return false;
        return this.isInvisible();
    }

    // ==================================================
    //                     Pet Control
    // ==================================================
    public boolean petControlsEnabled() { return true; }

    @Override
    public void startStealth() {
    	if(this.getCommandSenderWorld().isClientSide) {
            ParticleOptions particle = ParticleTypes.SMOKE;
            double d0 = this.random.nextGaussian() * 0.02D;
            double d1 = this.random.nextGaussian() * 0.02D;
            double d2 = this.random.nextGaussian() * 0.02D;
            for(int i = 0; i < 100; i++)
            	this.getCommandSenderWorld().addParticle(particle, this.position().x() + (double)(this.random.nextFloat() * this.getDimensions(Pose.STANDING).width() * 2.0F) - (double)this.getDimensions(Pose.STANDING).width(), this.position().y() + 0.5D + (double)(this.random.nextFloat() * this.getDimensions(Pose.STANDING).height()), this.position().z() + (double)(this.random.nextFloat() * this.getDimensions(Pose.STANDING).width() * 2.0F) - (double)this.getDimensions(Pose.STANDING).width(), d0, d1, d2);
        }
    	super.startStealth();
    }
}
