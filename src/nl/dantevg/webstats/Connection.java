package nl.dantevg.webstats;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.*;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class Connection {
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
			System.err.println("Failed to open I/O stream: " + e.getMessage());
		} catch (URISyntaxException | NoSuchElementException e) {
			System.err.println("Failed to parse URI: " + e.getMessage());
			System.err.println("HTTP request line: " + firstLine);
			HTTP.send(out, HTTP.STATUS_BAD_REQUEST, "");
		} finally {
			// Close input and output streams in the order they were defined
			try {
				socket.close();
				in.close();
				out.close();
			} catch (NullPointerException | IOException e) {
				System.err.println("Error closing stream: " + e.getMessage());
			}
		}
	}
	
	private static void route(URI uri, PrintWriter out) throws NoSuchElementException {
		String path = uri.getPath();
		if (path == null) throw new NoSuchElementException("No path present in request URI");
		switch (path) {
			case "/stats.json":
				HTTP.send(out, HTTP.STATUS_OK, Stats.getAll().toString());
				break;
			case "/scoreboard.json":
				HTTP.send(out, HTTP.STATUS_OK, Stats.getScoreboard().toString());
				break;
			case "/online.json":
				HTTP.send(out, HTTP.STATUS_OK, Stats.getOnline().toString());
				break;
			default:
				HTTP.send(out, HTTP.STATUS_NOT_FOUND, "");
		}
	}
	
}
