package com.lycanitesmobs.core.event.mobevent.trigger;

import com.google.gson.JsonObject;
import com.lycanitesmobs.core.event.mobevent.MobEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Starts a mob event when an altar is activated (the boss events: RahovartAltar, AsmodeusAltar, AmalgalichAltar).
 *
 * Port: the official attached itself to an AltarInfo, which isn't ported yet (altar phase). Until then the triggers
 * are kept in a by-altar-name registry that the altar phase (or a command) can look up with getTriggers().
 */
public class AltarMobEventTrigger extends MobEventTrigger {
	private static final Map<String, List<AltarMobEventTrigger>> TRIGGERS_BY_ALTAR = new HashMap<>();

	/** The name of the Altar to trigger from. **/
	protected String altarName = "";


	/** Returns the triggers attached to the named altar. **/
	public static List<AltarMobEventTrigger> getTriggers(String altarName) {
		return Collections.unmodifiableList(TRIGGERS_BY_ALTAR.getOrDefault(altarName, Collections.emptyList()));
	}


	/** Constructor **/
	public AltarMobEventTrigger(MobEvent mobEvent) {
		super(mobEvent);
	}

	public String getAltarName() {
		return this.altarName;
	}


	/** Loads this Mob Event Trigger from the provided JSON data. **/
	public void loadFromJSON(JsonObject json) {
		this.altarName = json.get("altarName").getAsString();
		TRIGGERS_BY_ALTAR.computeIfAbsent(this.altarName, name -> new ArrayList<>()).add(this);

		super.loadFromJSON(json);
	}


	/**
	 * Called when the associated altar has been activated for this trigger.
	 * @param entity The entity that activated the altar.
	 * @param world The world that the Altar was activated in.
	 * @param pos The central position of the Altar.
	 * @param variant The level of the activated the Altar.
	 */
	public boolean onActivate(Entity entity, Level world, BlockPos pos, int variant) {
		Player player = null;
		if(entity instanceof Player) {
			player = (Player)entity;
		}
		if(!this.canTrigger(world, player)) {
			return false;
		}
		return this.trigger(world, player, pos, 1, variant);
	}


	/** Called when this AltarMobEventTrigger is removed. **/
	public void onRemove() {
		List<AltarMobEventTrigger> triggers = TRIGGERS_BY_ALTAR.get(this.altarName);
		if(triggers != null) {
			triggers.remove(this);
		}
	}
}
