package nl.dantevg.webstats;

import nl.dantevg.webstats.database.DatabaseSource;
import nl.dantevg.webstats.placeholder.PlaceholderSource;
import nl.dantevg.webstats.scoreboard.ScoreboardSource;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WebStats extends JavaPlugin implements Runnable {
	protected static ScoreboardSource scoreboardSource;
	protected static DatabaseSource databaseSource;
	protected static PlaceholderSource placeholderSource;
	
	public static Logger logger;
	public static FileConfiguration config;
	public static boolean hasEssentials;
	
	private ServerSocket serverSocket;
	private Thread thread;
	
	// Gets run when the plugin is enabled on server startup
	@Override
	public void onEnable() {
		logger = getLogger();
		config = getConfig();
		
		hasEssentials = Bukkit.getPluginManager().getPlugin("Essentials") != null;
		
		// Config
		saveDefaultConfig();
		int port = config.getInt("port");
		
		// Register debug command
		DebugCommand debugCommand = new DebugCommand(this);
		getCommand("webstats").setExecutor(debugCommand);
		getCommand("webstats").setTabCompleter(debugCommand);
		
		// Set sources
		if (config.contains("objectives")) scoreboardSource = new ScoreboardSource();
		if (config.contains("database.config")) {
			try {
				databaseSource = new DatabaseSource();
			} catch (ConfigurationException e) {
				logger.log(Level.SEVERE, "Invalid database configuration", e);
			}
		}
		if (config.contains("placeholders")) {
			if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
				try {
					placeholderSource = new PlaceholderSource();
				} catch (ConfigurationException e) {
					logger.log(Level.SEVERE, "Invalid placeholder configuration", e);
				}
			} else {
				logger.log(Level.WARNING, "PlaceholderAPI not present but config contains placeholders (comment to remove this warning)");
			}
		}
		
		try {
			// Open server socket
			serverSocket = new ServerSocket(port);
			logger.log(Level.INFO, "Web server started on port " + port);
			
			// Start server in a new thread, otherwise `serverSocket.accept()` will block the main thread
			thread = new Thread(this, "WebStats");
			thread.start();
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Failed to open socket with port "
					+ port + ": " + e.getMessage(), e);
			getPluginLoader().disablePlugin(this);
		}
	}
	
	// Gets run when the plugin is disabled on server stop
	@Override
	public void onDisable() {
		// Close socket
		try {
			if (serverSocket != null) serverSocket.close();
		} catch (IOException e) {
			logger.log(Level.WARNING, "Failed to close socket: " + e.getMessage(), e);
		}
		
		// Stop thread
		try {
			if (thread != null) {
				thread.interrupt();
				thread.join();
			}
		} catch (InterruptedException e) {
			logger.log(Level.WARNING, "Failed to stop thread: " + e.getMessage());
		}
		
		// Let sources close connections
		if (databaseSource != null) databaseSource.disable();
		if (placeholderSource != null) placeholderSource.disable();
	}
	
	// Gets run in the new thread created on server startup
	@Override
	public void run() {
		try {
			while (!serverSocket.isClosed() && !thread.isInterrupted()) {
				// Accept new connections
				// Only one connection at a time possible, I don't expect heavy traffic
				HTTPConnection.start(serverSocket.accept());
			}
		} catch (IOException e) {
			if (!serverSocket.isClosed()) {
				// Print error when the socket was not closed (otherwise just stop)
				logger.log(Level.WARNING, "IO Exception: " + e.getMessage(), e);
			}
		}
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
	
	private @NotNull String getThreadStatus() {
		return "Thread status: " + (thread.isAlive() ? "alive" : "dead");
	}
	
	private @NotNull String getSocketStatus() {
		return "Socket status: " + (serverSocket.isClosed() ? "closed" : "open");
	}
	
	protected @NotNull String debug() {
		return getVersion() + "\n"
				+ getSources() + "\n"
				+ getThreadStatus() + "\n"
				+ getSocketStatus();
	}
	
}
