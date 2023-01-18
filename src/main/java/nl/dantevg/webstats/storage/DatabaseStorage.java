package nl.dantevg.webstats.storage;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import nl.dantevg.webstats.WebStats;
import nl.dantevg.webstats.database.DatabaseConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

public class DatabaseStorage implements StorageMethod {
	private final @NotNull DatabaseConnection conn;
	private final String tableName;
	private final String rowKey;
	private final String columnKey;
	
	public DatabaseStorage(@NotNull DatabaseConnection conn, String tableName, String rowKey, String columnKey) {
		this.conn = conn;
		this.tableName = tableName;
		this.rowKey = rowKey;
		this.columnKey = columnKey;
		
		if (!conn.connect()) return;
		
		// Create table on first use
		if (isFirstUse()) init();
	}
	
	@Override
	public boolean store(@NotNull Table<String, String, String> scores) {
		return store(scores, new ArrayList<>(scores.columnKeySet()));
	}
	
	@Override
	public boolean store(@NotNull Table<String, String, String> scores, @NotNull List<String> columns) {
		try (Connection connection = conn.getConnection();
		     PreparedStatement stmt = connection.prepareStatement("REPLACE INTO " + tableName + " VALUES (?, ?, ?);")) {
			removeOldColumns(connection, columns);
			for (Table.Cell<String, String, String> entry : scores.cellSet()) {
				stmt.setString(1, entry.getRowKey());
				stmt.setString(2, entry.getColumnKey());
				stmt.setString(3, entry.getValue());
				stmt.addBatch();
				WebStats.logger.log(Level.CONFIG, String.format("Saving %s: %s = %s",
						entry.getRowKey(), entry.getColumnKey(), entry.getValue()));
			}
			stmt.executeBatch();
			return true;
		} catch (SQLException e) {
			WebStats.logger.log(Level.SEVERE, "Could not update database " + conn.getDBName(), e);
			return false;
		}
	}
	
	@Override
	public @Nullable Result load() {
		try (PreparedStatement stmt = conn.getConnection()
				.prepareStatement("SELECT * FROM " + tableName + ";");
		     ResultSet resultSet = stmt.executeQuery()) {
			Table<String, String, String> stats = HashBasedTable.create();
			int nRows = 0;
			while (resultSet.next()) {
				String row = resultSet.getString(rowKey);
				String column = resultSet.getString(columnKey);
				String value = resultSet.getString("value");
				stats.put(row, column, value);
				nRows++;
			}
			WebStats.logger.log(Level.INFO, "Loaded " + nRows + " rows from database " + conn.getDBName());
			return new Result(new ArrayList<>(stats.columnKeySet()), stats);
		} catch (SQLException e) {
			WebStats.logger.log(Level.SEVERE, "Could not query database " + conn.getDBName(), e);
		}
		return null;
	}
	
	@Override
	public void close() {
		conn.disconnect();
	}
	
	private boolean isFirstUse() {
		try (ResultSet resultSet = conn.getConnection().getMetaData()
				.getTables(null, null, tableName, null)) {
			return !resultSet.next();
		} catch (SQLException e) {
			WebStats.logger.log(Level.WARNING, "Could not query database " + conn.getDBName(), e);
		}
		return false;
	}
	
	private void init() {
		try (PreparedStatement stmt = conn.getConnection().prepareStatement(
				String.format("CREATE TABLE %s (%s VARCHAR(36) NOT NULL, %s VARCHAR(255) NOT NULL, value VARCHAR(255), PRIMARY KEY(%s, %s));",
						tableName, rowKey, columnKey, rowKey, columnKey))) {
			stmt.executeUpdate();
			WebStats.logger.log(Level.INFO, "Created new table "
					+ tableName + " in database " + conn.getDBName());
		} catch (SQLException e) {
			WebStats.logger.log(Level.SEVERE, "Could not initialise database " + conn.getDBName(), e);
		}
	}
	
	private boolean removeOldColumns(@NotNull Connection connection, @NotNull List<String> columns) {
		// Don't try to remove 0 columns
		if (columns.isEmpty()) return true;
		
		String[] columnsPrepared = new String[columns.size()];
		Arrays.fill(columnsPrepared, "?");
		
		try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM " + tableName
				+ " WHERE " + columnKey + " NOT IN (" + String.join(", ", columnsPrepared) + ")")) {
			for (int i = 0; i < columns.size(); i++) stmt.setString(i + 1, columns.get(i));
			stmt.executeUpdate();
			return true;
		} catch (SQLException e) {
			WebStats.logger.log(Level.SEVERE, "Could not remove old entries from database " + conn.getDBName(), e);
			return false;
		}
	}
	
}
