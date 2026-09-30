package com.lycanitesmobs.client.event;

import com.lycanitesmobs.LycanitesMobs;
import com.lycanitesmobs.client.manager.KeyManager;
import com.lycanitesmobs.client.manager.TextureManager;
import com.lycanitesmobs.client.util.helpers.DrawHelper;
import com.lycanitesmobs.core.capabilities.entity.ExtendedPlayer;
import com.lycanitesmobs.core.data.config.ConfigDebug;
import com.lycanitesmobs.core.data.info.creature.CreatureInfo;
import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import com.lycanitesmobs.core.entity.base.RideableCreatureEntity;
import com.lycanitesmobs.core.entity.base.TameableCreatureEntity;
import com.lycanitesmobs.core.entity.util.CreatureRelationshipEntry;
import com.lycanitesmobs.core.item.summoningstaff.ItemStaffSummoning;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * HUD overlays: summoning focus (while holding a summoning staff), mount stamina + controls hint (while riding), the
 * taming reputation bar (when looking at a creature you have reputation with) and the creature debug overlay (config).
 *
 * <p>1.21 port: drawn as a NeoForge GUI layer above the experience bar (was RenderGuiOverlayEvent). Vanilla's
 * icons.png is gone, so the stamina bar uses the jump bar sprites it came from (same size and position).
 * Also draws the mob event title graphics (ClientMobEventEvents).
 */
public class OverlayEvents {
    private static final ResourceLocation LAYER_ID = ResourceLocation.fromNamespaceAndPath(LycanitesMobs.MODID, "overlay");
    private static final ResourceLocation JUMP_BAR_BACKGROUND_SPRITE = ResourceLocation.withDefaultNamespace("hud/jump_bar_background");
    private static final ResourceLocation JUMP_BAR_COOLDOWN_SPRITE = ResourceLocation.withDefaultNamespace("hud/jump_bar_cooldown");
    private static final ResourceLocation JUMP_BAR_PROGRESS_SPRITE = ResourceLocation.withDefaultNamespace("hud/jump_bar_progress");

