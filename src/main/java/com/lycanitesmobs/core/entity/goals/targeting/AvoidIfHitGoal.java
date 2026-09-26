package com.lycanitesmobs.core.entity.goals.targeting;

import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;

public class AvoidIfHitGoal extends FindAvoidTargetGoal {
    Class[] helpClasses = null;
    private int revengeTime;

    public AvoidIfHitGoal(BaseCreatureEntity setHost) {
        super(setHost);
    }

    public AvoidIfHitGoal setHelpCall(boolean setHelp) {
        this.callForHelp = setHelp;
        return this;
    }

    public AvoidIfHitGoal setHelpClasses(Class... setHelpClasses) {
        this.helpClasses = setHelpClasses;
        this.callForHelp = true;
        return this;
    }

    public AvoidIfHitGoal setSightCheck(boolean setSightCheck) {
        this.checkSight = setSightCheck;
        return this;
    }

    public AvoidIfHitGoal setOnlyNearby(boolean setNearby) {
        this.nearbyOnly = setNearby;
        return this;
    }

    public AvoidIfHitGoal setCantSeeTimeMax(int setCantSeeTimeMax) {
        this.cantSeeTimeMax = setCantSeeTimeMax;
        return this;
    }

    public boolean canUse() {
        if (this.host.getLastHurtByMob() == null)
            return false;

        if (this.host.shouldCreatureGroupRevenge(this.host.getLastHurtByMob())) {
            return false;
        }

        return this.host.getLastHurtByMobTimestamp() != this.revengeTime;
    }

    public void start() {
        this.target = this.host.getLastHurtByMob();
        this.revengeTime = this.host.getLastHurtByMobTimestamp();
        this.callNearbyForHelp();
        super.start();
    }
}
