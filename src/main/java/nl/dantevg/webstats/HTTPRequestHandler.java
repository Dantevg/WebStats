package nl.dantevg.webstats;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

public class HTTPRequestHandler implements HttpHandler {
	private static final Set<String> IGNORED_EXCEPTIONS = new HashSet<>(Arrays.asList(
			"broken pipe",
			"response headers not sent yet",
			"connection reset by peer"
	));
	
	// Map of resource names to their MIME-types
	private final Map<String, String> resources = new HashMap<>();
	
	public HTTPRequestHandler() {
		if (WebStatsConfig.getInstance().serveWebpage) {
			resources.put("/favicon.png", "image/png");
			resources.put("/index.html", "text/html");
			resources.put("/style.css", "text/css");
			resources.put("/WebStats-dist.js", "application/javascript");
			resources.put("/WebStats-dist.js.map", "application/json");
			
			WebStatsConfig.getInstance().additionalResources.forEach(path ->
					resources.put("/" + path, URLConnection.guessContentTypeFromName(path)));
		}
		
		attemptMigrateResources();
	}
	
	public void handle(@NotNull HttpExchange exchange) {
		try {
			handleInternal(exchange);
		} catch (Exception e) {
			if (!IGNORED_EXCEPTIONS.contains(e.getMessage().toLowerCase())) {
				String message = String.format("Caught an exception while handling a request from %s (%s %s)",
						exchange.getRemoteAddress().getAddress(),
						exchange.getRequestMethod(),
						exchange.getRequestURI());
				WebStats.logger.log(Level.WARNING, message, e);
			}
		} finally {
			exchange.close();
		}
	}
	
	private void handleInternal(@NotNull HttpExchange exchange) throws IOException {
		HTTPConnection httpConnection = new HTTPConnection(exchange);
		
		// Only handle GET-requests
		if (!exchange.getRequestMethod().equals("GET")) {
			httpConnection.sendEmptyStatus(HttpURLConnection.HTTP_BAD_METHOD);
			return;
		}
		
		// No URI present
		String path = exchange.getRequestURI().getPath();
		if (path == null) {
			httpConnection.sendEmptyStatus(HttpURLConnection.HTTP_BAD_REQUEST);
			return;
		}
		
		// Rewrite "/" to "/index.html"
		if (path.equals("/")) path = "/index.html";
		
		switch (path) {
			case "/stats.json":
				InetAddress ip = exchange.getRemoteAddress().getAddress();
				try {
					// Stats need to be gathered on the main thread,
					// see https://github.com/Dantevg/WebStats/issues/52
					StatData stats = Bukkit.getScheduler().callSyncMethod(
							WebStats.getPlugin(WebStats.class),
							() -> Stats.getAll(ip)).get();
					httpConnection.sendJson(new Gson().toJson(stats));
				} catch (InterruptedException ignored) {
					// do nothing
				} catch (ExecutionException e) {
					if (e.getCause() instanceof IOException) {
						throw (IOException) e.getCause();
					} else {
						throw new RuntimeException(e);
					}
				}
				break;
			case "/online.json":
				httpConnection.sendJson(new Gson().toJson(Stats.getOnline()));
				break;
			case "/tables.json":
				httpConnection.sendJson(new Gson().toJson(WebStatsConfig.getInstance().tables));
				break;
			case "/stats.csv":
				if (new File(WebStats.getPlugin(WebStats.class).getDataFolder(), "stats.csv").exists()) {
					httpConnection.sendFile("text/csv", "stats.csv");
				} else {
					httpConnection.sendEmptyStatus(HttpURLConnection.HTTP_NOT_FOUND);
				}
				break;
			default:
				if (resources.containsKey(path)) {
					httpConnection.sendFile(resources.get(path), "web" + path);
				} else {
					WebStats.logger.log(Level.CONFIG, "Got request for " + path + ", not found");
					httpConnection.sendEmptyStatus(HttpURLConnection.HTTP_NOT_FOUND);
				}
				break;
		}
	}
	
	private void attemptMigrateResources() {
		File dir = new File(WebStats.getPlugin(WebStats.class).getDataFolder(), "web");
		if (!dir.isDirectory()) dir.mkdirs();
		
		for (String path : resources.keySet()) {
			File oldFile = new File(WebStats.getPlugin(WebStats.class).getDataFolder(), path);
			if (oldFile.isFile()) {
				File newFile = new File(WebStats.getPlugin(WebStats.class).getDataFolder(), "web" + path);
				try {
					Files.move(oldFile.toPath(), newFile.toPath());
					WebStats.logger.log(Level.INFO, "Migrated web resource at '"
							+ oldFile.toPath() + "' to '" + newFile.toPath() + "'");
				} catch (IOException e) {
					WebStats.logger.log(Level.WARNING, "Could not migrate web resource at '"
							+ oldFile.toPath() + "' to '" + newFile.toPath() + "'", e);
				}
			}
		}
	}
	
}
