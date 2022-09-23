package nl.dantevg.webstats.webserver;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import nl.dantevg.webstats.WebStats;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.logging.Level;

public class WebServer {
	private static final String KEYSTORE_TYPE = "JKS";
	private static final String MANAGER_TYPE = "PKIX";
	
	private final HttpsServer server;
	
	public WebServer()
			throws IOException, NoSuchAlgorithmException, KeyStoreException,
			CertificateException, UnrecoverableKeyException, KeyManagementException {
		WebStats.logger.log(Level.INFO, "Enabling web server");
		
		int port = WebStats.config.getInt("port");
		String keystoreFile = WebStats.config.getString("keystore");
		String keystorePassword = WebStats.config.getString("keystore-password");
		
		if (keystoreFile == null || keystorePassword == null) {
			// TODO: handle
		}
		
		server = HttpsServer.create(new InetSocketAddress(port), 0);
		
		// https://stackoverflow.com/a/2323188
		
		SSLContext sslContext = SSLContext.getInstance("TLS");
		KeyStore keyStore = WebServer.getKeyStore(keystoreFile, keystorePassword);
		sslContext.init(WebServer.getKeyManagers(keyStore, keystorePassword),
				WebServer.getTrustManagers(keyStore), null);
		server.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
			public void configure(HttpsParameters params) {
				SSLContext ctx = getSSLContext();
				params.setSSLParameters(ctx.getDefaultSSLParameters());
			}
		});
		
		server.createContext("/", new HTTPRequestHandler());
		server.start();
		WebStats.logger.log(Level.INFO, "Web server started on port " + port);
	}
	
	public void stop(int delay) {
		server.stop(delay);
	}
	
	private static KeyStore getKeyStore(String keystoreFile, String keystorePassword)
			throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
		KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
		keyStore.load(WebStats.getResourceInputStream(keystoreFile), keystorePassword.toCharArray());
		return keyStore;
	}
	
	private static KeyManager[] getKeyManagers(KeyStore keyStore, String keystorePassword)
			throws NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException {
		KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(MANAGER_TYPE);
		keyManagerFactory.init(keyStore, keystorePassword.toCharArray());
		return keyManagerFactory.getKeyManagers();
	}
	
	private static TrustManager[] getTrustManagers(KeyStore keyStore)
			throws NoSuchAlgorithmException, KeyStoreException {
		TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(MANAGER_TYPE);
		trustManagerFactory.init(keyStore);
		return trustManagerFactory.getTrustManagers();
	}
	
}
