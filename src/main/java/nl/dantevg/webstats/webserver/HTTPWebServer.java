package nl.dantevg.webstats.webserver;

import com.sun.net.httpserver.HttpServer;
import nl.dantevg.webstats.WebStats;

import java.io.IOException;
import java.net.InetSocketAddress;

public class HTTPWebServer extends WebServer<HttpServer> {
	public HTTPWebServer() throws IOException {
		port = WebStats.config.getInt("port");
		server = HttpServer.create(new InetSocketAddress(port), 0);
	}
	
}
