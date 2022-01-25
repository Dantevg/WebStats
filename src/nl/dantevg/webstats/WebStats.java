package nl.dantevg.webstats;

import com.sun.net.httpserver.HttpServer;
import nl.dantevg.webstats.database.DatabaseSource;
import nl.dantevg.webstats.discord_webhook.DiscordWebhook;
import nl.dantevg.webstats.placeholder.PlaceholderSource;
import nl.dantevg.webstats.scoreboard.ScoreboardSource;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WebStats extends JavaPlugin {
	protected static ScoreboardSource scoreboardSource;
	protected static DatabaseSource databaseSource;
	protected static PlaceholderSource placeholderSource;
	protected static DiscordWebhook discordWebhook;
	
	protected static PlayerIPStorage playerIPStorage;
	
	public static Logger logger;
	public static FileConfiguration config;
	public static boolean hasEssentials;
	
	private HttpServer webserver;
	
	// Gets run when the plugin is enabled on server startup
	@Override
	public void onEnable() {
		logger = getLogger();
		config = getConfig();
		
		hasEssentials = Bukkit.getPluginManager().getPlugin("Essentials") != null;
		
		playerIPStorage = new PlayerIPStorage(this);
		
		// Config
		saveDefaultConfig();
		int port = config.getInt("port");
		
		// Register debug command
		CommandWebstats command = new CommandWebstats(this);
		getCommand("webstats").setExecutor(command);
		getCommand("webstats").setTabCompleter(command);
		
		// Enable sources
		if (config.contains("objectives")) scoreboardSource = new ScoreboardSource();
		if (config.contains("database.config")) {
			try {
				databaseSource = new DatabaseSource();
			} catch (InvalidConfigurationException e) {
				logger.log(Level.SEVERE, "Invalid database configuration", e);
			}
		}
		if (config.contains("placeholders")) {
			if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
				try {
					placeholderSource = new PlaceholderSource();
				} catch (InvalidConfigurationException e) {
					logger.log(Level.SEVERE, "Invalid placeholder configuration", e);
				}
			} else {
				logger.log(Level.WARNING, "PlaceholderAPI not present but config contains placeholders (comment to remove this warning)");
			}
		}
		
		// Start Discord webhook
		if (config.contains("discord-webhook-url")) {
			try {
				discordWebhook = new DiscordWebhook();
			} catch (MalformedURLException e) {
				logger.log(Level.SEVERE, "Malformed Discord webhook url", e);
			}
		}
		
		try {
			// Start web server
			webserver = HttpServer.create(new InetSocketAddress(port), 0);
			webserver.createContext("/", new HTTPRequestHandler());
			webserver.start();
			logger.log(Level.INFO, "Web server started on port " + port);
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Failed to start web server with port "
					+ port + ": " + e.getMessage(), e);
		}
	}
	
	// Gets run when the plugin is disabled on server stop
	@Override
	public void onDisable() {
		// Stop web server
		if (webserver != null) {
			logger.log(Level.INFO, "Stopping web server");
			// Do not wait for web server to stop, because it will always take
			// the max amount of time regardless
			webserver.stop(0);
			webserver = null;
		}
		
		// Let sources close connections
		if (databaseSource != null) databaseSource.disable();
		if (placeholderSource != null) placeholderSource.disable();
		playerIPStorage.disable();
	}
	
	void reload() {
		logger.log(Level.INFO, "Reload: disabling plugin");
		setEnabled(false);
		logger.log(Level.INFO, "Reload: re-enabling plugin");
		setEnabled(true);
		logger.log(Level.INFO, "Reload complete");
	}
	
	private @NotNull String getVersion() {
		return "WebStats " + getDescription().getVersion();
	}
	
	private @NotNull String getSources() {
		List<String> sources = new ArrayList<>();
		if (WebStats.scoreboardSource != null) sources.add("scoreboard");
		if (WebStats.placeholderSource != null) sources.add("PlaceholderAPI");
		if (WebStats.databaseSource != null) sources.add("database");
		return "Active sources: " + String.join(", ", sources);
	}
	
	protected @NotNull String debug() {
		return getVersion() + "\n"
				+ getSources();
	}
	
}
