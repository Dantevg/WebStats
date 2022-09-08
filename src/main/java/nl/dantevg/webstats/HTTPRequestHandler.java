package nl.dantevg.webstats;

import com.google.common.base.Splitter;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class HTTPRequestHandler implements HttpHandler {
	// Map of resource names to their MIME-types
	private final Map<String, String> resources = new HashMap<>();
	
	public HTTPRequestHandler() {
		boolean serveWebpage = WebStats.config.getBoolean("serve-webpage");
		
		if (serveWebpage) {
			resources.put("/index.html", "text/html");
			resources.put("/style.css", "text/css");
			resources.put("/WebStats-dist.js", "application/javascript");
		}
		
		attemptMigrateResources();
	}
	
	@Override
	public void handle(@NotNull HttpExchange exchange) throws IOException {
		HTTPConnection httpConnection = new HTTPConnection(exchange);
		
		// Only handle GET-requests
		if (!exchange.getRequestMethod().equals("GET")) {
			httpConnection.sendEmptyStatus(HttpURLConnection.HTTP_BAD_METHOD);
			exchange.close();
			return;
		}
		
		// No URI present
		String path = exchange.getRequestURI().getPath();
		if (path == null) {
			httpConnection.sendEmptyStatus(HttpURLConnection.HTTP_BAD_REQUEST);
			exchange.close();
			return;
		}
		
		// Rewrite "/" to "/index.html"
		if (path.equals("/")) path = "/index.html";
		
		switch (path) {
			case "/stats.json":
				InetAddress ip = exchange.getRemoteAddress().getAddress();
				String query = exchange.getRequestURI().getQuery();
				String table = null;
				if (query != null) {
					table = Splitter.on('&')
							.trimResults()
							.withKeyValueSeparator('=')
							.split(query)
							.get("table");
				}
				httpConnection.sendJson(new Gson().toJson(Stats.getAll(table, ip)));
				break;
			case "/online.json":
				httpConnection.sendJson(new Gson().toJson(Stats.getOnline()));
				break;
			case "/tables.json":
				httpConnection.sendJson(new Gson().toJson(Stats.getTables()));
			default:
				if (resources.containsKey(path)) {
					httpConnection.sendFile(resources.get(path), "web" + path);
				} else {
					WebStats.logger.log(Level.CONFIG, "Got request for " + path + ", not found");
					httpConnection.sendEmptyStatus(HttpURLConnection.HTTP_NOT_FOUND);
				}
				break;
		}
		
		exchange.close();
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
