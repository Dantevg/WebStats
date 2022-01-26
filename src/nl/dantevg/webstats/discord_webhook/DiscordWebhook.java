package nl.dantevg.webstats.discord_webhook;

import com.google.gson.Gson;
import nl.dantevg.webstats.StatData;
import nl.dantevg.webstats.Stats;
import nl.dantevg.webstats.WebStats;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class DiscordWebhook implements Runnable {
	private final WebStats plugin;
	
	private final @NotNull URL url;
	private final int updateInterval;
	private final @NotNull String sortColumn;
	private final @NotNull SortDirection sortDirection;
	private final int displayCount;
	private final @NotNull List<List<String>> embeds = new ArrayList<>();
	
	public DiscordWebhook(WebStats plugin) throws MalformedURLException {
		this.plugin = plugin;
		
		ConfigurationSection config = WebStats.config.getConfigurationSection("discord-webhook");
		
		url = new URL(config.getString("url"));
		updateInterval = config.getInt("update-interval", 10);
		sortColumn = config.getString("sort-column", "Player");
		sortDirection = SortDirection.fromString(
				config.getString("sort-direction", ""),
				SortDirection.DESCENDING);
		displayCount = config.getInt("display-count", 10);
		if (config.contains("columns")) {
			embeds.addAll((List<List<String>>) config.getList("columns"));
		}
		
		if (updateInterval > 0) {
			long delayTicks = 0;
			long periodTicks = updateInterval * 20L; // assume 20 tps
			Bukkit.getScheduler().runTaskTimer(plugin, this,
					delayTicks, periodTicks);
		}
	}
	
	@Override
	public void run() {
		WebStats.logger.log(Level.INFO, "Sending Discord webhook update");
		final StatData.Stats stats = Stats.getStats();
		
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			// Sort entries
			Map<String, Object> columnToSortBy = stats.scores.get(sortColumn);
			List<String> entries = new ArrayList<>(stats.entries);
			entries.sort((aRow, bRow) -> {
				String a = (String) columnToSortBy.get(aRow);
				String b = (String) columnToSortBy.get(bRow);
				
				try {
					// Try to convert a and b to numbers
					double aNum = Double.parseDouble(a);
					double bNum = Double.parseDouble(b);
					return (sortDirection == SortDirection.DESCENDING ? -1 : 1)
							* Double.compare(aNum, bNum);
				} catch (NumberFormatException e) {
					// a or b were not numbers, compare as strings
					return a.compareToIgnoreCase(b);
				}
			});
			
			// Create message
			Message message = new Message();
			if (embeds.isEmpty()) {
				List<String> columns = (stats.columns != null)
						? stats.columns
						: stats.scores.keySet().stream().sorted().collect(Collectors.toList());
				message.addEmbed(makeEmbed(stats, columns, entries));
			} else {
				for (List<String> columns : embeds) {
					message.addEmbed(makeEmbed(stats, columns, entries));
				}
			}
			
			// Send message
			try {
				send(message);
			} catch (IOException e) {
				WebStats.logger.log(Level.WARNING, "Could not send webhook message", e);
			}
		});
	}
	
	private @NotNull Embed makeEmbed(StatData.@NotNull Stats stats, @NotNull List<String> columns, @NotNull List<String> entries) {
		Embed embed = new Embed();
		embed.addField(new Embed.EmbedField("Player",
				String.join("\n", entries), true));
		
		for (String columnName : columns) {
			Map<String, Object> column = stats.scores.get(columnName);
			
			List<String> values = entries.stream()
					.limit(displayCount)
					.map((entryName) -> column.get(entryName) != null
							? (String) column.get(entryName) : "")
					.collect(Collectors.toList());
			
			embed.addField(new Embed.EmbedField(columnName,
					String.join("\n", values), true));
		}
		
		return embed;
	}
	
	private void send(Message message) throws IOException {
		HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
		conn.setRequestMethod("POST");
		conn.setDoOutput(true);
		conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
		
		conn.connect();
		try (OutputStream output = conn.getOutputStream()) {
			output.write(new Gson().toJson(message).getBytes());
		}
		
		if (conn.getResponseCode() < 200 || conn.getResponseCode() >= 300) {
			WebStats.logger.log(Level.WARNING, "Got !=2XX HTTP code " + conn.getResponseCode() + " from Discord");
		}
		conn.getInputStream().close();
		conn.disconnect();
	}
	
	private enum SortDirection {
		ASCENDING, DESCENDING;
		
		public static @NotNull SortDirection fromString(@NotNull String direction, @NotNull SortDirection def) {
			try {
				return SortDirection.valueOf(direction.toUpperCase());
			} catch (IllegalArgumentException e) {
				WebStats.logger.log(Level.WARNING, "Invalid direction value '" + direction + "', using default");
				return def;
			}
		}
	}
	
}
