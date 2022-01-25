package nl.dantevg.webstats.discord_webhook;

import java.util.ArrayList;
import java.util.List;

public class Message {
	String content;
	String username;
	String avatar_url;
	boolean tts; // text-to-speech
	List<Embed> embeds;
	
	public Message() {
	}
	
	public Message(String content) {
		this.content = content;
	}
	
	public Message(String content, String username, String avatar_url) {
		this.content = content;
		this.username = username;
		this.avatar_url = avatar_url;
	}
	
	public void addEmbed(Embed embed) {
		if (embeds == null) embeds = new ArrayList<>();
		embeds.add(embed);
	}
	
}
