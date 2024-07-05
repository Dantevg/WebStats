package nl.dantevg.webstats.webserver;

import nl.dantevg.webstats.WebStats;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;

public class HTTPSConfig {
	private static HTTPSConfig instance;
	
	public static final String DEFAULT_KEYSTORE_FILE = "webstats.p12";
	public static final String DEFAULT_KEYSTORE_PASSWORD = "webstats";
	
	public final String email;
	public final String domain;
	public final String token;
	
	public final String keystoreFile;
	public final String keystorePassword;
	
	public final boolean automatic;
	
	private HTTPSConfig() throws InvalidConfigurationException {
		ConfigurationSection section = WebStats.config.getConfigurationSection("https");
		if (section == null) {
			throw new InvalidConfigurationException("Invalid configuration: https should be a yaml object");
		}
		
		email = section.getString("email");
		domain = section.getString("domain");
		token = section.getString("token");
		automatic = (email != null && domain != null && token != null);
		if (!automatic && (email != null || domain != null || token != null)) {
			// Partial automatic configuration
			throw new InvalidConfigurationException("Invalid configuration: automatic HTTPS certificate management requires email, domain and token fields. If you do not want automatic certificate management, comment these out.");
		}
		
		keystoreFile = section.getString("keystore-file", DEFAULT_KEYSTORE_FILE);
		keystorePassword = section.getString("keystore-password", DEFAULT_KEYSTORE_PASSWORD);
	}
	
	public static HTTPSConfig getInstance(boolean forceNew) throws InvalidConfigurationException {
		if (instance == null || forceNew) instance = new HTTPSConfig();
		return instance;
	}
	
	public static HTTPSConfig getInstance() throws InvalidConfigurationException {
		return getInstance(false);
	}
	
}
