package nl.dantevg.webstats.discordwebhook;

import nl.dantevg.webstats.WebStats;
import nl.dantevg.webstats.WebStatsConfig;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DiscordConfig {
	private static DiscordConfig instance;
	
	public final @NotNull URL url;
	public final int updateInterval;
	public final int displayCount;
	public final @Nullable String title;
	public final boolean doOverrideIconAndName;
	public final @NotNull List<EmbedConfig> embeds = new ArrayList<>();
	
	private DiscordConfig() throws InvalidConfigurationException {
		ConfigurationSection config = WebStats.config.getConfigurationSection("discord-webhook");
		if (config == null) {
			throw new InvalidConfigurationException("discord-webhook must be a config section");
		}
		
		try {
			url = new URL(config.getString("url", ""));
		} catch (MalformedURLException e) {
			throw new InvalidConfigurationException(e);
		}
		updateInterval = config.getInt("update-interval", 5);
		displayCount = config.getInt("display-count", 10);
		title = config.getString("title");
		doOverrideIconAndName = config.getBoolean("override-icon-and-name", true);
		for (Map<?, ?> embed : config.getMapList("embeds")) {
			embeds.add(new EmbedConfig(embed));
		}
	}
	
	public static DiscordConfig getInstance(boolean forceNew) throws InvalidConfigurationException {
		if (instance == null || forceNew) instance = new DiscordConfig();
		return instance;
	}
	
	public static DiscordConfig getInstance() throws InvalidConfigurationException {
		return getInstance(false);
	}
	
	static class EmbedConfig {
		public final String title;
		public final @NotNull List<String> columns;
		public final @NotNull String sortColumn;
		public final @NotNull WebStatsConfig.SortDirection sortDirection;
		
		public EmbedConfig(Map<?, ?> map) {
			this.title = (String) map.get("title");
			String sortColumn = (String) map.get("sort-column");
			this.sortColumn = (sortColumn != null) ? sortColumn : "Player";
			String sortDirection = (String) map.get("sort-direction");
			this.sortDirection = WebStatsConfig.SortDirection.fromString(sortDirection, WebStatsConfig.SortDirection.DESCENDING);
			List<String> columns = (List<String>) map.get("columns");
			this.columns = (columns != null) ? columns : new ArrayList<>();
		}
	}
	
}
