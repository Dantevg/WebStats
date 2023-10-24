package nl.dantevg.webstats.placeholder;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import me.clip.placeholderapi.PlaceholderAPI;
import nl.dantevg.webstats.EntriesScores;
import nl.dantevg.webstats.EssentialsHelper;
import nl.dantevg.webstats.WebStats;
import nl.dantevg.webstats.WebStatsConfig;
import nl.dantevg.webstats.storage.StorageMethod;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.InvalidConfigurationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class PlaceholderSource {
	final PlaceholderConfig config;
	private final Map<UUID, CachedOfflinePlayer> offlinePlayerCache = new HashMap<>();
	
	private PlaceholderStorage storage;
	
	public PlaceholderSource() throws InvalidConfigurationException {
		WebStats.logger.log(Level.INFO, "Enabling placeholder source");
		config = PlaceholderConfig.getInstance(true);
		
		if (config.storeInFile || config.storeInDatabase != null) {
			storage = new PlaceholderStorage(this);
			storage.prune(new HashSet<>(config.placeholders.values()));
		}
		
		// Already cache all player names at the start of the server
		Set<OfflinePlayer> players = getEntriesAsPlayers();
		if (players.size() > 100)
			WebStats.logger.log(Level.INFO, "Caching player names of " + players.size() + " players");
		
		for (OfflinePlayer player : players) {
			offlinePlayerCache.put(player.getUniqueId(), new CachedOfflinePlayer(player));
		}
	}
	
	private @NotNull Set<OfflinePlayer> getEntriesAsPlayers() {
		Set<OfflinePlayer> entries = (!Bukkit.getOnlineMode() && WebStats.hasEssentials)
				? EssentialsHelper.getOfflinePlayers() : null;
		if (entries == null) entries = new HashSet<>();
		entries.addAll(Arrays.asList(Bukkit.getOfflinePlayers()));
		return entries;
	}
	
	private CachedOfflinePlayer getCachedPlayer(OfflinePlayer player) {
		// Only cache offline players, otherwise things go south
		if (player.isOnline()) {
			offlinePlayerCache.remove(player.getUniqueId());
			return new CachedOfflinePlayer(player);
		}
		
		CachedOfflinePlayer cachedPlayer = offlinePlayerCache.get(player.getUniqueId());
		if (cachedPlayer == null) {
			cachedPlayer = new CachedOfflinePlayer(player);
			offlinePlayerCache.put(player.getUniqueId(), cachedPlayer);
		}
		return cachedPlayer;
	}
	
	@NotNull Set<CachedOfflinePlayer> getEntriesAsCachedPlayers() {
		return getEntriesAsPlayers().stream()
				.map(this::getCachedPlayer)
				.collect(Collectors.toSet());
	}
	
	private @NotNull Set<String> getEntries() {
		return getEntriesAsPlayers().stream()      // all entries as OfflinePlayers
				.map(this::getCachedPlayer)
				.map(CachedOfflinePlayer::getName) // OfflinePlayer -> String
				.filter(Objects::nonNull)          // remove null names
				.collect(Collectors.toSet());
	}
	
	// Get up-to-date score for player, or stored one if the player is offline
	private @Nullable String getPlaceholderForPlayer(CachedOfflinePlayer player, String placeholder, String placeholderName) {
		if (player.getName() == null) return null;
		
		// If the player is online, get the most up-to-date value.
		// If the player is offline, get the stored value if it is stored, because placeholder
		// plugins may just yield 0 (which is indistinguishable from a real score of 0.)
		String score = null;
		if (storage != null) score = storage.getScore(player.getUniqueId(), placeholderName);
		if (player.getOfflinePlayer().isOnline() || !isPlaceholderSet(placeholder, score)) {
			score = PlaceholderAPI.setPlaceholders(player.getOfflinePlayer(), placeholder);
		}
		return isPlaceholderSet(placeholder, score) ? score : null;
	}
	
	private @Nullable String getPlaceholderForServer(String placeholder) {
		String score = PlaceholderAPI.setPlaceholders(null, placeholder);
		return isPlaceholderSet(placeholder, score) ? score : null;
	}
	
	// Get all scores for all players from PlaceholderAPI
	// Alternatively find stored scores from PlaceholderStorage
	private @NotNull Table<String, String, String> getScores() {
		Table<String, String, String> values = HashBasedTable.create();
		Set<CachedOfflinePlayer> players = getEntriesAsCachedPlayers();
		
		config.placeholders.forEach((placeholder, placeholderName) -> {
			if (WebStatsConfig.getInstance().serverColumns.contains(placeholderName)) {
				String score = getPlaceholderForServer(placeholder);
				if (score != null) values.put("#server", placeholderName, score);
			} else {
				for (CachedOfflinePlayer player : players) {
					String score = getPlaceholderForPlayer(player, placeholder, placeholderName);
					// Only add the score if it is not empty
					if (score != null) values.put(player.getName(), placeholderName, score);
				}
			}
		});
		return values;
	}
	
	// Get scores for single player from PlaceholderAPI
	// This method does NOT try to find stored scores from PlaceholderStorage
	@NotNull Map<String, String> getScoresForPlayer(@NotNull CachedOfflinePlayer player) {
		Map<String, String> scores = new HashMap<>();
		if (player.getName() == null) return scores;
		
		config.placeholders.forEach((placeholder, placeholderName) -> {
			String score = PlaceholderAPI.setPlaceholders(player.getOfflinePlayer(), placeholder);
			if (isPlaceholderSet(placeholder, score)) scores.put(placeholderName, score);
		});
		
		return scores;
	}
	
	public @NotNull EntriesScores getStats() {
		return new EntriesScores(getEntries(), getScores());
	}
	
	public void disable() {
		if (storage != null) storage.disable();
	}
	
	public void migrateStorage(Class<? extends StorageMethod> to) {
		storage.migrate(to);
	}
	
	public boolean deletePlayer(String playername) {
		return storage.deletePlayer(playername);
	}
	
	public static boolean isPlaceholderSet(String placeholder, @Nullable String value) {
		return value != null
				&& !value.equals("")
				&& !value.equalsIgnoreCase(placeholder);
	}
	
	public @NotNull String debug() {
		return (storage != null) ? storage.debug() : "";
	}
	
}
