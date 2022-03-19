package nl.dantevg.webstats.discordwebhook;

import com.google.gson.Gson;
import nl.dantevg.webstats.StatData;
import nl.dantevg.webstats.Stats;
import nl.dantevg.webstats.WebStats;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class DiscordWebhook implements Runnable {
	private static final String MESSAGE_ID_FILENAME = "discord-message-id.txt";
	private static final String WEBSTATS_ICON_URL = "https://raw.githubusercontent.com/Dantevg/WebStats/discord-webhook/img/icon-largemargins-96.png";
	
	private final WebStats plugin;
	
	private final @NotNull URL baseURL;
	private final int displayCount;
	private final @NotNull List<EmbedConfig> embeds = new ArrayList<>();
	
	private final DiscordMessage message = new DiscordMessage("WebStats", WEBSTATS_ICON_URL);
	
	public DiscordWebhook(WebStats plugin) throws InvalidConfigurationException {
		WebStats.logger.log(Level.INFO, "Enabling Discord webhook");
		this.plugin = plugin;
		
		ConfigurationSection config = WebStats.config.getConfigurationSection("discord-webhook");
		if (config == null) {
			throw new InvalidConfigurationException("discord-webhook must be a config section");
		}
		
		try {
			baseURL = new URL(config.getString("url", ""));
		} catch (MalformedURLException e) {
			throw new InvalidConfigurationException(e);
		}
		displayCount = config.getInt("display-count", 10);
		for (Map<?, ?> embed : config.getMapList("embeds")) {
			embeds.add(new EmbedConfig(embed));
		}
		
		loadMessageID();
		
		int updateInterval = config.getInt("update-interval", 5);
		if (updateInterval > 0) {
			long delayTicks = 0;
			long periodTicks = (long) updateInterval * 20 * 60; // assume 20 tps
			Bukkit.getScheduler().runTaskTimer(plugin, this,
					delayTicks, periodTicks);
		}
	}
	
	@Override
	public void run() {
		WebStats.logger.log(Level.CONFIG, "Sending Discord webhook update");
		final StatData.Stats stats = Stats.getStats();
		
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			List<String> entries = new ArrayList<>(stats.entries);
			
			message.removeEmbeds();
			
			// Fill message
			if (embeds.isEmpty()) {
				// Add one default embed
				List<String> columns = (stats.columns != null)
						? stats.columns
						: stats.scores.keySet().stream().sorted().collect(Collectors.toList());
				sortEntries(entries, null, SortDirection.DESCENDING);
				message.addEmbed(makeEmbed(stats, columns, entries));
			} else {
				// Add embeds according to config
				for (EmbedConfig embedConfig : embeds) {
					Map<String, String> columnToSortBy = stats.scores.get(embedConfig.sortColumn);
					sortEntries(entries, columnToSortBy, embedConfig.sortDirection);
					DiscordEmbed embed = makeEmbed(stats, embedConfig.columns, entries);
					if (embedConfig.title != null) embed.title = embedConfig.title;
					message.addEmbed(embed);
				}
			}
			
			DiscordEmbed lastEmbed = message.embeds.get(message.embeds.size() - 1);
			lastEmbed.timestamp = Instant.now().toString();
			String serverStatus = Bukkit.getServer().getOnlinePlayers().size() + " online";
			lastEmbed.footer = new DiscordEmbed.EmbedFooter(serverStatus);
			
			// Send message
			try {
				if (message.id != null) {
					editMessage(message);
				} else {
					sendMessage(message);
				}
			} catch (IOException e) {
				WebStats.logger.log(Level.WARNING, "Could not send webhook message", e);
			}
		});
	}
	
	public void disable() {
		if (message.id == null) return;
		
		DiscordEmbed lastEmbed = message.embeds.get(message.embeds.size() - 1);
		lastEmbed.timestamp = Instant.now().toString();
		lastEmbed.footer = new DiscordEmbed.EmbedFooter("offline");
		try {
			editMessage(message);
		} catch (IOException e) {
			WebStats.logger.log(Level.WARNING, "Could not send webhook message", e);
		}
	}
	
	private void loadMessageID() {
		File file = new File(plugin.getDataFolder(), MESSAGE_ID_FILENAME);
		try (Scanner scanner = new Scanner(file)) {
			message.id = scanner.nextLine();
			WebStats.logger.log(Level.INFO, "Loaded " + MESSAGE_ID_FILENAME);
		} catch (FileNotFoundException e) {
			WebStats.logger.log(Level.WARNING,
					MESSAGE_ID_FILENAME + " not present, creating a new message");
		}
	}
	
	private void storeMessageID() {
		if (message.id == null) return;
		File file = new File(plugin.getDataFolder(), MESSAGE_ID_FILENAME);
		plugin.getDataFolder().mkdirs();
		try (FileWriter writer = new FileWriter(file)) {
			writer.write(message.id);
			WebStats.logger.log(Level.INFO, "Saved " + MESSAGE_ID_FILENAME);
		} catch (IOException e) {
			WebStats.logger.log(Level.WARNING,
					"Could not write to " + MESSAGE_ID_FILENAME, e);
		}
	}
	
	private @NotNull DiscordEmbed makeEmbed(StatData.@NotNull Stats stats, @NotNull List<String> columns, @NotNull List<String> entries) {
		// Filter out empty rows
		List<String> nonEmptyEntries = entries.stream()
				.filter(entry -> columns.stream()
						.map(stats.scores::get)
						.filter(Objects::nonNull)
						.anyMatch(column -> column.get(entry) != null
								&& !column.get(entry).trim().isEmpty()))
				.collect(Collectors.toList());
		
		DiscordEmbed embed = new DiscordEmbed();
		embed.addField(new DiscordEmbed.EmbedField(
				"Player",
				nonEmptyEntries.stream()
						.limit(displayCount)
						.collect(Collectors.joining("\n")),
				true));
		
		for (String columnName : columns) {
			Map<String, String> column = stats.scores.get(columnName);
			if (column == null) continue;
			
			String values = nonEmptyEntries.stream()
					.limit(displayCount)
					.map((entryName) -> column.get(entryName) != null
							? column.get(entryName) : "")
					.collect(Collectors.joining("\n"));
			
			embed.addField(new DiscordEmbed.EmbedField(columnName, values, true));
		}
		
		return embed;
	}
	
	private void sendMessage(@NotNull DiscordMessage message) throws IOException {
		URL url = new URL(baseURL + "?wait=true");
		send(new HttpPost(url.toString()), message);
	}
	
	private void editMessage(@NotNull DiscordMessage message) throws IOException {
		URL url = new URL(baseURL + "/messages/" + message.id);
		send(new HttpPatch(url.toString()), message);
	}
	
	private void send(@NotNull HttpEntityEnclosingRequestBase request, @NotNull DiscordMessage message) throws IOException {
		try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {
			request.setHeader("Content-Type", "application/json; charset=UTF-8");
			request.setEntity(new StringEntity(new Gson().toJson(message)));
			
			try (final CloseableHttpResponse response = httpClient.execute(request)) {
				HttpEntity responseEntity = response.getEntity();
				if (responseEntity == null) {
					WebStats.logger.log(Level.WARNING, "Got no response content from Discord");
					return;
				}
				int status = response.getStatusLine().getStatusCode();
				InputStreamReader input = new InputStreamReader(responseEntity.getContent());
				handleResponse(message, input, status);
			}
		}
	}
	
	private void handleResponse(@NotNull DiscordMessage message, @NotNull InputStreamReader input, int status) throws IOException {
		if (isStatusCodeOk(status)) {
			if (message.id == null) {
				DiscordMessage responseMessage = new Gson().fromJson(input, DiscordMessage.class);
				message.id = responseMessage.id;
				storeMessageID();
			}
		} else {
			String response = new BufferedReader(input).lines().collect(Collectors.joining());
			DiscordError error = new Gson().fromJson(response, DiscordError.class);
			if (status == 404 && error.message.equals("Unknown Message")) {
				// Probably the message got deleted, so create a new one
				message.id = null;
				WebStats.logger.log(Level.WARNING,
						"Got unknown message response (did the message get deleted?), creating new message");
				sendMessage(message);
			} else {
				WebStats.logger.log(Level.WARNING, "Got HTTP code " + status + " response from Discord");
				WebStats.logger.log(Level.WARNING, response);
			}
		}
	}
	
	private static void sortEntries(List<String> entries, Map<String, String> column, SortDirection direction) {
		entries.sort((aRow, bRow) -> {
			String a, b;
			if (column == null) {
				// Null column means just sort player names
				a = aRow;
				b = bRow;
			} else {
				a = column.get(aRow);
				b = column.get(bRow);
			}
			
			try {
				// Handle null (because Double.parseDouble cannot handle null)
				if (a == null && b == null) return 0;
				else if (a == null) return -direction.toInt();
				else if (b == null) return direction.toInt();
				
				// Try to convert a and b to numbers
				double aNum = Double.parseDouble(a);
				double bNum = Double.parseDouble(b);
				return direction.toInt() * Double.compare(aNum, bNum);
			} catch (NumberFormatException e) {
				// a or b were not numbers, compare as strings
				return direction.toInt() * a.compareToIgnoreCase(b);
			}
		});
	}
	
	private static boolean isStatusCodeOk(int status) {
		return status >= 200 && status < 300;
	}
	
	private static class EmbedConfig {
		public final String title;
		public final @NotNull String sortColumn;
		public final @NotNull SortDirection sortDirection;
		public final @NotNull List<String> columns;
		
		public EmbedConfig(Map<?, ?> map) {
			this.title = (String) map.get("title");
			String sortColumn = (String) map.get("sort-column");
			this.sortColumn = (sortColumn != null) ? sortColumn : "Player";
			String sortDirection = (String) map.get("sort-direction");
			this.sortDirection = SortDirection.fromString(sortDirection, SortDirection.DESCENDING);
			List<String> columns = (List<String>) map.get("columns");
			this.columns = (columns != null) ? columns : new ArrayList<>();
		}
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
		
		public int toInt() {
			return (this == SortDirection.DESCENDING ? -1 : 1);
		}
	}
	
}
