package nl.dantevg.webstats;

import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.NoSuchElementException;

public class HTTP {
	public static final String STATUS_OK          = "200 OK";
	public static final String STATUS_BAD_REQUEST = "400 BAD REQUEST";
	public static final String STATUS_NOT_FOUND   = "404 NOT FOUND";
	
	public static void send(PrintWriter out, String code, String data) {
		out.print("HTTP/1.1 " + code + "\r\n");
		out.print("Access-Control-Allow-Origin: *\r\n"); // ADD CORS header
		out.print("\r\n");
		out.print(data);
		out.flush();
	}
	
	public static URI parseHeader(String firstLine) throws URISyntaxException, NoSuchElementException {
		String[] request = firstLine.split(" ");
		if (request.length < 2) {
			throw new NoSuchElementException("No request URI present in HTTP request");
		}
		return new URI(request[1]);
	}
	
}
