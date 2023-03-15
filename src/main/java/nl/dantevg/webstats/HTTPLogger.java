package nl.dantevg.webstats;

import com.sun.net.httpserver.HttpExchange;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class HTTPLogger {
	private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS'Z'").withZone(ZoneId.systemDefault());
	
	private final PrintWriter writer;
	private final Map<HttpExchange, Instant> activeExchanges = new HashMap<>();
	
	public HTTPLogger(File file) throws IOException {
		writer = new PrintWriter(new FileWriter(file, true), true);
		log("server start");
	}
	
	private void log(String str) {
		writer.println(String.format("[%s] %s", formatter.format(Instant.now()), str));
	}
	
	private void log(@NotNull HttpExchange exchange, String str) {
		if (activeExchanges.containsKey(exchange)) {
			Duration duration = Duration.between(activeExchanges.get(exchange), Instant.now());
			log(String.format("%8x @ %s | %s (%dms since connection start)",
					exchange.hashCode(),
					exchange.getRemoteAddress().getAddress(),
					str, duration.toMillis()));
		} else {
			log(String.format("%8x @ %s | %s",
					exchange.hashCode(),
					exchange.getRemoteAddress().getAddress(), str));
		}
	}
	
	public void connectionStart(@NotNull HttpExchange exchange) {
		activeExchanges.put(exchange, Instant.now());
		log(exchange, String.format("<- request: %s %s",
				exchange.getRequestMethod(), exchange.getRequestURI()));
	}
	
	public void respond(@NotNull HttpExchange exchange) {
		log(exchange, "-> respond");
		activeExchanges.remove(exchange);
	}
	
	public void respond(@NotNull HttpExchange exchange, int status) {
		log(exchange, "-> respond with status " + status);
		activeExchanges.remove(exchange);
	}
	
	public void exception(@NotNull HttpExchange exchange, Exception e) {
		log(exchange, "** exception: " + e);
		e.printStackTrace(writer);
	}
	
	public void close() {
		writer.close();
	}
}
