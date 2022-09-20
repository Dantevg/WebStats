package nl.dantevg.webstats;

import nl.dantevg.webstats.storage.CSVStorage;

import java.util.List;

public class StatExporter {
	private static final String FILENAME = "stats.csv";
	
	private final CSVStorage storage;
	
	public StatExporter() {
		storage = new CSVStorage(FILENAME, "Player");
	}
	
	public boolean export() {
		StatData.Stats stats = Stats.getStats();
		if (WebStats.config.contains("export-columns")) {
			List<String> columns = WebStats.config.getStringList("export-columns");
			return storage.append(stats.scores, columns);
		} else {
			return storage.append(stats.scores);
		}
	}
	
}
