package nl.dantevg.webstats;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class StatData {
	Map<String, Object> online;
	@SerializedName("scoreboard")
	public Stats stats;
	
	public StatData(Map<String, Object> online, Stats stats) {
		this.online = online;
		this.stats = stats;
	}
	
	@Override
	public String toString() {
		return new Gson().toJson(this);
	}
	
	public static class Stats {
		public Set<String> entries;
		public List<String> columns;
		public Map<String, Map<String, Object>> scores;
		
		public Stats(@NotNull EntriesScores entriesScores, List<String> columns) {
			this.entries = entriesScores.entries;
			this.columns = columns;
			this.scores = entriesScores.scores;
		}
		
		public Stats(@NotNull EntriesScores entriesScores) {
			this.entries = entriesScores.entries;
			this.scores = entriesScores.scores;
		}
		
	}
	
}
