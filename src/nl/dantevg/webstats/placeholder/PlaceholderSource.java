package nl.dantevg.webstats.placeholder;

import me.clip.placeholderapi.PlaceholderAPI;
import nl.dantevg.webstats.*;
import nl.dantevg.webstats.ConfigurationException;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class PlaceholderSource {
	private final Map<String, Object> placeholders;
	private PlaceholderStorer storer;
	
	public PlaceholderSource() throws ConfigurationException {
		ConfigurationSection section = WebStats.config.getConfigurationSection("placeholders");
		if (section == null) {
			throw new ConfigurationException("Invalid configuration: placeholders should be a key-value map");
		}
		
		placeholders = section.getValues(false);
		if (WebStats.config.contains("store-placeholders-database")) {
			storer = new PlaceholderStorer(this);
		}
		
		WebStats.logger.log(Level.INFO, "Enabled placeholder source");
	}
	
	Set<OfflinePlayer> getEntriesAsPlayers() {
		// Also get players from EssentialsX's userMap, for offline servers
		Set<OfflinePlayer> entries = (!Bukkit.getOnlineMode() && WebStats.hasEssentials)
				? EssentialsHelper.getOfflinePlayers()
				: new HashSet<>();
		entries.addAll(Arrays.asList(Bukkit.getOfflinePlayers()));
		return entries;
	}
	
	private Set<String> getEntries() {
		return getEntriesAsPlayers().stream()   // all entries as OfflinePlayers
				.map(OfflinePlayer::getName)  // OfflinePlayer -> String
				.filter(Objects::nonNull)     // remove null names
				.collect(Collectors.toSet());
	}
	
	private Map<String, Map<String, Object>> getScores() {
		Map<String, Map<String, Object>> values = new HashMap<>();
		Set<OfflinePlayer> players = getEntriesAsPlayers();
		
		placeholders.forEach((placeholder, placeholderName) -> {
			Map<String, Object> scores = new HashMap<>();
			for (OfflinePlayer player : players) {
				String name = player.getName();
				if (name == null) continue;
				String score = PlaceholderAPI.setPlaceholders(player, placeholder);
				if (!isPlaceholderSet(placeholder, score) && storer != null) {
					// If the placeholder was not substituted correctly, try the stored value
					score = storer.getScore(player.getUniqueId(), (String) placeholderName);
				}
				// Only add the score if it is not empty
				if (isPlaceholderSet(placeholder, score)) scores.put(name, score);
			}
			values.put((String) placeholderName, scores);
		});
		return values;
	}
	
	Map<String, String> getScoresForPlayer(OfflinePlayer player) {
		Map<String, String> scores = new HashMap<>();
		String name = player.getName();
		if (name == null) return scores;
		
		placeholders.forEach((placeholder, placeholderName) -> {
			String score = PlaceholderAPI.setPlaceholders(player, placeholder);
			if (isPlaceholderSet(placeholder, score)) scores.put((String) placeholderName, score);
		});
		
		return scores;
	}
	
	public EntriesScores getStats() {
		return new EntriesScores(getEntries(), getScores());
	}
	
	public void disable() {
		if (storer != null) storer.disconnect();
	}
	
	public static boolean isPlaceholderSet(String placeholder, String value) {
		return value != null
				&& !value.equals("")
				&& !value.equalsIgnoreCase(placeholder);
	}
	
	public String debug() {
		return (storer != null) ? storer.debug() : "";
	}
	
}
