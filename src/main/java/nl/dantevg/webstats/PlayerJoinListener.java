package nl.dantevg.webstats;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.net.InetSocketAddress;

public class PlayerJoinListener implements Listener {
	PlayerIPStorage storage;
	
	public PlayerJoinListener(PlayerIPStorage storage) {
		this.storage = storage;
	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		final InetSocketAddress address = event.getPlayer().getAddress();
		if (address != null) {
			String ip = address.getAddress().getHostAddress();
			String playername = event.getPlayer().getName();
			storage.addName(ip, playername);
		}
	}
	
}
