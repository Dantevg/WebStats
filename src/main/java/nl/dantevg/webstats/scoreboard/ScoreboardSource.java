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
import java.util.concurrent.Future;
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
			if (!config.allObjectives
					&& !config.objectives.contains(objective.getDisplayName())
					&& !config.objectives.contains(objective.getName())) continue;
			
			// Get player scores
			for (String entry : scoreboard.getEntries()) {
				Score s = objective.getScore(entry);
				if (s.isScoreSet()) values.put(
						entry,
						objective.getDisplayName(),
						String.valueOf(s.getScore()));
			}
		}
		return values;
	}
	
	/**
	 * This method will be called asynchronously
	 */
	public @NotNull Future<EntriesScores> getStats() {
		return Bukkit.getScheduler().callSyncMethod(
				WebStats.getPlugin(WebStats.class),
				() -> new EntriesScores(getEntries(), getScores()));
	}
	
}
