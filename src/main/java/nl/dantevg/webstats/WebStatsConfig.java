package nl.dantevg.webstats;

import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class WebStatsConfig {
	private static WebStatsConfig instance;
	
	public final int port;
	public final boolean useScoreboardSource;
	public final boolean useDatabaseSource;
	public final boolean usePlaceholderSource;
	public final boolean useDiscordWebhook;
	
	public final boolean storePlayerIPs;
	
	public final boolean serveWebpage;
	
	@Deprecated
	public final @Nullable List<String> columns;
	
	public final @NotNull List<String> serverColumns;
	public final @NotNull List<TableConfig> tables;
	
	private WebStatsConfig() {
		port = WebStats.config.getInt("port");
		
		useScoreboardSource = WebStats.config.contains("objectives");
		useDatabaseSource = WebStats.config.contains("database.config");
		usePlaceholderSource = WebStats.config.contains("placeholders");
		useDiscordWebhook = WebStats.config.contains("discord-webhook");
		
		storePlayerIPs = WebStats.config.getBoolean("store-player-ips");
		
		serveWebpage = WebStats.config.getBoolean("serve-webpage");
		
		columns = WebStats.config.getStringList("columns");
		serverColumns = WebStats.config.getStringList("server-columns");
		
		if (!WebStats.config.contains("tables")) {
			tables = Collections.emptyList();
		} else if (WebStats.config.getMapList("tables").isEmpty()) {
			tables = Arrays.asList(new TableConfig(null, null, null, null));
		} else {
			tables = WebStats.config.getMapList("tables").stream()
					.map(TableConfig::new)
					.collect(Collectors.toList());
		}
	}
	
	public static WebStatsConfig getInstance(boolean forceNew) {
		if (instance == null || forceNew) instance = new WebStatsConfig();
		return instance;
	}
	
	public static WebStatsConfig getInstance() {
		return getInstance(false);
	}
	
	public static class TableConfig {
		public final @Nullable String name;
		public final @Nullable List<String> columns;
		public final @Nullable String sortColumn;
		public final @Nullable SortDirection sortDirection;
		
		public TableConfig(@Nullable String name,
		                   @Nullable List<String> columns,
		                   @Nullable String sortColumn,
		                   @Nullable SortDirection sortDirection) {
			this.name = name;
			this.columns = columns;
			this.sortColumn = sortColumn;
			this.sortDirection = sortDirection;
		}
		
		public TableConfig(Map<?, ?> map) {
			this.name = (String) map.get("name");
			String sortColumn = (String) map.get("sort-column");
			this.sortColumn = (sortColumn != null) ? sortColumn : "Player";
			String sortDirection = (String) map.get("sort-direction");
			this.sortDirection = SortDirection.fromString(sortDirection, SortDirection.DESCENDING);
			List<String> columns = (List<String>) map.get("columns");
			this.columns = (columns != null) ? columns : new ArrayList<>();
		}
		
		@Override
		public String toString() {
			return new Gson().toJson(this);
		}
		
	}
	
	public enum SortDirection {
		ASCENDING, DESCENDING;
		
		public static @NotNull SortDirection fromString(@NotNull String direction, @NotNull SortDirection def) {
			try {
				return SortDirection.valueOf(direction.toUpperCase());
			} catch (IllegalArgumentException e) {
				WebStats.logger.log(Level.WARNING, "Invalid direction value '" + direction + "', using default");
				return def;
			}
		}
		
		public int toInt() {
			return (this == SortDirection.DESCENDING ? -1 : 1);
		}
	}
	
}