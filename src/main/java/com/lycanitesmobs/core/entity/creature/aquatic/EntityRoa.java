package com.lycanitesmobs.core.entity.creature.aquatic;

import com.lycanitesmobs.core.entity.IGroupBoss;
import com.lycanitesmobs.core.entity.IGroupHeavy;
import com.lycanitesmobs.core.entity.base.AgeableCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import com.lycanitesmobs.core.util.helpers.LMHelperClass;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Trimmed - original extends RideableCreatureEntity (mount/stamina/pet-control system, not
 * ported); rebased onto AgeableCreatureEntity, dropping mountAbility/riderEffects/
 * onDismounted/getPassengersRidingOffset/bag/pet-control. The "sharknado" spawn-event branches
 * (canWhirlpool/canBreatheAir/isFlying) are dropped - hasSpawnEventType()/extraAnimation01()
 * don't exist on this port's BaseCreatureEntity (spawn-event system not ported). Whirlpool AI
 * mechanic itself kept, just always water-triggered rather than mount-triggered.
 */
public class EntityRoa extends AgeableCreatureEntity implements Enemy {

    protected int whirlpoolRange = 6;
    protected int whirlpoolEnergy = 0;
    protected int whirlpoolEnergyMax = 5 * 20;
    protected boolean whirlpoolRecharging = true;

    public EntityRoa(EntityType<? extends EntityRoa> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = true;
        this.babySpawnChance = 0D;
        this.canGrow = true;
        this.setupMob();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setLongMemory(false));
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
                    if (entity == this || entity.getClass() == this.getClass() || entity == this.getControllingPassenger() || entity instanceof IGroupBoss || entity instanceof IGroupHeavy)
                        continue;
                    if (entity instanceof LivingEntity) {
                        // NOTE: original also skipped entities with the "weight" mod effect
                        // (ObjectManager.getEffect() returns a raw MobEffect, but
                        // LivingEntity.hasEffect() now needs a Holder<MobEffect> in 1.21.1 -
                        // dropped rather than guess at the right wrapping helper).
                        LivingEntity entityLivingBase = (LivingEntity) entity;
                        if (!this.canAttack(entityLivingBase))
                            continue;
                        if (!entity.isInWater())
                            continue;
                    }
                    ServerPlayer player = null;
                    if (entity instanceof ServerPlayer) {
                        player = (ServerPlayer) entity;
                        if (player.getAbilities().instabuild)
                            continue;
                    }
                    double xDist = this.position().x() - entity.position().x();
                    double zDist = this.position().z() - entity.position().z();
                    double xzDist = Math.max(Mth.sqrt(LMHelperClass.convertToFloat(xDist * xDist + zDist * zDist)), 0.01D);
                    double factor = 0.1D;
                    double motionCap = 10;
                    if (entity.getDeltaMovement().x() < motionCap && entity.getDeltaMovement().x() > -motionCap && entity.getDeltaMovement().z() < motionCap && entity.getDeltaMovement().z() > -motionCap) {
                        entity.push(
                                xDist / xzDist * factor + entity.getDeltaMovement().x() * factor,
                                0,
                                zDist / xzDist * factor + entity.getDeltaMovement().z() * factor
                        );
                    }
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
        if (!this.isInWater()) {
            return false;
        }
        return !this.whirlpoolRecharging && this.hasAttackTarget() && this.distanceTo(this.getTarget()) <= (this.whirlpoolRange * 3);
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
