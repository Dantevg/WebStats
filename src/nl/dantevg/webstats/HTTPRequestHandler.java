package nl.dantevg.webstats;

import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.util.logging.Level;

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
			case "/index.html":
			case "/":
				sendFile(exchange, "text/html", "resources/index.html");
				break;
			case "/style.css":
				sendFile(exchange, "text/css", "resources/style.css");
				break;
			case "/WebStats-dist.js":
				sendFile(exchange, "application/javascript", "resources/WebStats-dist.js");
				break;
			default:
				sendNotFound(exchange);
				break;
		}
		
		exchange.close();
	}
	
	private static void setHeaders(@NotNull HttpExchange exchange, String contentType) {
		exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
		exchange.getResponseHeaders().add("Content-Type", contentType + "; charset=UTF-8");
	}
	
	private static void send(@NotNull HttpExchange exchange, int status, @NotNull String contentType, @NotNull String response) throws IOException {
		setHeaders(exchange, contentType);
		
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
	
	private static void sendFile(@NotNull HttpExchange exchange, @NotNull String contentType, @NotNull String path) throws IOException {
		InputStream input = WebStats.getPlugin(WebStats.class).getResource(path);
		if (input == null) {
			WebStats.logger.log(Level.WARNING, "Could not find resource " + path);
			sendNotFound(exchange);
			return;
		}
		
		setHeaders(exchange, contentType);
		
		// Send headers and data
		exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, input.available());
		OutputStream output = exchange.getResponseBody();
		ByteStreams.copy(input, output);
		input.close();
		output.close();
	}
	
}
