package com.lycanitesmobs.core.entity.goals.actions;

import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

import java.util.EnumSet;

public class WanderGoal extends Goal {
    private BaseCreatureEntity host;

    private double speed = 1.0D;

    private double xPosition;
    private double yPosition;
    private double zPosition;

    public WanderGoal(BaseCreatureEntity setHost) {
        this.host = setHost;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    public WanderGoal setSpeed(double setSpeed) {
        this.speed = setSpeed;
        return this;
    }

    @Override
    public boolean canUse() {
        if (this.host.hasAttackTarget())
            return false;
        if (this.host.getAge() >= 100)
            return false;
        else if (!this.host.rollWanderChance())
            return false;
        else {
            Vector3d newTarget = RandomPositionGenerator.findRandomTarget(this.host, 10, 7, this.host.getFlyingHeight());
            if (newTarget == null) {
                return false;
            } else {
                BlockPos wanderPosition = this.host.getWanderPosition(new BlockPos((int) newTarget.x, (int) newTarget.y, (int) newTarget.z));
                this.xPosition = wanderPosition.getX();
                this.yPosition = wanderPosition.getY();
                this.zPosition = wanderPosition.getZ();
                return true;
            }
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (!this.host.useDirectNavigator()) {
            if (this.host.getNavigation().isDone()) {
                return false;
            } else if (this.host.distanceToSqr(new Vec3(this.xPosition, this.yPosition, this.zPosition)) < 4) {
                this.host.getNavigation().stop();
                return false;
            } else {
                return true;
            }
        } else {
            return !this.host.isDirectNavigationAtTarget() && this.host.isDirectNavigationTargetValid();
        }
    }

    @Override
    public void start() {
        if (!host.useDirectNavigator()) {
            this.host.getNavigation().moveTo(this.xPosition, this.yPosition, this.zPosition, this.speed);
        } else
            this.host.setDirectNavigationTarget(new BlockPos((int) this.xPosition, (int) this.yPosition, (int) this.zPosition), this.speed);
    }
}
