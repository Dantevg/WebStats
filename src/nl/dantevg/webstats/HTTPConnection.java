package nl.dantevg.webstats;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.*;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.logging.Level;

public class HTTPConnection {
	public static void start(Socket socket) {
		Scanner in = null;
		PrintWriter out = null;
		String firstLine = "";
		try {
			// Open input and output streams
			in = new Scanner(socket.getInputStream());
			out = new PrintWriter(socket.getOutputStream());
			
			// Route to correct end point
			firstLine = in.nextLine();
			route(HTTP.parseHeader(firstLine), out);
			
		} catch (IOException e) {
			WebStats.logger.log(Level.WARNING, "Failed to open I/O stream: " + e.getMessage(), e);
		} catch (URISyntaxException | NoSuchElementException e) {
			WebStats.logger.log(Level.FINE, "Failed to parse URI: " + e.getMessage(), e);
			WebStats.logger.log(Level.FINE, "IP: " + socket.getInetAddress().toString() + ", HTTP request line: " + firstLine);
			HTTP.send(out, HTTP.STATUS_BAD_REQUEST, "");
		} finally {
			// Close input and output streams in the order they were defined
			try {
				socket.close();
				in.close();
				out.close();
			} catch (NullPointerException | IOException e) {
				WebStats.logger.log(Level.WARNING, "Error closing stream: " + e.getMessage(), e);
			}
		}
	}
	
	private static void route(URI uri, PrintWriter out) throws NoSuchElementException {
		String path = uri.getPath();
		if (path == null) throw new NoSuchElementException("No path present in request URI");
		switch (path) {
			case "/stats.json":
				HTTP.send(out, HTTP.STATUS_OK, new Gson().toJson(Stats.getAll()));
				break;
			case "/online.json":
				HTTP.send(out, HTTP.STATUS_OK, new Gson().toJson(Stats.getOnline()));
				break;
			default:
				HTTP.send(out, HTTP.STATUS_NOT_FOUND, "");
		}
	}
	
}
