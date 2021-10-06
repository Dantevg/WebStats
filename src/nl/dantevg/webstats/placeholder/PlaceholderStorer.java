package nl.dantevg.webstats.placeholder;

import com.google.common.collect.HashBasedTable;
import nl.dantevg.webstats.ConfigurationException;
import nl.dantevg.webstats.WebStats;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;

public class PlaceholderStorer {
	private static final String TABLE_NAME = "WebStats_placeholders";
	
	private final PlaceholderSource placeholderSource;
	private final String dbname;
	private final HashBasedTable<UUID, String, String> data = HashBasedTable.create();
	
	private Connection conn;
	
	public PlaceholderStorer(PlaceholderSource placeholderSource) throws ConfigurationException {
		this.placeholderSource = placeholderSource;
		
		// Register events
		Bukkit.getPluginManager().registerEvents(new EventListener(this), WebStats.getPlugin(WebStats.class));
		
		// Connect to database
		String hostname = WebStats.config.getString("database.hostname");
		String username = WebStats.config.getString("database.username");
		String password = WebStats.config.getString("database.password");
		dbname = WebStats.config.getString("store-placeholders-database");
		
		if (hostname == null || username == null || password == null || dbname == null) {
			throw new ConfigurationException("Invalid configuration: missing hostname, username, password or database name");
		}
		try {
			conn = DriverManager.getConnection("jdbc:mysql://"
					+ hostname + "/" + dbname, username, password);
			WebStats.logger.log(Level.INFO, "Connected to placeholder database " + dbname);
		} catch (SQLException e) {
			WebStats.logger.log(Level.SEVERE, "Could not connect to placeholder database " + dbname, e);
			return;
		}
		
		// Create table on first use
		if (isFirstUse()) init();
		
		// Read persistently stored data
		load();
		
		// Update stored data with potentially new data
		update();
	}
	
	public boolean disconnect() {
		// since this is called when the server closes,
		// save all data to persistent database storage now
		saveAll();
		try {
			if (conn != null && !conn.isClosed()) conn.close();
			WebStats.logger.log(Level.INFO, "Disconnected from placeholder database " + dbname);
			return true;
		} catch (SQLException e) {
			WebStats.logger.log(Level.WARNING, "Could not disconnect from placeholder database " + dbname, e);
			return false;
		}
	}
	
	private boolean isFirstUse() {
		try (ResultSet resultSet = conn.getMetaData().getTables(null, null, TABLE_NAME, null)) {
			return !resultSet.next();
		} catch (SQLException e) {
			WebStats.logger.log(Level.WARNING, "Could not query placeholder database " + dbname, e);
		}
		return false;
	}
	
	private void init() {
		try (PreparedStatement stmt = conn.prepareStatement("CREATE TABLE " + TABLE_NAME
				+ " (uuid VARCHAR(36) NOT NULL, "
				+ "placeholder VARCHAR(255) NOT NULL, "
				+ "value VARCHAR(255), "
				+ "PRIMARY KEY(uuid, placeholder));")) {
			stmt.executeUpdate();
			WebStats.logger.log(Level.INFO, "Created new placeholder table");
		} catch (SQLException e) {
			WebStats.logger.log(Level.SEVERE, "Could not initialise placeholder database " + dbname, e);
		}
	}
	
	private void load() {
		try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM " + TABLE_NAME + ";");
		     ResultSet resultSet = stmt.executeQuery()) {
			while (resultSet.next()) {
				UUID uuid = UUID.fromString(resultSet.getString("uuid"));
				String placeholder = resultSet.getString("placeholder");
				String value = resultSet.getString("value");
				data.put(uuid, placeholder, value);
			}
		} catch (SQLException e) {
			WebStats.logger.log(Level.WARNING, "Could not query placeholder database " + dbname, e);
		}
	}
	
	private void update() {
		for (OfflinePlayer player : placeholderSource.getEntriesAsPlayers()) {
			placeholderSource.getScoresForPlayer(player).forEach((String placeholder, String value) ->
					data.put(player.getUniqueId(), placeholder, value));
		}
	}
	
	// Store placeholder data for player
	public void save(OfflinePlayer player) {
		Map<String, String> scores = placeholderSource.getScoresForPlayer(player);
		UUID uuid = player.getUniqueId();
		
		if(scores.isEmpty()) return;
		
		// Store in instance
		scores.forEach((placeholder, value) -> data.put(uuid, placeholder, value));
		
		// Store in database
		if (conn == null) return;
		
		String uuidStr = uuid.toString();
		try (PreparedStatement stmt = conn.prepareStatement("REPLACE INTO " + TABLE_NAME + " VALUES (?, ?, ?);")) {
			for (Map.Entry<String, String> entry : scores.entrySet()) {
				stmt.setString(1, uuidStr);
				stmt.setString(2, entry.getKey());
				stmt.setString(3, entry.getValue());
				stmt.addBatch();
			}
			stmt.executeUpdate();
		} catch (SQLException e) {
			WebStats.logger.log(Level.SEVERE, "Could not update placeholder database " + dbname, e);
		}
		
		WebStats.logger.log(Level.INFO, "Saved placeholders for player " + player.getName());
	}
	
	// Store placeholder data for all players
	public void saveAll() {
		for (OfflinePlayer player : placeholderSource.getEntriesAsPlayers()) save(player);
		WebStats.logger.log(Level.INFO, "Saved all placeholders");
	}
	
	public String getScore(UUID uuid, String placeholder) {
		return data.get(uuid, placeholder);
	}
	
}
