package nl.dantevg.webstats;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.*;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class Connection {
	public static void start(Socket socket){
		Scanner in = null;
		PrintWriter out = null;
		try {
			// Open input and output streams
			in = new Scanner(socket.getInputStream());
			out = new PrintWriter(socket.getOutputStream());
			
			// Route to correct end point
			route(HTTP.parseHeader(in), out);
			
		} catch (IOException e) {
			System.err.println("Failed to open I/O stream: " + e.getMessage());
		} catch (URISyntaxException | NoSuchElementException e) {
			System.err.println("Failed to parse URI: " + e.getMessage());
			HTTP.send(out, "400 BAD REQUEST", "");
		} finally {
			// Close input and output streams
			try {
				in.close();
				out.close();
				socket.close();
			} catch (NullPointerException | IOException e) {
				System.err.println("Error closing stream: " + e.getMessage());
			}
		}
	}
	
	private static void route(URI uri, PrintWriter out) throws NoSuchElementException {
		String path = uri.getPath();
		if(path == null) throw new NoSuchElementException();
		switch(path){
			case "/stats.json":
				HTTP.send(out, "200 OK", Stats.getAll().toString());
				break;
			case "/scoreboard.json":
				HTTP.send(out, "200 OK", Stats.getScoreboard().toString());
				break;
			case "/online.json":
				HTTP.send(out, "200 OK", Stats.getOnline().toString());
				break;
			default:
				HTTP.send(out, "404 NOT FOUND", "");
		}
	}
	
}
