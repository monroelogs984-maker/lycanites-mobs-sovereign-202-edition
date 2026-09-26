package com.lycanitesmobs.core.entity.goals.actions;

import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import com.lycanitesmobs.core.util.helpers.LMHelperClass;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;

import java.util.EnumSet;

public class AttackMeleeGoal extends Goal {
    private static final String DEBUG_KEY = "Pathing";

    private BaseCreatureEntity host;
    private LivingEntity attackTarget;
    private Path pathToTarget;

    private double speed = 1.0D;
    private Class targetClass;
    private boolean longMemory = true;
    private int attackTime;
    private double attackRange = 0.5D;
    private float maxChaseDistance = 1024F;
    private double damageScale = 1.0D;
    private boolean enabled = true;
    protected int phase = -1;

    private int failedPathFindingPenalty;
    private int failedPathFindingPenaltyMax = 5;
    private int failedPathFindingPenaltyPlayerMax = 0;
    private int repathTime;

    public AttackMeleeGoal(BaseCreatureEntity setHost) {
        this.host = setHost;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    public AttackMeleeGoal setSpeed(double setSpeed) {
        this.speed = setSpeed;
        return this;
    }

    public AttackMeleeGoal setDamageScale(double scale) {
        this.damageScale = scale;
        return this;
    }

    public AttackMeleeGoal setTargetClass(Class setTargetClass) {
        this.targetClass = setTargetClass;
        return this;
    }

    public AttackMeleeGoal setLongMemory(boolean setMemory) {
        this.longMemory = setMemory;
        return this;
    }

    public AttackMeleeGoal setRange(double range) {
        this.attackRange = range;
        return this;
    }

    public AttackMeleeGoal setMaxChaseDistanceSq(float distance) {
        this.maxChaseDistance = distance * distance;
        return this;
    }

    public AttackMeleeGoal setMaxChaseDistance(float distance) {
        this.maxChaseDistance = distance * distance;
        return this;
    }

    public AttackMeleeGoal setMissRate(int rate) {
        this.failedPathFindingPenaltyMax = rate;
        return this;
    }

    public AttackMeleeGoal setEnabled(boolean setEnabled) {
        this.enabled = setEnabled;
        return this;
    }

    public AttackMeleeGoal setPhase(int phase) {
        this.phase = phase;
        return this;
    }

    @Override
    public boolean canUse() {
        if (!this.enabled)
            return false;

        if (this.phase >= 0 && this.phase != this.host.getBattlePhase()) {
            return false;
        }

        if (this.host.hasPickupEntity() && !this.host.canAttackWithPickup()) {
            return false;
        }

        this.attackTarget = this.host.getTarget();
        if (this.attackTarget == null)
            return false;
        if (!this.attackTarget.isAlive())
            return false;
        if (this.host.distanceToSqr(this.attackTarget.position().x(), this.attackTarget.getBoundingBox().minY, this.attackTarget.position().z()) > this.maxChaseDistance)
            return false;
        if (this.targetClass != null && !this.targetClass.isAssignableFrom(this.attackTarget.getClass()))
            return false;

        if (--this.repathTime <= 0) {
            if (!this.host.useDirectNavigator()) {
                if (this.host.isFlying()) {
                    this.pathToTarget = this.host.getNavigation().createPath(this.attackTarget.position().x(), this.attackTarget.getBoundingBox().minY + this.host.getFlightOffset(), this.attackTarget.position().z(), 0);
                } else {
                    this.pathToTarget = this.host.getNavigation().createPath(this.attackTarget, 0);
                }
                this.repathTime = 4 + this.host.getRandom().nextInt(7);
                this.debugPath("canUse-createPath", this.pathToTarget);
                return this.pathToTarget != null;
            } else {
                return this.host.setDirectNavigationTarget(new BlockPos((int) Math.floor(attackTarget.position().x()), (int) Math.floor(attackTarget.position().y() + this.host.getFlightOffset()), (int) Math.floor(attackTarget.position().z())), this.speed);
            }
        }

        if (this.pathToTarget == null && !this.host.useDirectNavigator()) {
            this.debug("canUse-delay-no-path repathTime=" + this.repathTime);
            return false;
        }
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (!this.enabled)
            return false;
        if (this.phase >= 0 && this.phase != this.host.getBattlePhase()) {
            return false;
        }
        this.attackTarget = this.host.getTarget();
        if (this.attackTarget == null)
            return false;
        if (this.targetClass != null && !this.targetClass.isAssignableFrom(this.attackTarget.getClass()))
            return false;
        if (!this.host.isAlive() || !this.attackTarget.isAlive())
            return false;
        if (this.host.distanceToSqr(this.attackTarget.position().x(), this.attackTarget.getBoundingBox().minY, this.attackTarget.position().z()) > this.maxChaseDistance)
            return false;
        if (!this.longMemory)
            if (!this.host.useDirectNavigator() && this.host.getNavigation().isDone())
                return false;
            else if (this.host.useDirectNavigator() && (this.host.isDirectNavigationAtTarget() || !this.host.isDirectNavigationTargetValid()))
                return false;
        return this.host.positionNearHome(Mth.floor(attackTarget.position().x()), Mth.floor(attackTarget.position().y()), Mth.floor(attackTarget.position().z()));
    }

    @Override
    public void start() {
        if (!this.host.useDirectNavigator()) {
            if (this.pathToTarget == null) {
                this.debug("start-with-null-path");
            }
            this.host.getNavigation().moveTo(this.pathToTarget, this.speed);
        } else if (attackTarget != null) {
            this.host.setDirectNavigationTarget(new BlockPos((int) attackTarget.position().x(), (int) (attackTarget.getBoundingBox().minY + this.host.getFlightOffset()), (int) attackTarget.position().z()), speed);
        }
        this.repathTime = 0;
    }

    @Override
    public void stop() {
        this.host.getNavigation().stop();
        this.host.clearDirectNavigationTarget(1.0D);
        this.attackTarget = null;
        this.pathToTarget = null;
    }

    @Override
    public void tick() {
        this.host.getLookControl().setLookAt(this.attackTarget, 30.0F, 30.0F);

        if (this.longMemory || this.host.getSensing().hasLineOfSight(this.attackTarget)) {
            if (!this.host.useDirectNavigator() && --this.repathTime <= 0) {
                this.repathTime = this.failedPathFindingPenalty + 4 + this.host.getRandom().nextInt(7);
                boolean moved;
                if (this.host.isFlying()) {
                    moved = this.host.getNavigation().moveTo(this.attackTarget.position().x(), this.attackTarget.getBoundingBox().minY + this.host.getFlightOffset(), this.attackTarget.position().z(), this.speed);
                } else {
                    moved = this.host.getNavigation().moveTo(this.attackTarget, this.speed);
                }
                this.debugPath("tick-moveTo", this.host.getNavigation().getPath());
                Path currentPath = this.host.getNavigation().getPath();
                if (moved && currentPath != null) {
                    Node finalPathPoint = currentPath.getEndNode();
                    if (this.pathCanReachAttackTarget(finalPathPoint)) {
                        this.failedPathFindingPenalty = 0;
                    } else {
                        this.failedPathFindingPenalty += this.getFailedPathFindingPenalty();
                        this.debug("tick-path-end-miss penalty=" + this.failedPathFindingPenalty + " final=" + this.nodeToString(finalPathPoint));
                    }
                } else {
                    this.failedPathFindingPenalty += this.getFailedPathFindingPenalty();
                    this.debug("tick-null-path penalty=" + this.failedPathFindingPenalty);
                }
            } else if (this.host.useDirectNavigator()) {
                this.host.setDirectNavigationTarget(new BlockPos((int) this.attackTarget.position().x(), (int) (this.attackTarget.getBoundingBox().minY + this.host.getFlightOffset()), (int) this.attackTarget.position().z()), speed);
            }
        }

        if (this.host.distanceToSqr(this.attackTarget.position().x(), this.attackTarget.getBoundingBox().minY, this.attackTarget.position().z()) <= this.host.getMeleeAttackRange(this.attackTarget, this.attackRange)) {
            if (--this.attackTime <= 0) {
                this.attackTime = this.host.getMeleeCooldown();
                if (!this.host.getMainHandItem().isEmpty())
                    this.host.swing(InteractionHand.MAIN_HAND);
                this.host.attackMelee(this.attackTarget, this.damageScale);
            }

            double d0 = this.host.position().x() - this.attackTarget.position().x();
            double d1 = this.host.position().z() - this.attackTarget.position().z();
            float targetYaw = (float) (Math.atan2(d1, d0) * 180.0D / Math.PI) + 90.0F;
            float currentYaw = this.host.getYRot();
            float delta = Mth.wrapDegrees(targetYaw - currentYaw);
            if (delta < -30.0F) delta = -30.0F;
            if (delta > 30.0F) delta = 30.0F;
            float newYaw = currentYaw + delta;
            this.host.setYRot(newYaw);
            this.host.yBodyRot = newYaw;
            this.host.yHeadRot = newYaw;
        }
    }

    public int getFailedPathFindingPenalty() {
        if (this.attackTarget instanceof Player || this.host.isTamed()) {
            return this.failedPathFindingPenaltyPlayerMax;
        }
        return this.failedPathFindingPenaltyMax;
    }

    private void debugPath(String event, Path path) {
        if (!LMHelperClass.isDebugEnabled(DEBUG_KEY)) {
            return;
        }
        this.debug(event
                + " path=" + (path != null)
                + " done=" + this.host.getNavigation().isDone()
                + " nodes=" + (path != null ? path.getNodeCount() : 0)
                + " end=" + (path != null ? this.nodeToString(path.getEndNode()) : "null"));
    }

    private String nodeToString(Node node) {
        if (node == null) {
            return "null";
        }
        return node.x + "," + node.y + "," + node.z;
    }

    private boolean pathCanReachAttackTarget(Node node) {
        if (node == null || this.attackTarget == null) {
            return false;
        }

        double xDistance = this.attackTarget.position().x() - ((double) node.x + 0.5D);
        double zDistance = this.attackTarget.position().z() - ((double) node.z + 0.5D);
        double horizontalDistanceSq = xDistance * xDistance + zDistance * zDistance;
        double attackReach = Math.sqrt(this.host.getMeleeAttackRange(this.attackTarget, this.attackRange)) + 1.0D;

        double targetPathY = this.attackTarget.getBoundingBox().minY;
        if (this.host.isFlying()) {
            targetPathY += this.host.getFlightOffset();
        }
        double verticalReach = Math.max(2.0D, this.host.getBbHeight() + this.attackTarget.getBbHeight() + Math.abs(this.host.getFlightOffset()));

        return horizontalDistanceSq <= attackReach * attackReach && Math.abs((double) node.y - targetPathY) <= verticalReach;
    }

    private void debug(String message) {
        if (!LMHelperClass.isDebugEnabled(DEBUG_KEY)) {
            return;
        }
        if (this.attackTarget == null) {
            LMHelperClass.logDebug(DEBUG_KEY, this.host.getEncodeId() + "#" + this.host.getId() + " melee " + message);
            return;
        }
        LMHelperClass.logDebug(DEBUG_KEY,
                this.host.getEncodeId() + "#" + this.host.getId()
                        + " -> " + this.attackTarget.getEncodeId() + "#" + this.attackTarget.getId()
                        + " melee " + message
                        + " dist=" + String.format("%.2f", this.host.distanceTo(this.attackTarget))
                        + " los=" + this.host.getSensing().hasLineOfSight(this.attackTarget)
                        + " longMemory=" + this.longMemory
                        + " repath=" + this.repathTime);
    }
}
