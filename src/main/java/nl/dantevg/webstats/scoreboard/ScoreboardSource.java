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
import java.util.List;
import java.util.Set;
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
	
	private @NotNull Set<String> getEntries() {
		return new HashSet<>(scoreboard.getEntries());
	}
	
	private @NotNull Table<String, String, String> getScores() {
		Table<String, String, String> values = HashBasedTable.create();
		for (Objective objective : scoreboard.getObjectives()) {
			// Filter out objectives that do not appear in the list by either
			// their internal name or their display name
			if (!allObjectives
					&& !objectivesFilter.contains(objective.getDisplayName())
					&& !objectivesFilter.contains(objective.getName())) continue;
			
			// Get player scores
			for (String entry : scoreboard.getEntries()) {
				Score s = objective.getScore(entry);
				if (s.isScoreSet()) values.put(
						objective.getDisplayName(),
						entry,
						String.valueOf(s.getScore()));
			}
		}
		return values;
	}
	
	public @NotNull EntriesScores getStats() {
		return new EntriesScores(getEntries(), getScores());
	}
	
}
