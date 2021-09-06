package nl.dantevg.webstats;

import org.bukkit.Bukkit;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.json.simple.JSONObject;

import java.util.*;

public class ScoreboardSource {
	private final Scoreboard scoreboard;
	private final List<String> objectivesFilter;
	private final boolean allObjectives;
	
	public ScoreboardSource() {
		scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
		objectivesFilter = Main.config.getStringList("objectives");
		allObjectives = objectivesFilter.contains("*");
	}
	
	private Set<String> getEntries() {
		Set<String> entries = new HashSet<>();
		entries.addAll(scoreboard.getEntries());
		return entries;
	}
	
	private Map<String, JSONObject> getScores() {
		Map<String, JSONObject> objectives = new HashMap<>();
		for (Objective objective : scoreboard.getObjectives()) {
			// Filter objectives
			if (!allObjectives && !objectivesFilter.contains(objective.getDisplayName())) continue;
			
			// Get player scores
			JSONObject scores = new JSONObject();
			for (String entry : scoreboard.getEntries()) {
				Score s = objective.getScore(entry);
				if (s.isScoreSet()) scores.put(entry, s.getScore());
			}
			
			objectives.put(objective.getDisplayName(), scores);
		}
		return objectives;
	}
	
	public EntriesScores getStats() {
		return new EntriesScores(getEntries(), getScores());
	}
	
}
