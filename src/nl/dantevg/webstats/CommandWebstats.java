package nl.dantevg.webstats;

import org.bukkit.command.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class CommandWebstats implements CommandExecutor, TabCompleter {
	private final WebStats webstats;
	
	public CommandWebstats(WebStats webstats) {
		this.webstats = webstats;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (args.length == 1 && args[0].equalsIgnoreCase("debug")) {
			List<String> lines = new ArrayList<>();
			lines.add(webstats.debug());
			if (WebStats.placeholderSource != null) lines.add(WebStats.placeholderSource.debug());
			sender.sendMessage(String.join("\n", lines));
			return true;
		} else if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
			webstats.reload();
			if (!(sender instanceof ConsoleCommandSender)) sender.sendMessage("Reload complete");
			return true;
		}
		
		return false;
	}
	
	@Override
	public @NotNull List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
		List<String> completions = new ArrayList<>();
		if (args.length == 1) {
			completions.add("debug");
			completions.add("reload");
		}
		return completions;
	}
	
}
