package nl.dantevg.webstats;

import nl.dantevg.webstats.database.DatabaseSource;
import nl.dantevg.webstats.discordwebhook.DiscordWebhook;
import nl.dantevg.webstats.placeholder.PlaceholderSource;
import nl.dantevg.webstats.scoreboard.ScoreboardSource;
import nl.dantevg.webstats.webserver.WebServer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WebStats extends JavaPlugin {
	protected static ScoreboardSource scoreboardSource;
	protected static DatabaseSource databaseSource;
	protected static PlaceholderSource placeholderSource;
	
	protected static DiscordWebhook discordWebhook;
	protected static WebServer webserver;
	
	protected static PlayerIPStorage playerIPStorage;
	
	public static Logger logger;
	public static FileConfiguration config;
	public static boolean hasEssentials;
	
	// Gets run when the plugin is enabled on server startup
	@Override
	public void onEnable() {
		logger = getLogger();
		config = getConfig();
		
		hasEssentials = Bukkit.getPluginManager().getPlugin("Essentials") != null;
		
		playerIPStorage = new PlayerIPStorage(this);
		
		// Config
		saveDefaultConfig();
		
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
		
		if (config.contains("discord-webhook")) {
			try {
				discordWebhook = new DiscordWebhook(this);
			} catch (InvalidConfigurationException e) {
				logger.log(Level.SEVERE, "Invalid Discord webhook configuration", e);
			}
		}
		
		try {
			webserver = new WebServer();
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Failed to start web server with port "
					+ config.getInt("port") + ": " + e.getMessage(), e);
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
		if (discordWebhook != null) discordWebhook.disable();
	}
	
	void reload() {
		logger.log(Level.INFO, "Reload: disabling plugin");
		setEnabled(false);
		Bukkit.getScheduler().cancelTasks(this);
		logger.log(Level.INFO, "Reload: re-enabling plugin");
		reloadConfig();
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
	
	/**
	 * Get the input stream of a file in the plugin data folder, or in the jar
	 * if that does not exist.
	 * @param path the path to the file, relative to the plugin folder or the
	 *             jar root.   
	 * @return the input stream of the file.
	 */
	public static @Nullable InputStream getResourceInputStream(@NotNull String path) {
		WebStats plugin = WebStats.getPlugin(WebStats.class);
		try {
			// Find resource in plugin data folder
			return new FileInputStream(new File(plugin.getDataFolder(), path));
		} catch (FileNotFoundException e) {
			// Find resource in jar
			return plugin.getResource(path);
		}
	}
	
}