    private static DrawHelper drawHelper;
    private static final int MOUNT_MESSAGE_TIME_MAX = 10 * 20;
    private static int mountMessageTime = MOUNT_MESSAGE_TIME_MAX;

    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.EXPERIENCE_BAR, LAYER_ID, OverlayEvents::renderOverlay);
    }

    private static void renderOverlay(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.options.hideGui) {
            return;
        }
        if (drawHelper == null) {
            drawHelper = new DrawHelper(minecraft, minecraft.font);
        }

        guiGraphics.pose().pushPose();
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        int sWidth = minecraft.getWindow().getGuiScaledWidth();
        int sHeight = minecraft.getWindow().getGuiScaledHeight();

        // ========== Mob/World Events Title ==========
        com.lycanitesmobs.client.event.mobevent.ClientMobEventEvents.render(player.level(), guiGraphics, drawHelper, sWidth, sHeight);

        // ========== Summoning Focus Bar ==========
        ExtendedPlayer playerExt = ExtendedPlayer.getForPlayer(player);
        if (playerExt != null && !player.getAbilities().instabuild && (
                player.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof ItemStaffSummoning
                        || player.getItemInHand(InteractionHand.OFF_HAND).getItem() instanceof ItemStaffSummoning)) {
            int barYSpace = 10;
            int barXSpace = -1;
            int summonBarWidth = 9;
            int summonBarHeight = 9;
            int summonBarX = (sWidth / 2) + 10;
            int summonBarY = sHeight - 30 - summonBarHeight;
            summonBarY -= barYSpace;
            if (player.isEyeInFluid(FluidTags.WATER))
                summonBarY -= barYSpace;

            ResourceLocation emptyTex = TextureManager.getTexture("GUIPetSpiritEmpty");
            ResourceLocation fullTex = TextureManager.getTexture("GUIPetSpiritUsed");
            ResourceLocation fillTex = TextureManager.getTexture("GUIPetSpiritFilling");
            for (int n = 0; n < 10; n++) {
                int x = summonBarX + ((summonBarWidth + barXSpace) * (9 - n));
                drawHelper.drawTexture(guiGraphics, emptyTex, x, summonBarY, 0, 1, 1, summonBarWidth, summonBarHeight);
                int threshold = playerExt.getSummonFocusMax() - (n * playerExt.getSummonFocusCharge());
                if (playerExt.getSummonFocus() >= threshold) {
                    drawHelper.drawTexture(guiGraphics, fullTex, x, summonBarY, 0, 1, 1, summonBarWidth, summonBarHeight);
                } else if (playerExt.getSummonFocus() + playerExt.getSummonFocusCharge() > threshold) {
                    float scale = (float) (playerExt.getSummonFocus() % playerExt.getSummonFocusCharge()) / (float) playerExt.getSummonFocusCharge();
                    int w = Math.round(summonBarWidth * scale);
                    if (w > 0) {
                        drawHelper.drawTexture(guiGraphics, fillTex, x, summonBarY, 0, scale, 1, w, summonBarHeight);
                    }
                }
            }
        }

        // ========== Mount Stamina Bar ==========
        if (player.getVehicle() instanceof RideableCreatureEntity mount) {
            float mountStamina = mount.getStaminaPercent();

            // Mount Controls Message:
            if (mountMessageTime > 0) {
                MutableComponent mountMessage = Component.translatable("gui.mount.controls.prefix")
                        .append(" ").append(KeyManager.mountAbility.getTranslatedKeyMessage())
                        .append(" ").append(Component.translatable("gui.mount.controls.ability"));
                minecraft.gui.setOverlayMessage(mountMessage, false);
                mountMessageTime--;
            }

            int staminaBarWidth = 182;
            int staminaBarHeight = 5;
            int staminaEnergyWidth = (int) ((float) (staminaBarWidth + 1) * mountStamina);
            int staminaBarX = (sWidth / 2) - (staminaBarWidth / 2);
            int staminaBarY = sHeight - 32 + 3;
            guiGraphics.blitSprite(JUMP_BAR_BACKGROUND_SPRITE, staminaBarX, staminaBarY, staminaBarWidth, staminaBarHeight);
            if (staminaEnergyWidth > 0) {
                ResourceLocation fillSprite = "toggle".equals(mount.getStaminaType()) ? JUMP_BAR_COOLDOWN_SPRITE : JUMP_BAR_PROGRESS_SPRITE;
                guiGraphics.blitSprite(fillSprite, staminaBarWidth, staminaBarHeight, 0, 0, staminaBarX, staminaBarY, Math.min(staminaEnergyWidth, staminaBarWidth), staminaBarHeight);
            }
        } else {
            mountMessageTime = MOUNT_MESSAGE_TIME_MAX;
        }

        // ========== Taming Reputation Bar ==========
        HitResult mouseOver = minecraft.hitResult;
        if (mouseOver instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() instanceof BaseCreatureEntity creatureEntity) {
            CreatureInfo creatureInfo = creatureEntity.getCreatureInfo();
            CreatureRelationshipEntry relationshipEntry = creatureEntity.getRelationshipEntry(player);
            if (relationshipEntry != null && relationshipEntry.getReputation() > 0 && !creatureEntity.isTamed()) {
                float barWidth = 100;
                float barHeight = 11;
                float barX = ((float) sWidth / 2) - (barWidth / 2);
                float barY = (float) sHeight * 0.75F;
                float barCenter = barX + (barWidth / 2);

                drawHelper.drawTexture(guiGraphics, TextureManager.getTexture("GUIPetBarEmpty"), barX, barY, 0, 1, 1, barWidth, barHeight);
                float reputationNormal = Math.min(1, (float) relationshipEntry.getReputation() / creatureInfo.getTamingReputation());
                String barFillTexture = "GUIPetBarRespawn";
                if (relationshipEntry.getReputation() >= creatureInfo.getFriendlyReputation()) {
                    barFillTexture = "GUIPetBarHealth";
                }
                drawHelper.drawTexture(guiGraphics, TextureManager.getTexture(barFillTexture), barX, barY, 0, reputationNormal, 1, barWidth * reputationNormal, barHeight);
                String reputationText = Component.translatable("entity.reputation").getString() + ": " + relationshipEntry.getReputation() + "/" + creatureInfo.getTamingReputation();
                drawHelper.draw(guiGraphics, reputationText, barCenter - ((float) drawHelper.getStringWidth(reputationText) / 2), barY + 2, 0xFFFFFF);
            }
        }

        RenderSystem.disableBlend();
        guiGraphics.pose().popPose();
    }

    /**
     * Creature debug info in the F3 overlay, when the debug config's creatureOverlay is on.
     */
    public static void onDebugText(CustomizeGuiOverlayEvent.DebugText event) {
        if (ConfigDebug.INSTANCE == null || !ConfigDebug.INSTANCE.creatureOverlay.get()) {
            return;
        }
        HitResult mouseOver = Minecraft.getInstance().hitResult;
        if (!(mouseOver instanceof EntityHitResult entityHitResult) || !(entityHitResult.getEntity() instanceof BaseCreatureEntity creature)) {
            return;
        }
        Player player = Minecraft.getInstance().player;
        event.getLeft().add("");
        event.getLeft().add("Target Creature: " + creature.getName().getString());
        event.getLeft().add("Distance To player: " + creature.distanceTo(player));
        event.getLeft().add("Subspecies: " + creature.getSubspeciesIndex());
        event.getLeft().add("Variant: " + creature.getVariantIndex());
        event.getLeft().add("Size: " + creature.getSizeScale());
        event.getLeft().add("");
        event.getLeft().add("Health: " + creature.getHealth() + "/" + creature.getMaxHealth());
        event.getLeft().add("Speed: " + creature.getAttribute(Attributes.MOVEMENT_SPEED).getValue());
        event.getLeft().add("Armor: " + creature.getArmorValue());
        event.getLeft().add("");
        event.getLeft().add("Has Attack Target: " + creature.hasAttackTarget());
        event.getLeft().add("Has Avoid Target: " + creature.hasAvoidTarget());
        event.getLeft().add("Has Master Target: " + creature.hasMaster());
        event.getLeft().add("Has Parent Target: " + creature.hasParent());
        event.getLeft().add("");
        CreatureRelationshipEntry relationshipEntry = creature.getRelationshipEntry(player);
        event.getLeft().add("Reputation with Player: " + (relationshipEntry != null ? relationshipEntry.getReputation() : 0) + "/" + creature.getCreatureInfo().getTamingReputation());
        if (creature instanceof TameableCreatureEntity tameable) {
            event.getLeft().add("Owner ID: " + (tameable.getOwnerId() != null ? tameable.getOwnerId().toString() : "None"));
        }
    }
}
