package com.lycanitesmobs.core.entity.creature.aquatic;

import com.lycanitesmobs.core.entity.item.CustomItemEntity;
import com.lycanitesmobs.core.entity.base.AgeableCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.PathType;

/**
 * Trimmed - TemptGoal/canBeTempted dropped (not ported, see AgeableCreatureEntity's own note on
 * canBeTempted). "purelava" block check dropped (that block isn't registered in this port yet -
 * kept the plain Blocks.LAVA check). fleeHealthPercent field doesn't exist on the trimmed
 * BaseCreatureEntity - dropped (default flee behaviour applies instead).
 * getBrightnessForRender()/getDamageModifier() are dead code even in the original (no caller) -
 * dropped along with them. applyDropEffects()/CustomItemEntity dropped too - that custom item-
 * entity drop system isn't ported at all (core/entity/item/ doesn't exist in this port yet).
 */
public class EntityCephignis extends AgeableCreatureEntity {

    public EntityCephignis(EntityType<? extends EntityCephignis> entityType, Level world) {
        super(entityType, world);
        this.isLavaCreature = true;
        this.hasAttackSound = false;
        this.babySpawnChance = 0.01D;
        this.canGrow = true;
        this.isAggressiveByDefault = false;
        this.setupMob();
        this.setPathfindingMalus(PathType.LAVA, 0F);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setLongMemory(false));
    }

    @Override
    public float getBlockPathWeight(int x, int y, int z) {
        int waterWeight = 10;
        BlockPos pos = new BlockPos(x, y, z);
        if (this.getCommandSenderWorld().getBlockState(pos).getBlock() == Blocks.LAVA)
            return (super.getBlockPathWeight(x, y, z) + 1) * (waterWeight + 1);
        if (this.getTarget() != null)
            return super.getBlockPathWeight(x, y, z);
        if (this.lavaContact())
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
    public boolean canBeLeashed() {
        return true;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (source.is(DamageTypes.IN_WALL)) return true;
        return super.isInvulnerableTo(source);
    }

    @Override
    public boolean canBurn() {
        return false;
    }

    @Override
    public boolean waterDamage() {
        return true;
    }

    @Override
    public boolean canBreatheUnderlava() {
        return true;
    }

    @Override
    public boolean canBreatheAir() {
        return false;
    }


    @Override
    public void applyDropEffects(CustomItemEntity entityItem) {
        entityItem.setCanBurn(false);
    }
}
