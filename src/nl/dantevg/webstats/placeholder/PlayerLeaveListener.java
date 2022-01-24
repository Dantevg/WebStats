package nl.dantevg.webstats.placeholder;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerLeaveListener implements Listener {
	private final PlaceholderStorage storage;
	
	public PlayerLeaveListener(PlaceholderStorage storage) {
		this.storage = storage;
	}
	
	@EventHandler
	public void onPlayerLeave(PlayerQuitEvent event) {
		storage.save(event.getPlayer());
	}
}
