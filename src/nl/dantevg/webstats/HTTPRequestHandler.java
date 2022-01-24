package nl.dantevg.webstats;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;

public class HTTPRequestHandler implements HttpHandler {
	@Override
	public void handle(@NotNull HttpExchange exchange) throws IOException {
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
		
		switch (path) {
			case "/stats.json":
				InetAddress ip = exchange.getRemoteAddress().getAddress();
				httpConnection.sendJson(new Gson().toJson(Stats.getAll(ip)));
				break;
			case "/online.json":
				httpConnection.sendJson(new Gson().toJson(Stats.getOnline()));
				break;
			case "/index.html":
			case "/":
				httpConnection.sendFile("text/html", "resources/index.html");
				break;
			case "/style.css":
				httpConnection.sendFile("text/css", "resources/style.css");
				break;
			case "/WebStats-dist.js":
				httpConnection.sendFile("application/javascript", "resources/WebStats-dist.js");
				break;
			default:
				httpConnection.sendEmptyStatus(HttpURLConnection.HTTP_NOT_FOUND);
				break;
		}
		
		exchange.close();
	}
	
}
