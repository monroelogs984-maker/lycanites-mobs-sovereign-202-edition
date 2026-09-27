package com.lycanitesmobs.core.entity.creature.elemental;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attributes;
import com.lycanitesmobs.core.entity.base.TameableCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import com.lycanitesmobs.core.entity.goals.actions.abilities.StealthGoal;
import com.lycanitesmobs.core.data.info.ObjectLists;
import com.lycanitesmobs.core.util.helpers.AssetHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class EntityGrue extends TameableCreatureEntity implements Enemy {
    public static final byte ATTACK_NONE = 0, ATTACK_SWIPE = 1, ATTACK_BITE = 2;
    private static final double ATTACK_TELEPORT_RANGE = 32.0D;
    private static final double ATTACK_TELEPORT_RANGE_SQ = ATTACK_TELEPORT_RANGE * ATTACK_TELEPORT_RANGE;
    private static final EntityDataAccessor<Byte> ATTACK_ANIM = SynchedEntityData.defineId(EntityGrue.class, EntityDataSerializers.BYTE);

    private int teleportTime = 60;

    // ==================================================
    //                    Constructor
    // ==================================================
    public EntityGrue(EntityType<? extends EntityGrue> entityType, Level world) {
        super(entityType, world);

        // Setup:
        this.hasAttackSound = true;
        // spawnsInWater dropped: handled by the JSON spawn config in this port.
        this.setupMob();

        this.getAttribute(Attributes.STEP_HEIGHT).setBaseValue(1.0F);
    }

    // ========== Init AI ==========
    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimPriorityGoalIndex(), new StealthGoal(this).setStealthTime(20).setStealthAttack(true).setStealthMove(true));
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setLongMemory(true));
    }


    // ==================================================
    //                      Updates
    // ==================================================
    // ========== Living Update ==========
    @Override
    public void aiStep() {
        super.aiStep();

        // Random Target Teleporting:
        LivingEntity target = this.getTarget();
        if (!this.getCommandSenderWorld().isClientSide && this.canTeleportBehindTarget(target)) {
            if (this.teleportTime-- <= 0) {
                this.teleportTime = 60 + this.getRandom().nextInt(40);
                BlockPos teleportPosition = this.getFacingPosition(target, -target.getBbWidth() - 1D, 0);
                if (this.canTeleportTo(teleportPosition)) {
                    this.playJumpSound();
                    this.setPos(teleportPosition.getX(), teleportPosition.getY(), teleportPosition.getZ());
                }
            }
        }

        // Particles:
        if (this.getCommandSenderWorld().isClientSide)
            for (int i = 0; i < 2; ++i) {
                this.getCommandSenderWorld().addParticle(ParticleTypes.WITCH, this.position().x() + (this.random.nextDouble() - 0.5D) * (double) this.getBbWidth(), this.position().y() + this.random.nextDouble() * (double) this.getBbHeight(), this.position().z() + (this.random.nextDouble() - 0.5D) * (double) this.getBbWidth(), 0.0D, 0.0D, 0.0D);
            }
    }

    /**
     * Checks if this entity can teleport to the provided block position.
     *
     * @param pos The position to teleport to.
     * @return True if it's safe to teleport.
     */
    public boolean canTeleportTo(BlockPos pos) {
        for (int y = 0; y <= 1; y++) {
            BlockState blockState = this.getCommandSenderWorld().getBlockState(pos.offset(0, y, 0));
            if (blockState.canOcclude())
                return false;
        }
        return true;
    }

    private boolean canTeleportBehindTarget(LivingEntity target) {
        return target != null && target.isAlive() && this.distanceToSqr(target) <= ATTACK_TELEPORT_RANGE_SQ;
    }


    // ==================================================
    //                     Stealth
    // ==================================================
    @Override
    public boolean canStealth() {
        if (this.getCommandSenderWorld().isClientSide) return false;
        if (this.isAttackOnCooldown()) return false;
        return this.testLightLevel() <= 0;
    }

    @Override
    public void startStealth() {
        if (this.getCommandSenderWorld().isClientSide) {
            ParticleOptions particle = ParticleTypes.WITCH;
            double d0 = this.random.nextGaussian() * 0.02D;
            double d1 = this.random.nextGaussian() * 0.02D;
            double d2 = this.random.nextGaussian() * 0.02D;
            for (int i = 0; i < 100; i++)
                this.getCommandSenderWorld().addParticle(particle, this.position().x() + (double) (this.random.nextFloat() * this.getBbWidth() * 2.0F) - (double) this.getBbWidth(), this.position().y() + 0.5D + (double) (this.random.nextFloat() * this.getBbHeight()), this.position().z() + (double) (this.random.nextFloat() * this.getBbWidth() * 2.0F) - (double) this.getBbWidth(), d0, d1, d2);
        }
        super.startStealth();
    }


    // ==================================================
    //                      Attacks
    // ==================================================
    // ========== Melee Attack ==========
    @Override
    public boolean attackMelee(Entity target, double damageScale) {
        if (!super.attackMelee(target, damageScale))
            return false;

        this.revealFromAttack();
        boolean bite = this.getRandom().nextFloat() < 0.5F;
        this.startAttackAnim(bite);
        // Leech:
        if (this.isRareVariant() && target instanceof LivingEntity) {
            LivingEntity targetLiving = (LivingEntity) target;
            List<Holder<MobEffect>> goodEffects = new ArrayList<>();
            for (MobEffectInstance effectInstance : targetLiving.getActiveEffects()) {
                if (ObjectLists.inEffectList("buffs", effectInstance.getEffect().value()))
                    goodEffects.add(effectInstance.getEffect());
            }
            if (goodEffects.size() > 0) {
                if (goodEffects.size() > 1)
                    targetLiving.removeEffect(goodEffects.get(this.getRandom().nextInt(goodEffects.size())));
                else
                    targetLiving.removeEffect(goodEffects.get(0));
                float leeching = Math.max(1, this.getAttackDamage(damageScale) / 2);
                this.heal(leeching);
            }
        }

        return true;
    }

    private void revealFromAttack() {
        this.setStealth(0F);
        if (!this.hasEffect(MobEffects.INVISIBILITY)) {
            this.setInvisible(false);
        }
    }


    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ATTACK_ANIM, ATTACK_NONE);
    }

    public void startAttackAnim(boolean bite) {
        this.entityData.set(ATTACK_ANIM, bite ? ATTACK_BITE : ATTACK_SWIPE);
    }

    public byte getAttackAnimType() {
        return this.entityData.get(ATTACK_ANIM);
    }

    public float getMeleeAttackAnim(float pt) {
        int max = Math.max(1, this.getAttackCooldownMax());
        float cur = this.attackCooldown;
        float t = (max - (cur - pt)) / (float) max;
        return Mth.clamp(t, 0F, 1F);
    }

    // ==================================================
    //                     Abilities
    // ==================================================
    @Override
    public boolean isFlying() {
        return true;
    }

    @Override
    public boolean isStrongSwimmer() {
        return true;
    }


    // ==================================================
    //                     Pet Control
    // ==================================================
    public boolean petControlsEnabled() {
        return true;
    }

    // ==================================================
    //                     Equipment
    // ==================================================
    @Override
    public int getNoBagSize() {
        return 0;
    }

    @Override
    public int getBagSize() {
        return this.creatureInfo.getBagSize();
    }

    // ==================================================
    //                     Immunities
    // ==================================================
    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (source.is(DamageTypes.IN_WALL)) return true;
        return super.isInvulnerableTo(source);
    }

    /**
     * Returns true if this mob should be damaged by the sun.
     **/
    public boolean daylightBurns() {
        return false;
    }

    @Override
    public boolean creatureCanBreatheUnderwater() {
        return true;
    }


    // ==================================================
    //                       Visuals
    // ==================================================
    @Override
    public ResourceLocation getTexture(String suffix) {
        if (!this.hasCustomName() || !"Shadow Clown".equals(this.getCustomName().getString()))
            return super.getTexture(suffix);

        String textureName = this.getTextureName() + "_shadowclown";
        if (!"".equals(suffix)) {
            textureName += "_" + suffix;
        }
        return AssetHelper.entityTexture(textureName);
    }
}
