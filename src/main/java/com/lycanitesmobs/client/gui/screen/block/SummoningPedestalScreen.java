package com.lycanitesmobs.client.gui.screen.block;

import com.lycanitesmobs.client.gui.buttons.ButtonBase;
import com.lycanitesmobs.client.gui.screen.base.BaseContainerScreen;
import com.lycanitesmobs.client.gui.widgets.SummoningPedestalList;
import com.lycanitesmobs.client.manager.TextureManager;
import com.lycanitesmobs.core.block.blockentity.TileEntitySummoningPedestal;
import com.lycanitesmobs.core.capabilities.entity.ExtendedPlayer;
import com.lycanitesmobs.core.container.block.SummoningPedestalContainer;
import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import com.lycanitesmobs.core.entity.pets.SummonSet;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

/** The Summoning Pedestal screen: pick which summonable creature it keeps summoned, set their behaviour, feed it redstone. **/
public class SummoningPedestalScreen extends BaseContainerScreen<SummoningPedestalContainer> {
    public Player player;
    public ExtendedPlayer playerExt;
    protected TileEntitySummoningPedestal summoningPedestal;
    protected SummonSet summonSet;

    public SummoningPedestalList list;

    public int centerX;
    public int centerY;
    public int windowWidth;
    public int windowHeight;
    public int halfX;
    public int halfY;
    public int windowX;
    public int windowY;

