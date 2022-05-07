package nl.dantevg.webstats;

import com.google.common.collect.Table;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StatData {
	public Map<String, Object> online;
	@SerializedName("scoreboard")
	public Stats stats;
	public Set<String> playernames;
	
	public StatData(Map<String, Object> online, Stats stats) {
		this.online = online;
		this.stats = stats;
	}
	
	public StatData(Map<String, Object> online, Stats stats, Set<String> playernames) {
		this.online = online;
		this.stats = stats;
		this.playernames = playernames;
	}
	
	@Override
	public String toString() {
		return new Gson().toJson(this);
	}
	
	public static class Stats {
		public Set<String> entries;
		public List<String> columns;
		@JsonAdapter(TableAdapter.class)
		public Table<String, String, String> scores;
		
		public Stats(@NotNull EntriesScores entriesScores, List<String> columns) {
			this.entries = entriesScores.entries;
			this.columns = columns;
			this.scores = entriesScores.scores;
		}
		
		public Stats(@NotNull EntriesScores entriesScores) {
			entries = entriesScores.entries;
			scores = entriesScores.scores;
		}
		
		// This is necessary because Gson does not serialize Guava Tables correctly,
		// convert Table to Map-in-Map first
		private static class TableAdapter implements JsonSerializer<Table<?, ?, ?>> {
			@Override
			public JsonElement serialize(Table<?, ?, ?> table, Type type, JsonSerializationContext context) {
				return context.serialize(table.rowMap());
			}
		}
		
	}
	
}
