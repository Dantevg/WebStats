package nl.dantevg.webstats.scoreboard;

import nl.dantevg.webstats.EntriesScores;
import nl.dantevg.webstats.WebStats;
import org.bukkit.Bukkit;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import java.util.*;
import java.util.logging.Level;

public class ScoreboardSource {
	private final Scoreboard scoreboard;
	private final List<String> objectivesFilter;
	private final boolean allObjectives;
	
	public ScoreboardSource() {
		WebStats.logger.log(Level.INFO, "Enabling scoreboard source");
		
		scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
		objectivesFilter = WebStats.config.getStringList("objectives");
		allObjectives = objectivesFilter.contains("*");
	}
	
	private Set<String> getEntries() {
		return new HashSet<>(scoreboard.getEntries());
	}
	
	private Map<String, Map<String, Object>> getScores() {
		Map<String, Map<String, Object>> objectives = new HashMap<>();
		for (Objective objective : scoreboard.getObjectives()) {
			// Filter objectives
			if (!allObjectives && !objectivesFilter.contains(objective.getDisplayName())) continue;
			
			// Get player scores
			Map<String, Object> scores = new HashMap<>();
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