    public SummoningPedestalScreen(SummoningPedestalContainer container, Inventory playerInventory, Component name) {
        super(container, playerInventory, name);
        this.summoningPedestal = container.getSummoningPedestal();
        this.player = playerInventory.player;
        this.playerExt = ExtendedPlayer.getForPlayer(this.player);
        this.summonSet = this.summoningPedestal.getSummonSet();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void init() {
        this.windowWidth = 256;
        this.windowHeight = 166;
        this.halfX = this.windowWidth / 2;
        this.halfY = this.windowHeight / 2;
        this.windowX = (this.width / 2) - (this.windowWidth / 2);
        this.windowY = (this.height / 2) - (this.windowHeight / 2);
        this.centerX = this.windowX + (this.windowWidth / 2);
        this.centerY = this.windowY + (this.windowHeight / 2);
        // The container's slots are laid out against this window (fuel slot at 93,43; hotbar at 8,171).
        this.imageWidth = this.windowWidth;
        this.imageHeight = this.windowHeight;
        super.init();
        this.leftPos = this.windowX;
        this.topPos = this.windowY;
    }

    @Override
    protected void initWidgets() {
        int buttonSpacing = 2;
        int buttonWidth = (this.windowWidth / 4) - (buttonSpacing * 2);
        int buttonHeight = 20;
        int buttonX = this.centerX + buttonSpacing;
        int buttonXRight = buttonX + buttonWidth + buttonSpacing;
        int buttonY = this.windowY + 39 + buttonSpacing;

        buttonY += buttonHeight + (buttonSpacing * 2);
        this.addRenderableWidget(new ButtonBase(BaseCreatureEntity.GUI_COMMAND.SITTING.id, buttonX, buttonY, buttonWidth * 2, buttonHeight, Component.literal("..."), this));

        buttonY += buttonHeight + (buttonSpacing * 2);
        this.addRenderableWidget(new ButtonBase(BaseCreatureEntity.GUI_COMMAND.PASSIVE.id, buttonX, buttonY, buttonWidth, buttonHeight, Component.literal("..."), this));
        this.addRenderableWidget(new ButtonBase(BaseCreatureEntity.GUI_COMMAND.STANCE.id, buttonXRight, buttonY, buttonWidth, buttonHeight, Component.literal("..."), this));

        buttonY += buttonHeight + (buttonSpacing * 2);
        this.addRenderableWidget(new ButtonBase(BaseCreatureEntity.GUI_COMMAND.PVP.id, buttonX, buttonY, buttonWidth * 2, buttonHeight, Component.literal("..."), this));

        if (this.hasPets() && this.summoningPedestal.hasSummonSet()) {
            this.selectMinion(this.summoningPedestal.getSummonSet().getSummonType());
        }
        int listWidth = (this.windowWidth / 2) - (buttonSpacing * 4);
        int listHeight = this.windowHeight - (39 + buttonSpacing) - 16;
        int listTop = this.windowY + 39 + buttonSpacing;
        int listBottom = listTop + listHeight;
        int listX = this.windowX + (buttonSpacing * 2);
        this.list = new SummoningPedestalList(this, this.playerExt, listWidth, listHeight, listTop, listBottom, listX);
        // Renderable, so it's drawn by super.render() after the background (which 1.21's Screen.render draws again).
        if (this.hasPets()) {
            this.addRenderableWidget(this.list);
        }
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        ResourceLocation texture = this.getTexture();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(texture, this.windowX, this.windowY, 0, 0, this.windowWidth, this.windowHeight);
        guiGraphics.blit(texture, this.windowX + 40, this.windowY + this.windowHeight, 40, 224, this.windowWidth - 80, 29);

        if (!this.hasPets()) {
            return;
        }

        this.drawFuel(guiGraphics);
        this.drawCapacityBar(guiGraphics);
        this.drawProgressBar(guiGraphics);
    }

    @Override
    protected void renderWidgets(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        for (Object buttonObj : this.renderables) {
            if (buttonObj instanceof ButtonBase button) {
                // Inactive:
                if (!this.hasSelectedPet()) {
                    button.active = false;
                    button.visible = false;
                    continue;
                }
                button.active = true;
                button.visible = true;

                // Behaviour Buttons:
                if (button.buttonId == BaseCreatureEntity.GUI_COMMAND.SITTING.id)
                    button.setMessage(Component.literal(Component.translatable("gui.pet.sit").getString() + ": " + yesNo(this.summonSet.getSitting())));
                if (button.buttonId == BaseCreatureEntity.GUI_COMMAND.PASSIVE.id)
                    button.setMessage(Component.literal(Component.translatable("gui.pet.passive").getString() + ": " + yesNo(this.summonSet.getPassive())));
                if (button.buttonId == BaseCreatureEntity.GUI_COMMAND.STANCE.id)
                    button.setMessage(Component.translatable(this.summonSet.getAggressive() ? "gui.pet.aggressive" : "gui.pet.defensive"));
                if (button.buttonId == BaseCreatureEntity.GUI_COMMAND.PVP.id)
                    button.setMessage(Component.literal(Component.translatable("gui.pet.pvp").getString() + ": " + yesNo(this.summonSet.getPVP())));
            }
        }

        super.renderWidgets(guiGraphics, mouseX, mouseY, partialTicks);
    }

    private static String yesNo(boolean value) {
        return Component.translatable(value ? "common.yes" : "common.no").getString();
    }

    @Override
    protected void renderForeground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        if (!this.hasPets()) {
            this.drawHelper.drawString(guiGraphics, Component.translatable("gui.beastiary.summoning.empty.title").getString(), this.centerX - 96, this.windowY + 6, 0xFFFFFF);
            this.drawHelper.drawStringWrapped(guiGraphics, Component.translatable("gui.beastiary.summoning.empty.info").getString(), this.windowX + 16, this.windowY + 30, this.windowWidth - 32, 0xFFFFFF, false);
            return;
        }

        this.drawHelper.drawStringCentered(guiGraphics, this.getPedestalTitle().getString(), this.centerX, this.windowY + 6, 0xFFFFFF, false);
        this.drawHelper.drawString(guiGraphics, this.getEnergyTitle().getString(), this.windowX + 16, this.windowY + 20, 0xFFFFFF);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    }

