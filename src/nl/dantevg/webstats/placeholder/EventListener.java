package nl.dantevg.webstats.placeholder;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class EventListener implements Listener {
	private final PlaceholderStorer storer;
	
	public EventListener(PlaceholderStorer storer) {
		this.storer = storer;
	}
	
	@EventHandler
	public void onPlayerLeave(PlayerQuitEvent event) {
		storer.save(event.getPlayer());
	}
}
