package nl.dantevg.webstats;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.List;
import java.util.Set;

public class Stats {
	public static JSONArray getScoreboardEntries(Scoreboard scoreboard){
		Set<String> entries = scoreboard.getEntries();
		JSONArray entriesJson = new JSONArray();
		entriesJson.addAll(entries);
		return entriesJson;
	}
	
	public static JSONObject getScoreboardScores(Scoreboard scoreboard){
		List<String> objectivesFilter = Main.getPlugin(Main.class).getConfig().getStringList("objectives");
		boolean allObjectives = objectivesFilter.contains("*");
		
		JSONObject objectivesJson = new JSONObject();
		for (Objective objective : scoreboard.getObjectives()) {
			// Filter objectives
			if(!allObjectives && !objectivesFilter.contains(objective.getDisplayName())) continue;
			
			JSONObject objectiveJson = new JSONObject();
			
			// Get player scores
			for (String entry : scoreboard.getEntries()) {
				Score s = objective.getScore(entry);
				if(s.isScoreSet()){
					objectiveJson.put(entry, s.getScore());
				}
			}
			
			objectivesJson.put(objective.getDisplayName(), objectiveJson);
		}
		return objectivesJson;
	}
	
	public static JSONObject getScoreboard(){
		Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
		
		JSONArray entriesJson = getScoreboardEntries(scoreboard); // Get players
		JSONObject objectivesJson = getScoreboardScores(scoreboard); // Get objectives
		
		JSONObject scoreboardJson = new JSONObject();
		scoreboardJson.put("entries", entriesJson);
		scoreboardJson.put("scores", objectivesJson);
		
		return scoreboardJson;
	}
	
	public static JSONArray getOnline(){
		JSONArray playersJson = new JSONArray();
		for(Player p : Bukkit.getOnlinePlayers()){
			playersJson.add(p.getName());
		}
		return playersJson;
	}
	
	public static JSONObject getAll() {
		JSONObject json = new JSONObject();
		
		json.put("scoreboard", getScoreboard());
		json.put("online", getOnline());
		
		return json;
	}
}
