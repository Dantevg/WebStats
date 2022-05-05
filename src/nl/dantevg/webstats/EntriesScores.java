package nl.dantevg.webstats;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public class EntriesScores {
	public Set<String> entries;
	public Table<String, String, String> scores;
	
	public EntriesScores(Set<String> entries, Table<String, String, String> scores) {
		this.entries = entries;
		this.scores = scores;
	}
	
	public EntriesScores() {
		entries = new HashSet<>();
		scores = HashBasedTable.create();
	}
	
	public void add(@NotNull EntriesScores that) {
		entries.addAll(that.entries);
		scores.putAll(that.scores);
	}
	
}
