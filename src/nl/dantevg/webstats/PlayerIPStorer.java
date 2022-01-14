package nl.dantevg.webstats;

import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.util.*;
import java.util.logging.Level;

public class PlayerIPStorer implements Listener {
	private static final String FILENAME = "ip-to-names.yml";
	
	private final @NotNull File file;
	private final boolean persistent;
	private final Map<String, Set<String>> ipToNames = new HashMap<>();
	
	public PlayerIPStorer(@NotNull WebStats plugin) {
		this.persistent = WebStats.config.getBoolean("store-player-ips");
		file = new File(plugin.getDataFolder(), FILENAME);
		
		// Register events
		Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(this), plugin);
		
		// Read persistently stored data
		if (persistent) load();
	}
	
	public @NotNull Set<String> getNames(@NotNull InetAddress ip) {
		return getNames(ip.getHostAddress());
	}
	
	public @NotNull Set<String> getNames(String ip) {
		Set<String> names = ipToNames.get(ip);
		return names != null ? names : new HashSet<>();
	}
	
	public void addName(String ip, String name) {
		ipToNames.putIfAbsent(ip, new HashSet<>());
		ipToNames.get(ip).add(name);
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
		
		for (String ip : ipToNamesYaml.getKeys(false)) {
			for (String name : ipToNamesYaml.getStringList(ip)) {
				addName(decodeIP(ip), name);
			}
		}
	}
	
	public void save() {
		YamlConfiguration ipToNamesYaml = new YamlConfiguration();
		ipToNames.forEach((ip, names) -> ipToNamesYaml.set(encodeIP(ip), names));
		
		try {
			ipToNamesYaml.save(file);
		} catch (IOException e) {
			WebStats.logger.log(Level.WARNING, "Could not store " + file.getName(), e);
		}
	}
	
	public void disable() {
		if (persistent) save();
	}
	
	private static @NotNull String encodeIP(@NotNull String ip) {
		return ip.replace(".", "_");
	}
	
	private static @NotNull String decodeIP(@NotNull String ip) {
		return ip.replace("_", ".");
	}
	
}
