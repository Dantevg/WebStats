package nl.dantevg.webstats_discord;

import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;
import java.util.logging.Logger;

import static nl.dantevg.webstats.WebStats.config;

public class WebStatsDiscord extends JavaPlugin {
	public static Logger logger;
	
	public DiscordWebhook webhook;
	
	@Override
	public void onEnable() {
		logger = getLogger();
		
		if (!config.contains("discord-webhook")) {
			getLogger().log(Level.SEVERE,
					"WebStatsDiscord plugin present but config.yml does not contain discord-webhook, disabling");
			getPluginLoader().disablePlugin(this);
		}
		
		try {
			webhook = new DiscordWebhook(this);
			int updateInterval = config.getInt("update-interval", 10);
			if (updateInterval > 0) {
				long delayTicks = 0;
				long periodTicks = updateInterval * 20L; // assume 20 tps
				Bukkit.getScheduler().runTaskTimer(this, webhook,
						delayTicks, periodTicks);
			}
		} catch (InvalidConfigurationException e) {
			getLogger().log(Level.SEVERE, "Invalid Discord webhook configuration, disabling", e);
			getPluginLoader().disablePlugin(this);
		}
	}
	
	@Override
	public void onDisable() {
		Bukkit.getScheduler().cancelTasks(this);
		webhook.disable();
	}
	
	public void reload() {
		getLogger().log(Level.INFO, "Reload: disabling plugin");
		setEnabled(false);
		getLogger().log(Level.INFO, "Reload: re-enabling plugin");
		setEnabled(true);
		getLogger().log(Level.INFO, "Reload complete");
	}
	
}
