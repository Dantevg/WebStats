package nl.dantevg.webstats;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;

public class Stats {
	public static @NotNull Map<String, Object> getOnline() {
		Map<String, Object> players = new HashMap<>();
		for (Player p : Bukkit.getOnlinePlayers()) {
			players.put(p.getName(), (WebStats.hasEssentials && EssentialsHelper.isAFK(p)) ? "afk" : true);
		}
		return players;
	}
	
	/**
	 * This method should be called asynchronously as to avoid straining
	 * the main server thread.
	 * <p>
	 * Some stats need to be gathered on the main thread, each source's getStats
	 * method should be designed to handle that.
	 * See https://github.com/Dantevg/WebStats/issues/52
	 */
	public static @NotNull StatData.Stats getStats() {
		EntriesScores entriesScores = new EntriesScores();
		
		List<Future<EntriesScores>> futures = new ArrayList<>();
		
		if (WebStats.scoreboardSource != null) futures.add(WebStats.scoreboardSource.getStats());
		if (WebStats.databaseSource != null) futures.add(WebStats.databaseSource.getStats());
		if (WebStats.placeholderSource != null) futures.add(WebStats.placeholderSource.getStats());
		
		for (Future<EntriesScores> future : futures) {
			try {
				entriesScores.add(future.get());
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException(e);
			}
		}
		
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
