package nl.dantevg.webstats.discordwebhook;

import java.util.ArrayList;
import java.util.List;

public class DiscordMessage {
	String id; // only present in responses
	String content;
	String username;
	String avatar_url;
	Boolean tts; // text-to-speech
	List<DiscordEmbed> embeds;
	
	public DiscordMessage() {
	}
	
	public DiscordMessage(String content) {
		this.content = content;
	}
	
	public DiscordMessage(String username, String avatar_url) {
		this.username = username;
		this.avatar_url = avatar_url;
	}
	
	public DiscordMessage(String content, String username, String avatar_url) {
		this.content = content;
		this.username = username;
		this.avatar_url = avatar_url;
	}
	
	public void addEmbed(DiscordEmbed embed) {
		if (embeds == null) embeds = new ArrayList<>();
		embeds.add(embed);
	}
	
	public void removeEmbeds() {
		if (embeds != null) embeds.clear();
	}
	
}
