package com.lycanitesmobs.core.util;

import com.lycanitesmobs.LycanitesMobs;
import net.minecraft.network.chat.Component;

/**
 * Offline stand-in for the official version checker, which fetched the latest version from an online service
 * with SSL certificate checks disabled. This only reports the running version, for the Beastiary index screen.
 */
public class VersionChecker {
	private static final VersionChecker INSTANCE = new VersionChecker();

	protected final VersionInfo currentVersion = new VersionInfo(LycanitesMobs.versionNumber, LycanitesMobs.versionMC);

	public static VersionChecker getInstance() {
		return INSTANCE;
	}

	public VersionInfo getLatestVersion() {
		return this.currentVersion;
	}

	public static class VersionInfo {
		public String versionNumber;
		public String mcVersion;
		public String name = "Sovereign 202 Edition";
		public boolean isNewer = false;

		public VersionInfo(String versionNumber, String mcVersion) {
			this.versionNumber = versionNumber;
			this.mcVersion = mcVersion;
		}

		public String getUpdateNotes() {
			String content = "§l§n" + Component.translatable("gui.beastiary.index.changes").getString() + "§r";
			content += "\n§l" + Component.translatable("gui.beastiary.index.changes.name").getString() + ":§r " + this.name;
			return content;
		}
	}
}
