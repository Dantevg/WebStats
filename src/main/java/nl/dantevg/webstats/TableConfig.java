package nl.dantevg.webstats;

import com.google.gson.Gson;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TableConfig {
	public @Nullable String name;
	public @Nullable List<String> columns;
	public @Nullable String sortBy;
	public @Nullable Boolean sortDescending;
	
	public TableConfig(@Nullable String name,
	                   @Nullable List<String> columns,
	                   @Nullable String sortBy,
	                   @Nullable Boolean sortDescending) {
		this.name = name;
		this.columns = columns;
		this.sortBy = sortBy;
		this.sortDescending = sortDescending;
	}
	
	@Override
	public String toString() {
		return new Gson().toJson(this);
	}
}
