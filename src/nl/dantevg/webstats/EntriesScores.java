package nl.dantevg.webstats;

import org.jetbrains.annotations.NotNull;

import java.util.*;

public class EntriesScores {
	public Set<String> entries;
	public Map<String, Map<String, String>> scores;
	
	public EntriesScores(Set<String> entries, Map<String, Map<String, String>> scores) {
		this.entries = entries;
		this.scores = scores;
	}
	
	public EntriesScores() {
		this.entries = new HashSet<>();
		this.scores = new HashMap<>();
	}
	
	public void add(@NotNull EntriesScores that) {
		entries.addAll(that.entries);
		scores.putAll(that.scores);
	}
	
}
