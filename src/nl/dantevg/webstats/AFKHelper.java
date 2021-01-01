package nl.dantevg.webstats;

import com.earth2me.essentials.Essentials;
import org.bukkit.entity.Player;

public class AFKHelper {
	// This is in a separate class to make the Essentials plugin optional
	public static boolean isAFK(Player player){
		return Essentials.getPlugin(Essentials.class).getUser(player).isAfk();
	}
}
