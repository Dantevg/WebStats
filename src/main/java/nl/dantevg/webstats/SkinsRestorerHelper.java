package nl.dantevg.webstats;

import net.skinsrestorer.api.PropertyUtils;
import net.skinsrestorer.api.SkinsRestorer;
import net.skinsrestorer.api.SkinsRestorerProvider;
import net.skinsrestorer.api.VersionProvider;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.api.storage.PlayerStorage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Level;

public class SkinsRestorerHelper implements Listener {
	private final SkinsRestorer skinsRestorer = SkinsRestorerProvider.get();
	
	// Map from player UUIDs to skin IDs
	private final Map<UUID, String> skins = new HashMap<>();
	
	public SkinsRestorerHelper(WebStats plugin) {
		if (VersionProvider.isCompatibleWith("15")) {
			WebStats.logger.log(Level.WARNING, "WebStats was made for SkinsRestorer v15, but " + VersionProvider.getVersion() + " is present.");
		}
		
		Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		UUID uuid = event.getPlayer().getUniqueId();
		String playername = event.getPlayer().getName();
		if (!skins.containsKey(uuid)) {
			Bukkit.getScheduler().runTaskAsynchronously(
					WebStats.getPlugin(WebStats.class),
					() -> cacheSkin(uuid, playername));
		}
	}
	
	public @Nullable String getSkinID(UUID uuid, String playername) {
		if (skins.containsKey(uuid)) {
			return skins.get(uuid);
		} else {
			String skinID = getSkinIDUncached(uuid, playername);
			if (skinID != null) skins.put(uuid, skinID);
			return skinID;
		}
	}
	
	public Map<String, String> getSkinIDsForPlayers(Set<String> names) {
		Map<String, String> skins = new HashMap<>();
		OfflinePlayer[] players = Bukkit.getOfflinePlayers();
		for (String entry : names) {
			UUID uuid = Arrays.stream(players)
					.filter(p -> entry.equalsIgnoreCase(p.getName()))
					.findAny().map(OfflinePlayer::getUniqueId).orElse(null);
			String skinID = getSkinID(uuid, entry);
			if (skinID != null) skins.put(entry, skinID);
		}
		return skins;
	}
	
	// Should not be called on main server thread to avoid lag!
	private @Nullable String getSkinIDUncached(UUID uuid, String playername) {
		PlayerStorage playerStorage = skinsRestorer.getPlayerStorage();
		try {
			Optional<SkinProperty> skin = playerStorage.getSkinForPlayer(uuid, playername);
			return skin.map(PropertyUtils::getSkinTextureUrlStripped).orElse(null);
		} catch (DataRequestException e) {
			return null;
		}
	}
	
	private void cacheSkin(UUID uuid, String playername) {
		String skinID = getSkinIDUncached(uuid, playername);
		if (skinID != null) skins.put(uuid, skinID);
	}
}