    @Override
    public void actionPerformed(int buttonId) {
        if (!this.hasSelectedPet()) {
            return;
        }

        if (buttonId == BaseCreatureEntity.GUI_COMMAND.SITTING.id)
            this.summonSet.toggleSitting();
        if (buttonId == BaseCreatureEntity.GUI_COMMAND.FOLLOWING.id)
            this.summonSet.toggleFollowing();
        if (buttonId == BaseCreatureEntity.GUI_COMMAND.PASSIVE.id)
            this.summonSet.togglePassive();
        if (buttonId == BaseCreatureEntity.GUI_COMMAND.STANCE.id)
            this.summonSet.toggleAggressive();
        if (buttonId == BaseCreatureEntity.GUI_COMMAND.PVP.id)
            this.summonSet.togglePVP();

        if (buttonId < 100) {
            this.sendCommandsToServer();
        }
    }

    public MutableComponent getPedestalTitle() {
        return Component.translatable("gui.summoningpedestal");
    }

    public MutableComponent getEnergyTitle() {
        return Component.translatable("stat.portal");
    }

    public void drawFuel(GuiGraphics guiGraphics) {
        int fuelX = this.windowX + 132;
        int fuelY = this.windowY + 42;
        ResourceLocation texture = this.getTexture();
        guiGraphics.blit(texture, fuelX, fuelY, 47, 170, 18, 18);

        int barWidth = 38;
        int barHeight = 11;
        int barX = fuelX + 22;
        int barY = fuelY + 3;
        int barU = 218;
        int barV = 225;
        guiGraphics.blit(texture, barX, barY, barU, barV + barHeight, barWidth, barHeight);
        barWidth = Math.round((float) barWidth * this.summoningPedestal.getFuelFillRatio());
        guiGraphics.blit(texture, barX, barY, barU, barV, barWidth, barHeight);
    }

    public void drawCapacityBar(GuiGraphics guiGraphics) {
        int energyBarWidth = 9;
        int energyBarHeight = 9;
        int energyBarX = this.windowX + 16;
        int energyBarY = this.windowY + 40 - energyBarHeight;
        this.drawHelper.drawBar(guiGraphics, TextureManager.getTexture("GUIPetSpiritEmpty"), energyBarX, energyBarY, 0, energyBarWidth, energyBarHeight, 10, 10);
        this.drawHelper.drawBar(guiGraphics, TextureManager.getTexture("GUIPetSpirit"), energyBarX, energyBarY, 0, energyBarWidth, energyBarHeight, this.summoningPedestal.getCapacityUnits(), 10);
    }

    public void drawProgressBar(GuiGraphics guiGraphics) {
        int barWidth = (256 / 4) + 16;
        int barHeight = (32 / 4) + 2;
        int barX = this.centerX + 2;
        int barY = this.windowY + 26;
        this.drawHelper.drawTexture(guiGraphics, TextureManager.getTexture("GUIPetBarEmpty"), barX, barY, 0, 1, 1, barWidth, barHeight);
        float respawnNormal = this.summoningPedestal.getSummonProgressRatio();
        this.drawHelper.drawTexture(guiGraphics, TextureManager.getTexture("GUIPetBarRespawn"), barX, barY, 0, respawnNormal, 1, barWidth * respawnNormal, barHeight);
    }

    public void sendCommandsToServer() {
        this.summoningPedestal.sendSummonSetToServer(this.summonSet);
    }

    public void selectMinion(String minionName) {
        if (this.summonSet == null) {
            this.summonSet = this.summoningPedestal.getOrCreateSummonSet(this.playerExt);
        }
        this.summoningPedestal.selectSummonType(this.playerExt, minionName);
        this.sendCommandsToServer();
    }

    public String getSelectedMinionName() {
        if (this.summonSet == null)
            return null;
        return this.summonSet.getSummonType();
    }

    public boolean hasPets() {
        return this.playerExt != null && !this.playerExt.getBeastiary().getSummonableList().isEmpty();
    }

    public boolean hasSelectedPet() {
        return this.hasPets() && this.summonSet != null && !"".equals(this.summonSet.getSummonType());
    }

    protected ResourceLocation getTexture() {
        return TextureManager.getTexture("GUISummoningPedestal");
    }
}
