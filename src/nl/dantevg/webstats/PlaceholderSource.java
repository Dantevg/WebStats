package nl.dantevg.webstats;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.json.simple.JSONObject;

import java.util.*;

public class PlaceholderSource {
	private final Map<String, Object> placeholders;
	
	public PlaceholderSource() {
		placeholders = Main.config.getConfigurationSection("placeholders").getValues(false);
	}
	
	private List<String> getEntries() {
		List<String> entries = new ArrayList<>();
		for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
			entries.add(player.getName());
		}
		return entries;
	}
	
	private Map<String, JSONObject> getScores() {
		Map<String, JSONObject> values = new HashMap<>();
		for (Map.Entry<String, Object> entry : placeholders.entrySet()) {
			String placeholder = entry.getKey();
			String placeholderName = (String) entry.getValue();
			JSONObject scores = new JSONObject();
			for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
				scores.put(player.getName(), PlaceholderAPI.setPlaceholders(player, placeholder));
			}
			values.put(placeholderName, scores);
		}
		return values;
	}
	
	public EntriesScores getStats() {
		return new EntriesScores(getEntries(), getScores());
	}
	
}
