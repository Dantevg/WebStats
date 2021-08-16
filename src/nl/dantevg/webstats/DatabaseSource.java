package nl.dantevg.webstats;

import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseSource {
	private static final String MAIN_KEY = "player";
	
	private final Map<String, DatabaseConnection> connections = new HashMap<>();
	private final List<DatabaseConverter> conversions = new ArrayList<>();
	
	public DatabaseSource() {
		String hostname = Main.config.getString("database.hostname");
		String username = Main.config.getString("database.username");
		String password = Main.config.getString("database.password");
		
		List<Map<?, ?>> configItems = Main.config.getMapList("database.config");
		for (Map<?, ?> configItem : configItems) {
			String dbname = (String) configItem.get("database");
			DatabaseConnection conn;
			if (connections.containsKey(dbname)) {
				conn = connections.get(dbname);
			} else {
				conn = new DatabaseConnection(hostname, username, password, dbname);
				connections.put(dbname, conn);
				conn.connect();
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
		for (Map.Entry<String, Map<String, String>> player : players.entrySet()) {
			String playerName = player.getKey();
			data.entries.add(playerName);
			for (Map.Entry<String, String> values : player.getValue().entrySet()) {
				String statName = values.getKey();
				data.scores.putIfAbsent(statName, new JSONObject());
				data.scores.get(statName).put(playerName, values.getValue());
			}
		}
		
		return data;
	}
	
	public void disconnect() {
		for (DatabaseConnection conn : connections.values()) conn.disconnect();
	}
}
