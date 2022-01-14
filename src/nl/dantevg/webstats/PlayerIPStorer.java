package nl.dantevg.webstats;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerIPStorer implements Listener {
	private static final String FILENAME = "ips-names.yml";
	
	private final WebStats webStats;
	private final Map<String, List<String>> ipToName = new HashMap<>();
	
	public PlayerIPStorer(WebStats webStats) {
		this.webStats = webStats;
	}
	
	public List<String> getNames(String ip){
		// TODO: implement
		return null;
	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event){
		// TODO: implement
	}
	
}
