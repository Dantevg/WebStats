package nl.dantevg.webstats.scoreboard;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import nl.dantevg.webstats.EntriesScores;
import nl.dantevg.webstats.WebStats;
import org.bukkit.Bukkit;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

public class ScoreboardSource {
	private final ScoreboardConfig config;
	private final Scoreboard scoreboard;
	
	public ScoreboardSource() {
		WebStats.logger.log(Level.INFO, "Enabling scoreboard source");
		config = ScoreboardConfig.getInstance(true);
		
		scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
	}
	
	private @NotNull Set<String> getEntries() {
		return new HashSet<>(scoreboard.getEntries());
	}
	
	private @NotNull Table<String, String, String> getScores() {
		Table<String, String, String> values = HashBasedTable.create();
		for (Objective objective : scoreboard.getObjectives()) {
			// Filter out objectives that do not appear in the list by either
			// their internal name or their display name
			if (!shouldIncludeObjective(objective)) continue;
			
			// Get player scores
			for (String entry : scoreboard.getEntries()) {
				Score s = objective.getScore(entry);
				if (s.isScoreSet()) values.put(
						entry,
						getObjectiveName(objective),
						String.valueOf(s.getScore()));
			}
		}
		return values;
	}
	
	private boolean shouldIncludeObjective(Objective objective) {
		return config.allObjectives
				|| config.objectives.contains(objective.getDisplayName())
				|| config.objectives.contains(objective.getName());
	}
	
	private String getObjectiveName(Objective objective) {
		if (config.renameObjectives.containsKey(objective.getName())) {
			return config.renameObjectives.get(objective.getName());
		} else if (config.renameObjectives.containsKey(objective.getDisplayName())) {
			return config.renameObjectives.get(objective.getDisplayName());
		} else {
			return objective.getDisplayName();
		}
	}
	
	public @NotNull EntriesScores getStats() {
		return new EntriesScores(getEntries(), getScores());
	}
	
}
