package nl.dantevg.webstats;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.util.*;
import java.util.stream.Collectors;

public class Stats {
	public static @NotNull Map<String, Object> getOnline() {
		Map<String, Object> players = new HashMap<>();
		for (Player p : Bukkit.getOnlinePlayers()) {
			players.put(p.getName(), (WebStats.hasEssentials && EssentialsHelper.isAFK(p)) ? "afk" : true);
		}
		return players;
	}
	
	public static @NotNull StatData.Stats getStats() {
		EntriesScores entriesScores = new EntriesScores();
		
		if (WebStats.scoreboardSource != null) entriesScores.add(WebStats.scoreboardSource.getStats());
		if (WebStats.databaseSource != null) entriesScores.add(WebStats.databaseSource.getStats());
		if (WebStats.placeholderSource != null) entriesScores.add(WebStats.placeholderSource.getStats());
		
		if (WebStats.config.contains("server-columns")) entriesScores.entries.add("#server");
		
		// For backwards-compatibility with older web front-ends
		List<String> defaultColumns = getDefaultColumns();
		if (defaultColumns != null) {
			return new StatData.Stats(entriesScores, defaultColumns);
		} else {
			return new StatData.Stats(entriesScores);
		}
	}
	
	public static @NotNull List<TableConfig> getTables() {
		if (!WebStats.config.contains("tables")) return Collections.emptyList();
		
		if (WebStats.config.getMapList("tables").isEmpty())
			return Arrays.asList(new TableConfig(null, null, null, null));
		
		return WebStats.config.getMapList("tables").stream()
				.map(Stats::getTableConfigFromMap)
				.collect(Collectors.toList());
	}
	
	public static @NotNull StatData getAll() {
		return new StatData(getOnline(), getStats());
	}
	
	public static @NotNull StatData getAll(@NotNull InetAddress ip) {
		Set<String> playernames = WebStats.playerIPStorage.getNames(ip);
		return new StatData(getOnline(), getStats(), playernames);
	}
	
	private static @Nullable List<String> getDefaultColumns() {
		// Need to check for `columns` before `tables`, because `tables` will
		// always be present in the default (in-jar) config.
		if (WebStats.config.contains("columns")) {
			// Old config
			return WebStats.config.getStringList("columns");
		} else if (WebStats.config.contains("tables")) {
			// New config
			List<Map<?, ?>> tableConfigs = WebStats.config.getMapList("tables");
			if (tableConfigs.isEmpty()) {
				return Collections.emptyList();
			} else {
				return getTableConfigFromMap(tableConfigs.get(0)).columns;
			}
		}
		return null;
	}
	
	private static @NotNull TableConfig getTableConfigFromMap(@NotNull Map<?, ?> tableConfig) {
		String name = (String) tableConfig.get("name");
		List<String> columns = (List<String>) tableConfig.get("columns");
		String sortBy = (String) tableConfig.get("sort-by");
		Boolean sortDescending = (Boolean) tableConfig.get("sort-descending");
		return new TableConfig(name, columns, sortBy, sortDescending);
	}
	
}
