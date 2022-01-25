package nl.dantevg.webstats;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class HTTPRequestHandler implements HttpHandler {
	// Map of resource names to their MIME-types
	private static final Map<String, String> resources = new HashMap<>();
	
	public HTTPRequestHandler() {
		boolean serveWebpage = WebStats.config.getBoolean("serve-webpage");
		
		if (serveWebpage) {
			resources.put("/index.html", "text/html");
			resources.put("/style.css", "text/css");
			resources.put("/WebStats-dist.js", "application/javascript");
		}
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
				httpConnection.sendJson(new Gson().toJson(Stats.getAll(ip)));
				break;
			case "/online.json":
				httpConnection.sendJson(new Gson().toJson(Stats.getOnline()));
				break;
			default:
				if (resources.containsKey(path)) {
					httpConnection.sendFile(resources.get(path), "resources" + path);
				} else {
					WebStats.logger.log(Level.CONFIG, "Got request for " + path + ", not found");
					httpConnection.sendEmptyStatus(HttpURLConnection.HTTP_NOT_FOUND);
				}
				break;
		}
		
		exchange.close();
	}
	
}
