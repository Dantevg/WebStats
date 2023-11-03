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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Level;

public class SkinsRestorerHelper implements Listener {
	private final SkinsRestorer skinsRestorer = SkinsRestorerProvider.get();
	
	// Map from player UUIDs to skin IDs
	private final Map<UUID, String> skins = new HashMap<>();
	
	public SkinsRestorerHelper(WebStats plugin) {
		if (!VersionProvider.isCompatibleWith("15")) {
			WebStats.logger.log(Level.WARNING,
					"This version of WebStats expects SkinsRestorer version 15, but "
							+ VersionProvider.getVersion()
							+ " is present. There may be issues!");
		}
		
		// Cache all skins at startup to prevent lag when loading the webpage for the first time
		Bukkit.getScheduler().runTaskAsynchronously(
				WebStats.getPlugin(WebStats.class),
				() -> Arrays.stream(Bukkit.getOfflinePlayers()).forEach(this::cacheSkin));
		
		Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		OfflinePlayer player = event.getPlayer();
		if (!skins.containsKey(player.getUniqueId())) {
			Bukkit.getScheduler().runTaskAsynchronously(
					WebStats.getPlugin(WebStats.class),
					() -> cacheSkin(player));
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
	
	private void cacheSkin(OfflinePlayer player) {
		UUID uuid = player.getUniqueId();
		String skinID = getSkinIDUncached(uuid, player.getName());
		if (skinID != null) skins.put(uuid, skinID);
	}
	
	public Map<String, String> getSkinIDsForPlayers(Set<String> names) {
		Map<String, String> skins = new HashMap<>();
		for (String playername : names) {
			UUID uuid = getUUIDForPlayer(playername);
			if (uuid == null) continue;
			String skinID = getSkinID(uuid, playername);
			if (skinID != null) skins.put(playername, skinID);
		}
		return skins;
	}
	
	// Should not be called on main server thread to avoid lag!
	private @Nullable String getSkinIDUncached(UUID uuid, String playername) {
		WebStats.logger.log(Level.CONFIG, String.format("Getting skin for %s (%s)", uuid, playername));
		PlayerStorage playerStorage = skinsRestorer.getPlayerStorage();
		try {
			Optional<SkinProperty> skin = playerStorage.getSkinForPlayer(uuid, playername);
			return skin.map(PropertyUtils::getSkinTextureUrlStripped).orElse(null);
		} catch (DataRequestException e) {
			return null;
		}
	}
	
	private static @Nullable UUID getUUIDForPlayer(@NotNull String playername) {
		return Arrays.stream(Bukkit.getOfflinePlayers())
				.filter(p -> playername.equalsIgnoreCase(p.getName()))
				.max(Comparator.comparingLong(OfflinePlayer::getLastPlayed))
				.map(OfflinePlayer::getUniqueId).orElse(null);
	}
	
	protected @NotNull String debug() {
		List<String> skinIDs = new ArrayList<>();
		for (Map.Entry<UUID, String> skin : skins.entrySet()) {
			UUID uuid = skin.getKey();
			String playername = (uuid != null) ? Bukkit.getOfflinePlayer(uuid).getName() : null;
			skinIDs.add(String.format("%s (%s): %s",
					uuid, playername, skin.getValue()));
		}
		
		return "Cached skin IDs:\n  " + String.join("\n  ", skinIDs);
	}
	
}
