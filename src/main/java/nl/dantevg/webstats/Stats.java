package nl.dantevg.webstats;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.util.*;

public class Stats {
	public static @NotNull Map<String, Object> getOnline() {
		Map<String, Object> players = new HashMap<>();
		for (Player p : Bukkit.getOnlinePlayers()) {
			players.put(p.getName(), (WebStats.hasEssentials && EssentialsHelper.isAFK(p)) ? "afk" : true);
		}
		return players;
	}
	
	public static @NotNull StatData.Stats getStats(String table) {
		EntriesScores entriesScores = new EntriesScores();
		
		if (WebStats.scoreboardSource != null) entriesScores.add(WebStats.scoreboardSource.getStats());
		if (WebStats.databaseSource != null) entriesScores.add(WebStats.databaseSource.getStats());
		if (WebStats.placeholderSource != null) entriesScores.add(WebStats.placeholderSource.getStats());
		
		if (WebStats.config.contains("server-columns")) entriesScores.entries.add("#server");
		
		List<String> columns = getColumnsByTableName(table);
		if (columns != null) {
			return new StatData.Stats(entriesScores, columns);
		} else {
			return new StatData.Stats(entriesScores);
		}
	}
	
	public static @NotNull StatData getAll(String table) {
		return new StatData(getOnline(), getStats(table));
	}
	
	public static @NotNull StatData getAll(String table, @NotNull InetAddress ip) {
		Set<String> playernames = WebStats.playerIPStorage.getNames(ip);
		return new StatData(getOnline(), getStats(table), playernames);
	}
	
	private static @Nullable List<String> getColumnsByTableName(@Nullable String table) {
		if (WebStats.config.contains("tables")) {
			List<Map<?, ?>> tablesConfig = WebStats.config.getMapList("tables");
			for (Map<?, ?> tableConfig : tablesConfig) {
				String tableName = (String) tableConfig.get("name");
				if (table == null || tableName.equals(table)) {
					return (List<String>) tableConfig.get("columns");
				}
			}
		}
		
		if (WebStats.config.contains("columns")) {
			return new ArrayList<>(WebStats.config.getStringList("columns"));
		} else {
			return null;
		}
	}
	
}
