package nl.dantevg.webstats.database;

import nl.dantevg.webstats.WebStats;
import org.jetbrains.annotations.Nullable;

import java.sql.*;
import java.util.logging.Level;

public class DatabaseConnection {
	private final String hostname;
	private final String username;
	private final String password;
	private final String dbname;
	
	private @Nullable Connection conn;
	
	public DatabaseConnection(String hostname, String username, String password, String dbname) {
		this.hostname = hostname;
		this.username = username;
		this.password = password;
		this.dbname = dbname;
	}
	
	public String getDBName() {
		return dbname;
	}
	
	public boolean connect() {
		try {
			closeConnection(); // Close old invalid connection if present
			conn = DriverManager.getConnection(
					"jdbc:mysql://" + hostname + "/" + dbname + "?autoReconnect=true",
					username, password);
			WebStats.logger.log(Level.INFO, "Connected to database " + dbname);
			return true;
		} catch (SQLException e) {
			WebStats.logger.log(Level.WARNING, "Could not connect to database " + dbname, e);
			return false;
		}
	}
	
	public boolean disconnect() {
		try {
			closeConnection();
			WebStats.logger.log(Level.INFO, "Disconnected from database " + dbname);
			return true;
		} catch (SQLException e) {
			WebStats.logger.log(Level.WARNING, "Could not disconnect from database " + dbname, e);
			return false;
		}
	}
	
	public @Nullable Connection getConnection() throws SQLException {
		if (!isConnected()) connect();
		return conn;
	}
	
	public boolean isConnected() throws SQLException {
		return conn != null && !conn.isClosed() && conn.isValid(1);
	}
	
	private void closeConnection() throws SQLException {
		if (conn != null && !conn.isClosed()) conn.close();
		conn = null;
	}
	
}
