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
			playersJson.put(p.getName(), (Main.hasEssentials && EssentialsHelper.isAFK(p)) ? "afk" : true);
		}
		return playersJson;
	}
	
	public static JSONObject getStats() {
		Set<String> entries = new HashSet<>();
		JSONArray entriesJson = new JSONArray();
		JSONObject scores = new JSONObject();
		JSONArray columns = new JSONArray();
		
		if(Main.config.contains("columns")){
			columns.addAll(Main.config.getList("columns"));
		}
		
		if (Main.scoreboardSource != null) {
			EntriesScores entriesScores = Main.scoreboardSource.getStats();
			entries.addAll(entriesScores.entries);
			scores.putAll(entriesScores.scores);
		}
		if (Main.databaseSource != null) {
			EntriesScores entriesScores = Main.databaseSource.getStats();
			entries.addAll(entriesScores.entries);
			scores.putAll(entriesScores.scores);
		}
		if (Main.placeholderSource != null) {
			EntriesScores entriesScores = Main.placeholderSource.getStats();
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
