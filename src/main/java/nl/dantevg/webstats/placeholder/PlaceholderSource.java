package nl.dantevg.webstats.placeholder;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import me.clip.placeholderapi.PlaceholderAPI;
import nl.dantevg.webstats.EntriesScores;
import nl.dantevg.webstats.EssentialsHelper;
import nl.dantevg.webstats.WebStats;
import nl.dantevg.webstats.storage.StorageMethod;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class PlaceholderSource {
	private final Map<String, Object> placeholders;
	private PlaceholderStorage storage;
	
	public PlaceholderSource() throws InvalidConfigurationException {
		WebStats.logger.log(Level.INFO, "Enabling placeholder source");
		
		ConfigurationSection section = WebStats.config.getConfigurationSection("placeholders");
		if (section == null) {
			throw new InvalidConfigurationException("Invalid configuration: placeholders should be a key-value map");
		}
		
		placeholders = section.getValues(false);
		if (WebStats.config.contains("store-placeholders-database")
				|| WebStats.config.getBoolean("store-placeholders-in-file")) {
			storage = new PlaceholderStorage(this);
		}
	}
	
	@NotNull Set<OfflinePlayer> getEntriesAsPlayers() {
		// Also get players from EssentialsX's userMap, for offline servers
		Set<OfflinePlayer> entries = (!Bukkit.getOnlineMode() && WebStats.hasEssentials)
				? EssentialsHelper.getOfflinePlayers() : null;
		if (entries == null) entries = new HashSet<>();
		entries.addAll(Arrays.asList(Bukkit.getOfflinePlayers()));
		return entries;
	}
	
	private Set<String> getEntries() {
		return getEntriesAsPlayers().stream() // all entries as OfflinePlayers
				.map(OfflinePlayer::getName)  // OfflinePlayer -> String
				.filter(Objects::nonNull)     // remove null names
				.collect(Collectors.toSet());
	}
	
	// Get up-to-date score for player, or stored one if the player is offline
	private @Nullable String getPlaceholderForPlayer(OfflinePlayer player, String placeholder, String placeholderName) {
		String name = player.getName();
		if (name == null) return null;
		
		// If the player is online, get the most up-to-date value.
		// If the player is offline, get the stored value if it is stored, because placeholder
		// plugins may just yield 0 (which is indistinguishable from a real score of 0.)
		String score = null;
		if (storage != null) score = storage.getScore(player.getUniqueId(), placeholderName);
		if (player.isOnline() || !isPlaceholderSet(placeholder, score)) {
			score = PlaceholderAPI.setPlaceholders(player, placeholder);
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
		Set<OfflinePlayer> players = getEntriesAsPlayers();
		
		placeholders.forEach((placeholder, placeholderName) -> {
			if (WebStats.config.getStringList("server-columns").contains((String) placeholderName)) {
				String score = getPlaceholderForServer(placeholder);
				if (score != null) values.put("#server", (String) placeholderName, score);
			} else {
				for (OfflinePlayer player : players) {
					String score = getPlaceholderForPlayer(player, placeholder, (String) placeholderName);
					// Only add the score if it is not empty
					if (score != null) values.put(player.getName(), (String) placeholderName, score);
				}
			}
		});
		return values;
	}
	
	// Get scores for single player from PlaceholderAPI
	// This method does NOT try to find stored scores from PlaceholderStorage
	@NotNull Map<String, String> getScoresForPlayer(@NotNull OfflinePlayer player) {
		Map<String, String> scores = new HashMap<>();
		String name = player.getName();
		if (name == null) return scores;
		
		placeholders.forEach((placeholder, placeholderName) -> {
			String score = PlaceholderAPI.setPlaceholders(player, placeholder);
			if (isPlaceholderSet(placeholder, score)) scores.put((String) placeholderName, score);
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
	
	public static boolean isPlaceholderSet(String placeholder, @Nullable String value) {
		return value != null
				&& !value.equals("")
				&& !value.equalsIgnoreCase(placeholder);
	}
	
	public @NotNull String debug() {
		return (storage != null) ? storage.debug() : "";
	}
	
}
