package nl.dantevg.webstats;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;

public class HTTPRequestHandler implements HttpHandler {
	@Override
	public void handle(@NotNull HttpExchange exchange) throws IOException {
		// Only handle GET-requests
		if (!exchange.getRequestMethod().equals("GET")) return;
		
		String path = exchange.getRequestURI().getPath();
		if (path == null) return;
		
		switch (path) {
			case "/stats.json":
				InetAddress ip = exchange.getRemoteAddress().getAddress();
				sendJson(exchange, new Gson().toJson(Stats.getAll(ip)));
				break;
			case "/online.json":
				sendJson(exchange, new Gson().toJson(Stats.getOnline()));
				break;
			default:
				sendNotFound(exchange);
				break;
		}
		
		exchange.close();
	}
	
	private static void send(@NotNull HttpExchange exchange, int status, @NotNull String contentType, @NotNull String response) throws IOException {
		// Add CORS and content-type headers
		exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
		exchange.getResponseHeaders().add("Content-Type", contentType + "; charset=UTF-8");
		
		// Send headers and data
		byte[] responseBytes = response.getBytes();
		exchange.sendResponseHeaders(status, responseBytes.length);
		OutputStream output = exchange.getResponseBody();
		output.write(responseBytes);
		output.close();
	}
	
	private static void sendJson(@NotNull HttpExchange exchange, @NotNull String response) throws IOException {
		send(exchange, HttpURLConnection.HTTP_OK, "application/json", response);
	}
	
	private static void sendNotFound(@NotNull HttpExchange exchange) throws IOException {
		send(exchange, HttpURLConnection.HTTP_NOT_FOUND, "text/plain", "");
	}
	
}
