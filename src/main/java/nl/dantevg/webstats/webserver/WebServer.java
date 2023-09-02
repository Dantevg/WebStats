package nl.dantevg.webstats.webserver;

import com.sun.net.httpserver.HttpServer;
import nl.dantevg.webstats.WebStats;

import java.util.logging.Level;

public abstract class WebServer<T extends HttpServer> {
	protected int port;
	protected T server;
	
	public void start() {
		server.createContext("/", new HTTPRequestHandler());
		server.start();
		WebStats.logger.log(Level.INFO, "Web server started on port " + port);
	}
	
	public void stop(int i) {
		server.stop(i);
	}
}
