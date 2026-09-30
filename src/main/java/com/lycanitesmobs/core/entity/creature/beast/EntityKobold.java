package com.lycanitesmobs.core.entity.creature.beast;

import com.lycanitesmobs.core.data.info.ObjectLists;
import com.lycanitesmobs.core.entity.goals.actions.abilities.GetBlockGoal;
import com.lycanitesmobs.core.entity.goals.actions.abilities.GetItemGoal;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import com.lycanitesmobs.core.entity.base.TameableCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Trimmed - dropped GetItemGoal/GetBlockGoal (not ported - the "theivery"/"griefing" item- and
 * torch-stealing abilities) and the torch-looting aiStep block that went with them. Kept the
 * `theivery` flag wired into canPickupItems() (self-contained) and the
 * health-threshold canAttack()/shouldCreatureGroupRevenge() tweaks (self-contained). Dropped
 * MobType.UNDEFINED attribute assignment. (Bag drop, group hunt/flee restored 2026-09-28.)
 */
public class EntityKobold extends TameableCreatureEntity implements Enemy {
    // Fields restored from official (2026-09-28 method audit):
    private int torchLootingTime = 20;

    protected boolean griefing = true;
    protected boolean theivery = true;

    public EntityKobold(EntityType<? extends EntityKobold> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = true;
        this.spreadFire = false;
        this.canGrow = false;
        this.babySpawnChance = 0.1D;
        this.setupMob();
    }

    @Override
    protected void registerGoals() {
		if(this.theivery)
			this.goalSelector.addGoal(this.claimIdleGoalIndex(), new GetItemGoal(this).setDistanceMax(8).setSpeed(1.2D));
		if(this.griefing)
			this.goalSelector.addGoal(this.claimIdleGoalIndex(), new GetBlockGoal(this).setDistanceMax(8).setSpeed(1.2D).setBlockName("torch").setTamedLooting(false));

		super.registerGoals();

		this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setTargetClass(Player.class).setLongMemory(false));
		this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this));
    }

    @Override
    public void loadCreatureFlags() {
        this.griefing = this.creatureInfo.getFlag("griefing", this.griefing);
        this.theivery = this.creatureInfo.getFlag("theivery", this.theivery);
    }

    @Override
    public boolean shouldCreatureGroupRevenge(LivingEntity target) {
        if (target instanceof Player && (target.getHealth() / target.getMaxHealth()) <= 0.5F)
            return true;
        return super.shouldCreatureGroupRevenge(target);
    }

    @Override
    public boolean canAttack(LivingEntity targetEntity) {
        if (!this.isTamed() && (targetEntity.getHealth() / targetEntity.getMaxHealth()) > 0.5F)
            return false;
        return super.canAttack(targetEntity);
    }

    // Port fix: this overrode vanilla canPickUpLoot() (vanilla armour/loot pickup, not upstream behaviour), so the
    // Lycanites pickup loop and GetItemGoal/GetBlockGoal, which check canPickupItems(), never let Kobolds steal.
    @Override
    public boolean canPickupItems() {
        return this.theivery;
    }

    /**
     * Port fix: Kobolds carry their stolen loot without a bag. Upstream they had no slots without one, and
     * CreatureInventory.onInventoryChanged() drops everything in the bag slots when no bag is equipped, so a Kobold
     * picked an item up and dropped it again straight away (thievery never worked). It still spills on death.
     **/
    @Override
    public int getNoBagSize() {
        return this.theivery ? this.getBagSize() : super.getNoBagSize();
    }


    // ==================================================
    //   Restored from official 2026-09-28 (method audit)
    // ==================================================
	@Override
    public void aiStep() {
        super.aiStep();
        
        // Torch Looting:
        if(!this.isTamed() && this.getCommandSenderWorld().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING) && this.griefing) {
	        if(this.torchLootingTime-- <= 0) {
	        	this.torchLootingTime = 60;
	        	int distance = 2;
	        	String targetName = "torch";
	        	List possibleTargets = new ArrayList<BlockPos>();
	            for(int x = (int)this.position().x() - distance; x < (int)this.position().x() + distance; x++) {
	            	for(int y = (int)this.position().y() - distance; y < (int)this.position().y() + distance; y++) {
	            		for(int z = (int)this.position().z() - distance; z < (int)this.position().z() + distance; z++) {
                            BlockPos pos = new BlockPos(x, y, z);
	            			Block searchBlock = this.getCommandSenderWorld().getBlockState(pos).getBlock();
	                    	if(searchBlock != Blocks.AIR) {
	                    		BlockPos possibleTarget = null;
	                			if(ObjectLists.isName(searchBlock, targetName)) {
	                				this.getCommandSenderWorld().destroyBlock(pos, true);
	                				break;
	                			}
	                    	}
	                    }
	                }
	            }
	        }
        }
    }

	/**
	 * Kobolds drop the loot they stole when they despawn. Official: Forge's onRemovedFromWorld (also ran on chunk
	 * unload); here only an actual despawn/discard drops it, so unloading doesn't spill the bag every time.
	 **/
	@Override
	public void remove(RemovalReason reason) {
		if(!this.getCommandSenderWorld().isClientSide && reason == RemovalReason.DISCARDED && !this.isTamed() && this.inventory.hasBagItems()) {
			this.inventory.dropInventory();
		}
		super.remove(reason);
	}

	@Override
	public boolean shouldCreatureGroupFlee(LivingEntity target) {
		if(target instanceof Player && (target.getHealth() / target.getMaxHealth()) <= 0.5F)
			return false;
		return super.shouldCreatureGroupFlee(target);
	}

	@Override
	public boolean shouldCreatureGroupHunt(LivingEntity target) {
		if(target instanceof Player && (target.getHealth() / target.getMaxHealth()) <= 0.5F)
			return true;
		return super.shouldCreatureGroupHunt(target);
	}
}
