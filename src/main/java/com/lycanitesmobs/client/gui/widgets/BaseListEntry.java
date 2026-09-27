package com.lycanitesmobs.client.gui.widgets;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;

public abstract class BaseListEntry extends ObjectSelectionList.Entry<BaseListEntry> {
	public int index;

	@Override
	public void render(GuiGraphics guiGraphics, int index, int top, int left, int bottom, int right, int mouseX, int mouseY, boolean p_render_8_, float partialTicks) {}

	@Override
	public Component getNarration() {
		return Component.empty();
	}

	/**
	 * Called when the mouse is clicked on this entry.
	 * @return Returns true if the click should continue (allowing the list to scroll, etc) or false if it shouldn't.
	 */
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
		return true;
	}

	/**
	 * Called when the mouse is released on this entry.
	 * @return Returns true if the release should continue (allowing the list to scroll, etc) or false if it shouldn't.
	 */
	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
		this.onClicked();
		return true;
	}

	/**
	 * Called when this entry is clicked.
	 */
	protected abstract void onClicked();
}
