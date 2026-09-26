package com.lycanitesmobs.core.entity.creature.beast;

import com.lycanitesmobs.core.entity.base.RideableCreatureEntity;
import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import com.lycanitesmobs.core.util.helpers.AssetHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Heavily trimmed - the original extends RideableCreatureEntity (mount subsystem not ported)
 * and attacks purely at range via ProjectileManager (not ported either). Substituted a plain
 * AttackMeleeGoal so this creature isn't completely inert (the original never had a melee
 * fallback). Dropped entirely: attackRanged/fireProjectile, the whole mount-ability/stamina
 * system, getStrafeSpeed (ranged-strafe-only hook), and isMinion()/hasMaster() checks in
 * shouldExplodeInDaylight() (tame/master system not ported - replaced with just isTamed()/
 * isRareVariant()). The self-contained "explodes in daylight unless tamed/rare" mechanic and the
 * custom-name vampire-bat texture swap are both kept since they only need vanilla APIs plus
 * already-ported BaseCreatureEntity hooks.
 * PHASE 5e UPDATE (2026-09-26): re-parented to its official superclass now that TameableCreatureEntity is
 * ported (taming/ownership/pet behaviour work; RideableCreatureEntity is still a stub). Any wording above
 * about extending Base/AgeableCreatureEntity or taming being unported is outdated.
 */
public class EntityEpion extends RideableCreatureEntity implements Enemy {

    protected boolean griefing = true;

    public EntityEpion(EntityType<? extends EntityEpion> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = false;
        this.flySoundSpeed = 20;
        this.setupMob();
    }

    // NOTE: 1.21.1 replaced the old setMaxUpStep(float) setter with an overridable
    // maxUpStep() getter (default 0.0F on Entity) - override it directly instead.
    @Override
    public float maxUpStep() {
        return 1.0F;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setLongMemory(false));
    }

    @Override
    public void loadCreatureFlags() {
        this.griefing = this.creatureInfo.getFlag("griefing", this.griefing);
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (this.getCommandSenderWorld().isClientSide)
            for (int i = 0; i < 2; ++i) {
                this.getCommandSenderWorld().addParticle(ParticleTypes.WITCH, this.position().x() + (this.random.nextDouble() - 0.5D) * (double) this.getDimensions(Pose.STANDING).width(), this.position().y() + this.random.nextDouble() * (double) this.getDimensions(Pose.STANDING).height(), this.position().z() + (this.random.nextDouble() - 0.5D) * (double) this.getDimensions(Pose.STANDING).width(), 0.0D, 0.0D, 0.0D);
            }

        if (!this.getCommandSenderWorld().isClientSide && this.shouldExplodeInDaylight() && this.isAlive()) {
            int explosionRadius = this.getSubspeciesIndex() > 0 ? 3 : 2;
            explosionRadius = Math.max(2, Math.round((float) explosionRadius * (float) this.getSizeScale()));
            this.getCommandSenderWorld().explode(this, this.position().x(), this.position().y(), this.position().z(), explosionRadius, this.getDaylightExplosionInteraction());
            this.discard();
        }
    }

    @Override
    public boolean isFlying() {
        if (this.getCommandSenderWorld().isClientSide) return true;
        if (this.shouldExplodeInDaylight())
            return false;
        return true;
    }

    protected boolean shouldExplodeInDaylight() {
        if (this.isTamed() || this.isRareVariant())
            return false;
        if (!this.daylightBurns() || !this.getCommandSenderWorld().isDay())
            return false;

        BlockPos daylightPos = BlockPos.containing(this.getX(), this.getEyeY(), this.getZ());
        float brightness = this.getDaylightExplosionBrightness(daylightPos);
        return brightness > 0.5F && this.getCommandSenderWorld().canSeeSkyFromBelowWater(daylightPos);
    }

    protected float getDaylightExplosionBrightness(BlockPos daylightPos) {
        float rawBrightness = (float) this.getCommandSenderWorld().getMaxLocalRawBrightness(daylightPos) / 15.0F;
        float adjustedBrightness = rawBrightness / (4.0F - 3.0F * rawBrightness);
        return Mth.lerp(this.getCommandSenderWorld().dimensionType().ambientLight(), adjustedBrightness, 1.0F);
    }

    protected Level.ExplosionInteraction getDaylightExplosionInteraction() {
        return this.griefing ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE;
    }

    // NOTE: daylightBurns() isn't a real hook on BaseCreatureEntity/PathfinderMob - kept as a
    // plain helper (not an @Override) purely for shouldExplodeInDaylight() above to call.
    public boolean daylightBurns() {
        return !this.isTamed() && !this.isRareVariant();
    }

    @Override
    public ResourceLocation getTexture() {
        if (!this.hasCustomName() || !"Vampire Bat".equals(this.getCustomName().getString()))
            return super.getTexture();

        String textureName = this.getTextureName() + "_vampirebat";
        return AssetHelper.entityTexture(textureName);
    }

    @OnlyIn(Dist.CLIENT)
    public AABB getBoundingBoxForCulling() {
        return this.getBoundingBox().inflate(10, 10, 10).move(0, -5, 0);
    }
}
