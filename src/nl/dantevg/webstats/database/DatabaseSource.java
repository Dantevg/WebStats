package nl.dantevg.webstats.database;

import nl.dantevg.webstats.ConfigurationException;
import nl.dantevg.webstats.EntriesScores;
import nl.dantevg.webstats.WebStats;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class DatabaseSource {
	private static final String MAIN_KEY = "player";
	
	private final Map<String, DatabaseConnection> connections = new HashMap<>();
	private final List<DatabaseConverter> conversions = new ArrayList<>();
	
	public DatabaseSource() throws ConfigurationException {
		WebStats.logger.log(Level.INFO, "Enabling database source");
		
		String hostname = WebStats.config.getString("database.hostname");
		String username = WebStats.config.getString("database.username");
		String password = WebStats.config.getString("database.password");
		
		if (hostname == null || username == null || password == null) {
			throw new ConfigurationException("Invalid configuration: missing hostname, username or password");
		}
		
		List<Map<?, ?>> configItems = WebStats.config.getMapList("database.config");
		for (Map<?, ?> configItem : configItems) {
			String dbname = (String) configItem.get("database");
			DatabaseConnection conn;
			if (connections.containsKey(dbname)) {
				conn = connections.get(dbname);
			} else {
				conn = new DatabaseConnection(hostname, username, password, dbname);
				if(conn.connect()) connections.put(dbname, conn);
			}
			conversions.add(new DatabaseConverter(
					conn, (String) configItem.get("table"),
					(List<List<String>>) configItem.get("convert")));
		}
	}
	
	public EntriesScores getStats() {
		// Get and convert each database/table combination
		List<Map<String, String>> entries = new ArrayList<>();
		for (DatabaseConverter conversion : conversions) {
			entries.addAll(conversion.getValues());
		}
		
		// Transpose to players
		Map<String, Map<String, String>> players = new HashMap<>();
		for (Map<String, String> row : entries) {
			String player = row.get(MAIN_KEY);
			players.putIfAbsent(player, new HashMap<>());
			players.get(player).putAll(row);
			players.get(player).remove(MAIN_KEY);
		}
		
		// Transpose again to stats
		EntriesScores data = new EntriesScores();
		players.forEach((playerName, scores) -> {
			data.entries.add(playerName);
			scores.forEach((statName, score) -> {
				data.scores.putIfAbsent(statName, new HashMap<>());
				data.scores.get(statName).put(playerName, score);
			});
		});
		
		return data;
	}
	
	public void disable() {
		for (DatabaseConnection conn : connections.values()) conn.disconnect();
	}
	
}
