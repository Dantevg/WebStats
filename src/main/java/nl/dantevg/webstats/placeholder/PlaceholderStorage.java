package nl.dantevg.webstats.placeholder;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import nl.dantevg.webstats.WebStats;
import nl.dantevg.webstats.database.DatabaseConfig;
import nl.dantevg.webstats.database.DatabaseConnection;
import nl.dantevg.webstats.storage.CSVStorage;
import nl.dantevg.webstats.storage.DatabaseStorage;
import nl.dantevg.webstats.storage.StorageMethod;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Level;

public class PlaceholderStorage {
	private static final String FILENAME = "placeholders.csv";
	private static final String TABLE_NAME = "WebStats_placeholders";
	
	private final PlaceholderSource placeholderSource;
	private final HashBasedTable<UUID, String, String> data = HashBasedTable.create();
	private @NotNull StorageMethod storage;
	
	public PlaceholderStorage(PlaceholderSource placeholderSource) throws InvalidConfigurationException {
		WebStats.logger.log(Level.INFO, "Enabling placeholder storage");
		
		this.placeholderSource = placeholderSource;
		
		// Register events
		Bukkit.getPluginManager().registerEvents(
				new PlaceholderListener(this, placeholderSource.config.saveOnPluginDisable),
				WebStats.getPlugin(WebStats.class));
		
		if (placeholderSource.config.storeInFile) {
			storage = getCSVStorage();
		} else {
			storage = getDatabaseStorage();
		}
		
		// Read persistently stored data
		load();
		
		// Update stored data with potentially new data
		update();
	}
	
	private DatabaseStorage getDatabaseStorage() throws InvalidConfigurationException {
		DatabaseConfig dbConfig = DatabaseConfig.getInstance();
		
		return new DatabaseStorage(
				new DatabaseConnection(dbConfig.hostname, dbConfig.username,
						dbConfig.password, placeholderSource.config.storeInDatabase),
				TABLE_NAME, "uuid", "placeholder");
	}
	
	private CSVStorage getCSVStorage() {
		return new CSVStorage(FILENAME, "uuid");
	}
	
	public void disable() {
		// Don't save on server close if we already saved on plugin disable
		if (placeholderSource.config.saveOnPluginDisable) return;
		
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
		storage.close();
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
		for (CachedOfflinePlayer player : placeholderSource.getEntriesAsCachedPlayers()) {
			placeholderSource.getScoresForPlayer(player).forEach((String placeholder, String value) -> {
				data.put(player.getUniqueId(), placeholder, value);
				WebStats.logger.log(Level.CONFIG, String.format("Updated %s (%s): %s = %s",
						player.getUniqueId(), player.getName(), placeholder, value));
			});
		}
	}
	
	/**
	 * Store placeholder data for player in-memory.
	 *
	 * @param player the player to store the placeholders for
	 */
	public void save(@NotNull CachedOfflinePlayer player) {
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
		for (CachedOfflinePlayer player : placeholderSource.getEntriesAsCachedPlayers()) {
			save(player);
		}
		
		Table<String, String, String> dataString = HashBasedTable.create();
		for (Table.Cell<UUID, String, String> cell : data.cellSet()) {
			dataString.put(cell.getRowKey().toString(), cell.getColumnKey(), cell.getValue());
		}
		storage.store(dataString);
		
		WebStats.logger.log(Level.INFO, "Saved placeholders");
	}
	
	public void prune(Set<String> placeholders) {
		if (Sets.difference(data.columnKeySet(), placeholders).isEmpty()) return;
		WebStats.logger.log(Level.INFO, "Removing old placeholders " + Sets.difference(data.columnKeySet(), placeholders));
		data.columnKeySet().retainAll(placeholders);
	}
	
	public @Nullable String getScore(UUID uuid, String placeholder) {
		return data.get(uuid, placeholder);
	}
	
	public boolean migrateToCSV() {
		storage.close();
		storage = getCSVStorage();
		saveAll();
		return true;
	}
	
	public boolean migrateToDatabase() {
		StorageMethod source = storage;
		try {
			storage = getDatabaseStorage();
		} catch (InvalidConfigurationException e) {
			WebStats.logger.log(Level.WARNING, "Migration failed", e);
			return false;
		}
		source.close();
		saveAll();
		return true;
	}
	
	public boolean migrate(Class<? extends StorageMethod> to) {
		if (to == CSVStorage.class) {
			return migrateToCSV();
		} else if (to == DatabaseStorage.class) {
			return migrateToDatabase();
		} else {
			throw new UnsupportedOperationException("unknown migration destination type");
		}
	}
	
	public boolean deletePlayer(String playername) {
		return data.rowKeySet().removeIf(uuid ->
				playername.equalsIgnoreCase(Bukkit.getOfflinePlayer(uuid).getName()));
	}
	
	public boolean deletePlayer(UUID uuid) {
		return data.rowKeySet().remove(uuid);
	}
	
	protected @NotNull String debug() {
		List<String> loadedScores = new ArrayList<>();
		for (Table.Cell<UUID, String, String> cell : data.cellSet()) {
			UUID uuid = cell.getRowKey();
			String playerName = (uuid != null) ? Bukkit.getOfflinePlayer(uuid).getName() : null;
			loadedScores.add(String.format("%s (%s): %s = %s§r",
					uuid, playerName, cell.getColumnKey(), cell.getValue()));
		}
		
		return "Placeholder storage loaded placeholders:\n  " + String.join("\n  ", loadedScores);
	}
	
}
