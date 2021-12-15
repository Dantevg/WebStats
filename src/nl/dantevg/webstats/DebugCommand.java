package nl.dantevg.webstats;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class DebugCommand implements CommandExecutor, TabCompleter {
	private final WebStats webstats;
	
	public DebugCommand(WebStats webstats) {
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
		}
		
		return false;
	}
	
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
		List<String> completions = new ArrayList<>();
		if (args.length == 1) {
			completions.add("debug");
		}
		return completions;
	}
	
}
