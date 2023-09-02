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
			if (!WebStats.hasEssentials || !EssentialsHelper.isVanished(p)) {
				players.put(p.getName(), (WebStats.hasEssentials && EssentialsHelper.isAFK(p)) ? "afk" : true);
			}
		}
		return players;
	}
	
	public static @NotNull StatData.Stats getStats() {
		EntriesScores entriesScores = new EntriesScores();
		
		if (WebStats.scoreboardSource != null) entriesScores.add(WebStats.scoreboardSource.getStats());
		if (WebStats.databaseSource != null) entriesScores.add(WebStats.databaseSource.getStats());
		if (WebStats.placeholderSource != null) entriesScores.add(WebStats.placeholderSource.getStats());
		
		if (!WebStatsConfig.getInstance().serverColumns.isEmpty()) entriesScores.entries.add("#server");
		
		// For backwards-compatibility with older web front-ends
		List<String> defaultColumns = getDefaultColumns();
		if (defaultColumns != null) {
			return new StatData.Stats(entriesScores, defaultColumns);
		} else {
			return new StatData.Stats(entriesScores);
		}
	}
	
	public static @NotNull StatData getAll() {
		return new StatData(getOnline(), getStats());
	}
	
	public static @NotNull StatData getAll(@NotNull InetAddress ip) {
		Set<String> playernames = WebStats.playerIPStorage.getNames(ip);
		return new StatData(getOnline(), getStats(), playernames);
	}
	
	private static @Nullable List<String> getDefaultColumns() {
		WebStatsConfig webStatsConfig = WebStatsConfig.getInstance();
		// Need to check for `columns` before `tables`, because `tables` will
		// always be present in the default (in-jar) config.
		if (webStatsConfig.columns != null) {
			// Old config
			return webStatsConfig.columns;
		} else if (WebStats.config.contains("tables", true)) {
			// New config
			if (webStatsConfig.tables.isEmpty()) {
				return Collections.emptyList();
			} else {
				return webStatsConfig.tables.get(0).columns;
			}
		}
		return null;
	}
	
}
