package nl.dantevg.webstats.discord_webhook;

import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * https://discord.com/developers/docs/resources/channel#embed-object
 */
public class Embed {
	String title;
	String type = "rich";
	String description;
	String url;
	String timestamp; // ISO8601 timestamp
	int color;
	EmbedFooter footer;
	EmbedImage image;
	EmbedThumbnail thumbnail;
	EmbedVideo video;
	EmbedProvider provider;
	EmbedAuthor author;
	List<EmbedField> fields;
	
	public Embed() {
	}
	
	public Embed(String title, String description) {
		this.title = title;
		this.description = description;
	}
	
	public Embed(String title, String description, String url, String timestamp, int color) {
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
		boolean inline;
		
		public EmbedField(@NotNull String name, @NotNull String value) {
			this.name = name;
			this.value = value;
		}
		
		public EmbedField(@NotNull String name, @NotNull String value, boolean inline) {
			this.name = name;
			this.value = value;
			this.inline = inline;
		}
	}
	
}
