package nl.dantevg.webstats.database;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import nl.dantevg.webstats.WebStats;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

public class DatabaseConverter {
	private final DatabaseConnection conn;
	private final String table;
	private final List<List<String>> conversions;
	
	public DatabaseConverter(DatabaseConnection conn, String table, List<List<String>> conversions) {
		this.conn = conn;
		this.table = table;
		this.conversions = conversions;
	}
	
	public @NotNull List<Map<String, String>> getValues() {
		List<Map<String, String>> data = getTable(table);
		for (List<String> command : conversions) {
			switch (command.get(0)) {
				case "filter": filter(data, command); break;
				case "remove": remove(data, command); break;
				case "rename": rename(data, command); break;
				case "key-value": key_value(data, command); break;
				case "json": json(data, command); break;
				case "uuid": uuid(data, command); break;
				default: WebStats.logger.log(Level.WARNING, "Invalid conversion command '" + command.get(0) + "'"); break;
			}
		}
		return data;
	}
	
	private @NotNull List<Map<String, String>> getTable(String table) {
		List<Map<String, String>> data = new ArrayList<>();
		if (conn == null) return data;
		try (PreparedStatement stmt = conn.getConnection()
				.prepareStatement("SELECT * FROM " + table);
		     ResultSet results = stmt.executeQuery()) {
			ResultSetMetaData meta = results.getMetaData();
			int nColumns = meta.getColumnCount();
			while (results.next()) {
				Map<String, String> row = new HashMap<>();
				for (int i = 1; i <= nColumns; i++) {
					row.put(meta.getColumnLabel(i), results.getString(i));
				}
				data.add(row);
			}
		} catch (SQLException | NullPointerException e) {
			WebStats.logger.log(Level.WARNING, "Could not query database " + conn.getDBName(), e);
		}
		return data;
	}
	
	// [filter, column...]
	private static void filter(@NotNull List<Map<String, String>> data, @NotNull List<String> command) {
		for (Map<String, String> row : data) {
			// key does not appear in argument list (or key is "filter")
			row.entrySet().removeIf(entry -> command.indexOf(entry.getKey()) <= 0);
		}
	}
	
	// [remove, column...]
	private static void remove(@NotNull List<Map<String, String>> data, @NotNull List<String> command) {
		for (Map<String, String> row : data) {
			// key does appear in argument list (or key is "filter")
			row.entrySet().removeIf(entry -> command.indexOf(entry.getKey()) > 0);
		}
	}
	
	// [rename, from, to]
	private static void rename(@NotNull List<Map<String, String>> data, @NotNull List<String> command) {
		if (command.size() < 3) {
			WebStats.logger.log(Level.WARNING, "Conversion command 'rename' needs 2 arguments");
			return;
		}
		for (Map<String, String> row : data) {
			String from = command.get(1), to = command.get(2);
			row.put(to, row.get(from));
			row.remove(from);
		}
	}
	
	// [key-value, key-column, value-column]
	private static void key_value(@NotNull List<Map<String, String>> data, @NotNull List<String> command) {
		if (command.size() < 3) {
			WebStats.logger.log(Level.WARNING, "Conversion command 'key-value' needs 2 arguments");
			return;
		}
		for (Map<String, String> row : data) {
			String key = command.get(1), value = command.get(2);
			row.put(row.get(key), row.get(value));
			row.remove(key);
			row.remove(value);
		}
	}
	
	// [json, column]
	private static void json(@NotNull List<Map<String, String>> data, @NotNull List<String> command) {
		if (command.size() < 2) {
			WebStats.logger.log(Level.WARNING, "Conversion command 'json' needs 1 argument");
			return;
		}
		Type collectionType = new TypeToken<Map<String, String>>() {}.getType();
		Gson gson = new Gson();
		String column = command.get(1);
		for (Map<String, String> row : data) {
			try {
				row.putAll(gson.fromJson(row.get(column), collectionType));
			} catch (JsonSyntaxException e) {
				WebStats.logger.log(Level.WARNING, "Could not decode json data", e);
			}
			row.remove(column);
		}
	}
	
	// [uuid, column]
	private static void uuid(@NotNull List<Map<String, String>> data, @NotNull List<String> command) {
		if (command.size() < 2) {
			WebStats.logger.log(Level.WARNING, "Conversion command 'uuid' needs 1 argument");
			return;
		}
		for (Map<String, String> row : data) {
			String column = command.get(1);
			UUID uuid = UUID.fromString(row.get(column));
			String name = Bukkit.getOfflinePlayer(uuid).getName();
			if (name == null) name = getPlayerNameOnline(uuid);
			if (name != null) {
				row.remove(column);
				row.put("player", name);
			} else {
				WebStats.logger.log(Level.WARNING, "Unable to get player name for UUID " + uuid.toString());
			}
		}
	}
	
	private static @Nullable String getPlayerNameOnline(@NotNull UUID uuid) {
		try {
			URL url = new URL("https://api.mojang.com/user/profiles/" + uuid.toString() + "/names");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			// Data comes in from mojang API so un-checked "getAs..." *should* not result in exceptions?
			// Structure: array of objects containing fields "name" and optionally "changedToAt"
			JsonArray names = new JsonParser()
					.parse(new InputStreamReader(conn.getInputStream()))
					.getAsJsonArray();
			return names.get(names.size() - 1).getAsJsonObject().get("name").getAsString();
		} catch (IOException e) {
			WebStats.logger.log(Level.WARNING, "Unable to retrieve player name from Mojang API", e);
		}
		return null;
	}
	
}
