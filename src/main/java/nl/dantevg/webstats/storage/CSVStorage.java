package nl.dantevg.webstats.storage;

import com.google.common.collect.Table;
import nl.dantevg.webstats.WebStats;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;

public class CSVStorage implements StorageMethod {
	private final @NotNull File file;
	
	/**
	 * Map of columns to entry->score functions
	 */
	private final @NotNull Map<String, Function<@NotNull String, @Nullable String>> mapper;
	
	public CSVStorage(@NotNull String filename,
	                  @NotNull Map<String, Function<@NotNull String, @Nullable String>> mapper) {
		File datafolder = WebStats.getPlugin(WebStats.class).getDataFolder();
		file = new File(datafolder, filename);
		datafolder.mkdirs();
		this.mapper = mapper;
	}
	
	public CSVStorage(@NotNull String filename) {
		this(filename, new HashMap<>());
	}
	
	@Override
	public boolean store(@NotNull Table<String, String, String> scores) {
		return store(scores, new ArrayList<>(scores.rowKeySet()));
	}
	
	@Override
	public boolean store(@NotNull Table<String, String, String> scores, @NotNull List<String> columns) {
		if (ensureFileExists()) return false;
		try (FileWriter writer = new FileWriter(file, false)) {
			CSVPrinter printer = csvPrinterFromColumns(columns, writer);
			writeScores(printer, scores, columns);
			return true;
		} catch (IOException e) {
			WebStats.logger.log(Level.SEVERE, "Could not write scores to file " + file.getPath(), e);
			return false;
		}
	}
	
	/**
	 * Store the scores, append them to any previously existing scores. Write
	 * all columns present in the stats.
	 *
	 * @param scores the scores to store
	 * @return whether the storing was successful
	 */
	public boolean append(@NotNull Table<String, String, String> scores) {
		return append(scores, new ArrayList<>(scores.rowKeySet()));
	}
	
	/**
	 * Store the scores, append them to any previously existing scores.
	 *
	 * @param scores  the scores to store
	 * @param columns the columns to use. Only these columns will be written.
	 * @return whether the storing was successful
	 */
	public boolean append(@NotNull Table<String, String, String> scores, @NotNull List<String> columns) {
		if (ensureFileExists()) return false;
		try (FileWriter writer = new FileWriter(file, true)) {
			List<String> columnsFromHeader = readColumns();
			if (columnsFromHeader != null) {
				CSVPrinter printer = CSVFormat.DEFAULT.print(writer);
				writeScores(printer, scores, columnsFromHeader);
			} else {
				// No header present in CSV file, write header first.
				CSVPrinter printer = csvPrinterFromColumns(columns, writer);
				writeScores(printer, scores, columns);
			}
			return true;
		} catch (IOException e) {
			WebStats.logger.log(Level.SEVERE, "Could not write scores to file " + file.getPath(), e);
			return false;
		}
	}
	
	@Override
	public @Nullable Result load() {
		try {
			CSVParser parser = CSVFormat.DEFAULT.builder()
					.setHeader().setSkipHeaderRecord(true).build()
					.parse(new FileReader(file));
			List<Map<String, String>> stats = new ArrayList<>();
			for (CSVRecord record : parser) stats.add(record.toMap());
			return new Result(parser.getHeaderNames(), stats);
		} catch (IOException e) {
			WebStats.logger.log(Level.SEVERE, "Could not load scores from file " + file.getPath(), e);
			return null;
		}
	}
	
	/**
	 * Write the scores to the file.
	 *
	 * @param printer the printer to write to
	 * @param scores  the scores to write
	 * @param columns the columns to use. Only these columns will be written.
	 * @throws IOException
	 */
	private void writeScores(@NotNull CSVPrinter printer,
	                         @NotNull Table<String, String, String> scores,
	                         @NotNull List<String> columns)
			throws IOException {
		for (String entry : scores.columnKeySet()) {
			List<String> scoreList = new ArrayList<>();
			scoreList.add(entry); // Player's name or UUID
			boolean hasScores = false;
			for (String column : columns) {
				if (mapper.containsKey(column)) {
					String mapped = mapper.get(column).apply(entry);
					if (mapped != null) {
						scoreList.add(mapped);
						hasScores = true;
					} else {
						scoreList.add("");
					}
				} else if (scores.contains(column, entry)) {
					scoreList.add(scores.get(column, entry));
					hasScores = true;
				} else {
					// Add empty score so the columns stay aligned
					scoreList.add("");
				}
			}
			if (hasScores) printer.printRecord(scoreList);
		}
	}
	
	/**
	 * Attempt to read the columns from the CSV file header.
	 *
	 * @return the list of columns if the CSV header was present, or null otherwise
	 * @throws IOException if the file is not found
	 */
	private @Nullable List<String> readColumns() throws IOException {
		List<String> columns = CSVFormat.DEFAULT.parse(new FileReader(file)).getHeaderNames();
		return columns.size() > 0 ? columns : null;
	}
	
	private boolean ensureFileExists() {
		try {
			// Create new file if it did not yet exist
			file.createNewFile();
		} catch (IOException e) {
			WebStats.logger.log(Level.SEVERE, "Could not create file " + file.getName(), e);
			return false;
		}
		if (!file.isFile()) {
			// File does exist and is not a file (a directory)
			WebStats.logger.log(Level.SEVERE, "Could not create file " + file.getName()
					+ "because it is a directory. Please remove");
			return false;
		}
		return true;
	}
	
	private static CSVPrinter csvPrinterFromColumns(List<String> columns, FileWriter writer) throws IOException {
		columns = new ArrayList<>(columns);
		columns.add(0, "Player");
		return CSVFormat.DEFAULT.builder()
				.setHeader(columns.toArray(new String[0]))
				.build()
				.print(writer);
	}
	
}
