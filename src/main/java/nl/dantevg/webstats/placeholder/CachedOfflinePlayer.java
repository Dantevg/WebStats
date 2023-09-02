package nl.dantevg.webstats.placeholder;

import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Wrap an OfflinePlayer and cache its name. This dramatically improves
 * performance when calling OfflinePlayer::getName often.
 */
public class CachedOfflinePlayer {
	private final @NotNull OfflinePlayer player;
	private final @Nullable String name;
	
	public CachedOfflinePlayer(@NotNull OfflinePlayer player) {
		this.player = player;
		this.name = player.getName();
	}
	
	@NotNull
	public OfflinePlayer getOfflinePlayer() {
		return player;
	}
	
	@Nullable
	public String getName() {
		return name;
	}
	
	@NotNull
	public UUID getUniqueId() {
		return player.getUniqueId();
	}
}
