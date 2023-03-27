package nl.dantevg.webstats.placeholder;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import me.clip.placeholderapi.PlaceholderAPI;
import nl.dantevg.webstats.EntriesScores;
import nl.dantevg.webstats.EssentialsHelper;
import nl.dantevg.webstats.WebStats;
import nl.dantevg.webstats.WebStatsConfig;
import nl.dantevg.webstats.storage.StorageMethod;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class PlaceholderSource {
	final PlaceholderConfig config;
	
	private @Nullable PlaceholderStorage storage;
	
	public PlaceholderSource() throws InvalidConfigurationException {
		WebStats.logger.log(Level.INFO, "Enabling placeholder source");
		config = PlaceholderConfig.getInstance(true);
		
		if (config.storeInFile || config.storeInDatabase != null) {
			storage = new PlaceholderStorage(this);
			storage.prune(new HashSet<>(config.placeholders.values()));
		}
	}
	
	@NotNull Set<OfflinePlayerWithCachedName> getEntriesAsPlayers() {
		// Also get players from EssentialsX's userMap, for offline servers
		Set<OfflinePlayer> entries = (!Bukkit.getOnlineMode() && WebStats.hasEssentials)
				? EssentialsHelper.getOfflinePlayers() : null;
		if (entries == null) entries = new HashSet<>();
		entries.addAll(Arrays.asList(Bukkit.getOfflinePlayers()));
		return entries.stream().map(OfflinePlayerWithCachedName::new).collect(Collectors.toSet());
	}
	
	// Get up-to-date score for player, or stored one if the player is offline
	private @Nullable String getPlaceholderForPlayer(OfflinePlayerWithCachedName player, String placeholder, String placeholderName) {
		String name = player.getName();
		if (name == null) return null;
		
		// If the player is online, get the most up-to-date value.
		// If the player is offline, get the stored value if it is stored, because placeholder
		// plugins may just yield 0 (which is indistinguishable from a real score of 0.)
		String score = null;
		if (storage != null) score = storage.getScore(player.player.getUniqueId(), placeholderName);
		if (player.player.isOnline() || !isPlaceholderSet(placeholder, score)) {
			score = PlaceholderAPI.setPlaceholders(player.player, placeholder);
		}
		return isPlaceholderSet(placeholder, score) ? score : null;
	}
	
	private @Nullable String getPlaceholderForServer(String placeholder) {
		String score = PlaceholderAPI.setPlaceholders(null, placeholder);
		return isPlaceholderSet(placeholder, score) ? score : null;
	}
	
	private @NotNull Table<String, String, String> getScoresForPlayersAndServer(Set<OfflinePlayerWithCachedName> players) {
		Table<String, String, String> values = HashBasedTable.create();
		config.placeholders.forEach((placeholder, placeholderName) -> {
			if (WebStatsConfig.getInstance().serverColumns.contains(placeholderName)) {
				String score = getPlaceholderForServer(placeholder);
				if (score != null) values.put("#server", placeholderName, score);
			} else {
				for (OfflinePlayerWithCachedName player : players) {
					String score = getPlaceholderForPlayer(player, placeholder, placeholderName);
					// Only add the score if it is not empty
					if (score != null) values.put(player.getName(), placeholderName, score);
				}
			}
		});
		return values;
	}
	
	// This method should only be called when the offline storage is enabled
	private @NotNull Table<String, String, String> getOfflineScoresForPlayers(Set<OfflinePlayerWithCachedName> players) {
		Table<String, String, String> values = HashBasedTable.create();
		config.placeholders.forEach((placeholder, placeholderName) -> {
			if (!WebStatsConfig.getInstance().serverColumns.contains(placeholderName)) {
				for (OfflinePlayerWithCachedName player : players) {
					assert storage != null;
					String score = storage.getScore(player.player.getUniqueId(), placeholderName);
					// Only add the score if it is not empty
					if (score != null) values.put(player.getName(), placeholderName, score);
				}
			}
		});
		return values;
	}
	
	// Get scores for single player from PlaceholderAPI
	// This method does NOT try to find stored scores from PlaceholderStorage
	@NotNull Map<String, String> getScoresForPlayer(@NotNull OfflinePlayerWithCachedName player) {
		Map<String, String> scores = new HashMap<>();
		String name = player.getName();
		if (name == null) return scores;
		
		config.placeholders.forEach((placeholder, placeholderName) -> {
			String score = PlaceholderAPI.setPlaceholders(player.player, placeholder);
			if (isPlaceholderSet(placeholder, score)) scores.put(placeholderName, score);
		});
		
		return scores;
	}
	
	/**
	 * This method will be called asynchronously
	 */
	public @NotNull Future<EntriesScores> getStats() {
		if (this.storage == null) return getStatsAllSynchronous();
		
		CompletableFuture<Set<OfflinePlayerWithCachedName>> offlinePlayersFuture = new CompletableFuture<>();
		CompletableFuture<EntriesScores> onlineEntriesScoresFuture = new CompletableFuture<>();
		
		CompletableFuture<Table<String, String, String>> offlineScoresFuture =
				offlinePlayersFuture.thenApply(this::getOfflineScoresForPlayers);
		
		Bukkit.getScheduler().runTask(WebStats.getPlugin(WebStats.class), () -> {
			Set<OfflinePlayerWithCachedName> players = getEntriesAsPlayers();
			Set<String> playernames = players.stream()
					.map(OfflinePlayerWithCachedName::getName)
					.filter(Objects::nonNull)
					.collect(Collectors.toSet());
			Set<OfflinePlayerWithCachedName> onlinePlayers = players.stream()
					.filter(player -> player.player.isOnline())
					.collect(Collectors.toSet());
			players.removeAll(onlinePlayers);
			offlinePlayersFuture.complete(players);
			onlineEntriesScoresFuture.complete(new EntriesScores(playernames, getScoresForPlayersAndServer(onlinePlayers)));
		});
		
		return onlineEntriesScoresFuture.thenCombine(offlineScoresFuture, ((entriesScores, offlineScores) -> {
			entriesScores.scores.putAll(offlineScores);
			return entriesScores;
		}));
	}
	
	// Gather all placeholders on the main server thread because there is no
	// offline storage
	private @NotNull Future<EntriesScores> getStatsAllSynchronous() {
		return Bukkit.getScheduler().callSyncMethod(WebStats.getPlugin(WebStats.class), () -> {
			Set<OfflinePlayerWithCachedName> players = getEntriesAsPlayers();
			Set<String> playernames = players.stream()
					.map(OfflinePlayerWithCachedName::getName)
					.filter(Objects::nonNull)
					.collect(Collectors.toSet());
			return new EntriesScores(playernames, getScoresForPlayersAndServer(players));
		});
	}
	
	public void disable() {
		if (storage != null) storage.disable();
	}
	
	public void migrateStorage(Class<? extends StorageMethod> to) {
		storage.migrate(to);
	}
	
	public static boolean isPlaceholderSet(String placeholder, @Nullable String value) {
		return value != null
				&& !value.equals("")
				&& !value.equalsIgnoreCase(placeholder);
	}
	
	public @NotNull String debug() {
		return (storage != null) ? storage.debug() : "";
	}
	
	public static class OfflinePlayerWithCachedName {
		public final OfflinePlayer player;
		private final String name;
		
		public OfflinePlayerWithCachedName(OfflinePlayer player) {
			this.player = player;
			this.name = player.getName();
		}
		
		public String getName() {
			return name;
		}
	}
	
}
