package nl.dantevg.webstats;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class Stats {
	public static Map<String, Object> getOnline() {
		Map<String, Object> players = new HashMap<>();
		for (Player p : Bukkit.getOnlinePlayers()) {
			players.put(p.getName(), (WebStats.hasEssentials && EssentialsHelper.isAFK(p)) ? "afk" : true);
		}
		return players;
	}
	
	public static StatData.Stats getStats() {
		Set<String> entries = new HashSet<>();
		Map<String, Object> scores = new HashMap<>();
		
		if (WebStats.scoreboardSource != null) {
			EntriesScores entriesScores = WebStats.scoreboardSource.getStats();
			entries.addAll(entriesScores.entries);
			scores.putAll(entriesScores.scores);
		}
		if (WebStats.databaseSource != null) {
			EntriesScores entriesScores = WebStats.databaseSource.getStats();
			entries.addAll(entriesScores.entries);
			scores.putAll(entriesScores.scores);
		}
		if (WebStats.placeholderSource != null) {
			EntriesScores entriesScores = WebStats.placeholderSource.getStats();
			entries.addAll(entriesScores.entries);
			scores.putAll(entriesScores.scores);
		}
		
		if (WebStats.config.contains("columns")) {
			List<String> columns = new ArrayList<>(WebStats.config.getStringList("columns"));
			return new StatData.Stats(entries, columns, scores);
		} else {
			return new StatData.Stats(entries, scores);
		}
	}
	
	public static StatData getAll() {
		return new StatData(getOnline(), getStats());
	}
	
}
