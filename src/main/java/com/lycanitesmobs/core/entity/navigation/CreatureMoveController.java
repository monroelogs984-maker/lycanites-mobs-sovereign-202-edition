package com.lycanitesmobs.core.entity.navigation;

import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Phase 5g: ported from the official source. Walking mirrors vanilla MoveControl; strong swimmers get smooth
 * fish-like movement; flyers get direct 3D velocity steering.
 *
 * <p>1.21.1: NodeEvaluator.getBlockPathType(level, x, y, z) -> getPathType(Mob, BlockPos); BlockPathTypes -> PathType.
 * isControlledByRider() uses getControllingPassenger() only - TODO(port): also require canBeControlledByRider()
 * once RideableCreatureEntity is ported.
 */
public class CreatureMoveController extends MoveControl {

    protected BaseCreatureEntity entityCreature;
    /**
     * Used by flight movement for changing course, makes for smoother movement.
     **/
    protected int courseChangeCooldown;

    public CreatureMoveController(BaseCreatureEntity baseCreatureEntity) {
        super(baseCreatureEntity);
        this.entityCreature = baseCreatureEntity;
    }

    /**
     * Called on update to move the entity.
     **/
    @Override
    public void tick() {
        // Rider:
        if (this.isControlledByRider()) {
            return;
        }

        // Swimming:
        if (this.entityCreature.isStrongSwimmer() && this.entityCreature.isUnderWater()) {
            this.tickSwimming();
            return;
        }

        // Flying:
        if (this.entityCreature.isFlying() && !this.entityCreature.isUnderWater()) {
            this.tickFlying();
            return;
        }

        // Walking:
        this.tickWalking();
    }

    /**
     * Returns true if the entity is controlled by its rider.
     **/
    public boolean isControlledByRider() {
        return this.entityCreature != null && this.entityCreature.getControllingPassenger() instanceof Player;
    }

