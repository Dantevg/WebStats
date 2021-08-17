package nl.dantevg.webstats;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Stats {
	
	public static JSONObject getOnline() {
		JSONObject playersJson = new JSONObject();
		boolean hasEssentials = Bukkit.getPluginManager().getPlugin("Essentials") != null;
		for (Player p : Bukkit.getOnlinePlayers()) {
			playersJson.put(p.getName(), (hasEssentials && AFKHelper.isAFK(p)) ? "afk" : true);
		}
		return playersJson;
	}
	
	public static JSONObject getStats() {
		JSONArray entries = new JSONArray();
		JSONObject scores = new JSONObject();
		
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
		scoreboardJson.put("entries", entries);
		scoreboardJson.put("scores", scores);
		
		JSONObject json = new JSONObject();
		json.put("scoreboard", scoreboardJson);
		json.put("online", getOnline());
		
		return json;
	}
	
}
