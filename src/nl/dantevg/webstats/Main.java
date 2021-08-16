package nl.dantevg.webstats;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main extends JavaPlugin implements Runnable {
	protected static Logger logger;
	protected static FileConfiguration config;
	
	protected static ScoreboardSource scoreboardSource;
	protected static DatabaseSource databaseSource;
	
	private ServerSocket serverSocket;
	private Thread thread;
	
	// Gets run when the plugin is enabled on server startup
	@Override
	public void onEnable() {
		logger = getLogger();
		config = getConfig();
		
		// Config
		saveDefaultConfig();
		int port = config.getInt("port");
		
		// Set sources
		if (config.contains("objectives")) scoreboardSource = new ScoreboardSource();
		if (config.contains("database")) databaseSource = new DatabaseSource();
		
		try {
			// Open server socket
			serverSocket = new ServerSocket(port);
			logger.log(Level.INFO, "Web stats server started on port " + port);
			
			// Start server in a new thread, otherwise `serverSocket.accept()` will block the main thread
			thread = new Thread(this, "WebStats");
			thread.start();
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Failed to open socket: " + e.getMessage(), e);
		}
	}
	
	// Gets run when the plugin is disabled on server stop
	@Override
	public void onDisable() {
		// Close socket
		try {
			serverSocket.close();
		} catch (IOException e) {
			logger.log(Level.WARNING, "Failed to close socket: " + e.getMessage(), e);
		}
		// Stop thread
		try {
			thread.join(100); // Wait max 0.1s for the thread to stop
		} catch (InterruptedException e) {
			// Ignore
		}
	}
	
	// Gets run in the new thread created on server startup
	@Override
	public void run() {
		try {
			while (!serverSocket.isClosed()) {
				// Accept new connections
				// Only one connection at a time possible, I don't expect heavy traffic
				HTTPConnection.start(serverSocket.accept());
			}
		} catch (IOException e) {
			if (!serverSocket.isClosed()) {
				// Print error when the socket was not closed (otherwise just stop)
				logger.log(Level.WARNING, "IO Exception: " + e.getMessage(), e);
			}
		} finally {
			if (databaseSource != null) databaseSource.disconnect();
		}
	}
	
}
