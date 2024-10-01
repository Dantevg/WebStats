package nl.dantevg.webstats;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ExecutionException;

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
	
	public static @NotNull StatData getAll(@NotNull InetAddress ip) throws InterruptedException {
		try {
			// Stats need to be gathered on the main thread,
			// see https://github.com/Dantevg/WebStats/issues/52
			StatData.Stats stats = Bukkit.getScheduler().callSyncMethod(
					WebStats.getPlugin(WebStats.class),
					Stats::getStats).get();
			Set<String> playernames = WebStats.playerIPStorage.getNames(ip);
			Map<String, String> skins = (WebStats.skinsRestorerHelper != null)
					? WebStats.skinsRestorerHelper.getSkinIDsForPlayers(stats.entries)
					: null;
			return new StatData(getOnline(), stats, playernames, skins);
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
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
