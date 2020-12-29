package nl.dantevg.webstats;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.*;
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
			
		} catch (IOException | URISyntaxException e) {
			System.err.println(e.getMessage());
		} finally {
			// Close input and output streams
			try {
				in.close();
				out.close();
				socket.close();
			} catch (Exception e) {
				System.err.println("Error closing stream: " + e.getMessage());
			}
		}
	}
	
	private static void route(URI uri, PrintWriter out){
		switch(uri.getPath()){
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
