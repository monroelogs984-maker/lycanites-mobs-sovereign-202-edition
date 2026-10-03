package com.lycanitesmobs.core.item.equipment.imprint;

import com.lycanitesmobs.LycanitesMobs;
import com.lycanitesmobs.core.capabilities.entity.ExtendedEntity;
import com.lycanitesmobs.core.data.info.item.ItemConfig;
import com.lycanitesmobs.core.item.consumable.entity.ChargeItem;
import com.lycanitesmobs.core.item.equipment.ItemEquipment;
import com.lycanitesmobs.core.item.equipment.ItemEquipmentPart;
import com.lycanitesmobs.core.item.equipment.features.DamageEquipmentFeature;
import com.lycanitesmobs.core.item.equipment.features.EffectEquipmentFeature;
import com.lycanitesmobs.core.item.equipment.features.EquipmentFeature;
import com.lycanitesmobs.core.item.equipment.features.ProjectileEquipmentFeature;
import com.lycanitesmobs.core.item.equipment.features.SummonEquipmentFeature;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

/**
 * S202 equipment rework: an equipment part imprinted onto any weapon or tool (anything with attack damage and attack
 * speed). The part lives inside the host item as a data component and keeps its own level, experience and mana. A
 * passive part applies its on-hit features; an ability part fires its projectiles on right-click. Every trigger costs
 * 1 mana and the imprint goes inactive at 0 mana. The host item itself is never changed visually.
 */
