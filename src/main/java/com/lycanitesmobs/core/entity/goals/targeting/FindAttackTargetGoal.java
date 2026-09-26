package com.lycanitesmobs.core.entity.goals.targeting;

import com.lycanitesmobs.core.entity.util.CreatureRelationshipEntry;
import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import com.lycanitesmobs.core.util.helpers.LMHelperClass;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.*;

public class FindAttackTargetGoal extends TargetingGoal {
    private List<EntityType> targetTypes = new ArrayList<>();
    private List<Class<? extends Entity>> targetClasses = new ArrayList<>();

    protected boolean targetPlayers;
    private boolean requirePack = false;
    protected boolean tameTargeting = false;

    public FindAttackTargetGoal(BaseCreatureEntity setHost) {
        super(setHost);
        this.setFlags(EnumSet.of(Flag.TARGET));
    }

    public FindAttackTargetGoal setCheckSight(boolean bool) {
        this.checkSight = bool;
        return this;
    }

    public FindAttackTargetGoal addTargets(EntityType... targets) {
        this.targetTypes.addAll(Arrays.asList(targets));
        for (EntityType targetType : targets) {
            this.host.setHostileTo(targetType);
            if (targetType == EntityType.PLAYER)
                this.targetPlayers = true;
        }
        return this;
    }

    public FindAttackTargetGoal addTargets(Class<? extends Entity>... targets) {
        this.targetClasses.addAll(Arrays.asList(targets));
        for (Class<? extends Entity> targetType : targets) {
            this.host.setHostileTo(targetType);
            if (targetType.isAssignableFrom(Player.class))
                this.targetPlayers = true;
        }
        return this;
    }

    public FindAttackTargetGoal setOnlyNearby(boolean setNearby) {
        this.nearbyOnly = setNearby;
        return this;
    }

    public FindAttackTargetGoal setCantSeeTimeMax(int setCantSeeTimeMax) {
        this.cantSeeTimeMax = setCantSeeTimeMax;
        return this;
    }

    public FindAttackTargetGoal setRange(double range) {
        this.targetingRange = range;
        return this;
    }

    public FindAttackTargetGoal setHelpCall(boolean setHelp) {
        this.callForHelp = setHelp;
        return this;
    }

    public FindAttackTargetGoal setTameTargetting(boolean setTargetting) {
        this.tameTargeting = setTargetting;
        return this;
    }

    public FindAttackTargetGoal requiresPack() {
        this.requirePack = true;
        return this;
    }

    @Override
    protected LivingEntity getTarget() {
        return this.host.getTarget();
    }

    @Override
    protected void setTarget(LivingEntity newTarget) {
        this.host.setTarget(newTarget);
    }

    @Override
    protected boolean isValidTarget(LivingEntity target) {
        if (!this.targetTypes.isEmpty() && !this.targetTypes.contains(target.getType())) {
            return false;
        }

        if (!this.targetClasses.isEmpty()) {
            boolean isTargetClass = false;
            for (Class<? extends Entity> targetClass : this.targetClasses) {
                if (targetClass.isAssignableFrom(target.getClass())) {
                    isTargetClass = true;
                    break;
                }
            }
            if (!isTargetClass) {
                return false;
            }
        }

        if (!this.tameTargeting && this.host.isTamed()) {
            return false;
        }

        if (!this.host.canAttackType(target.getType())) {
            return false;
        }

        if (!this.host.canAttack(target)) {
            return false;
        }

        // Relationships Check:
        CreatureRelationshipEntry relationshipEntry = this.host.getRelationshipEntry(target);
        if (relationshipEntry != null && !relationshipEntry.canHunt()) {
            return false;
        }

        if (this.requirePack && !this.host.isInPack()) {
            return false;
        }
        return true;
    }

    @Override
    public boolean canUse() {
        if (!this.host.isAggressive() || this.host.hasFixateTarget()) {
            return false;
        }

        if (this.targetPlayers) {
            if (!this.host.isUpdateTickMultiple(10)) {
                return false;
            }
        } else {
            if (!this.host.isUpdateTickMultiple(40)) {
                return false;
            }
        }

        this.target = null;

        double distance = this.getTargetDistance();
        double heightDistance = 4.0D + this.host.getBbHeight();
        if (this.host.useDirectNavigator())
            heightDistance = distance;

        this.target = this.getNewTarget(distance, heightDistance, distance);
        if (this.target == null || !this.host.rollAttackTargetChance(this.target)) {
            return false;
        }
        if (this.callForHelp)
            this.callNearbyForHelp();

        return true;
    }

    @Override
    public boolean shouldStopTargeting(LivingEntity target) {
        return !this.isValidTarget(target);
    }

    @Override
    public LivingEntity getNewTarget(double rangeX, double rangeY, double rangeZ) {
        if (this.targetPlayers) {
            LivingEntity newTarget = null;
            try {
                List<? extends Player> players = this.host.getCommandSenderWorld().players();
                if (!players.isEmpty()) {
                    double nearestDistance = Double.MAX_VALUE;
                    for (Player player : players) {
                        if (this.targetSelector.test(player)) {
                            double distance = this.host.distanceToSqr(player);
                            if (distance < nearestDistance) {
                                nearestDistance = distance;
                                newTarget = player;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LMHelperClass.logWarningMessage("An exception occurred when player target selecting, this has been skipped to prevent a crash.");
                e.printStackTrace();
            }

            if (newTarget != null) {
                return newTarget;
            }
        }

        if (this.host.isUpdateTickMultiple(40)) {
            return super.getNewTarget(rangeX, rangeY, rangeZ);
        }

        return null;
    }
}
