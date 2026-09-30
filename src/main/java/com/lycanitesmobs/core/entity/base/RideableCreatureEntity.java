package com.lycanitesmobs.core.entity.base;

import com.lycanitesmobs.core.capabilities.entity.ExtendedPlayer;
import com.lycanitesmobs.core.data.info.ObjectLists;
import com.lycanitesmobs.core.data.info.creature.CreatureType;
import com.lycanitesmobs.core.entity.goals.actions.PlayerControlGoal;
import com.lycanitesmobs.core.entity.goals.targeting.CopyRiderAttackTargetGoal;
import com.lycanitesmobs.core.entity.goals.targeting.RevengeRiderGoal;
import com.lycanitesmobs.core.manager.CreatureManager;
import com.lycanitesmobs.core.manager.ObjectManager;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.common.CommonHooks;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import java.util.HashMap;

/**
 * Mounts: saddled, tamed creatures their owner can ride and steer, with rider effects, a mount ability and
 * rider-assisting targeting.
 *
 * 1.21.1 changes vs the original:
 * - Player-ridden movement: vanilla now calls travelRidden() -> travel() on the controlling client (and zeroes
 *   movement on the server, which gets the position from the client's vehicle move packets), so the original
 *   travel() override slots straight in.
 * - Entity.getPassengersRidingOffset()/getMyRidingOffset() are gone; BaseCreatureEntity keeps
 *   getPassengersRidingOffset()/getMountedZOffset() as its own methods and positionRider() applies them with the
 *   1.20.1 player riding offset (-0.35), so the per-creature offsets tuned for the original are unchanged.
 * - setFlyingSpeed() is gone; vanilla's getFlyingSpeed() for a player-ridden entity is already speed * 0.1,
 *   which is the original's getGlideScale() default, so the glide assignment is dropped.
 * - Mount inventory (K) needs the creature GUI, which isn't ported yet, so it does nothing for now.
 */
public abstract class RideableCreatureEntity extends TameableCreatureEntity {
    /** The 1.20.1 Player.getMyRidingOffset() value the original's per-creature mount offsets are tuned against. */
    protected static final double PLAYER_RIDING_OFFSET = -0.35D;

    protected Entity lastRiddenByEntity = null;

    // Jumping:
    protected boolean mountJumping = false;
    protected float jumpPower = 0.0F;

    protected boolean abilityToggled = false;
    protected boolean inventoryToggled = false;

    // ==================================================
    //                    Constructor
    // ==================================================
    protected RideableCreatureEntity(EntityType<? extends AgeableCreatureEntity> entityType, Level world) {
        super(entityType, world);
        this.hasJumpSound = true;
    }

    // ========== Init AI ==========
    @Override
    protected void registerGoals() {
        // Greater Actions:
        this.goalSelector.addGoal(this.claimPriorityGoalIndex(), new PlayerControlGoal(this));

        super.registerGoals();

        // Lesser Targeting:
        this.targetSelector.addGoal(this.claimReactTargetGoalIndex(), new RevengeRiderGoal(this));
        this.targetSelector.addGoal(this.claimReactTargetGoalIndex(), new CopyRiderAttackTargetGoal(this));
    }

    // ==================================================
    //                       Update
    // ==================================================
    @Override
    public void tick() {
        // Detect Dismount:
        if (this.lastRiddenByEntity != this.getControllingPassenger()) {
            if (this.lastRiddenByEntity != null)
                this.onDismounted(this.lastRiddenByEntity);
            this.lastRiddenByEntity = this.getControllingPassenger();
        }

        super.tick();
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (this.hasRiderTarget()) {
            // Rider Buffs:
            int riderEffectUpdateInterval = 10;
            LivingEntity riderLiving = this.getControllingPassenger();
            if (!this.getCommandSenderWorld().isClientSide && this.updateTick % riderEffectUpdateInterval == 0 && riderLiving != null) {
                this.updateRiderEffects(riderLiving);
            }

            // Mount Melee:
            if (!this.getCommandSenderWorld().isClientSide && this.hasAttackTarget() && this.updateTick % 20 == 0) {
                LivingEntity mountedAttackTarget = this.getTarget();
                if (mountedAttackTarget != null && this.canAttack(mountedAttackTarget) && this.distanceToSqr(mountedAttackTarget.position().x(), mountedAttackTarget.getBoundingBox().minY, mountedAttackTarget.position().z()) <= this.getMeleeAttackRange(mountedAttackTarget, 1)) {
                    this.attackMelee(this.getTarget(), 1);
                }
            }
        }
        else {
            this.abilityToggled = false;
            this.inventoryToggled = false;
        }
    }

