package nl.dantevg.webstats.placeholder;

import nl.dantevg.webstats.WebStats;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.logging.Level;

public class PlaceholderListener implements Listener {
	private static final Duration PLUGIN_DISABLE_WAIT = Duration.ofSeconds(30);
	
	private final PlaceholderStorage storage;
	private final boolean SAVE_ON_PLUGIN_DISABLE;
	private Instant lastPluginDisable = Instant.now();
	
	public PlaceholderListener(PlaceholderStorage storage, boolean saveOnPluginDisable) {
		this.storage = storage;
		SAVE_ON_PLUGIN_DISABLE = saveOnPluginDisable;
	}
	
	@EventHandler
	public void onPlayerLeave(PlayerQuitEvent event) {
		storage.save(new PlaceholderSource.OfflinePlayerWithCachedName(event.getPlayer()));
	}
	
	@EventHandler
	public void onPluginDisable(PluginDisableEvent event) {
		if (!SAVE_ON_PLUGIN_DISABLE) return;
		
		final Instant now = Instant.now();
		if (now.isAfter(lastPluginDisable.plus(PLUGIN_DISABLE_WAIT))) {
			WebStats.logger.log(Level.INFO, "Got plugin disable event, saving placeholders early");
			storage.saveAll();
		}
		lastPluginDisable = now;
	}
}
