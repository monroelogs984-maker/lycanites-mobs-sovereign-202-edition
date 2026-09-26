package com.lycanitesmobs.core.entity.creature.demon;

import net.minecraft.world.entity.Entity;
import org.joml.Vector3d;
import com.lycanitesmobs.core.entity.goals.actions.AttackRangedGoal;
import com.lycanitesmobs.core.entity.base.TameableCreatureEntity;
import com.lycanitesmobs.core.data.tag.LycanitesBlockTags;
import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import com.lycanitesmobs.core.manager.ObjectManager;
import com.lycanitesmobs.core.util.helpers.AssetHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Trimmed - original extends TameableCreatureEntity (tame/pet-control/bag, not ported). Dropped
 * the entity-pickup-and-throw melee follow-up (canPickupEntity/pickupEntity/getPickupEntity/
 * dropPickupEntity aren't ported beyond the bare field - see EntityStryder's note in
 * PORT_PLAN.md), ranged hellfireballs (AttackRangedGoal/fireProjectile need ProjectileManager,
 * not ported), and the client-side hellfire-orb visual sync (EntityRahovart.updateHellfireOrbs
 * is a rendering helper, not ported). Kept the self-contained hellfire ground-trail effect and
 * the Krampus custom-name texture swap, both pure asset/block calls with no missing deps.
 * PHASE 5e UPDATE (2026-09-26): re-parented to its official superclass now that TameableCreatureEntity is
 * ported (taming/ownership/pet behaviour work; RideableCreatureEntity is still a stub). Any wording above
 * about extending Base/AgeableCreatureEntity or taming being unported is outdated.
 * PHASE 6a UPDATE (2026-09-26): ranged attack restored (projectiles ported) - any wording above about a
 * substituted melee attack or ProjectileManager being unported is outdated.
 */
public class EntityBehemophet extends TameableCreatureEntity implements Enemy {

    public EntityBehemophet(EntityType<? extends EntityBehemophet> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = false;
        this.setupMob();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setLongMemory(false).setRange(1D).setMaxChaseDistance(8.0F));
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackRangedGoal(this).setSpeed(1.0D).setRange(16.0F).setMinChaseDistance(0F).setChaseTime(-1));
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.getCommandSenderWorld().isClientSide && this.isMoving() && this.tickCount % 5 == 0) {
            int trailHeight = 1;
            int trailWidth = 1;
            if (this.isRareVariant())
                trailWidth = 3;
            for (int y = 0; y < trailHeight; y++) {
                BlockState blockState = this.getCommandSenderWorld().getBlockState(this.blockPosition().offset(0, y, 0));
                if (blockState.is(LycanitesBlockTags.BEHEMOPHET_HELLFIRE_TRAIL_REPLACEABLE)) {
                    if (trailWidth == 1)
                        this.getCommandSenderWorld().setBlockAndUpdate(this.blockPosition().offset(0, y, 0), ObjectManager.getBlock("hellfire").defaultBlockState());
                    else
                        for (int x = -(trailWidth / 2); x < (trailWidth / 2) + 1; x++) {
                            for (int z = -(trailWidth / 2); z < (trailWidth / 2) + 1; z++) {
                                this.getCommandSenderWorld().setBlockAndUpdate(this.blockPosition().offset(x, y, z), ObjectManager.getBlock("hellfire").defaultBlockState());
                            }
                        }
                }
            }
        }
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (target instanceof EntityBelphegor)
            return false;
        return super.canAttack(target);
    }

    @Override
    public boolean canBurn() {
        return false;
    }

    @Override
    public ResourceLocation getTexture() {
        if (!this.hasCustomName() || !"Krampus".equals(this.getCustomName().getString()))
            return super.getTexture();

        String textureName = this.getTextureName() + "_krampus";
        return AssetHelper.entityTexture(textureName);
    }

    @Override
    public void attackRanged(Entity target, float range) {
        this.fireProjectile("hellfireball", target, range, 0, new Vector3d(0, 0, 0), 1.2f, 2f, 1F);
        super.attackRanged(target, range);
    }
}
