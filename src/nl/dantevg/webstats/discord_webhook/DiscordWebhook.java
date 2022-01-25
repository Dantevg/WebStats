package nl.dantevg.webstats.discord_webhook;

import com.google.gson.Gson;
import nl.dantevg.webstats.WebStats;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;

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
		// TODO: remove, for testing!
		Embed embed = new Embed();
		embed.addField(new Embed.EmbedField("Player", "RedPolygon\nNotch", true));
		embed.addField(new Embed.EmbedField("Deaths", "17\n", true));
		embed.addField(new Embed.EmbedField("Mine Diamond", "64\n256", true));
		
		Message message = new Message();
		message.addEmbed(embed);
		
		try {
			send(message);
		} catch (IOException e) {
			WebStats.logger.log(Level.WARNING, "Could not send webhook message", e);
		}
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
		
		InputStream input = conn.getInputStream();
		WebStats.logger.log(Level.INFO, "Response:");
		new BufferedReader(new InputStreamReader(input)).lines()
				.forEach((line) -> WebStats.logger.log(Level.INFO, line));
		input.close();
		conn.disconnect();
	}
	
}
