package nl.dantevg.webstats;

import nl.dantevg.webstats.database.DatabaseSource;
import nl.dantevg.webstats.discordwebhook.DiscordWebhook;
import nl.dantevg.webstats.placeholder.PlaceholderSource;
import nl.dantevg.webstats.scoreboard.ScoreboardSource;
import nl.dantevg.webstats.webserver.HTTPSWebServer;
import nl.dantevg.webstats.webserver.HTTPWebServer;
import nl.dantevg.webstats.webserver.WebServer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
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

public class WebStats extends JavaPlugin implements Listener {
	protected static ScoreboardSource scoreboardSource;
	protected static DatabaseSource databaseSource;
	protected static PlaceholderSource placeholderSource;
	
	protected static DiscordWebhook discordWebhook;
	protected static WebServer webserver;
	
	protected static PlayerIPStorage playerIPStorage;
	protected static StatExporter statExporter;
	
	public static Logger logger;
	public static FileConfiguration config;
	public static boolean hasEssentials;
	
	// SkinsRestorerHelper gets initialised in onPluginEnable, to avoid trying
	// to load load it before it is enabled
	public static SkinsRestorerHelper skinsRestorerHelper;
	
	// Gets run when the plugin is enabled on server startup
	@Override
	public void onEnable() {
		logger = getLogger();
		config = getConfig();
		
		Bukkit.getServer().getPluginManager().registerEvents(this, this);
		
		// Config
		saveDefaultConfig();
		WebStatsConfig configData = WebStatsConfig.getInstance(true);
		
		hasEssentials = Bukkit.getPluginManager().getPlugin("Essentials") != null;
		
		playerIPStorage = new PlayerIPStorage(this);
		statExporter = new StatExporter();
		
		// Register debug command
		CommandWebstats command = new CommandWebstats(this);
		getCommand("webstats").setExecutor(command);
		getCommand("webstats").setTabCompleter(command);
		
		// Enable sources
		if (configData.useScoreboardSource) scoreboardSource = new ScoreboardSource();
		if (configData.useDatabaseSource) {
			try {
				databaseSource = new DatabaseSource();
			} catch (InvalidConfigurationException e) {
				logger.log(Level.SEVERE, "Invalid database configuration", e);
			}
		}
		if (configData.usePlaceholderSource) {
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
		
		if (configData.useDiscordWebhook) {
			try {
				discordWebhook = new DiscordWebhook(this);
			} catch (InvalidConfigurationException e) {
				logger.log(Level.SEVERE, "Invalid Discord webhook configuration", e);
			}
		}
		
		if (Bukkit.getPluginManager().isPluginEnabled("SkinsRestorer")) {
			enableSkinsRestorer();
		}
		
		try {
			if (configData.useHTTPS) {
				webserver = new HTTPSWebServer();
			} else {
				webserver = new HTTPWebServer();
			}
			webserver.start();
		} catch (InvalidConfigurationException e) {
			logger.log(Level.SEVERE, "Invalid webserver configuration", e);
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Failed to start web server (port "
					+ config.getInt("port") + "): " + e.getMessage(), e);
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
		
		scoreboardSource = null;
		statExporter = null;
		skinsRestorerHelper = null;
		
		// Let sources close connections
		if (databaseSource != null) {
			databaseSource.disable();
			databaseSource = null;
		}
		if (placeholderSource != null) {
			placeholderSource.disable();
			placeholderSource = null;
		}
		if (playerIPStorage != null) {
			playerIPStorage.disable();
			playerIPStorage = null;
		}
		if (discordWebhook != null) {
			discordWebhook.disable();
			discordWebhook = null;
		}
	}
	
	@EventHandler
	public void onPluginEnable(PluginEnableEvent event) {
		if (event.getPlugin().getName().equals("SkinsRestorer")) {
			enableSkinsRestorer();
		}
	}
	
	private void enableSkinsRestorer() {
		logger.info("Enabling SkinsRestorer integration");
		try {
			skinsRestorerHelper = new SkinsRestorerHelper(this);
		} catch (NoClassDefFoundError e) {
			// SkinsRestorer versions before v15 have a completely different API
			// and will therefore fail to load.
			String version = Bukkit.getPluginManager()
					.getPlugin("SkinsRestorer")
					.getDescription()
					.getVersion();
			logger.log(Level.SEVERE, "Failed to load SkinsRestorer integration! WebStats is not compatible with version " + version + ".");
		} catch (IllegalStateException e) {
			// SkinsRestorer in proxy mode will throw IllegalStateException
			// when calling SkinsRestorerProvider.get()
			logger.log(Level.INFO, "SkinsRestorer integration is not enabled because SkinsRestorer is likely in proxy mode.");
		}
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
	 *
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
