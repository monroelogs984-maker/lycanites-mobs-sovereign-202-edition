package com.lycanitesmobs.core.entity.creature.reptile;

import com.lycanitesmobs.core.entity.IGroupHeavy;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import com.lycanitesmobs.core.entity.item.CustomItemEntity;
import com.lycanitesmobs.core.entity.base.TameableCreatureEntity;
import com.lycanitesmobs.core.entity.base.AgeableCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.PathType;

/**
 * Trimmed - was TameableCreatureEntity implements IGroupHeavy (neither ported, now
 * AgeableCreatureEntity). Dropped the random-lunging aiStep (leap() not ported) and the
 * lava-terraforming die() override (CustomItemEntity/applyDropEffects not ported - block
 * griefing on death is flavor, not core identity). canBreatheUnderwater() renamed to
 * creatureCanBreatheUnderwater() (LivingEntity.canBreatheUnderwater() is final in 1.21.1) - but
 * khalk only overrode canBreatheUnderlava/canBreatheAir, not that one, so unaffected.
 * PHASE 5e UPDATE (2026-09-26): re-parented to its official superclass now that TameableCreatureEntity is
 * ported (taming/ownership/pet behaviour work; RideableCreatureEntity is still a stub). Any wording above
 * about extending Base/AgeableCreatureEntity or taming being unported is outdated.
 */
public class EntityKhalk extends TameableCreatureEntity implements Enemy, IGroupHeavy {
    // Fields restored from official (2026-09-28 method audit):
    protected boolean lavaDeath = true;


    public EntityKhalk(EntityType<? extends EntityKhalk> entityType, Level world) {
        super(entityType, world);
        this.spawnsOnLand = true;
        this.spawnsInWater = true;
        this.isLavaCreature = true;
        this.hasAttackSound = true;
        this.canGrow = true;
        this.babySpawnChance = 0.01D;
        // Restored from official (2026-09-28 constructor audit):
        this.solidCollision = true;
        this.setupMob();
        this.setPathfindingMalus(PathType.LAVA, 0F);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this));
    }

    @Override
    public float getBlockPathWeight(int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        if (this.getCommandSenderWorld().getBlockState(pos).getBlock() == Blocks.LAVA)
            return (super.getBlockPathWeight(x, y, z) + 1) * 11F;

        if (this.getTarget() != null)
            return super.getBlockPathWeight(x, y, z);
        if (this.lavaContact())
            return -999999.0F;

        return super.getBlockPathWeight(x, y, z);
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean canBurn() {
        return false;
    }

    @Override
    public boolean waterDamage() {
        return true;
    }

    @Override
    public boolean canBreatheUnderlava() {
        return true;
    }

    @Override
    public boolean canBreatheAir() {
        return true;
    }

    @Override
    public void applyDropEffects(CustomItemEntity entityItem) {
        entityItem.setCanBurn(false);
    }


    // ==================================================
    //   Restored from official 2026-09-28 (method audit)
    // ==================================================
    // ==================================================
    //                      Updates
    // ==================================================
    // ========== Living Update ==========
    @Override
    public void aiStep() {
        super.aiStep();

        // Random Lunging:
        if (this.onGround() && !this.getCommandSenderWorld().isClientSide) {
            if (this.hasAttackTarget()) {
                if (this.random.nextInt(10) == 0)
                    this.leap(6.0F, 0.1D, this.getTarget());
            }
        }
    }

    // ==================================================
    //                      Death
    // ==================================================
    @Override
    public void die(DamageSource damageSource) {
        boolean wasDead = this.dead;
        super.die(damageSource);
        if (wasDead || !this.dead) {
            return;
        }

        if (!this.getCommandSenderWorld().isClientSide
                && this.getCommandSenderWorld().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)
                && this.lavaDeath
                && !this.isTamed()) {

            int lavaWidth = (int) Math.floor(this.getDimensions(Pose.STANDING).width()) - 1;
            int lavaHeight = (int) Math.floor(this.getDimensions(Pose.STANDING).height()) - 1;

            BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
            for (int x = (int) this.position().x() - lavaWidth; x <= (int) this.position().x() + lavaWidth; x++) {
                for (int y = (int) this.position().y(); y <= (int) this.position().y() + lavaHeight; y++) {
                    for (int z = (int) this.position().z() - lavaWidth; z <= (int) this.position().z() + lavaWidth; z++) {
                        mpos.set(x, y, z);
                        if (!this.getCommandSenderWorld().getBlockState(mpos).isAir()) continue;

                        boolean center = x == (int) this.position().x() && y == (int) this.position().y() && z == (int) this.position().z();
                        BlockState s = Blocks.LAVA.defaultBlockState();
                        if (s.hasProperty(LiquidBlock.LEVEL)) {
                            var p = LiquidBlock.LEVEL;
                            int min = p.getPossibleValues().stream().mapToInt(Integer::intValue).min().orElse(0);
                            int max = p.getPossibleValues().stream().mapToInt(Integer::intValue).max().orElse(15);
                            int v = center ? 0 : 8;
                            s = s.setValue(p, Mth.clamp(v, min, max));
                        }

                        this.getCommandSenderWorld().setBlock(mpos, s, 3);
                    }
                }
            }
        }
    }

    // ==================================================
    //                      Movement
    // ==================================================
    // ========== Movement Speed Modifier ==========
    @Override
    public float getAISpeedModifier() {
        if (this.lavaContact())
            return 2.0F;
        return 1.0F;
    }

    @Override
    public int getBagSize() {
        return this.creatureInfo.getBagSize();
    }

    // ==================================================
    //                   Brightness
    // ==================================================
    public float getBrightness() {
        return 1.0F;
    }

    // ==================================================
    //                     Equipment
    // ==================================================
    @Override
    public int getNoBagSize() {
        return 0;
    }

    @Override
    public void loadCreatureFlags() {
        this.lavaDeath = this.creatureInfo.getFlag("lavaDeath", this.lavaDeath);
    }

    // ==================================================
    //                     Pet Control
    // ==================================================
    public boolean petControlsEnabled() {
        return true;
    }

    // Restored from official 2026-09-29 (method audit) - hooked in BaseCreatureEntity.hurt().
    @Override
    public float getDamageModifier(DamageSource damageSrc) {
        if (damageSrc.is(DamageTypeTags.IS_FIRE))
            return 0F;
        return super.getDamageModifier(damageSrc);
    }
}
