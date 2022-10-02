package nl.dantevg.webstats;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import nl.dantevg.webstats.storage.CSVStorage;
import nl.dantevg.webstats.storage.StorageMethod;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;

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
		boolean success = storage.append(filterChanged(Stats.getStats()));
		if (success) WebStats.logger.log(Level.INFO, "Export finished");
		else WebStats.logger.log(Level.INFO, "Could not export stats");
		return success;
	}
	
	/**
	 * Filter to keep only entries that have at least one column changed since
	 * the last export.
	 *
	 * @param stats the stats to filter
	 * @return a new table with all entries without any changes removed
	 */
	private Table<String, String, String> filterChanged(StatData.Stats stats) {
		StorageMethod.Result lastExportStats = storage.load();
		if (lastExportStats == null) return stats.scores;
		
		Set<String> columns = stats.scores.columnKeySet();
		try {
			List<String> storedColumns = storage.readColumns();
			// Only keep columns that are actually stored.
			// Do not just use storedColumns to check, because that also
			// contains "timestamp" and "date", which change every time.
			if (storedColumns != null) columns.retainAll(storedColumns);
		} catch (IOException e) {
			// Ignore
		}
		
		Table<String, String, String> filteredStats = HashBasedTable.create(stats.scores);
		
		for (String entryName : stats.scores.rowKeySet()) {
			Map<String, String> newEntryMap = stats.scores.row(entryName);
			Map<String, String> storedEntryMap = lastExportStats.scores.row(entryName);
			boolean allSame = columns.stream()
					.allMatch(column -> Objects.equals(
							newEntryMap.getOrDefault(column, ""),
							storedEntryMap.getOrDefault(column, "")));
			// Remove entire entry if no columns changed
			if (allSame) filteredStats.rowKeySet().remove(entryName);
		}
		
		return filteredStats;
	}
	
}
