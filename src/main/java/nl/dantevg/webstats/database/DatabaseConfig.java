package nl.dantevg.webstats.database;

import nl.dantevg.webstats.WebStats;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DatabaseConfig {
	private static DatabaseConfig instance;
	
	public final String hostname;
	public final String username;
	public final String password;
	public final @NotNull List<TableConfig> config = new ArrayList<>();
	
	private DatabaseConfig() throws InvalidConfigurationException {
		ConfigurationSection config = WebStats.config.getConfigurationSection("database");
		if (config == null) {
			throw new InvalidConfigurationException("database must be a config section");
		}
		
		hostname = config.getString("hostname");
		username = config.getString("username");
		password = config.getString("password");
		
		if (hostname == null || username == null || password == null) {
			throw new InvalidConfigurationException("Invalid configuration: missing hostname, username or password");
		}
		
		List<Map<?, ?>> configItems = config.getMapList("config");
		for (Map<?, ?> configItem : configItems) {
			this.config.add(new TableConfig(configItem));
		}
	}
	
	public static DatabaseConfig getInstance(boolean forceNew) throws InvalidConfigurationException {
		if (instance == null || forceNew) instance = new DatabaseConfig();
		return instance;
	}
	
	public static DatabaseConfig getInstance() throws InvalidConfigurationException {
		return getInstance(false);
	}
	
	static class TableConfig {
		final String database;
		final String table;
		final List<List<String>> convert;
		
		public TableConfig(Map<?, ?> map) {
			database = (String) map.get("database");
			table = (String) map.get("table");
			convert = (List<List<String>>) map.get("convert");
		}
	}
	
}
