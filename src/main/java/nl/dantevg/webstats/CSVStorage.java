package nl.dantevg.webstats;

import com.google.common.collect.Table;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;

public class CSVStorage {
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
	
	/**
	 * Store the scores, overwrite any previously existing scores. Write all
	 * columns present in the stats.
	 *
	 * @param scores the scores to store
	 * @return whether the storing was successful
	 */
	public boolean store(@NotNull Table<String, String, String> scores) {
		return store(scores, new ArrayList<>(scores.rowKeySet()));
	}
	
	/**
	 * Store the scores, overwrite any previously existing scores.
	 *
	 * @param scores  the scores to store
	 * @param columns the columns to use. Only these columns will be written.
	 * @return whether the storing was successful
	 */
	public boolean store(@NotNull Table<String, String, String> scores, @NotNull List<String> columns) {
		if (ensureFileExists()) return false;
		try (FileWriter writer = new FileWriter(file, false)) {
			writeHeader(writer, columns);
			writeScores(writer, scores, columns);
			return true;
		} catch (IOException e) {
			WebStats.logger.log(Level.WARNING, "Could not write scores to file " + file.getPath(), e);
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
				writeScores(writer, scores, columnsFromHeader);
			} else {
				// No header present in CSV file, write header first.
				writeHeader(writer, columns);
				writeScores(writer, scores, columns);
			}
			return true;
		} catch (IOException e) {
			WebStats.logger.log(Level.WARNING, "Could not write scores to file " + file.getPath(), e);
			return false;
		}
	}
	
	/**
	 * Write the CSV header of columns to the file.
	 *
	 * @param writer  the writer to write to
	 * @param columns the columns to use as header
	 * @throws IOException
	 */
	private void writeHeader(@NotNull FileWriter writer, @NotNull List<String> columns) throws IOException {
		writer.append(String.join(",", columns)).append("\n");
	}
	
	/**
	 * Write the scores to the file.
	 *
	 * @param writer  the writer to write to
	 * @param scores  the scores to write
	 * @param columns the columns to use. Only these columns will be written.
	 * @throws IOException
	 */
	private void writeScores(@NotNull FileWriter writer,
	                         @NotNull Table<String, String, String> scores,
	                         @NotNull List<String> columns)
			throws IOException {
		for (String entry : scores.columnKeySet()) {
			List<String> scoreList = new ArrayList<>();
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
			if (hasScores) writer.append(String.join(",", scoreList)).append("\n");
		}
	}
	
	/**
	 * Attempt to read the columns from the CSV file header.
	 *
	 * @return the list of columns if the CSV header was present, or null otherwise
	 * @throws IOException if the file is not found
	 */
	private @Nullable List<String> readColumns() throws IOException {
		List<String> columns = new ArrayList<>();
		
		String line;
		try (Scanner lineScanner = new Scanner(file)) {
			line = lineScanner.nextLine();
		} catch (NoSuchElementException e) {
			return null;
		}
		
		Scanner columnScanner = new Scanner(line);
		columnScanner.useDelimiter(",");
		while (columnScanner.hasNext()) columns.add(columnScanner.next());
		columnScanner.close();
		
		return columns.size() > 0 ? columns : null;
	}
	
	private boolean ensureFileExists() {
		try {
			// Create new file if it did not yet exist
			file.createNewFile();
		} catch (IOException e) {
			WebStats.logger.log(Level.WARNING, "Could not create file " + file.getName(), e);
			return false;
		}
		if (!file.isFile()) {
			// File does exist and is not a file (a directory)
			WebStats.logger.log(Level.WARNING, "Could not create file " + file.getName()
					+ "because it is a directory. Please remove");
			return false;
		}
		return true;
	}
	
}
