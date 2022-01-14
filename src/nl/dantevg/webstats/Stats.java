package nl.dantevg.webstats;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

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
	
	public static @NotNull StatData.Stats getStats() {
		EntriesScores entriesScores = new EntriesScores();
		
		if (WebStats.scoreboardSource != null) entriesScores.add(WebStats.scoreboardSource.getStats());
		if (WebStats.databaseSource != null) entriesScores.add(WebStats.databaseSource.getStats());
		if (WebStats.placeholderSource != null) entriesScores.add(WebStats.placeholderSource.getStats());
		
		if (WebStats.config.contains("columns")) {
			List<String> columns = new ArrayList<>(WebStats.config.getStringList("columns"));
			return new StatData.Stats(entriesScores, columns);
		} else {
			return new StatData.Stats(entriesScores);
		}
	}
	
	public static @NotNull StatData getAll() {
		return new StatData(getOnline(), getStats());
	}
	
	public static @NotNull StatData getAll(InetAddress ip) {
		Set<String> playernames = WebStats.playerIPStorer.getNames(ip);
		return new StatData(getOnline(), getStats(), playernames);
	}
	
}
