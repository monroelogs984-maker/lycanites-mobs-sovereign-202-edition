package com.lycanitesmobs.core.entity.goals.targeting;

import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import net.minecraft.world.entity.LivingEntity;

public class FindAvoidTargetGoal extends TargetingGoal {
    private Class targetClass = LivingEntity.class;

    protected int targetChance = 0;
    protected boolean tameTargeting = false;

    public FindAvoidTargetGoal(BaseCreatureEntity setHost) {
        super(setHost);
    }

    public FindAvoidTargetGoal setChance(int setChance) {
        this.targetChance = setChance;
        return this;
    }

    public FindAvoidTargetGoal setTargetClass(Class setTargetClass) {
        this.targetClass = setTargetClass;
        return this;
    }

    public FindAvoidTargetGoal setSightCheck(boolean setSightCheck) {
        this.checkSight = setSightCheck;
        return this;
    }

    public FindAvoidTargetGoal setOnlyNearby(boolean setNearby) {
        this.nearbyOnly = setNearby;
        return this;
    }

    public FindAvoidTargetGoal setCantSeeTimeMax(int setCantSeeTimeMax) {
        this.cantSeeTimeMax = setCantSeeTimeMax;
        return this;
    }

    public FindAvoidTargetGoal setTameTargetting(boolean setTargetting) {
        this.tameTargeting = setTargetting;
        return this;
    }

    public FindAvoidTargetGoal setHelpCall(boolean setHelp) {
        this.callForHelp = setHelp;
        return this;
    }

    @Override
    protected LivingEntity getTarget() {
        return this.host.getAvoidTarget();
    }

    @Override
    protected void setTarget(LivingEntity newTarget) {
        this.host.setAvoidTarget(newTarget);
    }

    @Override
    protected boolean isValidTarget(LivingEntity target) {
        if (this.targetClass != null && !this.targetClass.isAssignableFrom(target.getClass()))
            return false;

        if (this.targetClass != this.host.getClass() && target.getClass() == this.host.getClass())
            return false;

        // NOTE: TameableCreatureEntity not ported yet - tamed creatures aren't excluded from
        // being avoid-targeted for now.

        return true;
    }

    @Override
    public boolean canUse() {
        if (!this.tameTargeting && this.host.isTamed()) {
            return false;
        }
        if (!this.host.isUpdateTickMultiple(60)) {
            return false;
        }
        if (this.targetChance > 0 && this.host.getRandom().nextInt(this.targetChance) != 0) {
            return false;
        }

        LivingEntity avoidTarget = this.getTarget();
        if (avoidTarget != null && !this.isValidTarget(avoidTarget)) {
            return false;
        }

        this.target = null;

        double distance = this.getTargetDistance();
        double heightDistance = 4.0D;
        if (this.host.useDirectNavigator())
            heightDistance = distance;
        this.target = this.getNewTarget(distance, heightDistance, distance);
        if (this.callForHelp)
            this.callNearbyForHelp();
        return this.target != null;
    }
}
