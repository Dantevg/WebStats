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
	private Map<String, String> skins = new HashMap<>();
	
	public SkinsRestorerHelper(WebStats plugin) {
		Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		String name = event.getPlayer().getName();
		if (!skins.containsKey(name)) {
			Bukkit.getScheduler().runTaskAsynchronously(
					WebStats.getPlugin(WebStats.class),
					() -> skins.put(name, getSkinIDUncached(name)));
		}
	}
	
	public @Nullable String getSkinID(String name) {
		if (skins.containsKey(name)) {
			return skins.get(name);
		} else {
			return getSkinIDUncached(name);
		}
	}
	
	public Map<String, String> getSkinIDsForPlayers(Set<String> names) {
		Map<String, String> skins = new HashMap<>();
		for (String entry : names) {
			skins.put(entry, WebStats.skinsRestorerHelper.getSkinID(entry));
		}
		return skins;
	}
	
	// Should not be called on main server thread to avoid lag!
	private static @Nullable String getSkinIDUncached(String name) {
		SkinsRestorerAPI skinsRestorer = SkinsRestorerAPI.getApi();
		IProperty profile = skinsRestorer.getSkinData(name);
		return (profile != null) ? skinsRestorer.getSkinTextureUrlStripped(profile) : null;
	}
}
