package com.lycanitesmobs.core.item.consumable.utility;

import com.lycanitesmobs.core.manager.ObjectManager;
import com.lycanitesmobs.core.item.base.BaseItem;
import net.minecraft.core.Holder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ItemCleansingCrystal extends BaseItem {

    // ==================================================
    //                   Constructor
    // ==================================================
    public ItemCleansingCrystal(Item.Properties properties) {
        super(properties);
        this.itemName = "cleansingcrystal";
        this.setup();
    }


    // ==================================================
    //                    Item Use
    // ==================================================
    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (!player.getAbilities().instabuild) {
            itemStack.setCount(Math.max(0, itemStack.getCount() - 1));
        }

        MobEffect cleansed = ObjectManager.getEffect("cleansed");
        if (!world.isClientSide && cleansed != null) {
            player.addEffect(new MobEffectInstance(Holder.direct(cleansed), 10 * 20));
        }

        return new InteractionResultHolder<>(InteractionResult.SUCCESS, itemStack);
    }
}
