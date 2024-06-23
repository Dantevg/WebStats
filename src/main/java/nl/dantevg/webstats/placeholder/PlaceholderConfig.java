package nl.dantevg.webstats.placeholder;

import nl.dantevg.webstats.WebStats;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PlaceholderConfig {
	private static final String DOT_REPLACEMENT = "[dot]";
	private static PlaceholderConfig instance;
	
	public final Map<String, String> placeholders;
	public final boolean storeInFile;
	public final @Nullable String storeInDatabase;
	public final boolean saveOnPluginDisable;
	public final Map<String, String> emptyValues;
	
	private PlaceholderConfig() throws InvalidConfigurationException {
		ConfigurationSection section = WebStats.config.getConfigurationSection("placeholders");
		if (section == null) {
			throw new InvalidConfigurationException("Invalid configuration: placeholders should be a key-value map");
		}
		
		placeholders = sanitizePlaceholderMap(section.getValues(false));
		storeInFile = WebStats.config.getBoolean("store-placeholders-in-file");
		storeInDatabase = WebStats.config.getString("store-placeholders-database");
		saveOnPluginDisable = WebStats.config.getBoolean("save-placeholders-on-plugin-disable");
		
		section = WebStats.config.getConfigurationSection("placeholder-empty-values");
		if (section != null) {
			emptyValues = sanitizePlaceholderMap(section.getValues(false));
		} else {
			emptyValues = Collections.emptyMap();
		}
	}
	
	public static PlaceholderConfig getInstance(boolean forceNew) throws InvalidConfigurationException {
		if (instance == null || forceNew) instance = new PlaceholderConfig();
		return instance;
	}
	
	public static PlaceholderConfig getInstance() throws InvalidConfigurationException {
		return getInstance(false);
	}
	
	// Remove non-string values from map, and replace any occurrence of '[dot]' with '.',
	// because Spigot's YAML parser cannot handle keys with dots (for permissions).
	private static Map<String, String> sanitizePlaceholderMap(Map<String, Object> input) {
		Map<String, String> output = new HashMap<>();
		input.forEach((key, value) -> {
			if (value instanceof String) output.put(key.replace(DOT_REPLACEMENT, "."), (String) value);
		});
		return output;
	}
	
}
