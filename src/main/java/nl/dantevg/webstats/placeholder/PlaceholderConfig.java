package nl.dantevg.webstats.placeholder;

import nl.dantevg.webstats.WebStats;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class PlaceholderConfig {
	private static PlaceholderConfig instance;
	
	public final Map<String, Object> placeholders;
	public final boolean storeInFile;
	public final @Nullable String storeInDatabase;
	public final boolean saveOnPluginDisable;
	
	private PlaceholderConfig() throws InvalidConfigurationException {
		ConfigurationSection section = WebStats.config.getConfigurationSection("placeholders");
		if (section == null) {
			throw new InvalidConfigurationException("Invalid configuration: placeholders should be a key-value map");
		}
		
		placeholders = section.getValues(false);
		storeInFile = WebStats.config.getBoolean("store-placeholders-in-file");
		storeInDatabase = WebStats.config.getString("store-placeholders-database");
		saveOnPluginDisable = WebStats.config.getBoolean("save-placeholders-on-plugin-disable");
	}
	
	public static PlaceholderConfig getInstance(boolean forceNew) throws InvalidConfigurationException {
		if (instance == null || forceNew) instance = new PlaceholderConfig();
		return instance;
	}
	
	public static PlaceholderConfig getInstance() throws InvalidConfigurationException {
		return getInstance(false);
	}
	
}
