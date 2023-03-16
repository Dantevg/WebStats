package nl.dantevg.webstats;

import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.util.*;
import java.util.logging.Level;

public class PlayerIPStorage {
	private static final String FILENAME = "ip-to-names.yml";
	
	private final @NotNull File file;
	private final Map<String, Set<String>> ipToNames = new HashMap<>();
	
	public PlayerIPStorage(@NotNull WebStats plugin) {
		file = new File(plugin.getDataFolder(), FILENAME);
		
		// Register events
		Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(this), plugin);
		
		// Read persistently stored data
		if (WebStatsConfig.getInstance().storePlayerIPs) load();
	}
	
	public @NotNull Set<String> getNames(@NotNull InetAddress ip) {
		return getNames(ip.getHostAddress());
	}
	
	public @NotNull Set<String> getNames(String ip) {
		Set<String> names = ipToNames.get(ip);
		return names != null ? names : new HashSet<>();
	}
	
	public void addName(String ip, String name) {
		// Remove old IP (they change)
		removeName(name);
		
		// Add new IP (could be the same one)
		ipToNames.putIfAbsent(ip, new HashSet<>());
		ipToNames.get(ip).add(name);
	}
	
	private void removeName(String name) {
		ipToNames.forEach((ip, names) -> names.remove(name));
		ipToNames.entrySet().removeIf(entry -> entry.getValue().isEmpty());
	}
	
	private void load() {
		YamlConfiguration ipToNamesYaml = new YamlConfiguration();
		try {
			ipToNamesYaml.load(file);
		} catch (FileNotFoundException e) {
			// Ignore, the file did not exist yet but will be created on server close
		} catch (IOException | InvalidConfigurationException e) {
			WebStats.logger.log(Level.WARNING, "Could not read " + file.getName(), e);
			return;
		}
		
		int nLoadedMappings = 0;
		for (String ip : ipToNamesYaml.getKeys(false)) {
			for (String name : ipToNamesYaml.getStringList(ip)) {
				addName(decodeIP(ip), name);
				nLoadedMappings++;
			}
		}
		
		WebStats.logger.log(Level.INFO, "Loaded " + nLoadedMappings + " ip-to-names mappings");
	}
	
	public void save() {
		YamlConfiguration ipToNamesYaml = new YamlConfiguration();
		ipToNames.forEach((ip, names) -> ipToNamesYaml.set(encodeIP(ip), names.toArray()));
		
		try {
			ipToNamesYaml.save(file);
			WebStats.logger.log(Level.INFO, "Saved player ip-to-names mappings");
		} catch (IOException e) {
			WebStats.logger.log(Level.WARNING, "Could not store " + file.getName(), e);
		}
	}
	
	public void disable() {
		if (WebStatsConfig.getInstance().storePlayerIPs) save();
	}
	
	public @NotNull String debug() {
		String persistentStatus = (WebStatsConfig.getInstance().storePlayerIPs ? "on" : "off");
		List<String> loadedIPs = new ArrayList<>();
		ipToNames.forEach((ip, names) -> loadedIPs.add(ip + ": " + names.toString()));
		return "Loaded ip to playername mapping: (persistent: " + persistentStatus + ")\n"
				+ String.join("\n", loadedIPs);
	}
	
	private static @NotNull String encodeIP(@NotNull String ip) {
		return ip.replace(".", "_");
	}
	
	private static @NotNull String decodeIP(@NotNull String ip) {
		return ip.replace("_", ".");
	}
	
}
