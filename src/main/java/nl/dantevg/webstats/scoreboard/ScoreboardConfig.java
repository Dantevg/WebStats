package nl.dantevg.webstats.scoreboard;

import nl.dantevg.webstats.WebStats;

import java.util.List;

public class ScoreboardConfig {
	private static ScoreboardConfig instance;
	
	public final List<String> objectives;
	public final boolean allObjectives;
	
	private ScoreboardConfig() {
		objectives = WebStats.config.getStringList("objectives");
		allObjectives = objectives.contains("*");
	}
	
	public static ScoreboardConfig getInstance(boolean forceNew) {
		if (instance == null || forceNew) instance = new ScoreboardConfig();
		return instance;
	}
	
	public static ScoreboardConfig getInstance() {
		return getInstance(false);
	}
	
}
