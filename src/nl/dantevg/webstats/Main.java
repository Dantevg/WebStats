package nl.dantevg.webstats;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.net.ServerSocket;

public class Main extends JavaPlugin implements Runnable {
	private int port;
	
	// Gets run when the plugin is enabled on server startup
	@Override
	public void onEnable() {
		// Config
		saveDefaultConfig();
		port = getConfig().getInt("port");
		
		// Start server in a new thread, otherwise `serverSocket.accept()` will block the main thread
		Thread thread = new Thread(this, "WebStats");
		thread.start();
	}
	
	// Gets run in the new thread created on server startup
	@Override
	public void run() {
		try {
			ServerSocket serverSocket = new ServerSocket(port);
			System.out.println("Web stats server started on port " + port);
			
			// Accept new connections
			while(true){
				// Only one connection at a time possible, I don't expect heavy traffic
				Connection.start(serverSocket.accept());
			}
			
		} catch (IOException e) {
			System.err.println("Web stats server IO Exception: " + e.getMessage());
		}
	}
	
}
