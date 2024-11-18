package nl.dantevg.webstats.scoreboard;

import nl.dantevg.webstats.WebStats;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class ScoreboardConfig {
	private static ScoreboardConfig instance;
	
	public final List<String> objectives;
	public final Map<String, String> renameObjectives = new HashMap<>();
	public final boolean allObjectives;
	
	private ScoreboardConfig() {
		// Get objectives as list of names
		objectives = WebStats.config.getStringList("objectives");
		allObjectives = objectives.contains("*");
		
		// Get objective renames as map of old name -> new name
		List<?> objectivesList = WebStats.config.getList("objectives", new ArrayList<>());
		for (Object objective : objectivesList) {
			if (objective instanceof Map) {
				Map<String, String> map = (Map<String, String>) objective;
				if (map.size() > 1) {
					WebStats.logger.log(Level.WARNING, "Can only enter one scoreboard objective rename at a time, ignoring " + map);
				} else {
					renameObjectives.putAll(map);
					objectives.addAll(map.keySet());
				}
			}
		}
	}
	
	public static ScoreboardConfig getInstance(boolean forceNew) {
		if (instance == null || forceNew) instance = new ScoreboardConfig();
		return instance;
	}
	
	public static ScoreboardConfig getInstance() {
		return getInstance(false);
	}
	
}
