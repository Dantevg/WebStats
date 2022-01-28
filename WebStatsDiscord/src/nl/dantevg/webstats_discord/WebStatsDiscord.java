package nl.dantevg.webstats_discord;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.MalformedURLException;
import java.util.logging.Level;

import static nl.dantevg.webstats.WebStats.config;

public class WebStatsDiscord extends JavaPlugin {
	@Override
	public void onEnable() {
		if (!config.contains("discord-webhook")) {
			getLogger().log(Level.SEVERE,
					"WebStatsDiscord plugin present but config.yml does not contain discord-webhook, disabling");
			getPluginLoader().disablePlugin(this);
		}
		
		try {
			DiscordWebhook webhook = new DiscordWebhook(this);
			int updateInterval = config.getInt("update-interval", 10);
			if (updateInterval > 0) {
				long delayTicks = 0;
				long periodTicks = updateInterval * 20L; // assume 20 tps
				Bukkit.getScheduler().runTaskTimer(this, webhook,
						delayTicks, periodTicks);
			}
		} catch (MalformedURLException e) {
			getLogger().log(Level.SEVERE, "Malformed Discord webhook url, disabling", e);
			getPluginLoader().disablePlugin(this);
		}
	}
	
	@Override
	public void onDisable() {
		Bukkit.getScheduler().cancelTasks(this);
	}
	
	public void reload() {
		getLogger().log(Level.INFO, "Reload: disabling plugin");
		setEnabled(false);
		getLogger().log(Level.INFO, "Reload: re-enabling plugin");
		setEnabled(true);
		getLogger().log(Level.INFO, "Reload complete");
	}
	
}
