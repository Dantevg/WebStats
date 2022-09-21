package nl.dantevg.webstats;

import nl.dantevg.webstats.storage.CSVStorage;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class StatExporter {
	private static final String FILENAME = "stats.csv";
	
	private final CSVStorage storage;
	
	public StatExporter() {
		Map<String, Function<String, String>> mapper = new HashMap<>();
		
		// Number of seconds since unix epoch, in UTC+0 timezone
		mapper.put("timestamp", entry -> Instant.now().getEpochSecond() + "");
		
		// Date in YYYY-MM-DD format, local timezone
		mapper.put("date", entry -> LocalDate.now().toString());
		
		storage = new CSVStorage(FILENAME, mapper, "Player");
	}
	
	public boolean export() {
		return storage.append(Stats.getStats().scores);
	}
	
}