    /**
     * Continuous effects applied to the rider every 10 ticks, creatures add their own (e.g. water breathing).
     **/
    public void riderEffects(LivingEntity rider) {
        if (!rider.canBreatheUnderwater() && this.creatureCanBreatheUnderwater() && rider.isInWater())
            rider.setAirSupply(300);
        for (MobEffectInstance effectInstance : rider.getActiveEffects().toArray(new MobEffectInstance[0])) {
            if (!this.canBeAffected(effectInstance) && ObjectLists.inEffectList("debuffs", effectInstance.getEffect().value())) {
                rider.removeEffect(effectInstance.getEffect());
            }
        }
    }

    private void updateRiderEffects(LivingEntity rider) {
        this.riderEffects(rider);

        // Protect Rider from Effects:
        if (!this.canBurn()) {
            rider.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, (5 * 20) + 5, 1));
        }
        for (MobEffectInstance effectInstance : rider.getActiveEffects().toArray(new MobEffectInstance[0])) {
            if (!this.canBeAffected(effectInstance))
                rider.removeEffect(effectInstance.getEffect());
        }
    }

    // ==================================================
    //                   Mount Ability
    // ==================================================
    /**
     * The mount's special ability, triggered while the rider holds the mount ability key (server side).
     **/
    public void mountAbility(Entity rider) {}

    public void onDismounted(Entity entity) {
        this.setDeltaMovement(Vec3.ZERO);
        if (this.isSitting()) {
            this.restrictTo(this.blockPosition(), (int) this.sittingGuardRange);
        }
    }

    // ==================================================
    //                     Movement
    // ==================================================
    /**
     * Lycanites' own check (not a vanilla hook): stops the AI move controller steering while the owner rides.
     **/
    @Override
    public boolean canBeControlledByRider() {
        if (this.getCommandSenderWorld().isClientSide)
            return true;
        return this.getControllingPassenger() == this.getOwner();
    }

    @Override
    public boolean isPushable() {
        if (this.getControllingPassenger() != null)
            return false;
        return super.isPushable();
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction moveFunction) {
        if (!this.hasPassenger(passenger)) {
            return;
        }
        double zOffset = this.getMountedZOffset();
        if (zOffset == 0) {
            zOffset = 0.00001D;
        }
        Vector3d mountOffset = this.getFacingPositionDouble(0, 0, 0, zOffset, this.getYRot());
        double riderOffset = passenger instanceof Player ? PLAYER_RIDING_OFFSET : 0;
        moveFunction.accept(passenger, this.getX() + mountOffset.x, this.getY() + this.getPassengersRidingOffset() + riderOffset, this.getZ() + mountOffset.z);
    }

    private void mount(Entity entity) {
        entity.setYRot(this.getYRot());
        entity.setXRot(this.getXRot());
        if (!this.getCommandSenderWorld().isClientSide)
            entity.startRiding(this);
    }

    public float getStrafeSpeed() {
        if (this.isSwimming() || this.isFlying()) {
            return 0.25F;
        }
        return 0.75F;
    }

    // ========== Move with Heading ==========
    @Override
    public void travel(Vec3 direction) {
        // Check if Mounted:
        if (!this.isTamed() || !this.hasSaddle() || !this.hasRiderTarget() || !(this.getControllingPassenger() instanceof LivingEntity) || !this.riderControl()) {
            super.travel(direction);
            return;
        }
        this.moveMountedWithHeading(direction.x(), direction.y(), direction.z());
    }

    public void moveMountedWithHeading(double strafe, double up, double forward) {
        // Apply Rider Movement:
        LivingEntity rider = this.getControllingPassenger();
        if (rider != null) {
            this.setYRot(rider.getYRot());
            this.yRotO = this.getYRot();
            this.setXRot(rider.getXRot() * 0.5F);
            this.setRot(this.getYRot(), this.getXRot());
            this.yHeadRot = this.yBodyRot = this.getYRot();
            strafe = rider.xxa * this.getStrafeSpeed();
            forward = rider.zza * this.getAISpeedModifier();
        }

        ExtendedPlayer playerExt = rider instanceof Player player ? ExtendedPlayer.getForPlayer(player) : null;

        // Swimming / Flying Controls:
        double verticalMotion = 0;
        if (this.isInWater() || this.isInLava() || this.isFlying()) {
            if (playerExt != null && playerExt.isControlActive(ExtendedPlayer.CONTROL_ID.JUMP)) {
                verticalMotion = this.creatureStats.getSpeed() * 20;
            }
            else if (playerExt != null && playerExt.isControlActive(ExtendedPlayer.CONTROL_ID.DESCEND)) {
                verticalMotion = -this.creatureStats.getSpeed() * 20;
            }
        }
        else {
            // Jumping Controls:
            if (!this.isMountJumping() && playerExt != null && playerExt.isControlActive(ExtendedPlayer.CONTROL_ID.JUMP)) {
                this.startJumping();
            }

            // Jumping Behaviour:
            if (this.getJumpPower() > 0.0F && !this.isMountJumping() && this.isControlledByLocalInstance()) {
                this.setDeltaMovement(this.getDeltaMovement().add(0, this.getMountJumpHeight() * (double) this.getJumpPower(), 0));
                if (this.hasEffect(MobEffects.JUMP))
                    this.setDeltaMovement(this.getDeltaMovement().add(0, ((float) (this.getEffect(MobEffects.JUMP).getAmplifier() + 1) * 0.1F), 0));
                this.setMountJumping(true);
                this.hasImpulse = true;
                if (forward > 0.0F) {
                    float f2 = Mth.sin(this.getYRot() * (float) Math.PI / 180.0F);
                    float f3 = Mth.cos(this.getYRot() * (float) Math.PI / 180.0F);
                    this.setDeltaMovement(this.getDeltaMovement().add(-0.4F * f2 * this.jumpPower, 0, 0.4F * f3 * this.jumpPower));
                }
                if (!this.getCommandSenderWorld().isClientSide)
                    this.playJumpSound();
                this.setJumpPower(0);
                CommonHooks.onLivingJump(this);
            }
        }

        // Ability Controls (the server also runs these when the control payload arrives):
        if (rider instanceof Player player) {
            this.handleRiderControls(player, playerExt);
        }

        // Apply Movement:
        if (this.isControlledByLocalInstance()) {
            this.setSpeed((float) this.getAttribute(Attributes.MOVEMENT_SPEED).getValue());
            if (!this.useDirectNavigator()) {
                if (this.isFlying() && !this.isInWater() && !this.isInLava()) {
                    this.moveRelative(0.1F, new Vec3(strafe, 0, forward));
                    this.move(MoverType.SELF, new Vec3(this.getDeltaMovement().x, verticalMotion / 16, this.getDeltaMovement().z));
                    this.setDeltaMovement(this.getDeltaMovement().multiply(0.8999999761581421D, 0.8999999761581421D, 0.8999999761581421D));
                }
                else if (this.isInWater()) {
                    if (!this.creatureCanBreatheUnderwater()) {
                        verticalMotion *= 0.25f;
                        strafe *= 0.25f;
                        forward *= 0.25f;
                    }
                    this.moveRelative(0.1F, new Vec3(strafe, 0, forward));
                    this.move(MoverType.SELF, this.getDeltaMovement().add(0, verticalMotion / 16, 0));
                    this.setDeltaMovement(this.getDeltaMovement().multiply(0.8999999761581421D, 0.8999999761581421D, 0.8999999761581421D));
                }
                else if (this.isInLava()) {
                    if (!this.isStrongSwimmer()) {
                        verticalMotion *= 0.25f;
                        strafe *= 0.5f;
                        forward *= 0.5f;
                    }
                    this.moveRelative(0.1F, new Vec3(strafe, 0, forward));
                    this.move(MoverType.SELF, this.getDeltaMovement().add(0, verticalMotion / 16, 0));
                    this.setDeltaMovement(this.getDeltaMovement().multiply(0.8999999761581421D, 0.8999999761581421D, 0.8999999761581421D));
                }
                else
                    super.travel(new Vec3(strafe, up, forward));
            }
            else
                this.moveWithDirectNavigator(strafe, forward);
        }

        // Clear Jumping:
        if (this.onGround() || this.isInWater() || this.isInLava()) {
            this.setJumpPower(0);
            this.setMountJumping(false);
        }

        // Animate Limbs:
        this.calculateEntityAnimation(true);
    }

    public void handleRiderControls(Player player, ExtendedPlayer playerExt) {
        if (player == null || playerExt == null || this.getControllingPassenger() != player) {
            return;
        }

        // Mount Ability:
        if (playerExt.isControlActive(ExtendedPlayer.CONTROL_ID.MOUNT_ABILITY)) {
            this.mountAbility(player);
            this.abilityToggled = true;
        }
        else {
            this.abilityToggled = false;
        }

        // Mount Inventory:
        if (playerExt.isControlActive(ExtendedPlayer.CONTROL_ID.MOUNT_INVENTORY)) {
            if (!this.inventoryToggled) {
                this.openGUI(player);
            }
            this.inventoryToggled = true;
        }
        else {
            this.inventoryToggled = false;
        }
    }

    // ========== Jumping Start ==========
    public void startJumping() {
        this.setJumpPower();
    }

    // ========== Jumping ==========
    public double getMountJumpHeight() {
        return 0.75D;
    }

    public boolean isMountJumping() {
        return this.mountJumping;
    }

    public void setMountJumping(boolean set) {
        this.mountJumping = set;
    }

    // ========== Jump Power ==========
    public void setJumpPower(int power) {
        if (power < 0)
            power = 0;
        if (power > 99)
            power = 99;
        if (power < 90)
            this.jumpPower = 1.0F * ((float) power / 89.0F);
        else
            this.jumpPower = 1.0F + (1.0F * ((float) (power - 89) / 10.0F));
    }

    public void setJumpPower() {
        this.setJumpPower(89);
    }

    public float getJumpPower() {
        return this.jumpPower;
    }

    // ========== Gliding ==========
    public double getGlideScale() {
        return 0.1F;
    }

    // ========== Rider Control ==========
    public boolean riderControl() {
        return true;
    }

    // ==================================================
    //                     Interact
    // ==================================================
    @Override
    public HashMap<Integer, String> getInteractCommands(Player player, @Nonnull ItemStack itemStack) {
        HashMap<Integer, String> commands = new HashMap<>(super.getInteractCommands(player, itemStack));

        // Mount:
        boolean mountingAllowed = CreatureManager.getInstance().getConfig().mountingEnabled();
        if (mountingAllowed && this.isFlying())
            mountingAllowed = CreatureManager.getInstance().getConfig().mountingFlightEnabled();
        if (this.canBeMounted(player) && !player.isShiftKeyDown() && !this.getCommandSenderWorld().isClientSide && mountingAllowed)
            commands.put(COMMAND_PIORITIES.MAIN.id, "Mount");

        return commands;
    }

    @Override
    public boolean performCommand(String command, Player player, ItemStack itemStack, InteractionHand hand) {
        // Mount:
        if (command.equals("Mount")) {
            this.playMountSound();
            this.clearMovement();
            this.setTarget(null);
            this.mount(player);
            return true;
        }

        return super.performCommand(command, player, itemStack, hand);
    }

    // ==================================================
    //                       Targets
    // ==================================================
    @Override
    public PlayerTeam getTeam() {
        if (this.hasRiderTarget()) {
            LivingEntity rider = this.getRider();
            if (rider != null)
                return rider.getTeam();
        }
        return super.getTeam();
    }

    @Override
    public boolean isAlliedTo(Entity target) {
        if (this.hasRiderTarget()) {
            LivingEntity rider = this.getRider();
            if (target == rider)
                return true;
            if (rider != null && target != null)
                return rider.isAlliedTo(target);
        }
        return super.isAlliedTo(target);
    }

    /**
     * Returns true if targetEntity is riding this creature, directly or nested (rider of a rider).
     **/
    public boolean isEntityPassenger(Entity targetEntity, Entity nestedRider) {
        for (Entity entity : nestedRider.getPassengers()) {
            if (entity.equals(targetEntity)) {
                return true;
            }
            if (this.isEntityPassenger(targetEntity, entity)) {
                return true;
            }
        }
        return false;
    }

    // ==================================================
    //                     Abilities
    // ==================================================
    public boolean canBeMounted(Entity entity) {
        if (this.getControllingPassenger() != null)
            return false;

        // Can Be Mounted By A Player:
        if (this.isTamed() && entity instanceof Player player) {
            if (player == this.getOwner())
                return this.hasSaddle() && !this.isBaby();
        }

        // Can Be Mounted By Mobs:
        else if (!this.isTamed() && !(entity instanceof Player)) {
            return !this.isBaby();
        }

        return false;
    }

    // ==================================================
    //                     Equipment
    // ==================================================
    public boolean hasSaddle() {
        ItemStack saddleStack = this.inventory.getEquipmentStack("saddle");
        CreatureType creatureType = this.creatureInfo.getCreatureType();
        return creatureType != null && !saddleStack.isEmpty() && saddleStack.getItem() == creatureType.getSaddleItem();
    }

    // ==================================================
    //                    Immunities
    // ==================================================
    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        Entity entity = source.getEntity();
        if (entity != null && this.getControllingPassenger() != null && this.isEntityPassenger(entity, this)) {
            return true;
        }
        return super.isInvulnerableTo(source);
    }

    @Override
    public float getFallResistance() {
        return 2;
    }

    // ==================================================
    //                       Sounds
    // ==================================================
    public void playMountSound() {
        this.playSound(ObjectManager.getSound(this.creatureInfo.getName() + "_mount"), 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
    }
}
