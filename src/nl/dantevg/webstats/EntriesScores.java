package nl.dantevg.webstats;

import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntriesScores {
	public List<String> entries;
	public Map<String, JSONObject> scores;
	
	public EntriesScores(List<String> entries, Map<String, JSONObject> scores) {
		this.entries = entries;
		this.scores = scores;
	}
	
	public EntriesScores() {
		this.entries = new ArrayList<>();
		this.scores = new HashMap<>();
	}
}
