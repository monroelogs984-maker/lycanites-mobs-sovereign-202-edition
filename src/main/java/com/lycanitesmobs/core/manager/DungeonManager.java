package com.lycanitesmobs.core.manager;

import com.google.gson.JsonObject;
import com.lycanitesmobs.core.data.info.ModInfo;
import com.lycanitesmobs.core.data.loaders.FileLoader;
import com.lycanitesmobs.core.data.loaders.JSONLoader;
import com.lycanitesmobs.core.data.loaders.StreamLoader;
import com.lycanitesmobs.core.util.helpers.LMHelperClass;
import com.lycanitesmobs.core.worldgen.dungeon.definition.DungeonTheme;

import java.util.HashMap;
import java.util.Map;

/**
 * Loads dungeon definitions from JSON.
 *
 * Port: themes only for now - they're also used outside dungeons (the Vespid Queen builds its hive from the
 * "vespid_hive" theme). TODO(port): structures, sectors and schematics come with the dungeon phase.
 */
public class DungeonManager extends JSONLoader {
    protected static DungeonManager INSTANCE;

    protected Map<String, DungeonTheme> themes = new HashMap<>();

    public static DungeonManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new DungeonManager();
        }
        return INSTANCE;
    }

    /**
     * Loads all JSON Dungeons.
     **/
    public void loadAllFromJson(ModInfo modInfo) {
        this.loadAllJson(modInfo, "Dungeon Theme", "dungeons/themes", "name", true, null, FileLoader.common(), StreamLoader.common());
        LMHelperClass.logDebug("Dungeon", "Complete! " + this.themes.size() + " Dungeon Themes Loaded In Total.");
    }

    @Override
    public void parseJson(ModInfo modInfo, String loadGroup, JsonObject json) {
        if ("Dungeon Theme".equals(loadGroup)) {
            DungeonTheme theme = new DungeonTheme();
            theme.loadFromJSON(json);
            this.addTheme(theme);
        }
    }

    /**
     * Adds a new Dungeon Theme to this Manager.
     **/
    public void addTheme(DungeonTheme theme) {
        if (this.themes.containsKey(theme.getName())) {
            LMHelperClass.logWarningMessage("[Dungeon Manager] Tried to add a Dungeon Theme with a name that is already in use: " + theme.getName());
            return;
        }
        this.themes.put(theme.getName(), theme);
    }

    /**
     * Gets a Theme by name or null if none can be found.
     **/
    public DungeonTheme getTheme(String name) {
        if (!this.themes.containsKey(name)) {
            LMHelperClass.logWarning("Dungeon", "Unable to find a theme called " + name);
            return null;
        }
        return this.themes.get(name);
    }
}
