package nl.dantevg.webstats;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.HashSet;
import java.util.Set;

public class Stats {
	
	public static JSONObject getOnline() {
		JSONObject playersJson = new JSONObject();
		for (Player p : Bukkit.getOnlinePlayers()) {
			playersJson.put(p.getName(), (WebStats.hasEssentials && EssentialsHelper.isAFK(p)) ? "afk" : true);
		}
		return playersJson;
	}
	
	public static JSONObject getStats() {
		Set<String> entries = new HashSet<>();
		JSONArray entriesJson = new JSONArray();
		JSONObject scores = new JSONObject();
		JSONArray columns = new JSONArray();
		
		if(WebStats.config.contains("columns")){
			columns.addAll(WebStats.config.getList("columns"));
		}
		
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
		
		JSONObject scoreboardJson = new JSONObject();
		entriesJson.addAll(entries);
		scoreboardJson.put("entries", entriesJson);
		scoreboardJson.put("columns", columns);
		scoreboardJson.put("scores", scores);
		
		JSONObject json = new JSONObject();
		json.put("scoreboard", scoreboardJson);
		json.put("online", getOnline());
		
		return json;
	}
	
}
