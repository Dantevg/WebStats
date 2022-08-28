package nl.dantevg.webstats.storage;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public interface StorageMethod {
	/**
	 * Store the scores, overwrite any previously existing scores. Write all
	 * columns present in the stats.
	 *
	 * @param scores the scores to store
	 * @return whether the storing was successful
	 */
	boolean store(@NotNull Table<String, String, String> scores);
	
	/**
	 * Store the scores, overwrite any previously existing scores.
	 *
	 * @param scores  the scores to store
	 * @param columns the columns to use. Only these columns will be written.
	 * @return whether the storing was successful
	 */
	boolean store(@NotNull Table<String, String, String> scores, @NotNull List<String> columns);
	
	@Nullable Result load();
	
	class Result {
		public final List<String> columns;
		public final Table<String, String, String> scores;
		
		public Result(List<String> columns, Table<String, String, String> scores) {
			this.columns = columns;
			this.scores = scores;
		}
		
		public Result(List<String> columns, List<Map<String, String>> scores) {
			this.columns = columns;
			this.scores = HashBasedTable.create();
			for (Map<String, String> entry : scores) {
				for (String column : columns) {
					this.scores.put(entry.get("uuid"), column, entry.get(column));
				}
			}
		}
		
	}
	
}
