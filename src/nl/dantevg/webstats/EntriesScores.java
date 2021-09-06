package nl.dantevg.webstats;

import org.json.simple.JSONObject;

import java.util.*;

public class EntriesScores {
	public Set<String> entries;
	public Map<String, JSONObject> scores;
	
	public EntriesScores(Set<String> entries, Map<String, JSONObject> scores) {
		this.entries = entries;
		this.scores = scores;
	}
	
	public EntriesScores() {
		this.entries = new HashSet<>();
		this.scores = new HashMap<>();
	}
}
