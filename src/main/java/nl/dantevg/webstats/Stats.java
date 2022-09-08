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
	
	public static @NotNull StatData.Stats getStats(@Nullable String table) {
		EntriesScores entriesScores = new EntriesScores();
		
		if (WebStats.scoreboardSource != null) entriesScores.add(WebStats.scoreboardSource.getStats());
		if (WebStats.databaseSource != null) entriesScores.add(WebStats.databaseSource.getStats());
		if (WebStats.placeholderSource != null) entriesScores.add(WebStats.placeholderSource.getStats());
		
		if (WebStats.config.contains("server-columns")) entriesScores.entries.add("#server");
		
		if (WebStats.config.contains("tables")) {
			Map<?, ?> tableConfig = getTableConfig(table);
			if (tableConfig == null) {
				return new StatData.Stats(entriesScores, Collections.emptyList());
			}
			List<String> columns = (List<String>) tableConfig.get("columns");
			String sortBy = (String) tableConfig.get("sort-by");
			Boolean sortDescending = (Boolean) tableConfig.get("sort-descending");
			return new StatData.Stats(entriesScores, columns, sortBy, sortDescending);
		} else {
			return new StatData.Stats(entriesScores, WebStats.config.getStringList("columns"));
		}
	}
	
	public static @NotNull List<String> getTables() {
		if (!WebStats.config.contains("tables")) return Collections.emptyList();
		
		List<String> tables = new ArrayList<>();
		for (Map<?, ?> tableConfig : WebStats.config.getMapList("tables")) {
			tables.add((String) tableConfig.get("name"));
		}
		
		return tables;
	}
	
	public static @NotNull StatData getAll(@Nullable String table) {
		return new StatData(getOnline(), getStats(table));
	}
	
	public static @NotNull StatData getAll(@Nullable String table, @NotNull InetAddress ip) {
		Set<String> playernames = WebStats.playerIPStorage.getNames(ip);
		return new StatData(getOnline(), getStats(table), playernames);
	}
	
	private static Map<?, ?> getTableConfig(String table) {
		if (WebStats.config.contains("tables")) {
			List<Map<?, ?>> tablesConfig = WebStats.config.getMapList("tables");
			for (Map<?, ?> tableConfig : tablesConfig) {
				String tableName = (String) tableConfig.get("name");
				if (table == null || tableName.equals(table)) {
					return tableConfig;
				}
			}
		}
		return null;
	}
	
}
