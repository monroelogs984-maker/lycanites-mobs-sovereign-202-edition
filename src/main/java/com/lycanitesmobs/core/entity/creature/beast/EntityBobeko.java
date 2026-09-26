package com.lycanitesmobs.core.entity.creature.beast;

import com.lycanitesmobs.core.data.tag.LycanitesBlockTags;
import com.lycanitesmobs.core.entity.base.AgeableCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import com.lycanitesmobs.core.manager.ObjectManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Trimmed - dropped TemptGoal (not ported), fleeHealthPercent (field not on BaseCreatureEntity -
 * not added here since this batch must not touch shared base-entity files while other creatures
 * are being ported in parallel), getNoBagSize/getBagSize (bag subsystem), and the whole
 * interact-command milk-bucket ability (getInteractCommands/performCommand - see
 * AgeableCreatureEntity's class doc, that system isn't ported at all). canBeLeashed() fixed to
 * the no-arg 1.21.1 Leashable signature (was canBeLeashed(Player) in 1.20.1).
 */
public class EntityBobeko extends AgeableCreatureEntity {

    public EntityBobeko(EntityType<? extends EntityBobeko> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = false;
        this.isAggressiveByDefault = false;
        this.setupMob();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setLongMemory(false));
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.getCommandSenderWorld().isClientSide && (this.tickCount % 10 == 0 || this.isMoving() && this.tickCount % 5 == 0)) {
            int trailHeight = 2;
            if (this.isBaby())
                trailHeight = 1;
            for (int y = 0; y < trailHeight; y++) {
                BlockState blockState = this.getCommandSenderWorld().getBlockState(this.blockPosition().offset(0, y, 0));
                if (blockState.is(LycanitesBlockTags.BOBEKO_FROST_CLOUD_REPLACEABLE))
                    this.getCommandSenderWorld().setBlockAndUpdate(this.blockPosition().offset(0, y, 0), ObjectManager.getBlock("frostcloud").defaultBlockState());
            }
        }

        if (this.getCommandSenderWorld().isClientSide)
            for (int i = 0; i < 2; ++i) {
                this.getCommandSenderWorld().addParticle(ParticleTypes.ITEM_SNOWBALL, this.position().x() + (this.random.nextDouble() - 0.5D) * (double) this.getDimensions(Pose.STANDING).width(), this.position().y() + this.random.nextDouble() * (double) this.getDimensions(Pose.STANDING).height(), this.position().z() + (this.random.nextDouble() - 0.5D) * (double) this.getDimensions(Pose.STANDING).width(), 0.0D, 0.0D, 0.0D);
            }
    }

    @Override
    public float getBlockPathWeight(int x, int y, int z) {
        BlockState blockState = this.getCommandSenderWorld().getBlockState(new BlockPos(x, y - 1, z));
        if (!blockState.isAir()) {
            if (blockState.is(LycanitesBlockTags.CREATURE_PATH_GRASS_PREFERRED) || blockState.is(LycanitesBlockTags.CREATURE_PATH_SNOW_PREFERRED))
                return 10F;
            if (blockState.is(LycanitesBlockTags.CREATURE_PATH_DIRT_PREFERRED) || blockState.is(LycanitesBlockTags.CREATURE_PATH_ICE_PREFERRED))
                return 7F;
        }
        return super.getBlockPathWeight(x, y, z);
    }

    @Override
    public boolean canBeLeashed() {
        return true;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (source.type().equals(ObjectManager.getDamageSource(this.level(), "ooze").type())) return true;
        return super.isInvulnerableTo(source);
    }

    @Override
    public boolean canBeAffected(MobEffectInstance potionEffect) {
        if (potionEffect.is(MobEffects.MOVEMENT_SLOWDOWN)) return false;
        if (potionEffect.is(MobEffects.HUNGER)) return false;
        return super.canBeAffected(potionEffect);
    }
}
