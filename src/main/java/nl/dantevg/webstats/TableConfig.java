package nl.dantevg.webstats;

import com.google.gson.Gson;

import java.util.List;

public class TableConfig {
	public String name;
	public List<String> columns;
	public String sortBy;
	public Boolean sortDescending;
	
	public TableConfig(String name, List<String> columns, String sortBy, Boolean sortDescending) {
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
