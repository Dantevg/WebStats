package nl.dantevg.webstats;

import net.skinsrestorer.api.SkinsRestorerAPI;
import net.skinsrestorer.api.property.IProperty;
import org.jetbrains.annotations.Nullable;

public class SkinsRestorerHelper {
	public static @Nullable String getSkinID(String name) {
		SkinsRestorerAPI skinsRestorer = SkinsRestorerAPI.getApi();
		IProperty profile = skinsRestorer.getSkinData(name);
		return (profile != null) ? skinsRestorer.getSkinTextureUrlStripped(profile) : null;
	}
}