public class Imprints {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, LycanitesMobs.MODID);

    /**
     * The imprinted part (the part ItemStack, with its level/experience/mana), held in vanilla's immutable
     * ItemContainerContents since a component can't be a mutable ItemStack. The fallback reads imprints saved by the
     * first version, which stored the bare ItemStack.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>> IMPRINT = COMPONENTS.register("imprint",
            () -> DataComponentType.<ItemContainerContents>builder()
                    .persistent(Codec.withAlternative(ItemContainerContents.CODEC, ItemStack.CODEC.xmap(stack -> ItemContainerContents.fromItems(List.of(stack)), contents -> contents.copyOne())))
                    .networkSynchronized(ItemContainerContents.STREAM_CODEC).build());

    /** Items that can always take an imprint, or never can. **/
    public static final TagKey<Item> IMPRINTABLE = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(LycanitesMobs.MODID, "imprintable"));
    public static final TagKey<Item> NOT_IMPRINTABLE = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(LycanitesMobs.MODID, "not_imprintable"));

    /** A part's damage feature was written as a whole weapon's damage, so only this share of it is added as a bonus. **/
    public static final double DAMAGE_BONUS_SCALE = 0.25D;

    public static void register(IEventBus modEventBus) {
        COMPONENTS.register(modEventBus);
        NeoForge.EVENT_BUS.addListener(Imprints::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(Imprints::onDamagePost);
        NeoForge.EVENT_BUS.addListener(Imprints::onRightClickItem);
    }


    // ==================================================
    //                    Stack Helpers
    // ==================================================
    /** True if the provided item can hold an imprint: it has attack damage and attack speed, or is tagged imprintable. **/
    public static boolean isEligible(ItemStack host) {
        if (host.isEmpty() || host.getItem() instanceof ItemEquipmentPart || host.getItem() instanceof ItemEquipment) {
            return false;
        }
        if (host.is(NOT_IMPRINTABLE)) {
            return false;
        }
        if (host.is(IMPRINTABLE)) {
            return true;
        }
        ItemAttributeModifiers modifiers = host.getAttributeModifiers();
        boolean damage = false;
        boolean speed = false;
        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            if (entry.attribute().is(Attributes.ATTACK_DAMAGE)) damage = true;
            if (entry.attribute().is(Attributes.ATTACK_SPEED)) speed = true;
        }
        return damage && speed;
    }

    public static boolean hasImprint(ItemStack host) {
        return !host.isEmpty() && host.has(IMPRINT.get());
    }

    /** Returns a copy of the imprinted part, or an empty stack. **/
    public static ItemStack getPart(ItemStack host) {
        if (!hasImprint(host)) {
            return ItemStack.EMPTY;
        }
        return host.get(IMPRINT.get()).copyOne();
    }

    public static void setPart(ItemStack host, ItemStack part) {
        if (part.isEmpty()) {
            host.remove(IMPRINT.get());
            return;
        }
        ItemStack stored = part.copy();
        stored.setCount(1);
        host.set(IMPRINT.get(), ItemContainerContents.fromItems(List.of(stored)));
    }

    /** Removes the imprint and returns the part, or an empty stack. **/
    public static ItemStack extract(ItemStack host) {
        ItemStack part = getPart(host);
        host.remove(IMPRINT.get());
        return part;
    }

    /** Imprints the part onto the host. Fails if the host isn't eligible, already has an imprint, or the item isn't a part. **/
    public static boolean imprint(ItemStack host, ItemStack part) {
        if (!isEligible(host) || hasImprint(host) || !(part.getItem() instanceof ItemEquipmentPart)) {
            return false;
        }
        setPart(host, part);
        return true;
    }

    public static int getMana(ItemStack host) {
        ItemStack part = getPart(host);
        if (part.getItem() instanceof ItemEquipmentPart partItem) {
            return partItem.getMana(part);
        }
        return 0;
    }

    /** Changes the imprint's mana by the amount (clamped 0 to max). **/
    public static void changeMana(ItemStack host, int amount) {
        ItemStack part = getPart(host);
        if (part.getItem() instanceof ItemEquipmentPart partItem) {
            partItem.setMana(part, partItem.getMana(part) + amount);
            setPart(host, part);
        }
    }

    /** True if the part has anything an imprint can use (structural parts like the wooden rod only had slots/harvest). **/
    public static boolean isImprintablePart(ItemStack part) {
        if (!(part.getItem() instanceof ItemEquipmentPart partItem)) {
            return false;
        }
        for (String featureType : SHOWN_FEATURES) {
            if (!partItem.getActiveFeaturesByType(part, featureType).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static final List<String> SHOWN_FEATURES = List.of("effect", "damage", "summon", "projectile");

    /**
     * What the part does as an imprint at its current level, one line per feature. A passive part skips its right-click
     * projectiles (dropped by the rework) and an ability part skips hit-procs, matching what the event handlers run.
     */
    public static List<MutableComponent> getFeatureSummaries(ItemStack part) {
        List<MutableComponent> summaries = new ArrayList<>();
        if (!(part.getItem() instanceof ItemEquipmentPart partItem)) {
            return summaries;
        }
        int level = partItem.getPartLevel(part);
        boolean ability = partItem.isImprintAbility();
        for (String featureType : SHOWN_FEATURES) {
            for (EquipmentFeature feature : partItem.getActiveFeaturesByType(part, featureType)) {
                if (feature instanceof ProjectileEquipmentFeature projectile && ability == "hit".equalsIgnoreCase(projectile.getProjectileTrigger())) {
                    continue;
                }
                MutableComponent summary = feature.getSummary(part, level);
                if (summary != null) {
                    summaries.add(summary);
                }
            }
        }
        return summaries;
    }

    /** The part held by a workstation slot: the stack itself if it's a part, or the imprint of an imprinted item. **/
    public static ItemStack getPartOf(ItemStack stack) {
        if (stack.getItem() instanceof ItemEquipmentPart) {
            return stack;
        }
        return getPart(stack);
    }

    /** Writes a changed part back into the slot stack it came from (getPartOf), returning the stack to put back. **/
    public static ItemStack setPartOf(ItemStack stack, ItemStack part) {
        if (stack.getItem() instanceof ItemEquipmentPart) {
            return part;
        }
        setPart(stack, part);
        return stack;
    }

    /** How much mana an item restores at the Equipment Station (official tiers, from the item config), 0 if none. **/
    public static int getManaRecharge(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        if (stack.getItem() instanceof ChargeItem) {
            return ItemConfig.getHighEquipmentRepairAmount();
        }
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if (contains(ItemConfig.getMaxEquipmentManaItems(), id)) return ItemEquipment.MANA_MAX;
        if (contains(ItemConfig.getHighEquipmentManaItems(), id)) return ItemConfig.getHighEquipmentRepairAmount();
        if (contains(ItemConfig.getMediumEquipmentManaItems(), id)) return ItemConfig.getMediumEquipmentRepairAmount();
        if (contains(ItemConfig.getLowEquipmentManaItems(), id)) return ItemConfig.getLowEquipmentRepairAmount();
        return 0;
    }

    private static boolean contains(List<? extends String> ids, String id) {
        return ids != null && ids.contains(id);
    }

    /** The imprinted part item if the host has one with mana left, else null. **/
    private static ItemEquipmentPart getActivePart(ItemStack host) {
        ItemStack part = getPart(host);
        if (part.getItem() instanceof ItemEquipmentPart partItem && partItem.getMana(part) > 0) {
            return partItem;
        }
        return null;
    }


    // ==================================================
    //                      Passives
    // ==================================================
    private static ItemStack getMeleeWeapon(DamageSource source) {
        if (!(source.getDirectEntity() instanceof Player player) || source.getEntity() != player) {
            return ItemStack.EMPTY;
        }
        // The real main-hand stack (not a copy), so mana changes are saved on the item the player holds.
        return player.getMainHandItem();
    }

    /** Bonus damage from the imprint's damage features. **/
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        ItemStack host = getMeleeWeapon(event.getSource());
        ItemEquipmentPart partItem = getActivePart(host);
        if (partItem == null) {
            return;
        }
        ItemStack part = getPart(host);
        double bonus = 0;
        for (EquipmentFeature feature : partItem.getActiveFeaturesByType(part, "damage")) {
            bonus += Math.max(1, Math.round(((DamageEquipmentFeature) feature).getDamageAmount() * DAMAGE_BONUS_SCALE));
        }
        if (bonus > 0) {
            event.setAmount(event.getAmount() + (float) bonus);
        }
    }

    /** On-hit effects, summons and hit-procs, then the mana cost. **/
    public static void onDamagePost(LivingDamageEvent.Post event) {
        ItemStack host = getMeleeWeapon(event.getSource());
        ItemEquipmentPart partItem = getActivePart(host);
        if (partItem == null || !(event.getSource().getEntity() instanceof Player player) || player.level().isClientSide) {
            return;
        }
        LivingEntity target = event.getEntity();
        ItemStack part = getPart(host);
        boolean used = false;
        for (EquipmentFeature feature : partItem.getActiveFeaturesByType(part, "effect")) {
            ((EffectEquipmentFeature) feature).onHitEntity(part, target, player);
            used = true;
        }
        for (EquipmentFeature feature : partItem.getActiveFeaturesByType(part, "summon")) {
            used = ((SummonEquipmentFeature) feature).onHitEntity(part, target, player) || used;
        }
        if (!partItem.isImprintAbility()) {
            for (EquipmentFeature feature : partItem.getActiveFeaturesByType(part, "projectile")) {
                used = ((ProjectileEquipmentFeature) feature).onHitEntity(part, target, player) || used;
            }
        }
        if (!partItem.getActiveFeaturesByType(part, "damage").isEmpty()) {
            used = true;
        }
        if (used) {
            changeMana(host, -1);
        }
    }


    // ==================================================
    //                      Abilities
    // ==================================================
    /**
     * Right-click fires an ability part's projectiles (each on its own cooldown). Items with their own right-click use
     * (bows, tridents, shields, food...) are left alone, and block interactions (stripping, tilling) happen first
     * because this event only fires when the item is used on nothing.
     */
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        ItemStack host = event.getItemStack();
        ItemEquipmentPart partItem = getActivePart(host);
        if (partItem == null || !partItem.isImprintAbility() || host.getUseAnimation() != UseAnim.NONE) {
            return;
        }
        Player player = event.getEntity();
        if (!player.level().isClientSide) {
            ItemStack part = getPart(host);
            boolean fired = false;
            ExtendedEntity playerExt = ExtendedEntity.getForEntity(player);
            List<EquipmentFeature> projectiles = partItem.getActiveFeaturesByType(part, "projectile");
            for (EquipmentFeature feature : projectiles) {
                ProjectileEquipmentFeature projectileFeature = (ProjectileEquipmentFeature) feature;
                if ("hit".equalsIgnoreCase(projectileFeature.getProjectileTrigger()) || playerExt == null) {
                    continue;
                }
                if (playerExt.getProjectileCooldown(2, projectileFeature.getProjectileName()) > 0) {
                    continue;
                }
                playerExt.setProjectileCooldown(2, projectileFeature.getProjectileName(), projectileFeature.getCooldown());
                projectileFeature.fireProjectile(player);
                fired = true;
            }
            if (fired) {
                changeMana(host, -1);
            }
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}
