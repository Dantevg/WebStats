package nl.dantevg.webstats;

import net.skinsrestorer.api.SkinsRestorerAPI;
import net.skinsrestorer.api.property.IProperty;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SkinsRestorerHelper implements Listener {
	private SkinsRestorerAPI skinsRestorer = SkinsRestorerAPI.getApi();
	
	// Map from skin names (not player names) to skin IDs
	private Map<String, String> skins = new HashMap<>();
	
	public SkinsRestorerHelper(WebStats plugin) {
		Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		String skinName = skinsRestorer.getSkinName(event.getPlayer().getName());
		if (skinName == null) return;
		if (!skins.containsKey(skinName)) {
			Bukkit.getScheduler().runTaskAsynchronously(
					WebStats.getPlugin(WebStats.class),
					() -> cacheSkin(skinName));
		}
	}
	
	public @Nullable String getSkinID(String playername) {
		String skinName = skinsRestorer.getSkinName(playername);
		if (skinName == null) return null;
		if (skins.containsKey(skinName)) {
			return skins.get(skinName);
		} else {
			String skinID = getSkinIDUncached(skinName);
			if (skinID != null) skins.put(skinName, skinID);
			return skinID;
		}
	}
	
	public Map<String, String> getSkinIDsForPlayers(Set<String> names) {
		Map<String, String> skins = new HashMap<>();
		for (String entry : names) {
			String skinID = getSkinID(entry);
			if (skinID != null) skins.put(entry, skinID);
		}
		return skins;
	}
	
	// Should not be called on main server thread to avoid lag!
	private @Nullable String getSkinIDUncached(String skinName) {
		IProperty profile = skinsRestorer.getSkinData(skinName);
		return (profile != null) ? skinsRestorer.getSkinTextureUrlStripped(profile) : null;
	}
	
	private void cacheSkin(String skinName) {
		String skinID = getSkinIDUncached(skinName);
		if (skinID != null) skins.put(skinName, skinID);
	}
}
