package com.lycanitesmobs.core.entity.creature.aquatic;

import com.lycanitesmobs.core.capabilities.entity.ExtendedEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import com.lycanitesmobs.core.entity.base.TameableCreatureEntity;
import com.lycanitesmobs.core.entity.base.AgeableCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Trimmed - original extends TameableCreatureEntity (tame/master/pet-control, not ported) and
 * has a whole entity-pickup-and-carry mechanic (getPickupEntity/dropPickupEntity/pickupEntity/
 * canPickupEntity/getPickupOffset/leap, plus the ExtendedEntity capability) that isn't ported -
 * only the bare hasPickupEntity()/pickupEntity field exist on this port's BaseCreatureEntity,
 * none of the rest. Dropped entirely rather than half-port it. getDamageModifier() restored
 * 2026-09-29 (called from BaseCreatureEntity.hurt()). setMaxUpStep()
 * fixed to the 1.21.1 maxUpStep() getter override (no setter exists anymore).
 * PHASE 5e UPDATE (2026-09-26): re-parented to its official superclass now that TameableCreatureEntity is
 * ported (taming/ownership/pet behaviour work; RideableCreatureEntity is still a stub). Any wording above
 * about extending Base/AgeableCreatureEntity or taming being unported is outdated.
 */
public class EntitySkylus extends TameableCreatureEntity implements Enemy {

    public EntitySkylus(EntityType<? extends EntitySkylus> entityType, Level world) {
        super(entityType, world);
        this.spawnsOnLand = false;
        this.spawnsInWater = true;
        this.hasAttackSound = true;
        this.babySpawnChance = 0.01D;
        this.canGrow = true;
        this.setupMob();
    }

    @Override
    public float maxUpStep() {
        return 1.0F;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setLongMemory(false).setRange(1D));
    }

    @Override
    public float getAISpeedModifier() {
        if (this.getHealth() > (this.getMaxHealth() / 2)) // Slower with shell.
            return 1.0F;
        return 2.0F;
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
    // ==================================================
    //                      Updates
    // ==================================================
	// ========== Living Update ==========
	@Override
    public void aiStep() {
        super.aiStep();

        // Entity Pickup Update:
        if(!this.getCommandSenderWorld().isClientSide && this.getControllingPassenger() == null && this.hasPickupEntity()) {

            // Random Dropping:
            ExtendedEntity extendedEntity = ExtendedEntity.getForEntity(this.getPickupEntity());
            if (extendedEntity != null)
                extendedEntity.setPickedUpByEntity(this);
            if (this.tickCount % 100 == 0 && this.getRandom().nextBoolean()) {
                this.dropPickupEntity();
            }
        }
    }

    // ==================================================
    //                      Attacks
    // ==================================================
    // ========== Melee Attack ==========
    @Override
    public boolean attackMelee(Entity target, double damageScale) {
    	if(!super.attackMelee(target, damageScale))
    		return false;

        // Pickup:
        if(target instanceof LivingEntity) {
            LivingEntity entityLivingBase = (LivingEntity)target;
            if(this.canPickupEntity(entityLivingBase)) {
                this.pickupEntity(entityLivingBase);
            }
        }
        
        return true;
    }

    @Override
    public int getBagSize() { return this.creatureInfo.getBagSize(); }

    // ==================================================
    //                     Equipment
    // ==================================================
    @Override
    public int getNoBagSize() { return 0; }

    @Override
    public double[] getPickupOffset(Entity entity) {
        return new double[]{0, 0, 2D};
    }

    // ==================================================
    //                     Pet Control
    // ==================================================
    public boolean petControlsEnabled() { return true; }

    @Override
    public void pickupEntity(LivingEntity entity) {
        super.pickupEntity(entity);
        this.leap(-1.0F, -0.5D);
    }

    // Restored from official 2026-09-29 (method audit) - hooked in BaseCreatureEntity.hurt().
    @Override
    public float getDamageModifier(DamageSource damageSrc) {
        if (this.getHealth() > (this.getMaxHealth() / 2)) // Stronger with shell.
            return 0.25F;
        return 1.0F;
    }
}
