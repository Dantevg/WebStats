package nl.dantevg.webstats.webserver;

import com.google.common.io.ByteStreams;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import nl.dantevg.webstats.WebStats;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

public class HTTPConnection {
	private final @NotNull HttpExchange exchange;
	
	public HTTPConnection(@NotNull HttpExchange exchange) {
		this.exchange = exchange;
	}
	
	private void setHeaders(String contentType) {
		Headers headers = exchange.getResponseHeaders();
		headers.add("Access-Control-Allow-Origin", "*");
		headers.add("Content-Type", contentType + "; charset=UTF-8");
		
		// Add cookies for javascript where to find the server
		// No "expires" attribute, so session cookies
		String host = exchange.getRequestHeaders().getFirst("Host");
		headers.add("Set-Cookie", "host=" + host + "; SameSite=Lax");
		
		// For pre-1.8 backwards-compatibility
		headers.add("Set-Cookie", "ip=" + host.split(":")[0]
				+ "; SameSite=Lax");
		headers.add("Set-Cookie", "port=" + exchange.getLocalAddress().getPort()
				+ "; SameSite=Lax");
	}
	
	public void send(int status, @NotNull String contentType, @NotNull String response) throws IOException {
		setHeaders(contentType);
		
		// Send headers and data
		byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
		exchange.sendResponseHeaders(status, responseBytes.length);
		OutputStream output = exchange.getResponseBody();
		output.write(responseBytes);
		output.close();
	}
	
	public void sendJson(@NotNull String response) throws IOException {
		send(HttpURLConnection.HTTP_OK, "application/json", response);
	}
	
	public void sendEmptyStatus(int status) throws IOException {
		setHeaders("text/plain");
		exchange.sendResponseHeaders(status, -1);
		exchange.getResponseBody().close();
	}
	
	public void sendFile(@NotNull String contentType, @NotNull String path) throws IOException {
		// Get input stream
		InputStream input = WebStats.getResourceInputStream(path);
		if (input == null) {
			WebStats.logger.log(Level.WARNING, "Could not find resource " + path);
			sendEmptyStatus(HttpURLConnection.HTTP_NOT_FOUND);
			return;
		}
		
		setHeaders(contentType);
		
		// Send headers and data
		exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, input.available());
		OutputStream output = exchange.getResponseBody();
		ByteStreams.copy(input, output);
		input.close();
		output.close();
	}
	
}
