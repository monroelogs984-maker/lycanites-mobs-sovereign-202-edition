package com.lycanitesmobs.core.item.consumable.entity;

import com.lycanitesmobs.core.data.info.element.ElementInfo;
import com.lycanitesmobs.core.data.info.projectile.ProjectileInfo;
import com.lycanitesmobs.core.entity.base.BaseProjectileEntity;
import com.lycanitesmobs.core.entity.base.TameableCreatureEntity;
import com.lycanitesmobs.core.entity.dispenser.BaseProjectileDispenseBehaviour;
import com.lycanitesmobs.core.item.base.BaseItem;
import com.lycanitesmobs.core.manager.DeferredLevelActionManager;
import com.lycanitesmobs.core.util.helpers.LMHelperClass;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * A throwable charge, one per JSON projectile. Sneak + use throws the projectile, dispensers fire it.
 */
public class ChargeItem extends BaseItem {
    /**
     * How much experience a Charge Item grants per element matched.
     **/
    public static int CHARGE_EXPERIENCE = 50;

    /**
     * The projectile info that this projectile charge item belongs to.
     **/
    public ProjectileInfo projectileInfo;

    public ChargeItem(Item.Properties properties, ProjectileInfo projectileInfo) {
        super(properties);
        this.projectileInfo = projectileInfo;
        this.itemName = projectileInfo.getChargeItemName();
        this.setup();
        DispenserBlock.registerBehavior(this, new BaseProjectileDispenseBehaviour(projectileInfo));
        LMHelperClass.logDebug("Projectile", "Created Charge Item: " + this.itemName);
    }

    @Override
    public Component getName(ItemStack itemStack) {
        return this.getProjectileName().copy().append(" ").append(Component.translatable("item.lycanitesmobs.charge"));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.addAll(this.getAdditionalDescriptions());
    }

    @Override
    public Component getDescription(ItemStack stack, @Nullable Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        return Component.translatable("item.lycanitesmobs.charge.description").withStyle(ChatFormatting.GREEN);
    }

    public List<MutableComponent> getAdditionalDescriptions() {
        List<MutableComponent> descriptions = new ArrayList<>();
        descriptions.add(Component.translatable("item.lycanitesmobs.charge.projectile").withStyle(ChatFormatting.GOLD)
                .append(" ").append(this.getProjectileName()));
        if (!this.getElements().isEmpty()) {
            descriptions.add(Component.translatable("item.lycanitesmobs.charge.elements").withStyle(ChatFormatting.DARK_AQUA)
                    .append(" ").append(this.getElementNames()));
        }
        return descriptions;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        if (!world.isClientSide && player.isShiftKeyDown()) {
            BaseProjectileEntity projectile = this.createProjectile(world, player);
            if (projectile == null) {
                LMHelperClass.logWarningMessage("Failed to create projectile from Charge Item: " + this.itemName);
                return InteractionResultHolder.fail(itemStack);
            }
            DeferredLevelActionManager.spawnEntity(world, player.blockPosition(), null, projectile);
            if (!player.getAbilities().instabuild) {
                itemStack.shrink(1);
            }
            if (projectile.getLaunchSound() != null) {
                this.playSound(world, player.blockPosition(), projectile.getLaunchSound(), SoundSource.NEUTRAL, 0.5F, 0.4F / (player.getRandom().nextFloat() * 0.4F + 0.8F));
            }
        }

        return InteractionResultHolder.success(itemStack);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity entity, InteractionHand hand) {
        // Pets are fed charges through their own interaction (mobInteract), don't also throw one.
        if (entity instanceof TameableCreatureEntity tameable && tameable.getPlayerOwner() == player) {
            return InteractionResult.SUCCESS;
        }
        return super.interactLivingEntity(stack, player, entity, hand);
    }

    @Nullable
    public BaseProjectileEntity createProjectile(Level world, Player player) {
        return this.projectileInfo.createProjectile(world, player);
    }

    public List<ElementInfo> getElements() {
        return this.projectileInfo.getElements();
    }

    public MutableComponent getElementNames() {
        MutableComponent elementNames = Component.literal("");
        boolean firstElement = true;
        for (ElementInfo element : this.getElements()) {
            if (!firstElement) {
                elementNames.append(", ");
            }
            firstElement = false;
            elementNames.append(element.getTitle());
        }
        return elementNames;
    }

    public Component getProjectileName() {
        return this.projectileInfo.getTitle();
    }
}
