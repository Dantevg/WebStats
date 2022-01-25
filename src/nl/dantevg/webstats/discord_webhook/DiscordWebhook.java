package nl.dantevg.webstats.discord_webhook;

import com.google.gson.Gson;
import nl.dantevg.webstats.WebStats;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class DiscordWebhook {
	private final URL url;
	
	public DiscordWebhook(URL url) {
		this.url = url;
	}
	
	public DiscordWebhook() throws MalformedURLException {
		String url = WebStats.config.getString("discord-webhook-url");
		this.url = new URL(url);
	}
	
	public void activate() {
		
	}
	
	private void send(Message message) throws IOException {
		HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
		conn.setRequestMethod("POST");
		conn.setDoOutput(true);
		conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
		
		conn.connect();
		try (OutputStream output = conn.getOutputStream()) {
			output.write(new Gson().toJson(message).getBytes());
		}
	}
	
}
