package com.lycanitesmobs.core.entity.creature.aquatic;

import com.lycanitesmobs.core.entity.IGroupHeavy;
import com.lycanitesmobs.core.entity.base.AgeableCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.PathType;

/**
 * Trimmed - original extends RideableCreatureEntity and has a whole entity-pickup-and-carry
 * mount ability (mountAbility/getPickupEntity/dropPickupEntity/canPickupEntity/pickupEntity,
 * plus the ExtendedEntity/ExtendedPlayer capabilities) - none of the pickup-specific methods
 * exist on this port's BaseCreatureEntity (only the bare hasPickupEntity()/pickupEntity field
 * do), and the tame/mount/stamina system isn't ported at all. Dropped entirely rather than
 * half-port it, same reasoning as EntitySkylus. getFallResistance()/canStandOnFluid()/onDamage()
 * overrides also dropped - not real hooks in this port's BaseCreatureEntity/damage pipeline.
 * setMaxUpStep() fixed to the 1.21.1 maxUpStep() getter override, BlockPathTypes renamed to
 * PathType.
 */
public class EntityStryder extends AgeableCreatureEntity implements IGroupHeavy {

    public EntityStryder(EntityType<? extends EntityStryder> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = true;
        this.babySpawnChance = 0D;
        this.canGrow = true;
        this.setupMob();
        this.hitAreaWidthScale = 1.5f;
        this.hitAreaHeightScale = 1;
        this.setPathfindingMalus(PathType.WATER, 0F);
    }

    @Override
    public float maxUpStep() {
        return 4.0F;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setLongMemory(false));
    }

    @Override
    public float getAISpeedModifier() {
        if (this.isInWater())
            return 1F;
        if (this.waterContact())
            return 0.9F;
        return 0.75F;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
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

    public BlockPos getWanderPosition(BlockPos wanderPosition) {
        BlockPos groundPos;
        for (groundPos = wanderPosition.below(); groundPos.getY() > 0 && !this.getCommandSenderWorld().getBlockState(groundPos).isSolid(); groundPos = groundPos.below()) {
        }
        return groundPos.above();
    }

    @Override
    public boolean creatureCanBreatheUnderwater() {
        return true;
    }
}
