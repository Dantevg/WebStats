package nl.dantevg.webstats;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.stream.Collectors;

// This is in a separate class to make the Essentials plugin optional
public class EssentialsHelper {
	public static boolean isAFK(Player player) {
		return Essentials.getPlugin(Essentials.class).getUser(player).isAfk();
	}
	
	public static boolean isVanished(Player player) {
		User user = Essentials.getPlugin(Essentials.class).getUser(player);
		return user.isVanished() || user.isHidden();
	}
	
	public static Set<OfflinePlayer> getOfflinePlayers() {
		return Essentials.getPlugin(Essentials.class).getUserMap().getAllUniqueUsers()
				.stream().map(Bukkit::getOfflinePlayer).collect(Collectors.toSet());
	}
	
}
