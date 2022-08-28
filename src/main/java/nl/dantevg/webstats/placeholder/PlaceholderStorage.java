package nl.dantevg.webstats.placeholder;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import nl.dantevg.webstats.storage.CSVStorage;
import nl.dantevg.webstats.storage.DatabaseStorage;
import nl.dantevg.webstats.storage.StorageMethod;
import nl.dantevg.webstats.WebStats;
import nl.dantevg.webstats.database.DatabaseConnection;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.InvalidConfigurationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class PlaceholderStorage {
	private static final String FILENAME = "placeholders.csv";
	private static final String TABLE_NAME = "WebStats_placeholders";
	
	private final PlaceholderSource placeholderSource;
	private final HashBasedTable<UUID, String, String> data = HashBasedTable.create();
	private final boolean saveOnPluginDisable;
	private final @NotNull StorageMethod storage;
	
	public PlaceholderStorage(PlaceholderSource placeholderSource) throws InvalidConfigurationException {
		WebStats.logger.log(Level.INFO, "Enabling placeholder storer");
		
		this.placeholderSource = placeholderSource;
		
		// Register events
		saveOnPluginDisable = WebStats.config.getBoolean("save-placeholders-on-plugin-disable");
		Bukkit.getPluginManager().registerEvents(
				new PlaceholderListener(this, saveOnPluginDisable),
				WebStats.getPlugin(WebStats.class));
		
		if (WebStats.config.contains("store-placeholders-database")) {
			String hostname = WebStats.config.getString("database.hostname");
			String username = WebStats.config.getString("database.username");
			String password = WebStats.config.getString("database.password");
			String dbname = WebStats.config.getString("store-placeholders-database");
			
			if (hostname == null || username == null || password == null || dbname == null) {
				throw new InvalidConfigurationException("Invalid configuration: missing hostname, username, password or database name");
			}
			
			storage = new DatabaseStorage(
					new DatabaseConnection(hostname, username, password, dbname),
					TABLE_NAME, "uuid", "placeholder");
		} else {
			storage = new CSVStorage(FILENAME);
		}
		
		// Read persistently stored data
		load();
		
		// Update stored data with potentially new data
		update();
	}
	
	public void disconnect() {
		// Don't save on server close if we already saved on plugin disable
		if (saveOnPluginDisable) return;
		
		// since this is called when the server closes,
		// save all data to persistent csv storage now
		try {
			saveAll();
		} catch (IllegalStateException e) {
			// Catch this exception to add a helpful message before
			// https://github.com/Dantevg/WebStats/issues/30
			if (e.getMessage().equals("zip file closed")) {
				WebStats.logger.log(Level.SEVERE, "A plugin providing PlaceholderAPI placeholders " +
						"was disabled before WebStats could save the latest placeholders. You can set " +
						"'save-placeholders-on-plugin-disable' to true in the config as a workaround.\n" +
						"Github issue: https://github.com/Dantevg/WebStats/issues/30\n" +
						"Here is the stack trace for your interest:", e);
			} else {
				// Rethrow, not the right error message
				throw e;
			}
		}
	}
	
	private void load() {
		StorageMethod.Result stats = storage.load();
		if (stats == null) return;
		data.clear();
		
		for (Table.Cell<String, String, String> cell : stats.scores.cellSet()) {
			data.put(UUID.fromString(cell.getRowKey()), cell.getColumnKey(), cell.getValue());
		}
	}
	
	private void update() {
		for (OfflinePlayer player : placeholderSource.getEntriesAsPlayers()) {
			placeholderSource.getScoresForPlayer(player).forEach((String placeholder, String value) -> {
				data.put(player.getUniqueId(), placeholder, value);
				WebStats.logger.log(Level.CONFIG, String.format("Updated %s (%s): %s = %s",
						player.getUniqueId().toString(), player.getName(), placeholder, value));
			});
		}
	}
	
	/**
	 * Store placeholder data for player in-memory.
	 *
	 * @param player the player to store the placeholders for
	 */
	public void save(@NotNull OfflinePlayer player) {
		Map<String, String> scores = placeholderSource.getScoresForPlayer(player);
		UUID uuid = player.getUniqueId();
		
		if (scores.isEmpty()) return;
		
		// Store in instance
		scores.forEach((placeholder, value) -> data.put(uuid, placeholder, value));
	}
	
	/**
	 * Store placeholder data for all players, both in-memory and in a file.
	 */
	public void saveAll() {
		for (OfflinePlayer player : placeholderSource.getEntriesAsPlayers()) {
			save(player);
		}
		
		Table<String, String, String> dataString = HashBasedTable.create();
		for (Table.Cell<UUID, String, String> cell : data.cellSet()) {
			dataString.put(cell.getRowKey().toString(), cell.getColumnKey(), cell.getValue());
		}
		storage.store(dataString);
		
		WebStats.logger.log(Level.INFO, "Saved all placeholders to " + FILENAME);
	}
	
	public @Nullable String getScore(UUID uuid, String placeholder) {
		return data.get(uuid, placeholder);
	}
	
	protected @NotNull String debug() {
		List<String> loadedScores = new ArrayList<>();
		for (Table.Cell<UUID, String, String> cell : data.cellSet()) {
			UUID uuid = cell.getRowKey();
			if (uuid == null) continue;
			String playerName = Bukkit.getOfflinePlayer(uuid).getName();
			loadedScores.add(String.format("%s (%s): %s = %s",
					uuid, playerName, cell.getColumnKey(), cell.getValue()));
		}
		
		return "Placeholder storage loaded placeholders:\n" + String.join("\n", loadedScores);
	}
	
}
