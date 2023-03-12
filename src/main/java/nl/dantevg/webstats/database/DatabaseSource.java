package nl.dantevg.webstats.database;

import nl.dantevg.webstats.EntriesScores;
import nl.dantevg.webstats.WebStats;
import org.bukkit.configuration.InvalidConfigurationException;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;

public class DatabaseSource {
	private static final String MAIN_KEY = "player";
	
	private final DatabaseConfig config;
	private final Map<String, DatabaseConnection> connections = new HashMap<>();
	private final List<DatabaseConverter> conversions = new ArrayList<>();
	
	public DatabaseSource() throws InvalidConfigurationException {
		WebStats.logger.log(Level.INFO, "Enabling database source");
		config = DatabaseConfig.getInstance(true);
		
		for (DatabaseConfig.TableConfig configItem : config.config) {
			DatabaseConnection conn;
			if (connections.containsKey(configItem.database)) {
				conn = connections.get(configItem.database);
			} else {
				conn = new DatabaseConnection(config.hostname, config.username, config.password, configItem.database);
				if (conn.connect()) connections.put(configItem.database, conn);
			}
			conversions.add(new DatabaseConverter(conn, configItem.table, configItem.convert));
		}
	}
	
	/**
	 * This method will be called asynchronously
	 */
	public @NotNull Future<EntriesScores> getStats() {
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
			scores.forEach((statName, score) -> data.scores.put(playerName, statName, score));
		});
		
		return new FutureTask<>(() -> data);
	}
	
	public void disable() {
		for (DatabaseConnection conn : connections.values()) conn.disconnect();
	}
	
}
