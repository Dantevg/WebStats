package nl.dantevg.webstats;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

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
		
		if (WebStats.config.contains("columns")) {
			return new StatData.Stats(entriesScores, WebStats.config.getStringList("columns"));
		} else {
			return new StatData.Stats(entriesScores);
		}
	}
	
	public static @NotNull List<TableConfig> getTables() {
		if (!WebStats.config.contains("tables")) return Collections.emptyList();
		
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
	
	private static TableConfig getTableConfigFromMap(Map<?, ?> tableConfig) {
		String name = (String) tableConfig.get("name");
		List<String> columns = (List<String>) tableConfig.get("columns");
		String sortBy = (String) tableConfig.get("sort-by");
		Boolean sortDescending = (Boolean) tableConfig.get("sort-descending");
		return new TableConfig(name, columns, sortBy, sortDescending);
	}
	
}
