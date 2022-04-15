package nl.dantevg.webstats.placeholder;

import me.clip.placeholderapi.PlaceholderAPI;
import nl.dantevg.webstats.EntriesScores;
import nl.dantevg.webstats.EssentialsHelper;
import nl.dantevg.webstats.WebStats;
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
		if (WebStats.config.contains("store-placeholders-database")) {
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
	
	// Get all scores for all players from PlaceholderAPI
	// Alternatively find stored scores from PlaceholderStorage
	private @NotNull Map<String, Map<String, String>> getScores() {
		Map<String, Map<String, String>> values = new HashMap<>();
		Set<OfflinePlayer> players = getEntriesAsPlayers();
		
		placeholders.forEach((placeholder, placeholderName) -> {
			Map<String, String> scores = new HashMap<>();
			for (OfflinePlayer player : players) {
				String name = player.getName();
				if (name == null) continue;
				
				// If the player is online, get the most up-to-date value or
				// the stored value.
				// If the player is offline, get the stored value immediately
				// because placeholder plugins may just yield 0 (which is
				// indistinguishable from a real score of 0.)
				String score = null;
				if (player.isOnline()) {
					score = PlaceholderAPI.setPlaceholders(player, placeholder);
				}
				if (!isPlaceholderSet(placeholder, score) && storage != null) {
					// If the placeholder was not substituted correctly, try the stored value
					score = storage.getScore(player.getUniqueId(), (String) placeholderName);
				}
				// Only add the score if it is not empty
				if (isPlaceholderSet(placeholder, score)) scores.put(name, score);
			}
			values.put((String) placeholderName, scores);
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
		if (storage != null) storage.disconnect();
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
