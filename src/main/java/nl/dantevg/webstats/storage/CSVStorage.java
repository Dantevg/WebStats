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
	private final String rowKey;
	
	/**
	 * Map of columns to entry->score functions
	 */
	private final @NotNull Map<String, Function<@NotNull String, @Nullable String>> mapper;
	
	public CSVStorage(@NotNull String filename,
	                  @NotNull Map<String, Function<@NotNull String, @Nullable String>> mapper,
	                  String rowKey) {
		File datafolder = WebStats.getPlugin(WebStats.class).getDataFolder();
		file = new File(datafolder, filename);
		this.rowKey = rowKey;
		datafolder.mkdirs();
		this.mapper = mapper;
	}
	
	public CSVStorage(@NotNull String filename, String rowKey) {
		this(filename, new HashMap<>(), rowKey);
	}
	
	@Override
	public boolean store(@NotNull Table<String, String, String> scores) {
		return store(scores, new ArrayList<>(scores.columnKeySet()));
	}
	
	@Override
	public boolean store(@NotNull Table<String, String, String> scores, @NotNull List<String> columns) {
		if (!ensureFileExists()) return false;
		try (FileWriter writer = new FileWriter(file, false)) {
			if (!columns.contains(rowKey)) {
				columns = new ArrayList<>(columns);
				columns.add(0, rowKey);
			}
			CSVPrinter printer = csvPrinterFromColumns(columns, writer);
			writeScores(printer, scores, columns);
			printer.close();
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
		return append(scores, new ArrayList<>(scores.columnKeySet()));
	}
	
	/**
	 * Store the scores, append them to any previously existing scores.
	 *
	 * @param scores  the scores to store
	 * @param columns the columns to use. Only these columns will be written.
	 * @return whether the storing was successful
	 */
	public boolean append(@NotNull Table<String, String, String> scores, @NotNull List<String> columns) {
		if (!ensureFileExists()) return false;
		try (FileWriter writer = new FileWriter(file, true)) {
			List<String> columnsFromHeader = readColumns();
			if (columnsFromHeader != null) {
				CSVPrinter printer = CSVFormat.DEFAULT.print(writer);
				writeScores(printer, scores, columnsFromHeader);
				printer.close();
			} else {
				// No header present in CSV file, write header first.
				if (!columns.contains(rowKey)) {
					columns = new ArrayList<>(columns);
					columns.add(0, rowKey);	
				}
				CSVPrinter printer = csvPrinterFromColumns(columns, writer);
				writeScores(printer, scores, columns);
				printer.close();
			}
			return true;
		} catch (IOException e) {
			WebStats.logger.log(Level.SEVERE, "Could not write scores to file " + file.getPath(), e);
			return false;
		}
	}
	
	@Override
	public @Nullable Result load() {
		if (!ensureFileExists()) return null;
		try {
			CSVParser parser = CSVFormat.DEFAULT.builder()
					.setHeader().setSkipHeaderRecord(true).build()
					.parse(new FileReader(file));
			List<Map<String, String>> stats = new ArrayList<>();
			for (CSVRecord record : parser) stats.add(record.toMap());
			
			List<String> columns = new ArrayList<>(parser.getHeaderNames());
			columns.remove(rowKey);
			
			return new Result(columns, stats, rowKey);
		} catch (IOException e) {
			WebStats.logger.log(Level.SEVERE, "Could not load scores from file " + file.getPath(), e);
			return null;
		}
	}
	
	@Override
	public void close() {
		// Do nothing
	}
	
	/**
	 * Attempt to read the columns from the CSV file header.
	 *
	 * @return the list of columns if the CSV header was present, or null otherwise
	 * @throws IOException if the file is not found
	 */
	public @Nullable List<String> readColumns() throws IOException {
		try (FileReader reader = new FileReader(file)) {
			CSVFormat csvFormat = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build();
			List<String> columns = csvFormat.parse(reader).getHeaderNames();
			return columns.size() > 0 ? columns : null;
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
		for (String entry : scores.rowKeySet()) {
			List<String> scoreList = new ArrayList<>();
			boolean hasScores = false;
			for (String column : columns) {
				if (column.equalsIgnoreCase(rowKey)) {
					scoreList.add(entry); // Player's name or UUID
				} else if (mapper.containsKey(column)) {
					String mapped = mapper.get(column).apply(entry);
					if (mapped != null) {
						scoreList.add(mapped);
						hasScores = true;
					} else {
						scoreList.add("");
					}
				} else if (scores.contains(entry, column)) {
					scoreList.add(scores.get(entry, column));
					hasScores = true;
				} else {
					// Add empty score so the columns stay aligned
					scoreList.add("");
				}
			}
			if (hasScores) printer.printRecord(scoreList);
		}
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
	
	private CSVPrinter csvPrinterFromColumns(List<String> columns, FileWriter writer) throws IOException {
		return CSVFormat.DEFAULT.builder()
				.setHeader(columns.toArray(new String[0]))
				.build()
				.print(writer);
	}
	
}
