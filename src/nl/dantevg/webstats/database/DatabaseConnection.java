package nl.dantevg.webstats.database;

import nl.dantevg.webstats.WebStats;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class DatabaseConnection {
	private final String hostname;
	private final String username;
	private final String password;
	private final String dbname;
	
	private Connection conn;
	
	public DatabaseConnection(String hostname, String username, String password, String dbname) {
		this.hostname = hostname;
		this.username = username;
		this.password = password;
		this.dbname = dbname;
	}
	
	public boolean connect() {
		try {
			if (conn == null) {
				conn = DriverManager.getConnection("jdbc:mysql://"
						+ hostname + "/" + dbname + "?autoReconnect=true", username, password);
				WebStats.logger.log(Level.INFO, "Connected to database " + dbname);
			}
			return true;
		} catch (SQLException e) {
			WebStats.logger.log(Level.WARNING, "Could not connect to database " + dbname, e);
			return false;
		}
	}
	
	public boolean disconnect() {
		try {
			if (conn != null && !conn.isClosed()) conn.close();
			WebStats.logger.log(Level.INFO, "Disconnected from database " + dbname);
			return true;
		} catch (SQLException e) {
			WebStats.logger.log(Level.WARNING, "Could not disconnect from database " + dbname, e);
			return false;
		}
	}
	
	public List<Map<String, String>> getTable(String table) {
		List<Map<String, String>> data = new ArrayList<>();
		if (conn == null) return data;
		try {
			PreparedStatement stmt = conn.prepareStatement("SELECT * FROM " + table);
			ResultSet results = stmt.executeQuery();
			ResultSetMetaData meta = results.getMetaData();
			int nColumns = meta.getColumnCount();
			while (results.next()) {
				Map<String, String> row = new HashMap<>();
				for (int i = 1; i <= nColumns; i++) {
					row.put(meta.getColumnLabel(i), results.getString(i));
				}
				data.add(row);
			}
		} catch (SQLException e) {
			WebStats.logger.log(Level.WARNING, "Could not query database " + dbname, e);
		}
		return data;
	}
	
}
