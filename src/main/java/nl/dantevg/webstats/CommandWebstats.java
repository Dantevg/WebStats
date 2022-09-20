package nl.dantevg.webstats;

import nl.dantevg.webstats.storage.CSVStorage;
import nl.dantevg.webstats.storage.DatabaseStorage;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
			lines.add(WebStats.playerIPStorage.debug());
			sender.sendMessage(String.join("\n", lines));
			return true;
		} else if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
			webstats.reload();
			if (!(sender instanceof ConsoleCommandSender)) sender.sendMessage("Reload complete");
			return true;
		} else if (args.length == 1 || args.length == 2 && args[0].equalsIgnoreCase("store")) {
			CSVStorage storage = new CSVStorage("stats.csv", "Player");
			StatData.Stats stats = Stats.getStats();
			List<String> columns;
			if (args.length == 2) {
				TableConfig table = Stats.getTables().stream()
						.filter(tc -> args[1].equalsIgnoreCase(tc.name))
						.findFirst()
						.orElse(null);
				if (table == null) {
					sender.sendMessage("No table '" + args[1] + "'");
					return true;
				}
				columns = table.columns;
			} else {
				columns = stats.columns;
			}
			if (columns != null ? storage.append(stats.scores, columns) : storage.append(stats.scores)) {
				sender.sendMessage("Storing stats finished");
			} else {
				sender.sendMessage("Could not store stats, check console");
			}
			return true;
		} else if (args.length == 2 && args[0].equalsIgnoreCase("migrate-placeholders-to")) {
			if (!args[1].equalsIgnoreCase("csv") && !args[1].equalsIgnoreCase("database")) {
				return false;
			}
			// Do this async because connecting to the database may take some time
			Bukkit.getScheduler().runTaskAsynchronously(webstats, () -> {
				if (args[1].equalsIgnoreCase("csv")) {
					WebStats.placeholderSource.migrateStorage(CSVStorage.class);
				} else if (args[1].equalsIgnoreCase("database")) {
					WebStats.placeholderSource.migrateStorage(DatabaseStorage.class);
				}
				sender.sendMessage("Migration complete. Remember to change config.yml to reflect these changes!");
			});
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
			completions.add("store");
			completions.add("migrate-placeholders-to");
		} else if (args.length == 2 && args[0].equalsIgnoreCase("migrate-placeholders-to")) {
			completions.add("database");
			completions.add("csv");
		} else if (args.length == 2 && args[0].equalsIgnoreCase("store")) {
			completions.addAll(Stats.getTables().stream()
					.map(tc -> tc.name)
					.filter(Objects::nonNull)
					.collect(Collectors.toList()));
		}
		return completions;
	}
	
}
