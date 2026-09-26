package com.lycanitesmobs.core.entity.creature.reptile;

import com.lycanitesmobs.core.entity.base.AgeableCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Trimmed - was RideableCreatureEntity implements IGroupHeavy/IGroupBoss checks (none ported,
 * now AgeableCreatureEntity). Kept the whirlpool crowd-control ability (self-contained), dropped
 * all mount-specific bits (riderEffects, mountAbility/stamina, onDismounted rider effect,
 * getPassengersRidingOffset) and the getControllingPassenger()-gated "mountedWhirlpool" branch.
 * canBreatheUnderwater() renamed to creatureCanBreatheUnderwater() since LivingEntity's own
 * canBreatheUnderwater() is final/tag-driven in 1.21.1.
 */
public class EntityThresher extends AgeableCreatureEntity implements Enemy {
    protected int whirlpoolRange = 8;
    protected int whirlpoolEnergy = 0;
    protected int whirlpoolEnergyMax = 5 * 20;
    protected boolean whirlpoolRecharging = true;

    public EntityThresher(EntityType<? extends EntityThresher> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = true;
        this.babySpawnChance = 0D;
        this.canGrow = true;
        this.setupMob();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setLongMemory(false).setRange(2));
    }

    @Override
    public void loadCreatureFlags() {
        this.whirlpoolRange = this.creatureInfo.getFlag("whirlpoolRange", this.whirlpoolRange);
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.getCommandSenderWorld().isClientSide) {
            if (this.whirlpoolRecharging) {
                if (++this.whirlpoolEnergy >= this.whirlpoolEnergyMax)
                    this.whirlpoolRecharging = false;
            }
            this.whirlpoolEnergy = Math.min(this.whirlpoolEnergy, this.whirlpoolEnergyMax);
            if (this.canWhirlpool()) {
                for (Entity entity : this.getNearbyEntities(Entity.class, null, this.whirlpoolRange)) {
                    if (entity == this)
                        continue;
                    if (entity instanceof LivingEntity livingEntity && !this.canAttack(livingEntity))
                        continue;
                    ServerPlayer player = null;
                    if (entity instanceof ServerPlayer serverPlayer) {
                        player = serverPlayer;
                        if (player.getAbilities().instabuild)
                            continue;
                    }
                    double xDist = this.position().x() - entity.position().x();
                    double zDist = this.position().z() - entity.position().z();
                    double xzDist = Math.max(Mth.sqrt((float) (xDist * xDist + zDist * zDist)), 0.01D);
                    double factor = 0.1D;
                    entity.push(
                            xDist / xzDist * factor + entity.getDeltaMovement().x() * factor,
                            factor,
                            zDist / xzDist * factor + entity.getDeltaMovement().z() * factor
                    );
                    if (player != null)
                        player.connection.send(new ClientboundSetEntityMotionPacket(entity));
                    else
                        entity.hurtMarked = true;
                }
                if (--this.whirlpoolEnergy <= 0)
                    this.whirlpoolRecharging = true;
            }
        }
    }

    public boolean canWhirlpool() {
        if (this.getCommandSenderWorld().isClientSide)
            return false;
        if (!this.isInWater())
            return false;
        return !this.whirlpoolRecharging && this.hasAttackTarget() && this.distanceTo(this.getTarget()) <= (this.whirlpoolRange * 3);
    }

    @Override
    public float getBlockPathWeight(int x, int y, int z) {
        Block block = this.getCommandSenderWorld().getBlockState(new BlockPos(x, y, z)).getBlock();
        if (block == Blocks.WATER)
            return (super.getBlockPathWeight(x, y, z) + 1) * 11F;
        if (this.getCommandSenderWorld().isRaining() && this.getCommandSenderWorld().canSeeSkyFromBelowWater(new BlockPos(x, y, z)))
            return (super.getBlockPathWeight(x, y, z) + 1) * 11F;

        if (this.getTarget() != null)
            return super.getBlockPathWeight(x, y, z);
        if (this.isInWater())
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
