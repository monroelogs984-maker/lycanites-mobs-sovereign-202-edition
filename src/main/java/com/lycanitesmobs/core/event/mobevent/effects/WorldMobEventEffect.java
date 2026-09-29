package com.lycanitesmobs.core.event.mobevent.effects;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ServerLevelData;

public class WorldMobEventEffect extends MobEventEffect {
	/** How long started rain/thunder lasts at least (ticks), 5 minutes. **/
	private static final int WEATHER_TICKS = 5 * 60 * 20;

	/** Controls the rain. Can be: none, start or stop. **/
	protected String rain = "none";

	/** Controls the thundering. Can be: none, start or stop. **/
	protected String thunder = "none";

	/** If set, changes the time of day. 20000 is good for night time events. This only moves the day time forwards and not backwards. **/
	protected int dayTime = -1;


	@Override
	public void loadFromJSON(JsonObject json) {
		if(json.has("rain"))
			this.rain = json.get("rain").getAsString();

		if(json.has("thunder"))
			this.thunder = json.get("thunder").getAsString();

		if(json.has("dayTime"))
			this.dayTime = json.get("dayTime").getAsInt();

		super.loadFromJSON(json);
	}


	@Override
	public void onUpdate(Level world, Player player, BlockPos pos, int level, int ticks, int variant) {
		if(ticks == 0) {
			// Port: the official checked "world instanceof ServerLevelData", which is never true (the level data is a
			// separate object), so thunder never started. Rain/thunder also get a duration here: with only the flag
			// set, vanilla's weather countdown could flip the weather back within ticks.
			ServerLevelData serverWorldInfo = null;
			if (world.getLevelData() instanceof ServerLevelData serverLevelData) {
				serverWorldInfo = serverLevelData;
			}

			// Rain:
			if ("start".equalsIgnoreCase(this.rain)) {
				world.getLevelData().setRaining(true);
				if (serverWorldInfo != null) {
					serverWorldInfo.setClearWeatherTime(0);
					serverWorldInfo.setRainTime(Math.max(serverWorldInfo.getRainTime(), WEATHER_TICKS));
				}
			} else if ("stop".equalsIgnoreCase(this.rain)) {
				world.getLevelData().setRaining(false);
			}

			// Lightning:
			if (serverWorldInfo != null) {
				if ("start".equalsIgnoreCase(this.thunder)) {
					serverWorldInfo.setThundering(true);
					serverWorldInfo.setThunderTime(Math.max(serverWorldInfo.getThunderTime(), WEATHER_TICKS));
				} else if ("stop".equalsIgnoreCase(this.thunder)) {
					serverWorldInfo.setThundering(false);
				}
			}

			// Day Time:
			if (this.dayTime >= 0) {
				int dayTime = 23999;
				long currentTime = world.getDayTime();
				int targetTime = this.dayTime;
				long excessTime = currentTime % dayTime;
				if (excessTime > targetTime) {
					targetTime += dayTime;
				}
				for (ServerLevel serverWorld : world.getServer().getAllLevels()) {
					serverWorld.setDayTime(currentTime - excessTime + targetTime);
				}
			}
		}
	}
}
