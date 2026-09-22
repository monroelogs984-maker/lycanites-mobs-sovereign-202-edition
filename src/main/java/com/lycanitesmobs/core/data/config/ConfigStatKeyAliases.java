package com.lycanitesmobs.core.data.config;

import com.lycanitesmobs.LycanitesMobs;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigStatKeyAliases {
    public static final String RANGED_SPEED = "rangedSpeed";
    public static final String BROKEN_RANGED_SPEED = "ranged_speed";

    private static final boolean RANGED_SPEED_ALIAS_NEEDED = detectRangedSpeedAliasConfig();

    private ConfigStatKeyAliases() {
    }

    public static boolean isRangedSpeed(String statName) {
        return RANGED_SPEED.equals(statName);
    }

    public static boolean shouldDefineRangedSpeedAlias() {
        return RANGED_SPEED_ALIAS_NEEDED;
    }

    public static double resolveCanonicalOrAlias(
            ModConfigSpec.ConfigValue<Double> canonicalValue,
            ModConfigSpec.ConfigValue<Double> aliasValue
    ) {
        double canonical = canonicalValue.get();
        if (aliasValue == null || Double.compare(canonical, canonicalValue.getDefault()) != 0) {
            return canonical;
        }

        double alias = aliasValue.get();
        if (Double.compare(alias, aliasValue.getDefault()) != 0) {
            return alias;
        }
        return canonical;
    }

    private static boolean detectRangedSpeedAliasConfig() {
        Path commonConfig = FMLPaths.CONFIGDIR.get().resolve(LycanitesMobs.MODID + "-common.toml");
        if (!Files.isRegularFile(commonConfig)) {
            return false;
        }

        try {
			for (String line : Files.readAllLines(commonConfig)) {
				String trimmed = line.trim();
				if (trimmed.startsWith(BROKEN_RANGED_SPEED)
						&& trimmed.substring(BROKEN_RANGED_SPEED.length()).trim().startsWith("=")) {
					return true;
				}
			}
        } catch (IOException ignored) {
            return false;
        }
        return false;
    }
}
