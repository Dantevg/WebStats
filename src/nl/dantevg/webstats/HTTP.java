package nl.dantevg.webstats;

import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class HTTP {
	public static void send(PrintWriter out, String code, String data){
		out.println("HTTP/1.1 " + code);
		out.println("Access-Control-Allow-Origin: *"); // ADD CORS header
		out.println();
		out.println(data);
		out.flush();
	}
	
	public static URI parseHeader(Scanner in) throws URISyntaxException, NoSuchElementException {
		in.next();
		String url = in.next();
		in.nextLine();
		return new URI(url);
	}
	
}
