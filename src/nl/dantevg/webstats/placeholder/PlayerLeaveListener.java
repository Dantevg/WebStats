package nl.dantevg.webstats.placeholder;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerLeaveListener implements Listener {
	private final PlaceholderStorer storer;
	
	public PlayerLeaveListener(PlaceholderStorer storer) {
		this.storer = storer;
	}
	
	@EventHandler
	public void onPlayerLeave(PlayerQuitEvent event) {
		storer.save(event.getPlayer());
	}
}
