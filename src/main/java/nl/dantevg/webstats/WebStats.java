package nl.dantevg.webstats;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import nl.dantevg.webstats.database.DatabaseSource;
import nl.dantevg.webstats.discordwebhook.DiscordWebhook;
import nl.dantevg.webstats.placeholder.PlaceholderSource;
import nl.dantevg.webstats.scoreboard.ScoreboardSource;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WebStats extends JavaPlugin {
	private static final String KEYSTORE_FILE = "resources/webstats.jks";
	private static final String KEYSTORE_PASSWORD = "webstats"; // TODO: change!
	
	protected static ScoreboardSource scoreboardSource;
	protected static DatabaseSource databaseSource;
	protected static PlaceholderSource placeholderSource;
	
	protected static DiscordWebhook discordWebhook;
	
	protected static PlayerIPStorage playerIPStorage;
	
	public static Logger logger;
	public static FileConfiguration config;
	public static boolean hasEssentials;
	
	private HttpsServer webserver;
	
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
		
		if (config.contains("discord-webhook")) {
			try {
				discordWebhook = new DiscordWebhook(this);
			} catch (InvalidConfigurationException e) {
				logger.log(Level.SEVERE, "Invalid Discord webhook configuration", e);
			}
		}
		
		try {
			// Start web server
			webserver = HttpsServer.create(new InetSocketAddress(port), 0);
			
			// Initialise TLS
			SSLContext sslContext = SSLContext.getInstance("TLS");
			
			// Initialise keystore
			KeyStore keyStore = KeyStore.getInstance("JKS");
			keyStore.load(getResource("resources/webstats.jks"), KEYSTORE_PASSWORD.toCharArray());
			
			// Set up key manager factory
			KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("PKIX");
			keyManagerFactory.init(keyStore, KEYSTORE_PASSWORD.toCharArray());
			
			// Set up trust manager factory
			TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("PKIX");
			trustManagerFactory.init(keyStore);
			
			// Setup HTTPS
			sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
			webserver.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
				public void configure(HttpsParameters params) {
					SSLContext ctx = getSSLContext();
					params.setSSLParameters(ctx.getDefaultSSLParameters());
				}
			});
			
			webserver.createContext("/", new HTTPRequestHandler());
			webserver.start();
			logger.log(Level.INFO, "Web server started on port " + port);
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Failed to start web server with port "
					+ port + ": " + e.getMessage(), e);
		} catch (NoSuchAlgorithmException e) {
			logger.log(Level.SEVERE, "No TLS implementation found", e);
		} catch (KeyStoreException e) {
			logger.log(Level.SEVERE, "No JKS keystore implementation found", e);
		} catch (CertificateException e) {
			logger.log(Level.SEVERE, "Can not not load certificate from keystore", e);
		} catch (UnrecoverableKeyException e) {
			logger.log(Level.SEVERE, "Can not read key from keystore, check password", e);
		} catch (KeyManagementException e) {
			logger.log(Level.SEVERE, "Can not initialise SSL context", e);
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
	
}