    /**
     * Used by land entities for ground movement.
     **/
    public void tickWalking() {
        float moveZ;
        if (this.operation == MoveControl.Operation.STRAFE) {
            float moveSpeed = (float) this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED);
            float scaledSpeed = (float) this.speedModifier * moveSpeed;
            float moveForward = this.strafeForwards;
            float moveStrafe = this.strafeRight;
            float velocity = Mth.sqrt(moveForward * moveForward + moveStrafe * moveStrafe);
            if (velocity < 1.0F) {
                velocity = 1.0F;
            }

            velocity = scaledSpeed / velocity;
            moveForward *= velocity;
            moveStrafe *= velocity;
            float yawSin = Mth.sin(this.mob.getYRot() * 0.017453292F);
            float yawCos = Mth.cos(this.mob.getYRot() * 0.017453292F);
            float moveX = moveForward * yawCos - moveStrafe * yawSin;
            moveZ = moveStrafe * yawCos + moveForward * yawSin;
            PathNavigation pathNavigator = this.mob.getNavigation();
            NodeEvaluator nodeProcessor = pathNavigator.getNodeEvaluator();
            if (nodeProcessor.getPathType(this.mob, BlockPos.containing(this.mob.getX() + (double) moveX, this.mob.getY(), this.mob.getZ() + (double) moveZ)) != PathType.WALKABLE) {
                this.strafeForwards = 1.0F;
                this.strafeRight = 0.0F;
                scaledSpeed = moveSpeed;
            }

            this.mob.setSpeed(scaledSpeed);
            this.mob.setZza(this.strafeForwards);
            this.mob.setXxa(this.strafeRight);
            this.operation = MoveControl.Operation.WAIT;
        } else if (this.operation == MoveControl.Operation.MOVE_TO) {
            this.operation = MoveControl.Operation.WAIT;
            double distanceX = this.wantedX - this.mob.getX();
            double distanceZ = this.wantedZ - this.mob.getZ();
            double distanceY = this.wantedY - this.mob.getY();
            double distanceXZ = distanceX * distanceX + distanceZ * distanceZ;
            double distanceSq = distanceX * distanceX + distanceY * distanceY + distanceZ * distanceZ;

            if (distanceSq < 2.500000277905201E-7D) {
                this.mob.setZza(0.0F);
                return;
            }

            moveZ = (float) (Math.atan2(distanceZ, distanceX) * 57.2957763671875D) - 90.0F;
            float newYaw = this.rotlerp(this.mob.getYRot(), moveZ, 90.0F);
            this.mob.setYRot(newYaw);
            this.mob.yBodyRot = newYaw;
            this.mob.yHeadRot = newYaw;

            this.mob.setSpeed((float) (this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));

            BlockPos entityPos = this.mob.blockPosition();
            BlockState blockState = this.mob.level().getBlockState(entityPos);
            VoxelShape collisionShape = blockState.getCollisionShape(this.mob.level(), entityPos);
            double jumpRange = Math.max(1.0F, this.mob.getBbWidth() + 0.25F);
            if (distanceY > (double) this.mob.maxUpStep() && distanceXZ < jumpRange
                    || !collisionShape.isEmpty()
                    && this.mob.getY() < collisionShape.max(Direction.Axis.Y) + (double) entityPos.getY()) {
                this.mob.getJumpControl().jump();
                this.operation = MoveControl.Operation.JUMPING;
            }
        } else if (this.operation == MoveControl.Operation.JUMPING) {
            this.mob.setSpeed((float) (this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));
            if (this.mob.onGround()) {
                this.operation = MoveControl.Operation.WAIT;
            }
        } else {
            this.mob.setZza(0.0F);
        }
    }

    /**
     * Used by strong swimmers for fast, smooth movement.
     **/
    public void tickSwimming() {
        if (this.operation == MoveControl.Operation.MOVE_TO && !this.entityCreature.getNavigation().isDone()) {
            double x = this.wantedX - this.entityCreature.getX();
            double y = this.wantedY - this.entityCreature.getY();
            double z = this.wantedZ - this.entityCreature.getZ();
            double distance = Math.sqrt(x * x + y * y + z * z);
            y = y / distance;
            float f = (float) (Math.atan2(z, x) * (180D / Math.PI)) - 90.0F;

            float newYaw = this.rotlerp(this.entityCreature.getYRot(), f, 90.0F);
            this.entityCreature.setYRot(newYaw);
            this.entityCreature.yBodyRot = newYaw;
            this.entityCreature.yHeadRot = newYaw;

            float f1 = (float) (this.speedModifier * this.entityCreature.getAttributeValue(Attributes.MOVEMENT_SPEED));
            this.entityCreature.setSpeed(this.entityCreature.getSpeed() + (f1 - this.entityCreature.getSpeed()) * 0.125F);

            double d4 = Math.sin((double) (this.entityCreature.tickCount + this.entityCreature.getId()) * 0.5D) * 0.05D;
            double d5 = Math.cos(this.entityCreature.getYRot() * 0.017453292F);
            double d6 = Math.sin(this.entityCreature.getYRot() * 0.017453292F);
            double motionX = d4 * d5;
            double motionZ = d4 * d6;
            d4 = Math.sin((double) (this.entityCreature.tickCount + this.entityCreature.getId()) * 0.75D) * 0.05D;
            double motionY = d4 * (d6 + d5) * 0.25D;
            motionY += (double) this.entityCreature.getSpeed() * y * 0.125D;
            this.entityCreature.setDeltaMovement(this.entityCreature.getDeltaMovement().add(motionX, motionY, motionZ));

            LookControl lookController = this.entityCreature.getLookControl();
            double d7 = this.entityCreature.getX() + x / distance * 2.0D;
            double d8 = (double) this.entityCreature.getEyeHeight() + this.entityCreature.getY() + y / distance;
            double d9 = this.entityCreature.getZ() + z / distance * 2.0D;
            double d10 = lookController.getWantedX();
            double d11 = lookController.getWantedY();
            double d12 = lookController.getWantedZ();

            if (!lookController.isLookingAtTarget()) {
                d10 = d7;
                d11 = d8;
                d12 = d9;
            }

            lookController.setLookAt(
                    d10 + (d7 - d10) * 0.125D,
                    d11 + (d8 - d11) * 0.125D,
                    d12 + (d9 - d12) * 0.125D,
                    10.0F,
                    40.0F
            );
        } else {
            this.entityCreature.setSpeed(0.0F);
        }
    }

    /**
     * Used by flyers for swift, fast air movement.
     **/
    public void tickFlying() {
        if (this.operation == MoveControl.Operation.MOVE_TO) {
            double xDistance = this.wantedX - this.entityCreature.getX();
            double yDistance = this.wantedY - this.entityCreature.getY();
            double zDistance = this.wantedZ - this.entityCreature.getZ();
            double distanceSq = xDistance * xDistance + yDistance * yDistance + zDistance * zDistance;

            if (distanceSq < 2.500000277905201E-7D) {
                this.operation = MoveControl.Operation.WAIT;
            } else if (this.courseChangeCooldown-- <= 0) {
                this.courseChangeCooldown += this.entityCreature.getRandom().nextInt(5) + 2;
                double distance = Math.sqrt(distanceSq);
                if (distance >= 1D) {
                    this.entityCreature.setSpeed((float) this.entityCreature.getAttributeValue(Attributes.MOVEMENT_SPEED));
                    double speed = (this.entityCreature.getSpeed() / 2.4D) * this.getSpeedModifier();
                    double motionX = xDistance / distance * speed;
                    double motionY = yDistance / distance * speed;
                    double motionZ = zDistance / distance * speed;
                    this.entityCreature.setDeltaMovement(this.entityCreature.getDeltaMovement().add(motionX, motionY, motionZ));
                } else {
                    this.operation = MoveControl.Operation.WAIT;
                }
            }
        }

        if (this.entityCreature.getTarget() != null) {
            LivingEntity target = this.entityCreature.getTarget();
            double distanceX = target.getX() - this.entityCreature.getX();
            double distanceZ = target.getZ() - this.entityCreature.getZ();
            float yaw = -((float) Math.atan2(distanceX, distanceZ)) * (180F / (float) Math.PI);
            this.entityCreature.setYRot(yaw);
            this.entityCreature.yBodyRot = yaw;
            this.entityCreature.yHeadRot = yaw;
        } else if (this.operation == MoveControl.Operation.MOVE_TO) {
            double motionX = this.entityCreature.getDeltaMovement().x();
            double motionZ = this.entityCreature.getDeltaMovement().z();
            if (motionX != 0.0D || motionZ != 0.0D) {
                float yaw = -((float) Math.atan2(motionX, motionZ)) * (180F / (float) Math.PI);
                this.entityCreature.setYRot(yaw);
                this.entityCreature.yBodyRot = yaw;
                this.entityCreature.yHeadRot = yaw;
            }
        }
    }
}
