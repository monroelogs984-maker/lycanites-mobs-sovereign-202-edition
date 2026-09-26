package com.lycanitesmobs.core.entity.creature.aquatic;

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
 * none of the rest. Dropped entirely rather than half-port it. getDamageModifier() also dropped
 * - it's not an actual override point anywhere in this port's damage pipeline. setMaxUpStep()
 * fixed to the 1.21.1 maxUpStep() getter override (no setter exists anymore).
 */
public class EntitySkylus extends AgeableCreatureEntity implements Enemy {

    public EntitySkylus(EntityType<? extends EntitySkylus> entityType, Level world) {
        super(entityType, world);
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
}
