package nl.dantevg.webstats.discordwebhook;

import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * https://discord.com/developers/docs/resources/channel#embed-object
 */
public class DiscordEmbed {
	// To be used for when a message would otherwise be empty, which is not allowed
	private static final String EMPTY_FILLER = ".";
	
	String title;
	final String type = "rich";
	String description;
	String url;
	String timestamp; // ISO8601 timestamp
	Integer color;
	EmbedFooter footer;
	EmbedImage image;
	EmbedThumbnail thumbnail;
	EmbedVideo video;
	EmbedProvider provider;
	EmbedAuthor author;
	List<EmbedField> fields;
	
	public DiscordEmbed() {
	}
	
	public DiscordEmbed(String title, String description) {
		this.title = title;
		this.description = description;
	}
	
	public DiscordEmbed(String title, String description, String url, String timestamp, Integer color) {
		this.title = title;
		this.description = description;
		this.url = url;
		this.timestamp = timestamp;
		this.color = color;
	}
	
	public void addField(EmbedField field) {
		if (fields == null) fields = new ArrayList<>();
		fields.add(field);
	}
	
	public static class EmbedFooter {
		@NotNull String text;
		URL icon_url;
		URL proxy_icon_url;
		
		public EmbedFooter(@NotNull String text) {
			this.text = text;
		}
	}
	
	public static class EmbedImage {
		@NotNull URL url;
		URL proxy_url;
		int height;
		int width;
		
		public EmbedImage(@NotNull URL url) {
			this.url = url;
		}
	}
	
	public static class EmbedThumbnail {
		@NotNull URL url;
		URL proxy_url;
		int height;
		int width;
		
		public EmbedThumbnail(@NotNull URL url) {
			this.url = url;
		}
	}
	
	public static class EmbedVideo {
		URL url;
		URL proxy_url;
		int height;
		int width;
	}
	
	public static class EmbedProvider {
		String name;
		URL url;
	}
	
	public static class EmbedAuthor {
		@NotNull String name;
		URL url;
		URL icon_url;
		URL proxy_icon_url;
		
		public EmbedAuthor(@NotNull String name) {
			this.name = name;
		}
	}
	
	public static class EmbedField {
		@NotNull String name;
		@NotNull String value;
		Boolean inline;
		
		public EmbedField(@NotNull String name, @NotNull String value) {
			this.name = name;
			this.value = value;
			fillEmpty();
		}
		
		public EmbedField(@NotNull String name, @NotNull String value, boolean inline) {
			this.name = name;
			this.value = value;
			this.inline = inline;
			fillEmpty();
		}
		
		public void fillEmpty() {
			if (this.name.trim().isEmpty()) this.name = EMPTY_FILLER;
			if (this.value.trim().isEmpty()) this.value = EMPTY_FILLER;
		}
	}
	
}
