package com.lycanitesmobs.core.entity.navigation;

import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import com.lycanitesmobs.core.util.helpers.LMHelperClass;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class DirectNavigator {
    BaseCreatureEntity host;
    private BlockPos targetPosition;

    private double flyingSpeed = 1.0D;
    private boolean faceMovement = true;
    private double speedModifier = 1.0D;

    public DirectNavigator(BaseCreatureEntity setHost) {
        this.host = setHost;
    }

    public DirectNavigator setSpeed(double setSpeed) {
        this.flyingSpeed = setSpeed;
        return this;
    }

    public DirectNavigator setFacing(boolean facing) {
        this.faceMovement = facing;
        return this;
    }

    public void setSpeedModifier(double speedModifier) {
        this.speedModifier = speedModifier;
    }

    public boolean hasTargetPosition() {
        return this.targetPosition != null;
    }

    public int getTargetPositionY() {
        return this.targetPosition.getY();
    }

    public boolean setTargetPosition(BlockPos targetPosition, double setSpeedMod) {
        if (isTargetPositionValid(targetPosition)) {
            this.targetPosition = targetPosition;
            this.speedModifier = setSpeedMod;
            return true;
        }
        return false;
    }

    public boolean setTargetPosition(Entity targetEntity, double setSpeedMod) {
        return this.setTargetPosition(new BlockPos((int) targetEntity.position().x(), (int) targetEntity.position().y(), (int) targetEntity.position().z()), setSpeedMod);
    }

    public boolean clearTargetPosition(double setSpeedMod) {
        return this.setTargetPosition((BlockPos) null, setSpeedMod);
    }

    public boolean isTargetPositionValid() {
        return isTargetPositionValid(this.targetPosition);
    }

    public boolean isTargetPositionValid(BlockPos targetPosition) {
        return true;
    }

    public double distanceToTargetPosition() {
        return this.host.distanceToSqr(this.targetPosition.getX(), this.targetPosition.getY(), this.targetPosition.getZ());
    }

    public boolean atTargetPosition() {
        if (targetPosition != null) {
            double speed = this.host.getAttribute(Attributes.MOVEMENT_SPEED).getValue() * 2;
            return this.distanceToTargetPosition() <= (this.host.getDimensions(Pose.STANDING).width() + speed);
        }
        return true;
    }

    private double randomStrafeAngle = 0;

    // NOTE: only used by flying "ghost" creatures (useDirectNavigator() == true), none ported
    // yet - simplified to a safe no-op for now rather than porting getFacingPosition/
    // travelFlying/travelSwimming/lavaContact (none exist on the trimmed BaseCreatureEntity).
    public void updateFlight() {
    }

    public void flightMovement(double moveStrafe, double moveForward) {
        this.host.updateLimbSwing();
    }

    protected void adjustRotationToWaypoint() {
        double distX = targetPosition.getX() - this.host.position().x();
        double distZ = targetPosition.getZ() - this.host.position().z();
        float fullAngle = (float) (Math.atan2(distZ, distX) * 180.0D / Math.PI);
        float angle = LMHelperClass.convertToFloat(LMHelperClass.wrapDegrees(fullAngle - this.host.yRotO));
        if (angle > 30.0F) angle = 30.0F;
        if (angle < -30.0F) angle = -30.0F;
        this.host.yBodyRot = this.host.yRotO += angle;
    }

    public void adjustRotationToTarget(BlockPos target) {
        double distX = target.getX() - this.host.position().x();
        double distZ = target.getZ() - this.host.position().z();
        float fullAngle = (float) (Math.atan2(distZ, distX) * 180.0D / Math.PI) - 90.0F;
        float angle = LMHelperClass.convertToFloat(LMHelperClass.wrapDegrees(fullAngle - this.host.yRotO));
        this.host.yRotO += angle;
    }
}
