package com.lycanitesmobs.core.entity.goals.actions;

import com.lycanitesmobs.core.entity.base.AgeableCreatureEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class FollowParentGoal extends FollowGoal {

    AgeableCreatureEntity host;

    public FollowParentGoal(AgeableCreatureEntity setHost) {
        super(setHost);
        this.host = setHost;
    }

    public FollowParentGoal setSpeed(double setSpeed) {
        this.speed = setSpeed;
        return this;
    }

    public FollowParentGoal setTargetClass(Class setTargetClass) {
        this.targetClass = setTargetClass;
        return this;
    }

    public FollowParentGoal setStrayDistance(double setDist) {
        this.strayDistance = setDist;
        return this;
    }

    public FollowParentGoal setLostDistance(double setDist) {
        this.lostDistance = setDist;
        return this;
    }

    public FollowParentGoal setFollowBehind(double setDist) {
        this.behindDistance = setDist;
        return this;
    }

    @Override
    public Entity getTarget() {
        return this.host.getParentTarget();
    }

    @Override
    public void setTarget(Entity entity) {
        if (entity instanceof LivingEntity)
            this.host.setParentTarget((LivingEntity) entity);
    }

    @Override
    public boolean canUse() {
        if (!this.host.shouldFollowParent())
            return false;
        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        if (!this.host.shouldFollowParent())
            return false;
        return super.canContinueToUse();
    }
}
